package noppes.npcs.rework.render;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.ModelPlayerAlt;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.far.FarCompat;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** M14.2 (RwNpcInstancing=1): one draw call for all twelve main-model parts of an ordinary
 * CNPC player model, issued exactly where the original issued twelve display lists, so draw
 * order, depth, layers, name, events and every later draw stay original. The CPU pose is the
 * existing model state; original postRender operations supply the part matrices, the GPU applies them.
 * A GLSL 1.50 compatibility + ARB_gpu_shader5 program repeats the fixed-function steps:
 * vertex transform, two-light colour-material lighting with normal rescale/normalize, texture
 * and lightmap modulate and fog. Alpha test, blending and depth stay fixed-function.
 *
 * <p>Anything else draws the original model: GUI, impostor baking, fake entities, other models
 * or renderers, outline mode, invisible NPCs, hurt overlay, extra texture units or lights,
 * texgen, OptiFine, an active GL program, no GL 3.2 + ARB_gpu_shader5, changed geometry
 * or child parts. Each refusal is counted by reason. Lighting state comes from GlStateManager;
 * original GL matrix operations and readbacks preserve driver rounding for each part.
 * Their cost is included in the controller cost pair. Render thread only.
 */
@SideOnly(Side.CLIENT)
public final class NpcBatch {
    public static final String[] REASONS = {"pass", "model", "state", "capability", "program", "geometry", "error"};
    static final int PASS = 0, MODEL = 1, STATE = 2, CAPABILITY = 3, PROGRAM = 4, GEOMETRY = 5, ERROR = 6;
    private static final long[] FALLBACK = new long[REASONS.length];
    public static long draws, uploads, deletes, programs, programFailures, drawFailures;
    /** Explicitly requested harness passes run outside the world pass (RenderWorldLastEvent). */
    public static boolean checkScope;
    /** Harness negative controls only: extra head rotation (degrees) and U shift (texture units). */
    public static float testPoseDegrees, testUvShift;
    /** Harness fault injection only (M14.3b4): 1 = invalid vertex source (compile), 2 = fragment
     * stage without main (link), 3 = exception after the program and VAO are bound (draw),
     * 4 = old matrix-vector clip transform (depth regression negative control). */
    public static int testProgramFault;
    /** Diagnostic only: observes the native part stack before it is popped. Never set in normal play. */
    public static java.util.function.IntConsumer testPartCapture;

    private static final int PARTS = 12;
    private static final ModelRenderer[] SCRATCH = new ModelRenderer[PARTS];
    private static final float[] MATRICES = new float[PARTS * 16];
    private static final float[] CLIPS = new float[PARTS * 16];
    private static final float[] MODEL_VIEW = new float[16], PROJECTION = new float[16];
    private static final FloatBuffer READ_MATRIX = BufferUtils.createFloatBuffer(16);
    private static final float[] CPU_AUDIT = new float[PARTS * 16];
    private static final float[] CPU_CLIPS = new float[PARTS * 16];
    private static final java.util.Set<Integer> AUDITED = new java.util.HashSet<Integer>();
    public static long auditElements;
    public static float auditMaxError;
    public static long auditClipElements, auditClipDifferent;
    public static float auditClipMaxError;
    private static int auditLogs;
    private static final FloatBuffer UPLOAD = BufferUtils.createFloatBuffer(PARTS * 16);
    private static final WeakHashMap<ModelBase, Entry> MODELS = new WeakHashMap<ModelBase, Entry>();
    private static final List<Entry> OWNED = new ArrayList<Entry>();

    private static boolean installed, reflected, worldPass, failed, frameOk, capable, arbVao, fogRadial;
    private static volatile boolean resetPending;
    private static int frame = Integer.MIN_VALUE;
    private static Object context, world;
    private static int program, locParts, locClips, locNormal, locLighting, locLight0, locLight1, locRadial, locUv, locLightmap, locFog;
    private static int vNormal, vLighting, vLight0, vLight1, vRadial, vLightmap, vFog;
    private static float vUv;

    // GlStateManager mirror (its own CPU cache of the GL state), resolved once.
    private static Field fCurrent, fCmFace, fCmMode, fFogMode, fActiveUnit;
    private static Object sLighting, sColorMaterial, sColorMaterialFlag, sFog, sFogFlag, sNormalize, sRescale;
    private static Object[] sLights, sTextures, sTexGen;

    private NpcBatch() { }

    /** Checks that need no GL: world pass, real world entity, exact renderer and model. */
    public static boolean accepts(Object renderer, ModelBase model, EntityNPCInterface npc) {
        if (!installed) { install(); }
        Minecraft mc = Minecraft.getMinecraft();
        if (!(worldPass || checkScope) || !mc.isCallingFromMinecraftThread() || mc.world == null || npc.world != mc.world
                || mc.world.getEntityByID(npc.getEntityId()) != npc || MinecraftForgeClient.getRenderPass() == 1) {
            return fallback(PASS);
        }
        if (renderer.getClass() != RenderCustomNpc.class || model.getClass() != ModelPlayerAlt.class) {
            return fallback(MODEL);
        }
        // The M14 main-model contract excludes special layers and riders. Decide before
        // binding a texture or drawing any part: their entire renderer must stay original,
        // including depth and the geometry subsequently reused by overlay/equipment layers.
        // URL skins stay original throughout their download lifecycle, including success
        // and failure. Do not inspect asynchronous downloader state on this hot path.
        if (npc.display.skinType == 2 || npc.isRiding() || !npc.display.getOverlayTexture().isEmpty()
                || !npc.getHeldItemMainhand().isEmpty() || !npc.getHeldItemOffhand().isEmpty()) {
            return fallback(MODEL);
        }
        for (net.minecraft.item.ItemStack armor : npc.getArmorInventoryList()) {
            if (!armor.isEmpty()) { return fallback(MODEL); }
        }
        return true;
    }

    /** Draws the main model after the original texture bind; false = caller draws the original. */
    public static boolean render(ModelBase model, EntityNPCInterface npc, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        int now = mc.getFrameTimer().getIndex();
        if (now != frame) { frame = now; beginFrame(mc); }
        if (!frameOk) { return fallback(capable && reflected ? PROGRAM : CAPABILITY); }
        // Other renderers can change these after our first draw in the SAME frame.
        // Refuse before uploading or binding anything: the original must see their state.
        if (GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != 0) { return fallback(PROGRAM); }
        if (GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING) != 0) { return fallback(STATE); }
        Entry e = entry((ModelPlayerAlt) model, scale);
        if (e == null) { return fallback(GEOMETRY); }
        if (!state()) { return fallback(STATE); }
        try {
            if (e.vao == 0) { upload(e); }
            boolean audit = checkScope && AUDITED.add(npc.getEntityId());
            if (audit) {
                readMatrix(GL11.GL_PROJECTION_MATRIX, PROJECTION);
                readMatrix(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW);
                NpcBatchPose.compute(e.fields, npc.isSneaking(), scale, MODEL_VIEW, CPU_AUDIT);
            }
            capturePose(e, npc, scale);
            if (audit) { auditPose(e, npc); }
            UPLOAD.clear(); UPLOAD.put(MATRICES).flip();
            GL20.glUseProgram(program);
            try {
                uniforms();
                GL20.glUniformMatrix4(locParts, false, UPLOAD);
                UPLOAD.clear(); UPLOAD.put(CLIPS).flip();
                GL20.glUniformMatrix4(locClips, false, UPLOAD);
                bindVao(e.vao);
                if (testProgramFault == 3) { throw new IllegalStateException("test draw fault"); }
                GL11.glDrawArrays(GL11.GL_QUADS, 0, e.mesh.vertices);
            } finally {
                bindVao(0);
                GL20.glUseProgram(0);
            }
        } catch (RuntimeException error) {
            // Original renderer until the next resource reload or world change.
            System.out.println("RWBATCH draw failed: " + error);
            drawFailures++;
            failed = true; frameOk = false; releaseAll();
            return fallback(ERROR);
        }
        // DrawArrays leaves the current normal and UV unspecified; the original lists leave
        // those of the last vertex of the last drawn part (same signed-byte conversion).
        for (int i = PARTS - 1; i >= 0; i--) {
            if (e.fields[i].showModel && !e.fields[i].isHidden && e.mesh.partVertices[i] > 0) {
                GL11.glNormal3b(e.mesh.lastNormal[i * 3], e.mesh.lastNormal[i * 3 + 1], e.mesh.lastNormal[i * 3 + 2]);
                GL13.glMultiTexCoord2f(GL13.GL_TEXTURE0, e.mesh.lastUv[i * 2], e.mesh.lastUv[i * 2 + 1]);
                break;
            }
        }
        draws++;
        return true;
    }

    private static void readMatrix(int name, float[] out) {
        readMatrix(name, out, 0);
    }

    private static void readMatrix(int name, float[] out, int at) {
        READ_MATRIX.clear();
        GL11.glGetFloat(name, READ_MATRIX);
        READ_MATRIX.get(out, at, 16);
    }

    /** Use the driver's own postRender composition, including its trigonometry and
     * intermediate precision. An approximately equal CPU matrix may move texel/face boundaries
     * in K22. The matrix stack is restored before drawing; this runs in every candidate pass,
     * including timed passes, rather than substituting a more accurate harness-only path. */
    private static void capturePose(Entry e, EntityNPCInterface npc, float scale) {
        GlStateManager.pushMatrix();
        try {
            if (npc.isSneaking()) { GlStateManager.translate(0.0f, 0.2f, 0.0f); }
            for (int i = 0; i < PARTS; i++) {
                ModelRenderer p = e.fields[i];
                int at = i * 16;
                if (!p.showModel || p.isHidden) {
                    Arrays.fill(MATRICES, at, at + 16, 0.0f);
                    MATRICES[at + 15] = 1.0f;
                    Arrays.fill(CLIPS, at, at + 16, 0.0f);
                    CLIPS[at + 15] = 1.0f;
                    continue;
                }
                GlStateManager.pushMatrix();
                try {
                    p.postRender(scale);
                    if (i == 0 && testPoseDegrees != 0.0f) {
                        GlStateManager.rotate(testPoseDegrees, 1.0f, 0.0f, 0.0f);
                    }
                    readMatrix(GL11.GL_MODELVIEW_MATRIX, MATRICES, at);
                    captureClip(at);
                    if (checkScope && testPartCapture != null) { testPartCapture.accept(i); }
                } finally { GlStateManager.popMatrix(); }
            }
        } finally { GlStateManager.popMatrix(); }
    }

    /** Let the same driver compose projection * model-view, rather than assuming its
     * product rounds like the Java double dot products. Work on a temporary projection
     * stack entry: the actual projection and model-view are unchanged on return/exception.
     * This is the production path too, so all matrix calls/readbacks enter the cost pair.
     * The exact RenderCustomNpc main-model call site is in GL_MODELVIEW mode. */
    private static void captureClip(int at) {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        try {
            GlStateManager.pushMatrix();
            try {
                READ_MATRIX.clear();
                READ_MATRIX.put(MATRICES, at, 16).flip();
                GL11.glMultMatrix(READ_MATRIX);
                readMatrix(GL11.GL_PROJECTION_MATRIX, CLIPS, at);
            } finally { GlStateManager.popMatrix(); }
        } finally { GlStateManager.matrixMode(GL11.GL_MODELVIEW); }
    }

    /** Harness only: compare the rejected CPU composition with the native matrices once
     * per NPC, outside the timed window. Diagnostic only; image pass criteria are unchanged. */
    private static void auditPose(Entry e, EntityNPCInterface npc) {
        NpcBatchPose.project(PROJECTION, MATRICES, CPU_CLIPS);
        for (int i = 0; i < PARTS; i++) {
            ModelRenderer p = e.fields[i];
            if (!p.showModel || p.isHidden) { continue; }
            for (int k = 0; k < 16; k++) {
                int at = i * 16 + k;
                float clipError = Math.abs(CLIPS[at] - CPU_CLIPS[at]);
                auditClipElements++;
                if (Float.floatToRawIntBits(CLIPS[at]) != Float.floatToRawIntBits(CPU_CLIPS[at])) {
                    auditClipDifferent++;
                }
                if (clipError > auditClipMaxError) {
                    auditClipMaxError = clipError;
                    if (auditClipLogs++ < 8) {
                        System.out.println("RWBATCH clip npc=" + npc.getEntityId() + " part=" + i + " element=" + k
                                + " GL=" + CLIPS[at] + " CPU=" + CPU_CLIPS[at] + " error=" + clipError);
                    }
                }
                float error = Math.abs(MATRICES[i * 16 + k] - CPU_AUDIT[i * 16 + k]);
                auditElements++;
                if (error > auditMaxError) {
                    auditMaxError = error;
                    if (auditLogs++ < 8) {
                        System.out.println("RWBATCH matrix npc=" + npc.getEntityId() + " part=" + i + " element=" + k
                                + " GL=" + MATRICES[i * 16 + k] + " CPU=" + CPU_AUDIT[i * 16 + k] + " error=" + error);
                    }
                }
            }
        }
    }

    public static void beginMatrixAudit() {
        AUDITED.clear(); auditElements = 0; auditMaxError = 0.0f; auditLogs = 0;
        auditClipElements = auditClipDifferent = 0; auditClipMaxError = 0.0f; auditClipLogs = 0;
    }

    private static int auditClipLogs;

    public static void endMatrixAudit() { AUDITED.clear(); }

    private static Entry entry(ModelPlayerAlt m, float scale) {
        ModelRenderer[] f = SCRATCH;
        // ModelPlayerAlt.render order.
        f[0] = m.bipedHead; f[1] = m.bipedBody; f[2] = m.bipedRightArm; f[3] = m.bipedLeftArm;
        f[4] = m.bipedRightLeg; f[5] = m.bipedLeftLeg; f[6] = m.bipedHeadwear; f[7] = m.bipedLeftLegwear;
        f[8] = m.bipedRightLegwear; f[9] = m.bipedLeftArmwear; f[10] = m.bipedRightArmwear; f[11] = m.bipedBodyWear;
        Entry e = MODELS.get(m);
        if (e == null || !Arrays.equals(e.fields, f) || e.scale != scale) {
            if (e != null) { release(e); OWNED.remove(e); }
            e = new Entry(m, f.clone(), scale);
            MODELS.put(m, e); OWNED.add(e);
        }
        if (e.mesh == null) { return null; }
        for (ModelRenderer p : f) {
            if (p.childModels != null && !p.childModels.isEmpty()) { return null; }
        }
        if (e.checked != frame) {
            e.checked = frame;
            for (int i = 0; i < PARTS; i++) {
                if (!e.geometry[i].matches((ModelScaleRenderer) f[i], scale)) {
                    release(e); e.capture();
                    break;
                }
            }
        }
        return e.mesh == null ? null : e;
    }

    /** GL state the program repeats, from GlStateManager's cache; false = original path. */
    private static boolean state() {
        try {
            // activeTextureUnit is the unit index (setActiveTexture stores texture - defaultTexUnit).
            if (fActiveUnit.getInt(null) != 0) { return refuse("unit"); }
            if (!on(sTextures[0])) { return refuse("texture0"); }
            for (int u = 2; u < Math.min(8, sTextures.length); u++) { if (on(sTextures[u])) { return refuse("texture" + u); } }
            for (int l = 2; l < sLights.length; l++) { if (on(sLights[l])) { return refuse("light" + l); } }
            for (Object g : sTexGen) { if (on(g)) { return refuse("texgen"); } }
            boolean lighting = on(sLighting);
            if (lighting && (!on(sColorMaterialFlag) || fCmFace.getInt(sColorMaterial) != GL11.GL_FRONT_AND_BACK
                    || fCmMode.getInt(sColorMaterial) != GL11.GL_AMBIENT_AND_DIFFUSE)) { return refuse("colormaterial"); }
            int fog = 0;
            if (on(sFogFlag)) {
                int mode = fFogMode.getInt(sFog);
                fog = mode == GL11.GL_LINEAR ? 1 : mode == GL11.GL_EXP ? 2 : mode == GL11.GL_EXP2 ? 3 : -1;
                if (fog < 0) { return refuse("fogmode"); }
            }
            nLighting = lighting ? 1 : 0;
            nLight0 = on(sLights[0]) ? 1 : 0;
            nLight1 = on(sLights[1]) ? 1 : 0;
            nNormal = on(sNormalize) ? 2 : on(sRescale) ? 1 : 0;
            nLightmap = on(sTextures[1]) ? 1 : 0;
            nFog = fog;
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }
    private static int nLighting, nLight0, nLight1, nNormal, nLightmap, nFog;
    private static final java.util.Set<String> REFUSED = new java.util.HashSet<String>();

    /** Logs each distinct state refusal once, so a run shows which GL state kept the original path. */
    private static boolean refuse(String what) {
        if (REFUSED.add(what)) { System.out.println("RWBATCH state refused: " + what); }
        return false;
    }

    private static boolean on(Object booleanState) throws IllegalAccessException {
        return fCurrent.getBoolean(booleanState);
    }

    /** Uniform values persist in the program; set only changed ones (program bound). */
    private static void uniforms() {
        if (vLighting != nLighting) { GL20.glUniform1i(locLighting, vLighting = nLighting); }
        if (vLight0 != nLight0) { GL20.glUniform1i(locLight0, vLight0 = nLight0); }
        if (vLight1 != nLight1) { GL20.glUniform1i(locLight1, vLight1 = nLight1); }
        if (vNormal != nNormal) { GL20.glUniform1i(locNormal, vNormal = nNormal); }
        if (vLightmap != nLightmap) { GL20.glUniform1i(locLightmap, vLightmap = nLightmap); }
        if (vFog != nFog) { GL20.glUniform1i(locFog, vFog = nFog); }
        int radial = fogRadial ? 1 : 0;
        if (vRadial != radial) { GL20.glUniform1i(locRadial, vRadial = radial); }
        if (vUv != testUvShift) { GL20.glUniform2f(locUv, vUv = testUvShift, 0.0f); }
    }

    /** Never delete names belonging to another GL context. */
    private static ContextCapabilities syncContext() {
        ContextCapabilities caps = GLContext.getCapabilities();
        if (caps != context) {
            // IDs of another context must not be deleted in this one.
            for (Entry e : OWNED) { e.vao = 0; e.vbo = 0; }
            program = 0;
            context = caps;
            arbVao = !caps.OpenGL30 && caps.GL_ARB_vertex_array_object;
            // Explicit fma/precise fixes the demonstrated clip-coordinate rounding error.
            // Never silently compile the old approximate transform on older hardware.
            capable = caps.OpenGL32 && caps.GL_ARB_gpu_shader5;
        }
        return caps;
    }

    /** Once per frame: context, world, resource reload, capability and fog distance.
     * Program/VAO bindings are checked per draw, before any candidate GL mutation. */
    private static void beginFrame(Minecraft mc) {
        ContextCapabilities caps = syncContext();
        if (resetPending || mc.world != world) {
            releaseAll();
            world = mc.world; resetPending = false; failed = false;
        }
        Iterator<Entry> it = OWNED.iterator();
        while (it.hasNext()) { Entry e = it.next(); if (e.owner.get() == null) { release(e); it.remove(); } }
        frameOk = capable && reflected && !failed && !FarCompat.optifine()
                && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == 0;
        if (frameOk && program == 0) {
            program = buildProgram();
            if (program == 0) { programFailures++; failed = true; frameOk = false; }
        }
        // EntityRenderer.setupFog selects radial fog distance where GL_NV_fog_distance exists.
        fogRadial = frameOk && caps.GL_NV_fog_distance && GL11.glGetInteger(0x855A) == 0x855B;
    }

    private static void upload(Entry e) {
        int buffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int client = GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        e.vbo = GL15.glGenBuffers();
        e.vao = arbVao ? ARBVertexArrayObject.glGenVertexArrays() : GL30.glGenVertexArrays();
        e.context = context;
        try {
            bindVao(e.vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, e.vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, e.mesh.bytes.duplicate(), GL15.GL_STATIC_DRAW);
            // Conventional arrays, so the program reads gl_Vertex, gl_Normal and gl_MultiTexCoord0
            // with the same conversions as the original lists (signed byte normals).
            GL11.glVertexPointer(4, GL11.GL_FLOAT, NpcBatchMesh.STRIDE, 0L);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, NpcBatchMesh.STRIDE, 16L);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glNormalPointer(GL11.GL_BYTE, NpcBatchMesh.STRIDE, 24L);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        } finally {
            bindVao(0);
            GL13.glClientActiveTexture(client);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        }
        uploads++;
    }

    private static void bindVao(int id) {
        if (arbVao) { ARBVertexArrayObject.glBindVertexArray(id); } else { GL30.glBindVertexArray(id); }
    }

    private static void release(Entry e) {
        if (e.vao != 0 || e.vbo != 0) {
            if (e.context == GLContext.getCapabilities()) {
                if (e.vao != 0) { if (arbVao) { ARBVertexArrayObject.glDeleteVertexArrays(e.vao); } else { GL30.glDeleteVertexArrays(e.vao); } }
                if (e.vbo != 0) { GL15.glDeleteBuffers(e.vbo); }
                deletes++;
            }
            e.vao = 0; e.vbo = 0;
        }
    }

    private static void releaseAll() {
        for (Entry e : OWNED) { release(e); }
        if (program != 0) {
            if (context == GLContext.getCapabilities()) { GL20.glDeleteProgram(program); }
            program = 0;
        }
    }

    /** Reload/unload must retire resources even with the feature OFF and no later draw.
     * Forge's server-world events must never access the client's GL context. */
    private static void resetResources() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isCallingFromMinecraftThread()) {
            resetPending = true;
            mc.addScheduledTask(NpcBatch::resetResources);
            return;
        }
        syncContext();
        releaseAll();
        MODELS.clear(); OWNED.clear();
        resetPending = false; failed = false; frameOk = false; frame = Integer.MIN_VALUE;
    }

    public static int live() {
        int n = 0;
        for (Entry e : OWNED) { if (e.vao != 0) { n++; } }
        return n;
    }

    public static long[] fallbacks() { return FALLBACK.clone(); }

    public static String fallbackText() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < REASONS.length; i++) {
            if (FALLBACK[i] != 0) { b.append(b.length() == 0 ? "" : ",").append(REASONS[i]).append('=').append(FALLBACK[i]); }
        }
        return b.length() == 0 ? "none" : b.toString();
    }

    public static boolean programReady() { return program != 0 && !failed; }

    private static boolean fallback(int reason) {
        FALLBACK[reason]++;
        return false;
    }

    private static int buildProgram() {
        String vertex = testProgramFault == 1 ? VERTEX + "\nrwTestCompileFault" : VERTEX;
        if (testProgramFault == 4) {
            vertex = vertex.replace(PRECISE_POSITION, "    gl_Position = rwClips[index] * vec4(gl_Vertex.xyz, 1.0);");
        }
        String fragment = testProgramFault == 2 ? "#version 120\nvoid rwTestNoMain() { }" : FRAGMENT;
        int vs = shader(GL20.GL_VERTEX_SHADER, vertex), fs = vs == 0 ? 0 : shader(GL20.GL_FRAGMENT_SHADER, fragment);
        if (fs == 0) { if (vs != 0) { GL20.glDeleteShader(vs); } return 0; }
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs); GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        GL20.glDetachShader(p, vs); GL20.glDetachShader(p, fs);
        GL20.glDeleteShader(vs); GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.out.println("RWBATCH program link failed: " + GL20.glGetProgramInfoLog(p, 4096));
            GL20.glDeleteProgram(p);
            return 0;
        }
        locParts = GL20.glGetUniformLocation(p, "rwParts");
        locClips = GL20.glGetUniformLocation(p, "rwClips");
        locNormal = GL20.glGetUniformLocation(p, "rwNormalMode");
        locLighting = GL20.glGetUniformLocation(p, "rwLighting");
        locLight0 = GL20.glGetUniformLocation(p, "rwLight0");
        locLight1 = GL20.glGetUniformLocation(p, "rwLight1");
        locRadial = GL20.glGetUniformLocation(p, "rwFogRadial");
        locUv = GL20.glGetUniformLocation(p, "rwUvShift");
        locLightmap = GL20.glGetUniformLocation(p, "rwLightmapOn");
        locFog = GL20.glGetUniformLocation(p, "rwFogMode");
        GL20.glUseProgram(p);
        GL20.glUniform1i(GL20.glGetUniformLocation(p, "rwTex"), 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(p, "rwLightmap"), 1);
        GL20.glUseProgram(0);
        // Unknown uniform values: the first draw sets every one.
        vNormal = vLighting = vLight0 = vLight1 = vRadial = vLightmap = vFog = -1;
        vUv = Float.NaN;
        programs++;
        return p;
    }

    private static int shader(int type, String source) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, source);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.out.println("RWBATCH shader compile failed: " + GL20.glGetShaderInfoLog(s, 4096));
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    private static void install() {
        installed = true;
        try {
            fCurrent = field(Class.forName("net.minecraft.client.renderer.GlStateManager$BooleanState"), "currentState", "field_179201_b");
            sLighting = staticField("lightingState", "field_179158_b");
            sLights = (Object[]) staticField("lightState", "field_179159_c");
            sColorMaterial = staticField("colorMaterialState", "field_179156_d");
            sColorMaterialFlag = field(sColorMaterial.getClass(), "colorMaterial", "field_179191_a").get(sColorMaterial);
            fCmFace = field(sColorMaterial.getClass(), "face", "field_179189_b");
            fCmMode = field(sColorMaterial.getClass(), "mode", "field_179190_c");
            sFog = staticField("fogState", "field_179155_g");
            sFogFlag = field(sFog.getClass(), "fog", "field_179049_a").get(sFog);
            fFogMode = field(sFog.getClass(), "mode", "field_179047_b");
            sNormalize = staticField("normalizeState", "field_179161_n");
            sRescale = staticField("rescaleNormalState", "field_179172_r");
            fActiveUnit = field(GlStateManager.class, "activeTextureUnit", "field_179162_o");
            Object[] textures = (Object[]) staticField("textureState", "field_179174_p");
            sTextures = new Object[textures.length];
            for (int i = 0; i < textures.length; i++) {
                sTextures[i] = field(textures[i].getClass(), "texture2DState", "field_179060_a").get(textures[i]);
            }
            Object texGen = staticField("texGenState", "field_179166_k");
            String[][] coords = {{"s", "field_179064_a"}, {"t", "field_179062_b"}, {"r", "field_179063_c"}, {"q", "field_179061_d"}};
            sTexGen = new Object[coords.length];
            for (int i = 0; i < coords.length; i++) {
                Object coord = field(texGen.getClass(), coords[i][0], coords[i][1]).get(texGen);
                sTexGen[i] = field(coord.getClass(), "textureGen", "field_179067_a").get(coord);
            }
            reflected = sLights.length >= 2 && sTextures.length >= 2;
        } catch (Exception e) {
            System.out.println("RWBATCH GlStateManager mirror unavailable: " + e);
            reflected = false;
        }
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(manager -> resetResources());
        MinecraftForge.EVENT_BUS.register(new Listener());
    }

    private static Field field(Class<?> owner, String dev, String srg) {
        Field f = ReflectionHelper.findField(owner, dev, srg);
        f.setAccessible(true);
        return f;
    }

    private static Object staticField(String dev, String srg) throws IllegalAccessException {
        return field(GlStateManager.class, dev, srg).get(null);
    }

    /** The world pass is from the render tick start to RenderWorldLastEvent; GUI comes later. */
    public static final class Listener {
        @SubscribeEvent
        public void unload(WorldEvent.Unload event) {
            if (event.getWorld().isRemote) { resetResources(); }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void renderTick(TickEvent.RenderTickEvent event) {
            worldPass = event.phase == TickEvent.Phase.START;
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void worldLast(RenderWorldLastEvent event) {
            worldPass = false;
        }
    }

    private static final class Entry {
        final WeakReference<ModelBase> owner;
        final ModelRenderer[] fields;
        final float scale;
        PartGeometry[] geometry;
        NpcBatchMesh mesh;
        int vao, vbo, checked = Integer.MIN_VALUE;
        Object context;

        Entry(ModelBase model, ModelRenderer[] fields, float scale) {
            this.owner = new WeakReference<ModelBase>(model);
            this.fields = fields;
            this.scale = scale;
            capture();
        }

        /** Frozen part geometry and the combined mesh; null mesh = this model stays original. */
        void capture() {
            this.mesh = null;
            PartGeometry[] g = new PartGeometry[PARTS];
            for (int i = 0; i < PARTS; i++) {
                if (fields[i] == null || fields[i].getClass() != ModelScaleRenderer.class) { return; }
                g[i] = PartGeometry.capture((ModelScaleRenderer) fields[i], scale);
                if (g[i] == null) { return; }
            }
            this.geometry = g;
            this.mesh = NpcBatchMesh.build(g);
        }
    }

    // On the measured driver the fixed transform accumulates x, y, z, w, while
    // matrix-vector multiplication starts with y then fuses x: the 1-ULP x displacement changes
    // raster setup of a narrow face and exceeded Q475 depth by 6.9e-6 (Q522).
    // precise prevents reassociation; explicit fma retains fixed-path fused rounding.
    private static final String PRECISE_POSITION = String.join("\n",
        "    mat4 clip = rwClips[index];",
        "    precise vec4 position = clip[0] * gl_Vertex.x;",
        "    position = fma(clip[1], vec4(gl_Vertex.y), position);",
        "    position = fma(clip[2], vec4(gl_Vertex.z), position);",
        "    position = fma(clip[3], vec4(1.0), position);",
        "    gl_Position = position;");

    private static final String VERTEX = String.join("\n",
        "#version 150 compatibility",
        "#extension GL_ARB_gpu_shader5 : require",
        "invariant gl_Position;",
        "uniform mat4 rwParts[12];",
        "uniform mat4 rwClips[12];",
        "uniform int rwNormalMode;",
        "uniform bool rwLighting;",
        "uniform bool rwLight0;",
        "uniform bool rwLight1;",
        "uniform bool rwFogRadial;",
        "uniform vec2 rwUvShift;",
        "varying vec2 rwUv;",
        "varying vec2 rwLm;",
        "varying float rwFogDist;",
        "vec4 rwLight(gl_LightSourceParameters s, vec3 n, vec3 eye, vec4 c) {",
        "    vec3 l;",
        "    float att = 1.0;",
        "    if (s.position.w == 0.0) {",
        "        l = normalize(s.position.xyz);",
        "    } else {",
        "        vec3 d = s.position.xyz - eye;",
        "        float dist = length(d);",
        "        l = d / dist;",
        "        att = 1.0 / (s.constantAttenuation + s.linearAttenuation * dist + s.quadraticAttenuation * dist * dist);",
        "        if (s.spotCutoff != 180.0) {",
        "            float sd = dot(-l, normalize(s.spotDirection));",
        "            att *= sd >= s.spotCosCutoff ? pow(max(sd, 0.0), s.spotExponent) : 0.0;",
        "        }",
        "    }",
        "    float nl = max(dot(n, l), 0.0);",
        "    vec4 r = s.ambient * c + nl * s.diffuse * c;",
        "    if (nl > 0.0 && any(greaterThan(gl_FrontMaterial.specular.rgb, vec3(0.0)))) {",
        "        vec3 h = normalize(l + vec3(0.0, 0.0, 1.0));",
        "        r += pow(max(dot(n, h), 0.0), gl_FrontMaterial.shininess) * s.specular * gl_FrontMaterial.specular;",
        "    }",
        "    return att * r;",
        "}",
        "void main() {",
        "    int index = int(gl_Vertex.w + 0.5);",
        "    mat4 part = rwParts[index];",
        "    vec4 eye = part * vec4(gl_Vertex.xyz, 1.0);",
        PRECISE_POSITION,
        "    vec4 c = gl_Color;",
        "    if (rwLighting) {",
        // Inverse transpose of the complete model-view 3x3 by cofactors.
        "        vec3 a0 = part[0].xyz, a1 = part[1].xyz, a2 = part[2].xyz;",
        "        vec3 c0 = cross(a1, a2), c1 = cross(a2, a0), c2 = cross(a0, a1);",
        "        mat3 inv = mat3(c0, c1, c2) / dot(a0, c0);",
        "        vec3 n = inv * gl_Normal;",
        "        if (rwNormalMode == 2) {",
        "            n = normalize(n);",
        "        } else if (rwNormalMode == 1) {",
        // GL_RESCALE_NORMAL: f = 1 / |third row of the inverse model-view 3x3|.
        "            vec3 row = inv[2];",
        "            n *= inversesqrt(dot(row, row));",
        "        }",
        "        vec4 lit = gl_FrontMaterial.emission + gl_LightModel.ambient * c;",
        "        if (rwLight0) { lit += rwLight(gl_LightSource[0], n, eye.xyz, c); }",
        "        if (rwLight1) { lit += rwLight(gl_LightSource[1], n, eye.xyz, c); }",
        "        c = clamp(vec4(lit.rgb, c.a), 0.0, 1.0);",
        "    }",
        "    gl_FrontColor = c;",
        "    gl_BackColor = c;",
        "    rwUv = (gl_TextureMatrix[0] * vec4(gl_MultiTexCoord0.st, 0.0, 1.0)).st + rwUvShift;",
        "    rwLm = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;",
        "    rwFogDist = rwFogRadial ? length(eye.xyz) : abs(eye.z);",
        "}");

    private static final String FRAGMENT = String.join("\n",
        "#version 150 compatibility",
        "uniform sampler2D rwTex;",
        "uniform sampler2D rwLightmap;",
        "uniform bool rwLightmapOn;",
        "uniform int rwFogMode;",
        "varying vec2 rwUv;",
        "varying vec2 rwLm;",
        "varying float rwFogDist;",
        "void main() {",
        "    vec4 c = gl_Color * texture2D(rwTex, rwUv);",
        "    if (rwLightmapOn) { c *= texture2D(rwLightmap, rwLm); }",
        "    if (rwFogMode != 0) {",
        "        float f;",
        "        if (rwFogMode == 1) { f = (gl_Fog.end - rwFogDist) * gl_Fog.scale; }",
        "        else if (rwFogMode == 2) { f = exp(-gl_Fog.density * rwFogDist); }",
        "        else { float e = gl_Fog.density * rwFogDist; f = exp(-e * e); }",
        "        c.rgb = mix(gl_Fog.color.rgb, c.rgb, clamp(f, 0.0, 1.0));",
        "    }",
        "    gl_FragColor = c;",
        "}");
}
