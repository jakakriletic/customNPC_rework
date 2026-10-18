# Povzetek JFR: k-idle-50-2026-10-18-222334.jfr

Nit: `Client thread`. Trajanje posnetka: 14 s.
Vzorcev CPU niti: 38 (vseh niti 1357). Alokacij niti (ocena iz TLAB vzorcev): 388.5 MB = 27.8 MB/s (vse niti 528.8 MB, dogodkov niti 54).

Vzorci vkljucujejo jdk.NativeMethodSample z utezjo 2: nativnih (JNI) 34 = 89.5 % utezenih vzorcev niti.

### CPU: lastni cas po metodi (vrh sklada)

| metoda | vzorcev | % |
|---|---:|---:|
| `org.lwjgl.opengl.WindowsContextImplementation.nSwapBuffers` | 28 | 73.7 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 4 | 10.5 |
| `org.lwjgl.opengl.GL11.nglEnd` | 2 | 5.3 |
| `java.util.concurrent.ConcurrentLinkedQueue.isEmpty` | 1 | 2.6 |
| `it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapIterator.nextEntry` | 1 | 2.6 |
| `java.util.HashMap.getNode` | 1 | 2.6 |
| `net.minecraft.world.chunk.Chunk.onTick` | 1 | 2.6 |

### CPU: lastni cas po vrstici

| mesto | vzorcev | % |
|---|---:|---:|
| `org.lwjgl.opengl.WindowsContextImplementation.nSwapBuffers` | 28 | 73.7 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 4 | 10.5 |
| `org.lwjgl.opengl.GL11.nglEnd` | 2 | 5.3 |
| `java.util.concurrent.ConcurrentLinkedQueue.isEmpty:429` | 1 | 2.6 |
| `it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapIterator.nextEntry:633` | 1 | 2.6 |
| `java.util.HashMap.getNode:573` | 1 | 2.6 |
| `net.minecraft.world.chunk.Chunk.onTick:1149` | 1 | 2.6 |

### CPU: vkljucni cas (noppes.* in net.minecraft*)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraft.client.Minecraft.runGameLoop` | 38 | 100.0 |
| `net.minecraft.client.Minecraft.run` | 38 | 100.0 |
| `net.minecraft.client.main.Main.main` | 38 | 100.0 |
| `net.minecraft.client.Minecraft.updateDisplay` | 28 | 73.7 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntity` | 6 | 15.8 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntityStatic` | 6 | 15.8 |
| `net.minecraft.client.renderer.RenderGlobal.renderEntities` | 6 | 15.8 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorldPass` | 6 | 15.8 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorld` | 6 | 15.8 |
| `net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender` | 6 | 15.8 |
| `net.minecraft.client.multiplayer.WorldClient.tick` | 4 | 10.5 |
| `net.minecraft.client.Minecraft.runTick` | 4 | 10.5 |
| `net.minecraft.client.renderer.GlStateManager.glDrawArrays` | 4 | 10.5 |
| `net.minecraft.client.renderer.WorldVertexBufferUploader.draw` | 4 | 10.5 |
| `net.minecraft.client.renderer.Tessellator.draw` | 4 | 10.5 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.doRender` | 4 | 10.5 |
| `net.minecraft.client.renderer.entity.RenderLiving.doRender` | 4 | 10.5 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRender` | 4 | 10.5 |
| `noppes.npcs.client.renderer.RenderCustomNpc.doRender` | 4 | 10.5 |
| `net.minecraft.client.multiplayer.ChunkProviderClient.tick` | 3 | 7.9 |
| `net.minecraft.world.chunk.Chunk.onTick` | 2 | 5.3 |
| `net.minecraft.client.renderer.entity.Render.renderShadow` | 2 | 5.3 |
| `net.minecraft.client.renderer.entity.Render.doRenderShadowAndFire` | 2 | 5.3 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRenderShadowAndFire` | 2 | 5.3 |
| `noppes.npcs.client.layer.LayerEyes.render` | 2 | 5.3 |
| `noppes.npcs.client.layer.LayerInterface.doRenderLayer` | 2 | 5.3 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderLayers` | 2 | 5.3 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderLayers` | 2 | 5.3 |
| `net.minecraft.client.renderer.GlStateManager.glEnd` | 2 | 5.3 |
| `net.minecraft.client.gui.FontRenderer.renderDefaultChar` | 2 | 5.3 |
| `net.minecraft.client.gui.FontRenderer.renderChar` | 2 | 5.3 |
| `net.minecraft.client.gui.FontRenderer.renderStringAtPos` | 2 | 5.3 |
| `net.minecraft.client.gui.FontRenderer.renderString` | 2 | 5.3 |
| `net.minecraft.client.gui.FontRenderer.drawString` | 2 | 5.3 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderLivingLabel` | 2 | 5.3 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderName` | 2 | 5.3 |
| `net.minecraft.client.renderer.entity.Render.doRender` | 2 | 5.3 |
| `net.minecraft.client.multiplayer.WorldClient.updateBlocks` | 1 | 2.6 |

### CPU: po prvem noppes okvirju od vrha sklada

| metoda | vzorcev | % |
|---|---:|---:|
| `(brez noppes okvirja)` | 32 | 84.2 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRenderShadowAndFire` | 2 | 5.3 |
| `noppes.npcs.client.layer.LayerEyes.render` | 2 | 5.3 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderLivingLabel` | 2 | 5.3 |

### Alokacije: po razredu objekta

| razred | MB | % |
|---|---:|---:|
| `net.minecraft.util.math.BlockPos$MutableBlockPos` | 131.1 | 33.7 |
| `net.minecraft.util.math.BlockPos` | 117.2 | 30.2 |
| `net.minecraft.util.math.BlockPos$2` | 37.0 | 9.5 |
| `net.minecraftforge.client.event.RenderLivingEvent$Post` | 22.2 | 5.7 |
| `char[]` | 22.2 | 5.7 |
| `net.minecraftforge.client.event.RenderLivingEvent$Pre` | 14.8 | 3.8 |
| `java.util.HashMap$Node` | 7.4 | 1.9 |
| `net.minecraftforge.client.event.RenderGameOverlayEvent$Post` | 7.4 | 1.9 |
| `float[]` | 7.4 | 1.9 |
| `int[]` | 7.4 | 1.9 |
| `net.minecraftforge.client.event.RenderGameOverlayEvent$Pre` | 7.4 | 1.9 |
| `java.util.ArrayList$Itr` | 7.0 | 1.8 |

### Alokacije: po prvem ne-JDK okvirju

| mesto | MB | % |
|---|---:|---:|
| `net.minecraft.util.math.BlockPos.offset:197` | 109.8 | 28.3 |
| `net.minecraft.entity.Entity.getBrightness:1511` | 101.5 | 26.1 |
| `net.minecraft.util.math.BlockPos.getAllInBoxMutable:387` | 37.0 | 9.5 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.doRender:211` | 22.2 | 5.7 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderLivingLabel:91` | 22.2 | 5.7 |
| `net.minecraft.entity.Entity.getBrightnessForRender:1493` | 14.8 | 3.8 |
| `net.minecraft.util.math.BlockPos$2$1.computeNext:398` | 14.8 | 3.8 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.doRender:86` | 14.8 | 3.8 |
| `net.minecraft.client.multiplayer.WorldClient.refreshVisibleChunks:152` | 7.4 | 1.9 |
| `net.minecraft.world.World.rayTraceBlocks:1040` | 7.4 | 1.9 |
| `net.minecraftforge.client.GuiIngameForge.post:914` | 7.4 | 1.9 |
| `paulscode.sound.CommandObject.<init>:483` | 7.4 | 1.9 |
| `net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes:124` | 7.4 | 1.9 |
| `net.minecraftforge.client.GuiIngameForge.pre:910` | 7.4 | 1.9 |
| `net.minecraft.client.renderer.RenderGlobal.renderBlockLayer:1190` | 7.0 | 1.8 |

### Alokacije: vkljucno (noppes.* in net.minecraft*)

| metoda | MB | % |
|---|---:|---:|
| `net.minecraft.client.Minecraft.runGameLoop` | 388.5 | 100.0 |
| `net.minecraft.client.Minecraft.run` | 388.5 | 100.0 |
| `net.minecraft.client.main.Main.main` | 388.5 | 100.0 |
| `net.minecraft.launchwrapper.Launch.launch` | 388.5 | 100.0 |
| `net.minecraft.launchwrapper.Launch.main` | 388.5 | 100.0 |
| `net.minecraftforge.gradle.GradleStartCommon.launch` | 388.5 | 100.0 |
| `net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender` | 373.7 | 96.2 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorld` | 358.9 | 92.4 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorldPass` | 351.5 | 90.5 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntity` | 344.5 | 88.7 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntityStatic` | 344.5 | 88.7 |
| `net.minecraft.client.renderer.RenderGlobal.renderEntities` | 344.5 | 88.7 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.doRender` | 182.9 | 47.1 |
| `net.minecraft.client.renderer.entity.RenderLiving.doRender` | 182.9 | 47.1 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRender` | 182.9 | 47.1 |
| `noppes.npcs.client.renderer.RenderCustomNpc.doRender` | 182.9 | 47.1 |
| `net.minecraft.client.renderer.entity.Render.renderShadow` | 161.6 | 41.6 |
| `net.minecraft.client.renderer.entity.Render.doRenderShadowAndFire` | 161.6 | 41.6 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRenderShadowAndFire` | 161.6 | 41.6 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderLayers` | 116.3 | 29.9 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderLayers` | 116.3 | 29.9 |
| `net.minecraft.util.math.BlockPos.offset` | 109.8 | 28.3 |
| `net.minecraft.util.math.BlockPos$MutableBlockPos.offset` | 109.8 | 28.3 |
| `net.minecraft.util.math.BlockPos.down` | 109.8 | 28.3 |
| `net.minecraft.entity.Entity.getBrightness` | 101.5 | 26.1 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.setBrightness` | 101.5 | 26.1 |
| `net.minecraft.util.math.BlockPos.getAllInBoxMutable` | 37.0 | 9.5 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderLivingLabel` | 22.2 | 5.7 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderName` | 22.2 | 5.7 |
| `net.minecraft.client.renderer.entity.Render.doRender` | 22.2 | 5.7 |
| `net.minecraft.entity.Entity.getBrightnessForRender` | 14.8 | 3.8 |
| `noppes.npcs.client.layer.LayerEyes.render` | 14.8 | 3.8 |
| `noppes.npcs.client.layer.LayerInterface.doRenderLayer` | 14.8 | 3.8 |
| `net.minecraft.util.math.BlockPos$2$1.computeNext` | 14.8 | 3.8 |
| `net.minecraftforge.client.GuiIngameForge.renderGameOverlay` | 14.8 | 3.8 |
| `net.minecraft.client.multiplayer.WorldClient.refreshVisibleChunks` | 7.4 | 1.9 |
| `net.minecraft.client.multiplayer.WorldClient.updateBlocks` | 7.4 | 1.9 |
| `net.minecraft.client.multiplayer.WorldClient.tick` | 7.4 | 1.9 |
| `net.minecraft.client.Minecraft.runTick` | 7.4 | 1.9 |
| `net.minecraft.world.World.rayTraceBlocks` | 7.4 | 1.9 |
| `net.minecraft.entity.Entity.rayTrace` | 7.4 | 1.9 |
| `net.minecraft.client.renderer.EntityRenderer.getMouseOver` | 7.4 | 1.9 |
| `net.minecraftforge.client.GuiIngameForge.post` | 7.4 | 1.9 |
| `net.minecraftforge.client.GuiIngameForge.renderHUDText` | 7.4 | 1.9 |
| `net.minecraft.client.audio.SoundManager.setListener` | 7.4 | 1.9 |
| `net.minecraft.client.audio.SoundHandler.setListener` | 7.4 | 1.9 |
| `net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes` | 7.4 | 1.9 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.applyRotations` | 7.4 | 1.9 |
| `noppes.npcs.client.renderer.RenderNPCInterface.applyRotations` | 7.4 | 1.9 |
| `net.minecraftforge.client.GuiIngameForge.pre` | 7.4 | 1.9 |
| `net.minecraftforge.client.GuiIngameForge.renderExperience` | 7.4 | 1.9 |
| `net.minecraft.client.renderer.RenderGlobal.renderBlockLayer` | 7.0 | 1.8 |

### Alokacije: po prvem noppes okvirju od vrha sklada

| metoda | MB | % |
|---|---:|---:|
| `noppes.npcs.client.renderer.RenderNPCInterface.doRenderShadowAndFire` | 161.6 | 41.6 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderLayers` | 101.5 | 26.1 |
| `(brez noppes okvirja)` | 44.0 | 11.3 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRender` | 37.0 | 9.5 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderLivingLabel` | 22.2 | 5.7 |
| `noppes.npcs.client.layer.LayerEyes.render` | 14.8 | 3.8 |
| `noppes.npcs.client.renderer.RenderNPCInterface.applyRotations` | 7.4 | 1.9 |

### Druge niti (vzorcev CPU)

| nit | vzorcev |
|---|---:|
| `Netty Server IO #1` | 444 |
| `AWT-Windows` | 444 |
| `Netty Server IO #0` | 414 |
| `Server thread` | 13 |
| `Thread-9` | 4 |
