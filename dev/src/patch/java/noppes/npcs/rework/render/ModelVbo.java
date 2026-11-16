package noppes.npcs.rework.render;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.*;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.opengl.*;

/** M14.1b experimental fixed-function submission, never a replacement for NPC rendering.
 * Only the ordinary main-model invocation may authorize its own exact parts; armor,
 * foreign models, GUI, baker and rigs cannot inherit that authorization. All GL ownership
 * and context scopes live on the Minecraft render thread. Default 0 does not enter here.
 */
@SideOnly(Side.CLIENT)
public final class ModelVbo {
    private static final ModelScaleRenderer[] NONE = new ModelScaleRenderer[0];
    private static ModelScaleRenderer[] allowed = NONE;
    private static boolean allowedCapable;
    // Per-model part membership, rebuilt only when a part field is reassigned. The first
    // cost pair allocated a set and arrays for every NPC every frame (Q482).
    private static final WeakHashMap<ModelBase, Parts> PARTS = new WeakHashMap<ModelBase, Parts>();
    private static final ModelRenderer[] SCRATCH = new ModelRenderer[12];
    private static final WeakHashMap<ModelScaleRenderer, Mesh> CACHE = new WeakHashMap<ModelScaleRenderer, Mesh>();
    private static final List<Mesh> OWNED = new ArrayList<Mesh>();
    private static final Map<String, long[]> FALLBACK = new TreeMap<String, long[]>();
    private static boolean installed, optifine, failed, gl14;
    private static int textureUnits;
    private static volatile boolean pendingReset;
    private static Object context, world;
    public static long draws, uploads, deletes;

    private ModelVbo() { }

    public static Scope beginWorld(Object renderer, ModelBase model, EntityNPCInterface npc) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isCallingFromMinecraftThread() || mc.world == null || mc.currentScreen != null
                || npc.world != mc.world || mc.world.getEntityByID(npc.getEntityId()) != npc
                || MinecraftForgeClient.getRenderPass() != 0
                || !renderer.getClass().getName().equals("noppes.npcs.client.renderer.RenderCustomNpc")) {
            return new Scope(NONE, false);
        }
        return beginParts(model);
    }

    /** Same membership boundary used by the explicitly requested GPU diagnostic. Capability,
     * display-list compilation and the active program are read once here per model invocation,
     * not per part: each glGet is a driver round trip (Q482).
     */
    public static Scope beginParts(ModelBase model) {
        if (CustomNpcs.RwModelVbo != 1 || !Minecraft.getMinecraft().isCallingFromMinecraftThread()
                || (model.getClass() != ModelPlayerAlt.class && model.getClass() != ModelBipedAlt.class)) {
            return new Scope(NONE, false);
        }
        install(); maintain();
        ModelBiped b = (ModelBiped) model;
        ModelRenderer[] f = SCRATCH;
        f[0] = b.bipedHead; f[1] = b.bipedHeadwear; f[2] = b.bipedBody; f[3] = b.bipedLeftArm;
        f[4] = b.bipedRightArm; f[5] = b.bipedLeftLeg; f[6] = b.bipedRightLeg;
        if (model instanceof ModelPlayer) {
            ModelPlayer p = (ModelPlayer) model;
            f[7] = p.bipedBodyWear; f[8] = p.bipedLeftArmwear; f[9] = p.bipedRightArmwear;
            f[10] = p.bipedLeftLegwear; f[11] = p.bipedRightLegwear;
        } else { Arrays.fill(f, 7, 12, null); }
        Parts parts = PARTS.get(model);
        if (parts == null || !Arrays.equals(parts.fields, f)) { parts = new Parts(f); PARTS.put(model, parts); }
        return new Scope(parts.allowed, capable());
    }

    private static final class Parts {
        final ModelRenderer[] fields;
        final ModelScaleRenderer[] allowed;
        Parts(ModelRenderer[] f) {
            fields = f.clone();
            List<ModelScaleRenderer> list = new ArrayList<ModelScaleRenderer>();
            for (ModelRenderer p : f) {
                if (p != null && p.getClass() == ModelScaleRenderer.class && !list.contains(p)) { list.add((ModelScaleRenderer) p); }
            }
            allowed = list.toArray(NONE);
        }
    }

    private static boolean allowed(ModelScaleRenderer part) {
        for (ModelScaleRenderer p : allowed) { if (p == part) { return true; } }
        return false;
    }

    public static final class Scope implements AutoCloseable {
        private final ModelScaleRenderer[] previous;
        private final boolean previousCapable;
        private boolean closed;
        private Scope(ModelScaleRenderer[] parts, boolean capable) {
            previous = allowed; previousCapable = allowedCapable; allowed = parts; allowedCapable = capable;
        }
        @Override public void close() {
            if (!closed) { allowed = previous; allowedCapable = previousCapable; closed = true; }
        }
    }

    /** Freeze at the original list's compilation, never guess the contents of an older list.
     * Mutating public geometry after compilation uses that original list until recompiled.
     */
    public static void compiled(ModelScaleRenderer part, float scale) {
        Mesh old = CACHE.remove(part);
        if (old != null) { release(old); OWNED.remove(old); }
        if (!allowed(part)) { return; }
        try {
            ModelVboGeometry geometry = ModelVboGeometry.capture(part, scale);
            if (geometry == null) { fallback("unsupported-geometry"); return; }
            Mesh mesh = new Mesh(part, geometry, part.displayList);
            CACHE.put(part, mesh); OWNED.add(mesh);
        } catch (RuntimeException error) { fallback("geometry-error"); }
    }

    public static boolean draw(ModelScaleRenderer part, float scale) {
        if (!allowed(part)) { return fallback("outside-main-model"); }
        if (!allowedCapable || failed) { return fallback("capability-or-program"); }
        Mesh mesh = CACHE.get(part);
        if (mesh == null || mesh.list != part.displayList) { return fallback("uncaptured-list"); }
        try {
            if (!mesh.geometry.matches(part, scale)) { release(mesh); return fallback("mutated-geometry"); }
            if (mesh.geometry.vertices == 0) { return true; }
            if (mesh.id == 0) { upload(mesh); }
        } catch (RuntimeException error) {
            release(mesh); failed = true; return fallback("upload-error");
        }
        // Save descriptors, client enables, all texture-coordinate arrays and ARRAY_BUFFER binding.
        // No state readback/screenshot audit here; the diagnostic checks the observable result.
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mesh.id);
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glDisableClientState(GL11.GL_INDEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_EDGE_FLAG_ARRAY);
            if (gl14) {
                GL11.glDisableClientState(GL14.GL_SECONDARY_COLOR_ARRAY);
                GL11.glDisableClientState(GL14.GL_FOG_COORDINATE_ARRAY);
            }
            for (int i = 0; i < textureUnits; i++) {
                GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            }
            GL13.glClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 24, 0L);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 24, 12L);
            GL11.glNormalPointer(GL11.GL_BYTE, 24, 20L);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            GL11.glDrawArrays(GL11.GL_QUADS, 0, mesh.geometry.vertices);
            draws++;
        } finally { GL11.glPopClientAttrib(); }
        // DrawArrays leaves current normal/UV unspecified. Match the legacy signed-byte
        // conversion and the last vertex that the vanilla display list leaves behind.
        int last = (mesh.geometry.vertices - 1) * 24;
        GL11.glNormal3b(mesh.geometry.bytes.get(last + 20), mesh.geometry.bytes.get(last + 21), mesh.geometry.bytes.get(last + 22));
        GL13.glMultiTexCoord2f(OpenGlHelper.defaultTexUnit, mesh.geometry.bytes.getFloat(last + 12), mesh.geometry.bytes.getFloat(last + 16));
        return true;
    }

    private static boolean capable() {
        ContextCapabilities caps = GLContext.getCapabilities();
        return !optifine && !failed && caps.OpenGL15 && GL11.glGetInteger(GL11.GL_LIST_INDEX) == 0
                && (!caps.OpenGL20 || GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == 0);
    }

    private static void upload(Mesh mesh) {
        int binding = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            mesh.id = GL15.glGenBuffers(); mesh.context = context;
            if (mesh.id == 0) { throw new IllegalStateException("No VBO name"); }
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mesh.id);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mesh.geometry.bytes.duplicate(), GL15.GL_STATIC_DRAW);
            if (GL15.glGetBufferParameteri(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE) != mesh.geometry.bytes.remaining()) {
                throw new IllegalStateException("Incomplete VBO upload");
            }
            uploads++;
        } finally { GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, binding); }
    }

    private static boolean fallback(String why) {
        long[] n = FALLBACK.get(why);
        if (n == null) { FALLBACK.put(why, n = new long[1]); }
        n[0]++; return false;
    }
    public static Map<String, Long> fallbacks() {
        Map<String, Long> copy = new TreeMap<String, Long>();
        for (Map.Entry<String, long[]> e : FALLBACK.entrySet()) { copy.put(e.getKey(), e.getValue()[0]); }
        return copy;
    }
    public static int live() { int n = 0; for (Mesh mesh : OWNED) { if (mesh.id != 0) { n++; } } return n; }

    /** Resource reload/world transition: discard GPU objects, keep weak frozen CPU snapshots
     * paired with their existing display lists. Next eligible draw reuploads those exact bytes.
     */
    public static void reset() {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) { pendingReset = true; return; }
        for (Mesh mesh : OWNED) { release(mesh); }
        failed = false; pendingReset = false;
    }

    private static void release(Mesh mesh) {
        if (mesh.id != 0) {
            // A destroyed context's IDs must never be deleted in a new context with reused names.
            if (mesh.context == GLContext.getCapabilities()) { GL15.glDeleteBuffers(mesh.id); deletes++; }
            mesh.id = 0;
        }
    }

    private static void maintain() {
        Object now = GLContext.getCapabilities(), currentWorld = Minecraft.getMinecraft().world;
        if (pendingReset || context != now || world != currentWorld || CustomNpcs.RwModelVbo != 1) {
            reset(); context = now; world = currentWorld;
            ContextCapabilities caps = (ContextCapabilities) now;
            gl14 = caps.OpenGL14;
            textureUnits = caps.OpenGL13 ? GL11.glGetInteger(GL13.GL_MAX_TEXTURE_UNITS) : 1;
        }
    }

    private static void sweep() {
        Iterator<Mesh> it = OWNED.iterator();
        while (it.hasNext()) { Mesh m = it.next(); if (m.owner.get() == null) { release(m); it.remove(); } }
    }

    private static void install() {
        if (installed) { return; }
        installed = true;
        try { Class.forName("net.optifine.Config", false, ModelVbo.class.getClassLoader()); optifine = true; }
        catch (LinkageError presentButBroken) { optifine = true; }
        catch (ClassNotFoundException absent) {
            try { Class.forName("Config", false, ModelVbo.class.getClassLoader()); optifine = true; }
            catch (LinkageError presentButBroken) { optifine = true; }
            catch (ClassNotFoundException alsoAbsent) { /* vanilla/Forge */ }
        }
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(manager -> reset());
        MinecraftForge.EVENT_BUS.register(new Listener());
    }

    public static final class Listener {
        @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) { maintain(); sweep(); }
        }
    }

    private static final class Mesh {
        final WeakReference<ModelScaleRenderer> owner;
        final ModelVboGeometry geometry;
        final int list;
        int id;
        Object context;
        Mesh(ModelScaleRenderer part, ModelVboGeometry geometry, int list) {
            owner = new WeakReference<ModelScaleRenderer>(part); this.geometry = geometry; this.list = list;
        }
    }
}
