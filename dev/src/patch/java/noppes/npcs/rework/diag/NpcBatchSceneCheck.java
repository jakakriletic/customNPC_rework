package noppes.npcs.rework.diag;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.ModelPlayerAlt;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.rework.far.FarLook;
import noppes.npcs.rework.far.ImpostorBaker;
import noppes.npcs.rework.rig.RigData;
import noppes.npcs.rework.rig.client.RigClient;
import noppes.npcs.rework.render.NpcBatch;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/** M14.3b1: whole living-renderer proof, explicitly requested, never a timing window.
 * Real loaded client NPC; fixtures change its client copy for one synchronous render callback.
 * Pose values are frozen inputs, not claims that navigation/combat AI was exercised.
 * Uses the production renderer, texture binding, layers, name and Forge Pre/Post, with the
 * same original/candidate/original FBO and Q475 comparator as the earlier native proof.
 * The separate contexts job exercises M16, CNPC's GUI preview and the baker's render stage.
 * Optional mods, URL transfers, the full atlas pipeline and L2 remain separate live scenarios. */
@SideOnly(Side.CLIENT)
public final class NpcBatchSceneCheck {
    private static final String[] KEYS = {"ordinary-name", "walk", "attack", "bow-pose", "sneak",
        "skin-alex", "tint", "global-scale", "part-scale", "dark-lightmap", "block-lightmap",
        "fog-linear", "fog-exp", "fog-exp2", "hurt", "death", "overlay", "armor", "armor-glint",
        "held-item", "riding", "morph-zombie", "outline", "cancel-pre", "ordinary-after-specials"};
    private static final String[] CONTEXT_KEYS = {"ordinary-name", "rig-idle", "rig-walk", "rig-hurt", "rig-death",
        "gui-ordinary", "gui-rig", "bake-0", "bake-45", "bake-90", "bake-135", "bake-180", "bake-225",
        "bake-270", "bake-315", "ordinary-after-specials"};
    private final Minecraft mc;
    private final Framebuffer fb;
    private final EntityCustomNpc npc;
    private final RenderCustomNpc renderer;
    private final JsonArray checks = new JsonArray(), rows = new JsonArray();
    private final String images;
    private final boolean contexts;
    private EntityNPCInterface target;
    private final NBTTagCompound display, model;
    private final StringBuilder trace = new StringBuilder();
    private boolean observing, cancel, ok = true;
    private String key;

    private NpcBatchSceneCheck(Minecraft mc, Framebuffer fb, EntityCustomNpc npc, String images, boolean contexts) {
        this.mc = mc; this.fb = fb; this.npc = npc; this.images = images;
        this.contexts = contexts; target = npc;
        renderer = (RenderCustomNpc) mc.getRenderManager().getEntityRenderObject(npc);
        display = npc.display.writeToNBT(new NBTTagCompound()); model = npc.modelData.writeToNBT();
    }

    static JsonObject run(Minecraft mc, Framebuffer fb, EntityCustomNpc npc, String images) throws Exception {
        return new NpcBatchSceneCheck(mc, fb, npc, images, false).run();
    }

    static JsonObject runContexts(Minecraft mc, Framebuffer fb, EntityCustomNpc npc, String images) throws Exception {
        return new NpcBatchSceneCheck(mc, fb, npc, images, true).run();
    }

    private void check(String name, boolean pass, String detail) {
        JsonObject c = new JsonObject(); c.addProperty("key", name); c.addProperty("ok", pass); c.addProperty("detail", detail);
        checks.add(c); ok &= pass;
        System.out.println("M143B CHECK " + name + " ok=" + pass + " " + detail);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void pre(RenderLivingEvent.Pre e) {
        if (observing && e.getEntity() == target) { trace.append("pre,"); if (cancel) { e.setCanceled(true); } }
    }

    @SubscribeEvent public void post(RenderLivingEvent.Post e) {
        if (observing && e.getEntity() == target) { trace.append("post,"); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private JsonObject run() throws Exception {
        NBTTagCompound saved = new NBTTagCompound(); npc.writeEntityToNBT(saved);
        int savedAnimation = npc.currentAnimation, savedTicks = npc.ticksExisted;
        int savedCore = CustomNpcs.RwAnimCore, savedBatch = CustomNpcs.RwNpcInstancing;
        RigData savedRig = npc.rwRig;
        long savedBlink = npc.modelData.eyes.blinkStart;
        boolean savedUpdate = npc.updateClient;
        net.minecraft.util.ResourceLocation savedTexture = npc.textureLocation, savedGlow = npc.textureGlowLocation;
        float sx = npc.scaleX, sy = npc.scaleY, sz = npc.scaleZ;
        float limb = npc.limbSwing, amount = npc.limbSwingAmount, prevAmount = npc.prevLimbSwingAmount;
        float swing = npc.swingProgress, prevSwing = npc.prevSwingProgress;
        int hurt = npc.hurtTime, death = npc.deathTime, maxHurt = npc.maxHurtTime;
        boolean savedShadow = mc.getRenderManager().isRenderShadow();
        boolean savedHideGui = mc.gameSettings.hideGUI;
        float savedViewX = mc.getRenderManager().playerViewX, savedViewY = mc.getRenderManager().playerViewY;
        boolean savedOutline = ReflectionHelper.findField(net.minecraft.client.renderer.entity.RenderManager.class,
                "renderOutlines", "field_178639_r").getBoolean(mc.getRenderManager());
        SavedState savedState = new SavedState();
        List<LayerRenderer> layers = (List<LayerRenderer>) ReflectionHelper.findField(RenderLivingBase.class,
                "layerRenderers", "field_177097_h").get(renderer);
        if (layers == null || !(renderer.getMainModel() instanceof ModelPlayerAlt)) { throw new IllegalStateException("ordinary production renderer/layers required"); }
        LayerRenderer<EntityCustomNpc> witness = new LayerRenderer<EntityCustomNpc>() {
            @Override public void doRenderLayer(EntityCustomNpc e, float l, float a, float p, float age, float y, float h, float s) {
                if (observing && e == target) { trace.append("layer,"); }
            }
            @Override public boolean shouldCombineTextures() { return false; }
        };
        layers.add(witness); MinecraftForge.EVENT_BUS.register(this);
        try {
            // Deterministic input to the living renderer; no shadow/world terrain in this FBO.
            mc.getRenderManager().setRenderShadow(false);
            mc.gameSettings.hideGUI = false;
            // Match billboards to the FBO camera below, not to the world's player
            // (who looks from -Z). The FBO looks from +Z with Rx(12)*Ry(-20).
            // Living rendering enables culling before names: using the player's
            // opposite yaw culls every glyph, even on the original path.
            mc.getRenderManager().playerViewX = 12;
            mc.getRenderManager().playerViewY = 160;
            Shot baseline = null, dark = null, rigIdle = null;
            for (String k : contexts ? CONTEXT_KEYS : KEYS) {
                key = k; configure();
                shot(0); shot(1); // lazy original lists, skin and candidate mesh outside the comparison
                Shot a = shot(0), b = shot(1), a2 = shot(0);
                NpcBatchCheck.Cmp cmp = NpcBatchCheck.compare(a.color, a.depth, b.color, b.depth, pixels());
                boolean exact = unsupported();
                boolean empty = key.equals("cancel-pre");
                boolean image = exact ? equal(a, b) : cmp.imageEqual();
                boolean repeat = equal(a, a2) && a.state.equals(a2.state) && a.events.equals(a2.events);
                boolean state = a.state.equals(b.state);
                boolean rig = contexts && (key.startsWith("rig-") || key.equals("gui-rig"));
                String events = rig ? "" : empty ? "pre," : key.equals("morph-zombie") ? "pre,post," : "pre,layer,post,";
                boolean eventOk = a.events.equals(events) && b.events.equals(events) && a2.events.equals(events);
                boolean route = a.draws == 0 && a2.draws == 0 && b.draws == (exact ? 0 : 1) && (exact || b.fallbacks == 0);
                if (contexts) {
                    route &= a.rigFrames == (rig ? 1 : 0) && b.rigFrames == (rig ? 1 : 0) && a2.rigFrames == (rig ? 1 : 0);
                    if ((key.startsWith("gui-") && !rig) || key.startsWith("bake-")) {
                        route &= b.fallbacks == 1 && b.passFallbacks == 1;
                    }
                }
                // No silent no-op: every visible fixture must actually cover depth pixels.
                boolean visible = empty || cmp.union > 0;
                // Full block light and full sky light can both saturate the real lightmap
                // to white. Compare block light with the dark fixture at the SAME sky
                // coordinate, so this control detects the block channel's visible effect.
                long changed = baseline == null ? 0 : difference(a, key.equals("block-lightmap") ? dark : baseline);
                if (contexts && key.startsWith("rig-") && !key.equals("rig-idle")) { changed = difference(a, rigIdle); }
                if (key.equals("rig-idle")) { rigIdle = a; }
                if (key.equals("dark-lightmap")) { dark = a; }
                boolean feature = key.equals("ordinary-name") || key.equals("ordinary-after-specials") || empty || changed > 0;
                JsonObject row = new JsonObject(); row.addProperty("key", key); row.addProperty("exact", exact);
                row.addProperty("repeatEqual", repeat); row.addProperty("stateEqual", state); row.addProperty("imageEqual", image);
                row.addProperty("eventsEqual", eventOk); row.addProperty("visible", visible); row.addProperty("featureVisible", feature);
                row.addProperty("featurePixels", changed); row.addProperty("draws", b.draws); row.addProperty("fallbacks", b.fallbacks);
                row.addProperty("offDraws", a.draws); row.addProperty("repeatDraws", a2.draws);
                row.addProperty("events", b.events); row.addProperty("reasons", b.reasons); row.add("pixels", new Gson().toJsonTree(cmp));
                if (contexts) {
                    row.addProperty("rigFrames", b.rigFrames); row.addProperty("offRigFrames", a.rigFrames);
                    row.addProperty("repeatRigFrames", a2.rigFrames); row.addProperty("passFallbacks", b.passFallbacks);
                    row.addProperty("fakeEntity", target != npc);
                    row.addProperty("worldRegistered", mc.world.getEntityByID(target.getEntityId()) == target);
                }
                row.add("originalState", new Gson().toJsonTree(a.state)); row.add("candidateState", new Gson().toJsonTree(b.state)); rows.add(row);
                check(key, repeat && state && image && eventOk && route && visible && feature,
                    "repeat=" + repeat + " state=" + state + " image=" + image + " events=" + b.events + " draws=" + b.draws
                    + " fallback=" + b.fallbacks + " featurePixels=" + changed + " " + cmp);
                check(key + "-gl", a.gl == 0 && b.gl == 0 && a2.gl == 0, "GL=" + a.gl + "/" + b.gl + "/" + a2.gl);
                png(a.color, images + "-" + key + "-A.png", null);
                png(b.color, images + "-" + key + "-B.png", null);
                png(a.color, images + "-" + key + "-diff.png", b.color);
                if (key.equals("ordinary-name")) {
                    baseline = a;
                    npc.display.setShowName(1);
                    Shot noName = shot(0);
                    check("name-visible", difference(a, noName) > 0, "pixels=" + difference(a, noName)
                            + " gui=" + Minecraft.isGuiEnabled() + " view=" + (mc.getRenderManager().renderViewEntity != null)
                            + " distance=" + npc.getDistanceSq(mc.player) + " height=" + npc.height
                            + " show=" + npc.display.showName());
                    png(noName.color, images + "-name-hidden-A.png", null);
                    npc.display.setShowName(0);
                    NpcBatch.testPoseDegrees = 5;
                    Shot badPose = shot(1); NpcBatch.testPoseDegrees = 0;
                    NpcBatch.testUvShift = 1f / 64;
                    Shot badUv = shot(1); NpcBatch.testUvShift = 0;
                    check("pose-negative-control", !NpcBatchCheck.compare(a.color, a.depth, badPose.color, badPose.depth, pixels()).imageEqual(), "5 degrees");
                    check("uv-negative-control", !NpcBatchCheck.compare(a.color, a.depth, badUv.color, badUv.depth, pixels()).imageEqual(), "1 texel");
                }
                if (key.equals("ordinary-after-specials")) {
                    check("recovery-baseline", equal(a, baseline) && a.state.equals(baseline.state), "ordinary image/state after all fixtures");
                }
            }
        } finally {
            observing = false; cancel = false; NpcBatch.testPoseDegrees = 0; NpcBatch.testUvShift = 0;
            CustomNpcs.RwNpcInstancing = savedBatch; CustomNpcs.RwAnimCore = savedCore;
            NpcBatch.checkScope = true;
            mc.getRenderManager().setRenderOutlines(savedOutline); renderer.setRenderOutlines(savedOutline); npc.dismountRidingEntity();
            npc.readEntityFromNBT(saved); npc.rwRig = savedRig;
            npc.currentAnimation = savedAnimation; npc.ticksExisted = savedTicks;
            npc.modelData.eyes.blinkStart = savedBlink; npc.updateClient = savedUpdate;
            npc.textureLocation = savedTexture; npc.textureGlowLocation = savedGlow;
            npc.limbSwing = limb; npc.limbSwingAmount = amount; npc.prevLimbSwingAmount = prevAmount;
            npc.swingProgress = swing; npc.prevSwingProgress = prevSwing; npc.hurtTime = hurt; npc.deathTime = death;
            npc.maxHurtTime = maxHurt;
            npc.scaleX = sx; npc.scaleY = sy; npc.scaleZ = sz;
            mc.getRenderManager().setRenderShadow(savedShadow);
            mc.gameSettings.hideGUI = savedHideGui;
            mc.getRenderManager().playerViewX = savedViewX;
            mc.getRenderManager().playerViewY = savedViewY;
            layers.remove(witness); MinecraftForge.EVENT_BUS.unregister(this);
            savedState.restore();
        }
        JsonObject result = new JsonObject(); result.addProperty("success", ok); result.add("checks", checks); result.add("rows", rows);
        return result;
    }

    /** The initial M14 contract keeps special models/layers/riders on the complete original path. */
    private boolean unsupported() {
        if (contexts) { return !key.equals("ordinary-name") && !key.equals("ordinary-after-specials"); }
        return key.equals("hurt") || key.equals("death") || key.equals("overlay") || key.startsWith("armor")
            || key.equals("held-item") || key.equals("riding") || key.equals("morph-zombie") || key.equals("outline") || key.equals("cancel-pre");
    }

    private void configure() {
        target = npc;
        if (contexts) { CustomNpcs.RwAnimCore = 1; npc.rwRig = new RigData(); }
        npc.dismountRidingEntity(); npc.display.readToNBT(display.copy()); npc.modelData.readFromNBT(model.copy());
        npc.modelData.setEntityClass(null); npc.modelData.eyes.blinkStart = 0;
        npc.display.setName("M143B Name"); npc.display.setTitle("Renderer proof"); npc.display.setShowName(0);
        npc.display.setSkinTexture("minecraft:textures/entity/steve.png"); npc.display.skinType = 0;
        npc.display.setTint(0xFFFFFF); npc.display.setSize(5); npc.display.setOverlayTexture("");
        npc.textureLocation = null; npc.textureGlowLocation = null;
        npc.currentAnimation = 0; npc.hurtTime = npc.deathTime = 0; npc.ticksExisted = 80;
        npc.limbSwing = npc.limbSwingAmount = npc.prevLimbSwingAmount = 0;
        npc.swingProgress = npc.prevSwingProgress = 0; npc.scaleX = npc.scaleY = npc.scaleZ = 0.9375f;
        for (int p = 0; p < 6; p++) { npc.display.setModelScale(p, 1, 1, 1); }
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) { npc.setItemStackToSlot(slot, ItemStack.EMPTY); }
        mc.getRenderManager().setRenderOutlines(key.equals("outline")); cancel = key.equals("cancel-pre");
        switch (key) {
            case "walk": npc.limbSwing = 2.3f; npc.limbSwingAmount = npc.prevLimbSwingAmount = 0.8f; break;
            case "attack": npc.swingProgress = npc.prevSwingProgress = 0.6f; break;
            case "bow-pose": npc.currentAnimation = 6; break;
            case "sneak": npc.currentAnimation = 4; break;
            case "skin-alex": npc.display.setSkinTexture("minecraft:textures/entity/alex.png"); break;
            case "tint": npc.display.setTint(0xA0D080); break;
            case "global-scale": npc.scaleX = 0.7f; npc.scaleY = 1.1f; npc.scaleZ = 0.85f; break;
            case "part-scale": npc.display.setModelScale(0, 1.3f, 0.8f, 1.1f); npc.display.setModelScale(1, 0.8f, 1.1f, 0.9f); break;
            case "hurt": npc.hurtTime = 6; break;
            case "death": npc.deathTime = 9; break;
            case "overlay": npc.display.setOverlayTexture("minecraft:textures/entity/steve.png"); break;
            case "armor": case "armor-glint":
                ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                if (key.equals("armor-glint")) { chest.addEnchantment(Enchantments.PROTECTION, 1); }
                npc.setItemStackToSlot(EntityEquipmentSlot.CHEST, chest); break;
            case "held-item": npc.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD)); break;
            case "riding": npc.startRiding(new EntityHorse(mc.world), true); break;
            case "morph-zombie": npc.modelData.setEntityClass(EntityZombie.class); break;
            default: break;
        }
        if (contexts) {
            npc.maxHurtTime = 10;
            // No client ticks occur during the synchronous callback. Rig time is ticks+partial,
            // not wall time; each A/B/A2 observes the same production pose evaluation.
            if (key.startsWith("rig-") || key.equals("gui-rig")) { npc.rwRig.model = "knight"; }
            if (key.equals("rig-walk")) { npc.limbSwing = 2.3f; npc.limbSwingAmount = npc.prevLimbSwingAmount = 0.8f; }
            if (key.equals("rig-hurt")) { npc.hurtTime = 6; }
            if (key.equals("rig-death")) { npc.deathTime = 9; }
            if (key.startsWith("gui-")) { npc.display.setShowName(1); }
            if (key.startsWith("bake-")) {
                NBTTagCompound spawn = npc.writeSpawnData();
                target = ImpostorBaker.createEntity(mc, FarLook.describe("customnpcs:customnpc", spawn));
                if (target == null || mc.world.getEntityByID(target.getEntityId()) == target) {
                    throw new IllegalStateException("Baker must create an unregistered entity");
                }
            }
        }
    }

    private Shot shot(int mode) {
        CustomNpcs.RwNpcInstancing = mode;
        GL20.glUseProgram(0); NpcBatchCompatHarness.bindVao(0); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        fb.framebufferClear(); fb.bindFramebuffer(true);
        GlStateManager.enableDepth(); GlStateManager.depthMask(true); GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.colorMask(true, true, true, true); GlStateManager.disableBlend(); GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f); GlStateManager.disableCull(); GlStateManager.disableFog();
        // A disabled fog still retains its mode; reset it for independent fixtures and
        // the final state comparison, rather than leaking EXP2 from the preceding row.
        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        GlStateManager.disableNormalize(); GlStateManager.disableRescaleNormal();
        for (int i = 2; i < 8; i++) { GlStateManager.disableLight(i); }
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE2); GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0); GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1); GL11.glNormal3f(0, 0, 1); GL11.glTexCoord2f(0, 0);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity(); GL11.glOrtho(-1.8, 1.8, -1.5, 2.1, -16, 16);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity(); GlStateManager.translate(0, -0.5, -5);
        GlStateManager.rotate(12, 1, 0, 0); GlStateManager.rotate(-20, 0, 1, 0);
        // The GUI helper owns its model transform; supply an actual GUI pixel projection.
        if (contexts && key.startsWith("gui-")) {
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity();
            GlStateManager.ortho(0, fb.framebufferWidth, fb.framebufferHeight, 0, -1000, 1000);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity();
        }
        mc.getRenderManager().playerViewX = 12; mc.getRenderManager().playerViewY = 160;
        mc.getRenderManager().setRenderShadow(false);
        RenderHelper.enableStandardItemLighting(); mc.entityRenderer.enableLightmap();
        // Real lightmap texture, frozen coordinates represent night and block-light extremes.
        float block = key.equals("dark-lightmap") ? 16 : key.equals("block-lightmap") ? 240 : 144;
        float sky = key.equals("dark-lightmap") || key.equals("block-lightmap") ? 16 : 240;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, block, sky);
        if (key.startsWith("fog-")) {
            GlStateManager.setFog(key.equals("fog-linear") ? GlStateManager.FogMode.LINEAR : key.equals("fog-exp") ? GlStateManager.FogMode.EXP : GlStateManager.FogMode.EXP2);
            GlStateManager.setFogStart(2); GlStateManager.setFogEnd(10); GlStateManager.setFogDensity(0.15f);
            FloatBuffer fog = BufferUtils.createFloatBuffer(4); fog.put(new float[] {0.25f, 0.4f, 0.6f, 1}).flip();
            GL11.glFog(GL11.GL_FOG_COLOR, fog); GlStateManager.enableFog();
        }
        NpcBatch.checkScope = !(contexts && key.startsWith("gui-"));
        trace.setLength(0); long draws = NpcBatch.draws; long[] before = NpcBatch.fallbacks(); int rigFrames = RigClient.frames;
        observing = true;
        try {
            if (contexts && key.startsWith("gui-")) {
                GuiNPCInterface gui = new GuiNPCInterface(npc) { @Override public void save() { } };
                gui.mouseX = 108; gui.mouseY = 90;
                gui.drawNpc(npc, 128, 190, 2, 180);
            } else if (contexts && key.startsWith("bake-")) {
                // Call the actual baker render stage, with its actual unregistered entity.
                // checkScope remains true: refusal must come from entity membership, not time.
                ImpostorBaker.drawModel(mc, mc.getRenderManager().getEntityRenderObject(target), target,
                        Float.parseFloat(key.substring(5)));
            } else { mc.getRenderManager().renderEntity(npc, 0, 0, 0, 180, 0.5f, false); }
        }
        finally { observing = false; }
        Shot s = new Shot(); s.draws = NpcBatch.draws - draws; s.events = trace.toString(); s.rigFrames = RigClient.frames - rigFrames;
        long[] after = NpcBatch.fallbacks(); StringBuilder reasons = new StringBuilder();
        for (int i = 0; i < before.length; i++) {
            long delta = after[i] - before[i]; s.fallbacks += delta;
            if (i == 0) { s.passFallbacks = delta; }
            if (delta != 0) { reasons.append(NpcBatch.REASONS[i]).append('=').append(delta).append(','); }
        }
        s.reasons = reasons.toString(); s.state = NpcBatchCompatHarness.state();
        for (int flag : new int[] {GL11.GL_DEPTH_TEST, GL11.GL_BLEND, GL11.GL_ALPHA_TEST, GL11.GL_CULL_FACE,
                GL11.GL_LIGHTING, GL11.GL_FOG, GL11.GL_NORMALIZE, 0x803A}) {
            s.state.put("enabled-" + flag, Boolean.toString(GL11.glIsEnabled(flag)));
        }
        for (int value : new int[] {GL11.GL_BLEND_SRC, GL11.GL_BLEND_DST, GL11.GL_ALPHA_TEST_FUNC,
                GL11.GL_FOG_MODE, GL11.GL_COLOR_MATERIAL_FACE, GL11.GL_COLOR_MATERIAL_PARAMETER}) {
            s.state.put("int-" + value, Integer.toString(GL11.glGetInteger(value)));
        }
        FloatBuffer current = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_CURRENT_COLOR, current);
        for (int i = 0; i < 4; i++) { s.state.put("color-" + i, Integer.toHexString(Float.floatToRawIntBits(current.get(i)))); }
        s.color = BufferUtils.createByteBuffer(pixels() * 4); s.depth = BufferUtils.createFloatBuffer(pixels());
        GL11.glReadPixels(0, 0, fb.framebufferWidth, fb.framebufferHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, s.color);
        GL11.glReadPixels(0, 0, fb.framebufferWidth, fb.framebufferHeight, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, s.depth);
        s.gl = GL11.glGetError(); return s;
    }

    private int pixels() { return fb.framebufferWidth * fb.framebufferHeight; }
    private long difference(Shot a, Shot b) {
        long n = 0;
        for (int i = 0; i < pixels(); i++) {
            if (a.color.getInt(i * 4) != b.color.getInt(i * 4) || Float.floatToRawIntBits(a.depth.get(i)) != Float.floatToRawIntBits(b.depth.get(i))) { n++; }
        }
        return n;
    }
    private boolean equal(Shot a, Shot b) { return difference(a, b) == 0; }

    private void png(ByteBuffer c, String path, ByteBuffer other) throws Exception {
        int w = fb.framebufferWidth, h = fb.framebufferHeight;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) { for (int x = 0; x < w; x++) {
            int at = 4 * (y * w + x), rgb = other == null ? (c.get(at + 3) & 255) << 24 : 0xFF000000;
            for (int k = 0; k < 3; k++) {
                int value = c.get(at + k) & 255;
                if (other != null) { value = Math.min(255, 32 * Math.abs(value - (other.get(at + k) & 255))); }
                rgb |= value << (16 - 8 * k);
            }
            image.setRGB(x, h - 1 - y, rgb);
        } }
        if (!ImageIO.write(image, "png", new File(path))) { throw new IllegalStateException("PNG writer unavailable"); }
    }

    private static final class Shot {
        ByteBuffer color; FloatBuffer depth; Map<String, String> state;
        String events, reasons; long draws, fallbacks, passFallbacks; int gl, rigFrames;
    }

    /** Restore Minecraft's mirrors before the caller restores raw GL attributes. */
    private static final class SavedState {
        final int fogMode = GL11.glGetInteger(GL11.GL_FOG_MODE), alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),
                cmFace = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE), cmMode = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER),
                src = GL11.glGetInteger(GL11.GL_BLEND_SRC), dst = GL11.glGetInteger(GL11.GL_BLEND_DST),
                srcAlpha = GL11.glGetInteger(GL14_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14_BLEND_DST_ALPHA);
        final float fogDensity = GL11.glGetFloat(GL11.GL_FOG_DENSITY), fogStart = GL11.glGetFloat(GL11.GL_FOG_START),
                fogEnd = GL11.glGetFloat(GL11.GL_FOG_END), alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF),
                lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        final boolean l0 = GL11.glIsEnabled(GL11.GL_LIGHT0), l1 = GL11.glIsEnabled(GL11.GL_LIGHT1),
                cm = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        static final int GL14_BLEND_SRC_ALPHA = 0x80CB, GL14_BLEND_DST_ALPHA = 0x80CA;
        void restore() {
            GlStateManager.setFog(fogMode == GL11.GL_LINEAR ? GlStateManager.FogMode.LINEAR
                    : fogMode == GL11.GL_EXP ? GlStateManager.FogMode.EXP : GlStateManager.FogMode.EXP2);
            GlStateManager.setFogDensity(fogDensity); GlStateManager.setFogStart(fogStart); GlStateManager.setFogEnd(fogEnd);
            GlStateManager.alphaFunc(alphaFunc, alphaRef); GlStateManager.colorMaterial(cmFace, cmMode);
            GlStateManager.tryBlendFuncSeparate(src, dst, srcAlpha, dstAlpha);
            if (l0) { GlStateManager.enableLight(0); } else { GlStateManager.disableLight(0); }
            if (l1) { GlStateManager.enableLight(1); } else { GlStateManager.disableLight(1); }
            if (cm) { GlStateManager.enableColorMaterial(); } else { GlStateManager.disableColorMaterial(); }
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        }
    }
}
