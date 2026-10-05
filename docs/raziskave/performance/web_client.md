# Client-side (FPS / frame time) optimization for rendering many humanoid NPCs in MC 1.12.2 Forge (CustomNPCs)

Scope note: web research plus reading the project's decompiled reference source (`dev/reference-src/noppes/...`). Nothing was built or run. Local-source citations are file paths, not URLs. Statements marked "from MCP 1.12 knowledge" are from the researcher's familiarity with vanilla 1.12.2 source and were NOT web-verified in this session. Treat them as hypotheses to confirm in the decompiled sources.

## 1. Why are many entities slow to render in 1.12.2?

### Takeaway
In 1.12.2 the per-model geometry is already cheap, because `ModelRenderer` compiles each box into a GL display list once. That holds for CustomNPCs' own `ModelScaleRenderer` and `Model2DRenderer` too. The cost of hundreds of NPCs is therefore per-entity CPU/driver overhead: matrix push/pop and transforms per part, state changes, texture binds per skin, layers (armor, held items), shadows, and nametags drawn through the immediate-mode FontRenderer. Each of these multiplies by entity count, and again by the shadow pass when shaders are on.

### Cited Findings
- Vanilla `ModelRenderer` has a private `compileDisplayList` method; `render` compiles the display list on first use and then draws it with `glCallList`. Child models are handled recursively — [Forge 1.12.2 javadoc ModelRenderer](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.12.2/net/minecraft/client/model/ModelRenderer.html); [ModelRenderer source mirror (1.8, same pattern)](https://github.com/vmarchaud/Alkazia/blob/master/1.8/Client/src/minecraft/net/minecraft/client/model/ModelRenderer.java)
- CustomNPCs' own part renderers do the same thing. `ModelScaleRenderer` has `isCompiled` and calls `GLAllocation.generateDisplayLists(1)` once, then `GlStateManager.callList(displayList)` every frame. `Model2DRenderer` uses the same pattern — local: `dev/reference-src/noppes/npcs/client/model/ModelScaleRenderer.java` (lines 29, 54–59, 88–95); `dev/reference-src/noppes/npcs/client/model/Model2DRenderer.java` (lines 31, 66–71, 97–108)
- CustomNPCs' nametag path (`RenderNPCInterface.renderLivingLabel`) does the following per NPC per frame:
  - push/translate/rotate/scale matrices, disable lighting, depthMask(false), enable blend, set the blend func;
  - draw the optional `<title>` string;
  - draw the name string twice when the viewer is within 4 blocks (once with depth disabled at reduced alpha, once normally), plus `getStringWidth` per string;
  - restore state.
  The label is skipped when `distanceSq > 512` (about 22.6 blocks) — local: `dev/reference-src/noppes/npcs/client/renderer/RenderNPCInterface.java` (lines 61–74, 84–122)
- CustomNPCs also renders chat-bubble messages (`messages.renderMessages`) in the same label path, and per-NPC shadows (`shadowSize = npc.width` / `display.getSize()/10`) — local: `RenderNPCInterface.java` lines 65–68, 76–81, 208
- Glow layer: CustomNPCs binds a second texture (`textureGlowLocation`) per NPC when glow is used — local: `RenderNPCInterface.java` line 178
- The 1.12-era FontRenderer draws each character in OpenGL immediate mode (`glBegin(GL_TRIANGLE_STRIP)` per character in `renderDefaultChar`). Third-party font renderers later moved to tessellator/buffer batching — [FML FontRenderer patch](https://github.com/MinecraftForge/FML/blob/master/patches/minecraft/net/minecraft/client/gui/FontRenderer.java.patch); [search summary of legacy renderDefaultChar](https://pastebin.com/gfcqeMnr) (1.5-era code; 1.12.2 retains the per-glyph immediate-mode approach per MCP 1.12 knowledge)
- In modern versions (1.17+), the move away from `glBegin` to buffered batch submission is the general direction for performance — [Fabric docs: basic rendering concepts](https://docs.fabricmc.net/1.20.4/develop/rendering/basic-concepts)
- ImmediatelyFast, a modern mod that only batches immediate-mode draws, reports about 3.75x FPS on entity-heavy servers and about 1.79x on text/sign-heavy scenes (Ryzen 5 1600 / GTX 1060). This is strong evidence that immediate-mode per-object submission, not GPU fill, dominates entity- and text-heavy scenes — [ImmediatelyFast GitHub](https://github.com/RaphiMC/ImmediatelyFast)
- OptiFine shader packs render a shadow pass first. The `shadowEntities` property controls whether entities are rendered into the shadow map, so with shaders, entities may be drawn twice per frame — [shaders.properties ordering docs](https://shaders.properties/current/reference/shadersproperties/ordering/); [OptiFine issue #5412 (shadow pass always executed)](https://github.com/sp614x/optifine/issues/5412)
- Complex animated models are expensive in general. Users report about 1 FPS with more than 100 GeckoLib animated blocks, and "Minecraft is not optimized to render complex entities" — [GeckolibBetterFPS (Modpack Index summary)](https://www.modpackindex.com/mod/97713/geckolibbetterfps) (newer MC versions; anecdotal)

### Inferences
- From MCP 1.12 knowledge (verify in source): `RenderLivingBase.doRender` does the following per entity:
  - interpolates rotations;
  - calls `setupRotations` and `preRenderCallback`;
  - computes limb swing;
  - calls `setBrightness`/`setDoRenderBrightness` (texture-env combiner state for the hurt/red overlay);
  - binds the entity texture;
  - calls `renderModel`, then `renderLayers` (biped armor, held item, arrows, custom head, elytra/cape on players);
  - calls `renderName` and then `doRenderShadowAndFire`.
  `Render.renderShadow` iterates the blocks under the entity and emits a Tessellator quad per block. That is CPU work that scales with shadow size, so a CustomNPCs NPC with a large `display.getSize()` gets a bigger shadow loop.
- Because geometry is already in display lists, converting models to VBOs alone is unlikely to give a large gain. The win comes from reducing per-entity calls: state changes, binds, matrix ops, label/text calls, layers, and the shadow pass.
- The armor and held-item layers likely dominate when NPCs wear armor or hold items. Held items go through `RenderItem`/baked-model rendering through the Tessellator, which is not display-listed. This is an inference; measure it.
- Each unique URL/skin texture means another `bindTexture` call per NPC. Sorting NPCs by texture before rendering would reduce binds, but vanilla `RenderGlobal` iterates entities in list order. This is an inference and needs measurement.

### Gaps
- No published per-call cost breakdown specific to 1.12 `RenderLivingBase` was found. A spark/JFR profile of the project's own benchmark is needed.
- No 1.12.2-specific source confirming the `glBegin` usage line in `FontRenderer.renderDefaultChar` was fetched this session. Confirm in the decompiled MC jar.

## 2. Entity culling and "Sodium-like" ports: do they help entities or only chunks?

### Takeaway
Two different "Entity Culling" mods exist for 1.12.2, and both skip rendering of occluded entities without touching logic:
- tr7zw's (legacy, async raytracing, unsupported now);
- meldexun's (GL 4.3 hardware occlusion queries with a raytrace fallback, OptiFine G5 compatible), with "huge" gains reported on scenes of about 200 entities.

The chunk-renderer replacements (Nothirium, Vintagium, Celeritas) help chunk rendering. Only Celeritas Extra adds entity-specific controls: an entity render distance cap, toggles, and a render profiler. All of the chunk-renderer replacements are incompatible with OptiFine.

### Cited Findings
- tr7zw Entity Culling "uses async path-tracing to hide BlockEntities/Entities that are not visible". It uses spare CPU threads, runs alongside the main thread, and skips only rendering, not logic. It has an entity whitelist, an option to treat leaves as solid, and a debug keybind to toggle it for testing. Versions 1.12.2/1.8.9/1.7.10 are "unsupported; no updates planned" — [tr7zw/EntityCulling GitHub](https://github.com/tr7zw/EntityCulling)
- tr7zw legacy 1.12.2 Forge build `1.6.3-1.12.2` on Modrinth (about 1.26M downloads); a crash issue exists on 1.12.2 Forge — [Modrinth version](https://modrinth.com/mod/entityculling/version/1.6.3-1.12.2); [Legacy_EntityCulling issue #6](https://github.com/tr7zw/Legacy_EntityCulling/issues/6)
- A 1.12.2 optimization guide notes "There is an edge case where Entity Culling can crash the game" — [MC-Optimization-Guide 1.12.2](https://github.com/Polytetrafluoroethylene-PTFE/MC-Optimization-Guide/blob/main/mods-n-stuff/1.12.2.md)
- meldexun Entity Culling (1.12.2):
  - "OpenGL occlusion culling implementation which gives you pixel-perfect bounding box culling with only one frame delay and a very small overhead" (requires GL 4.3);
  - otherwise a "raytraced culling implementation", which is more CPU-heavy;
  - tested in a Chocolate Quest Repoured dungeon with about 200 on-screen entities, with "huge improvements" (no number given);
  - requires MixinBootstrap and RenderLib;
  - versions 2.0.8+ are compatible with OptiFine G5 (older ones with F5);
  - client only.
  — [CurseForge entity-culling (meldexun)](https://www.curseforge.com/minecraft/mc-mods/entity-culling); [description page](https://www.curseforge.com/minecraft/mc-mods/entity-culling/description)
- UsefulMods list for 1.12: meldexun Entity Culling, "small client-side performance core mod which improves the rendering of entities and tile entities"; listed incompatibilities: Pixelmon, Sync — [TheUsefulLists Performance112](https://github.com/TheUsefulLists/UsefulMods/blob/main/Performance/Performance112.md)
- Nothirium "changes Minecraft's chunk rendering engine to use modern OpenGL". It is incompatible with OptiFine, VanillaFix, CensoredASM, FarPlane2, LittleTiles, Albedo, and partly ColoredLux. Vintagium is a Sodium fork for 1.12.2 and is incompatible with OptiFine — [TheUsefulLists Performance112](https://github.com/TheUsefulLists/UsefulMods/blob/main/Performance/Performance112.md)
- Celeritas is an Embeddium port for 1.12.2. It is incompatible with Nothirium/Vintagium and with OptiFine, and its gains on Forge are "significantly lower" than on Cleanroom Loader — [MC-Optimization-Guide 1.12.2](https://github.com/Polytetrafluoroethylene-PTFE/MC-Optimization-Guide/blob/main/mods-n-stuff/1.12.2.md)
- Celeritas Extra (Cleanroom; requires Celeritas 2.4.0-dev.4+, Actinium, or Nothirium) adds:
  - a configurable entity render distance limit (0 = vanilla behavior) and a tile-entity limit, with class-based exemptions;
  - toggles for armor stands, item frames, and paintings;
  - toggles for player name tags;
  - an FPS overlay with 1% low and 0.1% low;
  - entity and tile-entity render profiler sections.
  — [Celeritas-Extra GitHub](https://github.com/Sumire-Labs/Celeritas-Extra)
- Sodium (modern) explicitly optimizes immediate-mode entity rendering. `ModelPartMixin` flattens cuboids into a `ModelCuboid` array, and rendering uses allocation-free vertex writing with direct matrix transforms — [DeepWiki Sodium 0.5 entity/item optimizations (search summary)](https://deepwiki.com/cogiraxi/sodium-1.20.1-stable-0.5/7.2-entity-and-item-rendering-optimizations); [Sodium changelog](https://modrinth.com/mod/sodium/changelog?hl=en-US)
- VanillaFix: ticks only visible animated textures ("up to 3x on very large modpacks"); this is texture animation, not entities. FoamFix: memory and TPS. BetterFps: general FPS/math tweaks — [VanillaFix CurseForge](https://www.curseforge.com/minecraft/mc-mods/vanillafix); [FoamFix CurseForge](https://www.curseforge.com/minecraft/mc-mods/foamfix-optimization-mod); [search summary](https://help.akliz.net/docs/performance-mods)

### Inferences
- For CustomNPCs, occlusion culling is the single biggest "free" win in towns and dungeons where most NPCs are behind walls. Visuals are identical for visible entities, apart from possible one-frame pop-in with the GL-query approach.
- CustomNPCs could implement its own cheap culling inside its renderer, behind a switch:
  - skip the label/bubble/shadow work when the entity is outside the frustum (vanilla already frustum-culls by bounding box);
  - or integrate with meldexun's culling by keeping NPC bounding boxes correct. Large `display.getSize()` NPCs need a correctly sized render bounding box (`getRenderBoundingBox`) or they will be culled wrongly.
- Since 1.12.2 Sodium ports (Vintagium/Celeritas) do not, per the sources found, port Sodium's entity cuboid fast-path, their effect on NPC-heavy scenes is probably limited to the chunk portion of frame time. This needs verification.

### Gaps
- No numeric FPS/frame-time measurements were found for meldexun or tr7zw culling on 1.12.2.
- Not verified whether Vintagium/Celeritas include entity-rendering optimizations (the GitHub repos were not fetched successfully).
- meldexun's GitHub README could not be fetched (404 on raw README), so config options (e.g. shadow-pass culling under shaders) remain unconfirmed.

## 3. LOD for entities: reduced detail, impostors, animation throttling, nametag distance

### Takeaway
There is no established 1.12.2 mod doing true entity LOD (impostors or reduced meshes). The practical, proven levers in 1.12.2 are distance-based ones:
- per-type entity render distance (Entity Distance mod, Celeritas Extra);
- shorter nametag distance (CustomNPCs already cuts labels at about 22.6 blocks);
- skipping layers, shadows, and animation for far entities.

Impostor/billboard LOD only exists for modern versions together with Distant Horizons.

### Cited Findings
- Vanilla `Entity.isInRangeToRenderDist` compares distance against the "average edge length * 64 * renderDistanceWeight". Forge modders override this method for custom render distance. `renderDistanceWeight` above 1.0 reportedly had no effect in the era discussed — [Minecraft Forum: changing an entity's render distance](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/modification-development/2246866-changing-an-entities-render-distance); [Forge forums 1.16.5 thread](https://forums.minecraftforge.net/topic/100575-1165-extending-an-entities-render-distance/)
- The "Entity Distance" mod (1.12.2 Forge, MIT, updated May 2024):
  - per-entity-type rendering distance sliders (client) and tracking distance (server);
  - blacklist/whitelist;
  - the effective distance is the lower of the two;
  - pitched for "potato computers".
  — [CurseForge Entity Distance 1.12.2](https://www.curseforge.com/minecraft/mc-mods/entity-distance-1-12-2)
- Celeritas Extra: entity render distance limit, class-based exemptions, and an item-frame LOD option — [Celeritas-Extra GitHub](https://github.com/Sumire-Labs/Celeritas-Extra)
- LOD Entity Rendering (modern) renders entities inside Distant Horizons LODs; there is no 1.12 equivalent — [lodentityrendering GitHub](https://github.com/steves-underwater-paradise/lodentityrendering)
- GeckolibBetterFPS (modern GeckoLib): about 25% faster GeckoLib entity rendering through math/logic optimizations in the render path, reducing redundant calculations — [Modpack Index](https://www.modpackindex.com/mod/97713/geckolibbetterfps); [CurseForge files](https://www.curseforge.com/minecraft/mc-mods/geckolibbetterfps/files/all)
- OptiFine "Smart Animations" stops animating textures you cannot see; that is texture animation, not entity models — [search summary of OptiFine settings](https://www.howtogeek.com/202653/how-to-optimize-optifine-for-a-smooth-minecraft-experience/)
- CustomNPCs' label cutoff is hard-coded as `distanceSq > 512`, and the double-drawn name is limited to 4 blocks — local: `RenderNPCInterface.java` lines 61–64, 110–114

### Inferences
- Viable switchable LOD tiers for a mod that owns its renderer, none of which change the default visuals:
  - (a) Beyond distance D1, skip layers (armor, held item, glow) and the shadow.
  - (b) Beyond D2, freeze limb swing / head rotation interpolation, or update the animation every N frames.
  - (c) Beyond D3, render a simplified mesh (body cuboids only, with no overlay "second layer" boxes such as jacket/sleeves/hat). This halves box count on 64x64 skins.
  - (d) Optionally render a camera-facing impostor from a cached render-to-texture. This is the hardest option, and lighting/skin changes need invalidation.
- Shorter label distance and merging title+name into one pass are cheap. Changing the distance changes visuals, so it must be behind a switch. Merging into one pass may not change visuals if done carefully.

### Gaps
- No published measurements found for entity LOD in Minecraft (any version) beyond the GeckolibBetterFPS ~25% figure.
- "Not Enough Animations", "Entity Model Features", MCA and Millénaire NPC renderer costs: no specific performance data found this session.

## 4. Batching / instancing on 1.12 OpenGL (LWJGL 2, GL 2.1 baseline)

### Takeaway
On a GL 2.1 baseline, the realistic techniques are:
- keep display lists per part (already done);
- reduce per-entity state changes;
- batch label text into one Tessellator draw.

Hardware instancing (`ARB_draw_instanced`) is spec'd against GL 2.1 but needs GL 3.0 or EXT_gpu_shader4 and shaders. It would require a custom shader path that conflicts with OptiFine shaders, so it is not a default-safe option. Modern evidence (ImmediatelyFast, Sodium) shows batching immediate-mode submissions yields 1.5–3.75x in entity/text-heavy scenes.

### Cited Findings
- `ARB_draw_instanced`:
  - requires OpenGL 2.0 plus (EXT_gpu_shader4 or NV_vertex_program4 or OpenGL 3.0); the spec is written against GL 2.1;
  - provides `glDrawArraysInstancedARB`/`glDrawElementsInstancedARB` and `gl_InstanceIDARB` to "accelerate such use cases while restricting the number of API calls".
  — [Khronos ARB_draw_instanced spec](https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_instanced.txt)
- ImmediatelyFast's "custom buffer implementation which batches draw calls and uploads data to the GPU in a more efficient way" reports these gains:
  - entities about 3.75x, text about 1.79x, HUD about 1.52x, maps about 6.2x (via texture atlasing);
  - incompatible with OptiFine.
  — [ImmediatelyFast GitHub](https://github.com/RaphiMC/ImmediatelyFast)
- 1.12 chunks use one GL render list per 16³ section; entities are rendered from `EntityRenderer.renderWorld` → RenderGlobal — [greyminecraftcoder: rendering the world](http://greyminecraftcoder.blogspot.com/2013/07/rendering-world-more-details-including.html)
- Display list usage pattern (`glCallList`) for prebuilt geometry in Forge modding — [Jabelar GL11 tips](http://jabelarminecraft.blogspot.com/p/minecraft-forge-172-quick-tips-gl11-and.html)

### Inferences
- Display lists in GL 2.1 compatibility profiles are generally well optimized by NVIDIA/AMD drivers. Replacing them with VBOs on 1.12 is unlikely to give a measurable gain on its own, and OptiFine shaders already intercept the fixed-function path. (This is general GL knowledge, not sourced here.)
- Skin texture atlasing (packing many 64x64 NPC skins into one atlas) would remove per-NPC `bindTexture` calls, but it requires rewriting UVs per NPC. Display lists bake UVs, so it would need one list per atlas slot or a texture-matrix offset (`glMatrixMode(GL_TEXTURE)` translate/scale). The texture-matrix approach keeps display lists reusable and stays fixed-function, so it is OptiFine-friendly in principle. Mipmapping/bleeding at atlas borders could change visuals slightly; 64x64 skins use nearest filtering, so the risk is low but must be tested.
- Sorting the CustomNPCs render order by texture would need control over RenderGlobal's entity loop, for example a deferred-render queue: collect NPC draws in `doRender` and flush them in `RenderWorldLastEvent`. That breaks the interleaving with translucent passes and OptiFine shaders' gbuffers programs, which is high risk.
- The safest batching target is nametags. Collect all label strings in a frame and emit them with one Tessellator `BufferBuilder` pass using the font texture. This avoids per-glyph immediate mode and repeated state setup.

### Gaps
- No 1.12-specific instancing or VBO experiment for entities with published numbers was found.
- No source found on how OptiFine shaders handle custom instanced or shader-based entity draws in 1.12. Assume incompatibility unless tested.

## 5. Nametags / health bars

### Takeaway
Text is a known hot spot. In the modern ImmediatelyFast mod, batching and caching text gave about 1.79x on text-heavy scenes. CustomNPCs' label path draws up to three strings per NPC (title, plus the name twice near the player), each with full state setup, so labels are a prime target. Any fix can keep pixel-identical output when done as batching rather than removal.

### Cited Findings
- ImmediatelyFast: text rendering (signs, holograms, nametags) with "character caching"; about 1.79x FPS on text/sign rendering — [ImmediatelyFast GitHub](https://github.com/RaphiMC/ImmediatelyFast)
- Celeritas Extra exposes "Toggle player name tags" and item-frame name tags as performance options — [Celeritas-Extra GitHub](https://github.com/Sumire-Labs/Celeritas-Extra)
- Per-character immediate-mode glyph drawing in legacy FontRenderer — [FML FontRenderer patch](https://github.com/MinecraftForge/FML/blob/master/patches/minecraft/net/minecraft/client/gui/FontRenderer.java.patch)
- CustomNPCs label details: title drawn first; name drawn with depth off at about 1/3 alpha within 4 blocks, then normally; `getStringWidth` computed per draw; faction color; cutoff at 22.6 blocks — local: `RenderNPCInterface.java` lines 84–122

### Inferences
- Cheap, visual-identical improvements:
  - cache `getStringWidth` results per NPC name/title (invalidate on change);
  - skip label work when the label is off-screen;
  - pre-build a display list per (name, color) string and reuse it across frames. Text glyphs with fixed color can be compiled into a list; invalidate on name/title/faction-color change.
  Display-list text caching was a common legacy-era technique; this is an inference, not sourced here.
- Visual-changing options (switch only): a lower label distance, hiding titles beyond X, no see-through name pass.

### Gaps
- No measured per-nametag cost on 1.12.2 was found.
- No 1.12 Forge mod that batches nametags (an ImmediatelyFast equivalent) was found.

## 6. Client-side animation cost and pose caching

### Takeaway
Little published data exists for 1.12. Modern evidence (GeckolibBetterFPS about 25%, Sodium's allocation-free model path) indicates that per-frame math/allocation in model setup is a measurable cost. For CustomNPCs, the per-frame `setRotationAngles` work, plus any CustomNPCs animation and part-scale logic, scales linearly with visible NPCs.

### Cited Findings
- GeckolibBetterFPS: about 25% faster GeckoLib entity rendering and fewer micro-stutters through "mathematical and logic optimizations", reducing redundant calculations in the render path — [Modpack Index](https://www.modpackindex.com/mod/97713/geckolibbetterfps)
- Sodium avoids redundant allocations and uses specialized vertex writing for entity models — [DeepWiki Sodium summary](https://deepwiki.com/cogiraxi/sodium-1.20.1-stable-0.5/7.2-entity-and-item-rendering-optimizations)
- Roblox-domain analogy (not Minecraft): throttling NPC updates into batches per frame took 1000 NPCs from about 30 to about 60 FPS — [Roblox devforum NPC performance tips](https://devforum.roblox.com/t/performance-tips-network-bandwidth-client-framerate-etc-for-custom-npc-replication/4202236) (cross-engine; weak evidence)

### Inferences
- Pose caching options:
  - (a) Compute the pose once per client tick for far NPCs and reuse it across frames, interpolating only near NPCs. This changes visuals (choppier motion at a distance), so it must be behind a switch.
  - (b) Avoid per-frame allocations (new float arrays/vectors, string concatenation such as `"<" + title + ">"` in the label path) to cut GC pressure. This is visual-identical. Allocation hot spots can be found with spark `--alloc`.

### Gaps
- No CustomNPCs-specific or 1.12 animation-cost measurements found.

## 7. Measuring client performance properly

### Takeaway
Use frame-time percentiles (1% / 0.1% lows), not average FPS. Combine:
- a sampling profiler on the render thread (spark `/sparkc` with async-profiler, or JFR);
- a GPU frame debugger (RenderDoc/Nsight, attached through the launcher's child process) to count draw calls and state changes.

Use a fixed camera and a fixed NPC count for repeatability.

### Cited Findings
- spark is available for Forge 1.12.2 (v1.5.2 added async-profiler support), and "Spark Unforged" is a 1.12.2 backport — [CurseForge spark 1.12.2 file](https://www.curseforge.com/minecraft/mc-mods/spark/files/3245793); [Modrinth Spark Unforged](https://modrinth.com/mod/spark-unforged/version/1.11.2)
- spark client options:
  - `/sparkc` on Forge/Fabric clients;
  - `--thread <name>`, `--thread *`, `--regex`;
  - `--interval <ms>` (default 4 ms);
  - `--only-ticks-over <ms>`;
  - `--force-java-sampler`;
  - `--alloc` for allocation profiling;
  - `--ignore-sleeping`, `--combine-all`, `--comment`.
  — [spark Command Usage docs](https://spark.lucko.me/docs/Command-Usage)
- Celeritas Extra FPS overlay shows 1% low and 0.1% low, plus entity and tile-entity render profiler sections (Cleanroom/Celeritas only) — [Celeritas-Extra GitHub](https://github.com/Sumire-Labs/Celeritas-Extra)
- OptiFine Lagometer: an on-screen meter of frame/GPU/chunk load, with some overhead of its own — [How-To Geek OptiFine guide](https://www.howtogeek.com/202653/how-to-optimize-optifine-for-a-smooth-minecraft-experience/)
- RenderDoc: attach to the launcher with "capture child processes", then press F12 to capture. The Event Browser lists every draw call, and Pipeline State shows state. Nsight Graphics Frame Debugger uses F11 and has a GPU Trace Profiler — [shaderLABS wiki: Attaching Graphics Debuggers](https://shaderlabs.org/wiki/Attaching_Graphics_Debuggers_to_Minecraft)
- tr7zw Entity Culling has a debug key to toggle culling on the fly for A/B testing. With shaders, F3 counts can look inflated because of shadow passes — [tr7zw/EntityCulling GitHub](https://github.com/tr7zw/EntityCulling)
- Anecdote: one user saw 25 FPS with CustomNPCs alone, 7–20 FPS combined with other mods, and over 50 FPS after removing OptiFine+CustomNPCs. There are no controls, so it is weak evidence — [Minecraft Forum "Terrible fps"](https://www.minecraftforum.net/forums/support/java-edition-support/2352761-terrible-fps)

### Inferences
- Repeatable client benchmark recipe:
  - fixed world seed/backup with N NPCs spawned at fixed positions (e.g. 50/100/200/400), with AI disabled or fixed so poses are deterministic;
  - teleport to a fixed position, set yaw/pitch, and disable view bobbing and particles;
  - fixed render distance, GUI scale, and resolution, with VSync off and an unlimited frame cap;
  - warm up for about 30 s, then record frame times for 60 s by logging `System.nanoTime()` deltas in a `RenderTickEvent` handler to CSV, and report the median, p95, p99, 1% low, and 0.1% low;
  - repeat 3x and run with/without each switch.
- Run spark `/sparkc profiler --thread "Client thread"` (the 1.12 render thread is the "Client thread"; verify the thread name) during the window to attribute time. Use `--alloc` separately.
- Capture one RenderDoc frame to count draw calls per NPC before and after changes. Note that RenderDoc may have limited support for legacy compatibility-profile features such as display lists; this was not verified.

### Gaps
- Not confirmed whether RenderDoc fully supports GL compatibility-profile display lists in 1.12 captures (the shaderLABS page does not say).
- No ready-made 1.12.2 client benchmark mod with frame-time percentile export was found (other than Celeritas Extra's overlay, which needs Cleanroom).

## 8. Known CustomNPCs client-lag reports and fixes in forks

### Takeaway
Public reports of CustomNPCs lag are mostly anecdotal and often server-side (TPS). Forks have done targeted cache work: CustomNPC+ 1.8.5 added a client image cache for URL textures. No fork was found that rewrote NPC rendering for many-entity FPS.

### Cited Findings
- CustomNPC-Plus issue #92:
  - about 100 NPCs in 2 groups, aggro range 64;
  - lag/TPS drop when the player is in survival or flying over them;
  - closed as "enhancement" with no root cause posted;
  - performance was said to be better on 1.16.5.
  — [KAMKEEL/CustomNPC-Plus #92](https://github.com/KAMKEEL/CustomNPC-Plus/issues/92)
- CustomNPC+ 1.8.5 introduced a "Client Image Cache for Particles, NPC URL Textures, URL Items, etc." for performance. CustomNPC+ supports URL64 full-size skins — [CustomNPC-Plus GitHub/releases (search summary)](https://github.com/KAMKEEL/CustomNPC-Plus/releases); [npc-and-tools.md](https://github.com/KAMKEEL/CustomNPC-Plus/blob/master/update/npc-and-tools.md)
- BetaZavr CustomNPCs Unofficial 1.12.2 (v4.431, Aug 2025) is an actively maintained fork — [GitHub BetaZavr/CustomNPCs_1.12.2-Unofficial](https://github.com/BetaZavr/CustomNPCs_1.12.2-Unofficial); [CurseForge](https://www.curseforge.com/minecraft/mc-mods/customnpcs-unofficial-from-betazavr)
- CustomNPCs downloads URL skins via `ImageDownloadAlt` (a `ThreadDownloadImageData`-style async download) with a default-skin fallback — local: `RenderNPCInterface.java` line 269
- Analogous skin-mod report: Quick Skin Mod, with FPS falling from 400 to 20–30 with about 30 skinned players visible. Candidate causes are allocation per lookup or texture handling, but it is undiagnosed; the maintainers say "Profile ... before choosing a change" — [Quick-Skin-Mod #2053](https://github.com/The-Plum-Team/Quick-Skin-Mod/issues/2053)
- Anecdote of CustomNPCs plus OptiFine causing low FPS (no data) — [Minecraft Forum "Terrible fps"](https://www.minecraftforum.net/forums/support/java-edition-support/2352761-terrible-fps)

### Inferences
- Check whether CustomNPCs resolves the skin `ResourceLocation` (and any `TextureCache` lookups) per frame per NPC rather than caching it on the entity. Per-frame map lookups or string building for URL textures would be a CPU hot spot at hundreds of NPCs. See `dev/reference-src/noppes/npcs/client/TextureCache.java` and the `getEntityTexture` path.
- BetaZavr's fork changelogs were not reviewed for rendering performance changes. A targeted code diff of their renderer against the original could reveal fixes.

### Gaps
- No GitHub/Reddit/CurseForge report with numbers for CustomNPCs 1.12.2 client FPS versus NPC count was found.
- No evidence was found of render-side performance fixes in BetaZavr's fork (not searched in depth).
- Reddit r/feedthebeast threads were not reached in this session.
