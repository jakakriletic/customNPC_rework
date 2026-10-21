package noppes.npcs.rework.far;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * M5-V.4: peka impostorjev na klientu (raziskava M5-V.0 §5.4, D-046).
 *
 * <p>Iz opisa videza ({@link FarClient#description}) zgradi lazno entiteto (ni dodana v svet; precedens
 * {@code GuiNpcMobSpawnerAdd}), jo z rendererjem CNPC ({@code Render.doRender}, brez sence, ognja in imena)
 * narise v lasten framebuffer {@link #FB_W} x {@link #FB_H} z ortogonalno projekcijo pod
 * {@link ImpostorFrames#ANGLES} koti, z isto standardno osvetlitvijo entitet in brez lightmapa (svetlobo
 * kraja doda izris, M5-V.5). Samodejni obrez: unija alfe vseh kotov, simetricno okoli osi entitete; ce se
 * dotika roba, se vidno polje podvoji (najvec dvakrat). Okvirji se ploskovno pomanjsajo v najvec 32 x 64
 * tekslov, dobijo mipmape z ohranjeno pokritostjo alfe in gredo v {@link ImpostorAtlas}.
 *
 * <p>Proracun: najvec <b>en videz na slicico</b> ({@code RenderTickEvent} START, pred izrisom sveta).
 * Brez podpore za framebuffer se peka izklopi ({@link #noFbo}). Pri skinu igralca ali URL se peka odlozi,
 * dokler renderer ne pozna teksture. Samo z {@code RwFarView >= 1}; z 0 se ne zgodi nic.
 */
@SideOnly(Side.CLIENT)
public final class ImpostorBaker {
    public static final int FB_W = 128;
    public static final int FB_H = 256;
    /** Najvec slicic, ko se peka caka na teksturo skina; nato se speče s tem, kar renderer ima. */
    static final int MAX_WAITS = 600;

    static final LinkedHashSet<Long> QUEUE = new LinkedHashSet<Long>();
    static final Set<Long> FAILED = new HashSet<Long>();
    static final Map<Long, Integer> WAITS = new HashMap<Long, Integer>();
    private static ImpostorAtlas atlas;
    private static Framebuffer fb;
    private static IntBuffer pixels;

    public static long baked;
    public static long bakeNs;
    public static long bakeNsMax;
    public static long failures;
    public static long deferred;
    public static long clipped;
    public static long retries;
    public static boolean noFbo;

    private ImpostorBaker() {
    }

    /** Klice {@link FarClient#install}. */
    static void install() {
        MinecraftForge.EVENT_BUS.register(new ImpostorBaker());
    }

    /** Videz z znanim opisom naj se speče (ce se ni in ni propadel). */
    static void want(long hash) {
        if (hash != 0L && !FAILED.contains(hash) && (atlas == null || !atlas.contains(hash))) {
            QUEUE.add(hash);
        }
    }

    /** Ob izklopu {@code RwFarView}: sprosti atlas in framebuffer. */
    static void release() {
        QUEUE.clear();
        WAITS.clear();
        if (atlas != null) {
            atlas.delete();
            atlas = null;
        }
        if (fb != null) {
            fb.deleteFramebuffer();
            fb = null;
        }
    }

    public static boolean available() {
        if (!OpenGlHelper.isFramebufferEnabled()) {
            noFbo = true;
            return false;
        }
        return true;
    }

    /** Atlas po stikalu {@code RwImpostorAtlas} (omejen z {@code GL_MAX_TEXTURE_SIZE}); ob spremembi nov. */
    public static ImpostorAtlas atlas() {
        int want = ImpostorAtlas.sanitize(CustomNpcs.RwImpostorAtlas);
        int max = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        while (want > 1024 && want > max) {
            want /= 2;
        }
        if (atlas == null || atlas.size != want) {
            if (atlas != null) {
                atlas.delete();
            }
            atlas = new ImpostorAtlas(want);
        }
        return atlas;
    }

    /** Pecenih videzov v atlasu (0 brez atlasa). */
    public static int inAtlas() {
        return atlas == null ? 0 : atlas.count();
    }

    public static long evictions() {
        return atlas == null ? 0L : atlas.slots.evictions;
    }

    /** Nadomestki z znanim opisom, katerih videz se ni pecen (in ni propadel). */
    public static int missing() {
        int n = 0;
        for (int[] e : FarClient.STORE.values()) {
            Long hash = e[4] == 0 ? null : FarClient.LOOK_IDS.get(e[4]);
            if (hash != null && FarClient.DESCS.containsKey(hash) && !FAILED.contains(hash)
                    && (atlas == null || !atlas.contains(hash))) {
                ++n;
            }
        }
        return n;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (ev.phase != TickEvent.Phase.START || CustomNpcs.RwFarView < 1 || QUEUE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || !available()) {
            return;
        }
        for (int skip = 0; skip < 32 && !QUEUE.isEmpty(); ++skip) {
            Iterator<Long> it = QUEUE.iterator();
            long hash = it.next();
            it.remove();
            if (FAILED.contains(hash) || atlas().contains(hash)) {
                continue;
            }
            NBTTagCompound desc = FarClient.DESCS.get(hash);
            if (desc == null) {
                continue;
            }
            bake(mc, hash, desc, true);
            return;
        }
    }

    /** Peka brez proracuna in brez cakanja na teksturo (preverba {@code ImpostorCheck}). */
    public static ImpostorAtlas.Entry bakeNow(Minecraft mc, long hash, NBTTagCompound desc) {
        if (!available()) {
            return null;
        }
        ImpostorAtlas.Entry e = atlas().get(hash);
        return e != null ? e : bake(mc, hash, desc, false);
    }

    private static ImpostorAtlas.Entry bake(Minecraft mc, long hash, NBTTagCompound desc, boolean mayWait) {
        long t0 = System.nanoTime();
        ImpostorAtlas.Entry result = null;
        boolean pushed = false;
        try {
            EntityNPCInterface npc = createEntity(mc, desc);
            Render<Entity> render = null;
            if (npc != null) {
                render = mc.getRenderManager().getEntityRenderObject(npc);
            }
            if (render == null) {
                fail(hash);
                return null;
            }
            Object r = render;
            if (r instanceof RenderNPCInterface) {
                ((RenderNPCInterface) r).getEntityTexture(npc);
                if (mayWait && npc.display.skinType != 0 && npc.textureLocation == null) {
                    int w = WAITS.containsKey(hash) ? WAITS.get(hash) : 0;
                    if (w < MAX_WAITS) {
                        WAITS.put(hash, w + 1);
                        QUEUE.add(hash);
                        ++deferred;
                        return null;
                    }
                }
            }
            WAITS.remove(hash);
            if (fb == null) {
                fb = new Framebuffer(FB_W, FB_H, true);
                fb.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
                pixels = BufferUtils.createIntBuffer(FB_W * FB_H);
            }
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            pushed = true;
            double viewH = Math.max(npc.height, npc.width * 2.0f) * 1.5 + 0.5;
            double yLo = 0.0;
            double yHi = 0.0;
            int[][] imgs = new int[ImpostorFrames.ANGLES][];
            int[] bounds = null;
            for (int attempt = 0; attempt < 3; ++attempt) {
                yLo = npc.height * 0.5 - viewH * 0.5;
                yHi = yLo + viewH;
                bounds = null;
                for (int k = 0; k < ImpostorFrames.ANGLES; ++k) {
                    imgs[k] = renderOrtho(mc, render, npc, k * 45.0f, viewH * 0.25, yLo, yHi);
                    bounds = ImpostorFrames.union(bounds, ImpostorFrames.alphaBounds(imgs[k], FB_W, FB_H));
                }
                if (bounds == null || !ImpostorFrames.touchesEdge(bounds, FB_W, FB_H)) {
                    break;
                }
                if (attempt == 2) {
                    ++clipped;
                    break;
                }
                viewH *= 2.0;
                ++retries;
            }
            if (bounds == null) {
                fail(hash);
                return null;
            }
            int[] crop = ImpostorFrames.symmetricCrop(bounds, FB_W / 2);
            int[] size = ImpostorFrames.frameSize(crop[2], crop[3]);
            double ppb = FB_H / viewH;
            float width = (float) (crop[2] / ppb);
            float top = (float) (yHi - crop[1] / ppb);
            float bottom = (float) (yHi - (crop[1] + crop[3]) / ppb);
            int[][][] cells = new int[ImpostorFrames.ANGLES][][];
            for (int k = 0; k < ImpostorFrames.ANGLES; ++k) {
                int[] frame = ImpostorFrames.resample(imgs[k], FB_W, FB_H, crop[0], crop[1], crop[2], crop[3], size[0], size[1]);
                cells[k] = ImpostorFrames.mipChain(ImpostorFrames.cell(frame, size[0], size[1]));
            }
            result = atlas().store(hash, cells, size[0], size[1], width, bottom, top - bottom);
            ++baked;
            long dt = System.nanoTime() - t0;
            bakeNs += dt;
            bakeNsMax = Math.max(bakeNsMax, dt);
            return result;
        } catch (RuntimeException e) {
            fail(hash);
            return null;
        } finally {
            if (pushed) {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
                mc.getFramebuffer().bindFramebuffer(true);
            }
        }
    }

    private static void fail(long hash) {
        ++failures;
        FAILED.add(hash);
        WAITS.remove(hash);
    }

    /** Lazna entiteta iz opisa (ni v svetu), na mestu kamere (stopnja L0, K5), brez imena. */
    public static EntityNPCInterface createEntity(Minecraft mc, NBTTagCompound desc) {
        String cls = desc.getString(FarLook.CLS);
        if (cls.isEmpty()) {
            return null;
        }
        Entity e = EntityList.createEntityByIDFromName(new ResourceLocation(cls), mc.world);
        if (!(e instanceof EntityNPCInterface)) {
            return null;
        }
        EntityNPCInterface npc = (EntityNPCInterface) e;
        npc.readSpawnData(desc);
        npc.display.setShowName(1);
        Entity view = mc.getRenderViewEntity();
        if (view != null) {
            npc.setPosition(view.posX, view.posY, view.posZ);
            npc.prevPosX = npc.lastTickPosX = view.posX;
            npc.prevPosY = npc.lastTickPosY = view.posY;
            npc.prevPosZ = npc.lastTickPosZ = view.posZ;
        }
        npc.updateHitbox();
        npc.ticksExisted = 0;
        npc.limbSwing = 0.0f;
        npc.limbSwingAmount = 0.0f;
        npc.prevLimbSwingAmount = 0.0f;
        return npc;
    }

    /**
     * Narise entiteto v izhodiscu z yaw {@code yaw} v trenutni framebuffer in matrike; standardna
     * osvetlitev entitet se nastavi s trenutnim modelview (pri peki identiteta), brez lightmapa in megle.
     */
    public static void drawModel(Minecraft mc, Render<Entity> render, EntityNPCInterface npc, float yaw) {
        npc.rotationYaw = npc.prevRotationYaw = yaw;
        npc.renderYawOffset = npc.prevRenderYawOffset = yaw;
        npc.rotationYawHead = npc.prevRotationYawHead = yaw;
        npc.rotationPitch = npc.prevRotationPitch = 0.0f;
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.entityRenderer.disableLightmap();
        RenderHelper.enableStandardItemLighting();
        render.doRender(npc, 0.0, 0.0, 0.0, yaw, 1.0f);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** En kot v framebuffer peke; vrne ARGB, vrstica 0 zgoraj. */
    private static int[] renderOrtho(Minecraft mc, Render<Entity> render, EntityNPCInterface npc, float yaw,
            double halfW, double yLo, double yHi) {
        fb.framebufferClear();
        fb.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(-halfW, halfW, yLo, yHi, -1000.0, 1000.0);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        drawModel(mc, render, npc, yaw);
        return readArgb(FB_W, FB_H, pixels);
    }

    /** Prebere barvo trenutnega framebufferja kot ARGB, vrstica 0 zgoraj. */
    public static int[] readArgb(int w, int h, IntBuffer buf) {
        buf.clear();
        GlStateManager.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
        GlStateManager.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
        GlStateManager.glReadPixels(0, 0, w, h, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
        int[] img = new int[w * h];
        for (int y = 0; y < h; ++y) {
            int src = y * w;
            int dst = (h - 1 - y) * w;
            for (int x = 0; x < w; ++x) {
                img[dst + x] = buf.get(src + x);
            }
        }
        return img;
    }
}
