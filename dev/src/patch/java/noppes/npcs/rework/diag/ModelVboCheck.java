package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.client.model.ModelPlayerAlt;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.CustomNpcs;
import noppes.npcs.rework.render.ModelVbo;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** M14.1b: explicitly requested, untimed GPU characterization of the VBO part boundary.
 * No world, network or renderer replacement. Production counters prove actual VBO draws. The original mapped
 * ModelScaleRenderer is defined in an isolated class loader; every other class is shared.
 * A(original)/B(patch)/A2(original) execute in one render callback with frozen inputs.
 */
@SideOnly(Side.CLIENT)
public final class ModelVboCheck {
    private static final int W = 256, H = 256;
    private static final float SCALE = 1f / 16;
    private static final String CLASS = "noppes.npcs.client.model.ModelScaleRenderer";
    private final Properties job;
    private final JsonArray rows = new JsonArray();
    private final JsonArray shots = new JsonArray();
    private final List<ModelRenderer> owned = new ArrayList<ModelRenderer>();
    private boolean ran;
    private ModelBase fixture;
    private boolean authorize = true;

    private ModelVboCheck(Properties job) { this.job = job; }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("modelVboCheck"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("mappedJar")) { throw new IllegalArgumentException("Incomplete model baseline job"); }
        MinecraftForge.EVENT_BUS.register(new ModelVboCheck(p));
        return true;
    }

    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (ran || event.phase != TickEvent.Phase.END || !(mc.currentScreen instanceof GuiMainMenu)) { return; }
        ran = true;
        JsonObject proof = new JsonObject();
        try {
            run(mc, proof);
            proof.addProperty("success", true);
        } catch (Throwable error) {
            error.printStackTrace();
            proof.addProperty("success", false);
            proof.addProperty("error", error.toString());
        } finally {
            proof.add("rows", rows); proof.add("screenshots", shots);
            try (Writer out = Files.newBufferedWriter(new File(job.getProperty("out")).toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, out);
            } catch (IOException error) { error.printStackTrace(); }
            System.out.println("M141B DONE success=" + proof.get("success"));
            mc.shutdown();
        }
    }

    private void run(Minecraft mc, JsonObject proof) throws Exception {
        int oldSwitch = CustomNpcs.RwModelVbo;
        CustomNpcs.RwModelVbo = 1;
        require(OpenGlHelper.isFramebufferEnabled(), "Framebuffer required");
        require(GL11.glGetError() == GL11.GL_NO_ERROR, "Clean initial GL error state");
        proof.addProperty("renderer", GL11.glGetString(GL11.GL_RENDERER));
        proof.addProperty("vendor", GL11.glGetString(GL11.GL_VENDOR));
        proof.addProperty("glVersion", GL11.glGetString(GL11.GL_VERSION));
        proof.addProperty("width", W); proof.addProperty("height", H);
        proof.addProperty("scope", "isolated ordinary CNPC model parts, no full NPC/layers/world or timing");
        Class<?> original = originalClass();
        require(original != ModelScaleRenderer.class && original.getClassLoader() != ModelScaleRenderer.class.getClassLoader(), "Independent original class");
        proof.addProperty("oracleClassLoaderIndependent", true);
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int clientActive = GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        require(active == OpenGlHelper.defaultTexUnit && clientActive == OpenGlHelper.defaultTexUnit, "Menu texture units at default");
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        FloatBuffer clearColor = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int program = GLContext.getCapabilities().OpenGL20 ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        Framebuffer fb = null;
        DynamicTexture texture = null;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        try {
            if (GLContext.getCapabilities().OpenGL20) { GL20.glUseProgram(0); }
            texture = new DynamicTexture(64, 64);
            int[] pixels = texture.getTextureData();
            for (int y = 0; y < 64; y++) { for (int x = 0; x < 64; x++) {
                pixels[y * 64 + x] = 0xff000000 | (x * 4) << 16 | (y * 4) << 8 | (((x + y) % 2 == 0) ? 0x33 : 0xcc);
            } }
            texture.updateDynamicTexture();
            fb = new Framebuffer(W, H, true);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            ModelBiped[] models = {new ModelPlayerAlt(0, false), new ModelPlayerAlt(0, true), new ModelBipedAlt(0)};
            int parts = 0, calls = 0;
            for (int m = 0; m < models.length; m++) {
                fixture = models[m];
                List<ModelScaleRenderer> list = parts(models[m]);
                require(list.size() == (m < 2 ? 12 : 7), "Exact model part count " + m);
                parts += list.size();
                for (int p = 0; p < list.size(); p++) {
                    ModelScaleRenderer patched = list.get(p);
                    owned.add(patched);
                    ModelRenderer oracle = (ModelRenderer) original.getConstructor(ModelBase.class, EnumParts.class)
                            .newInstance(new ModelBase() {}, patched.part);
                    owned.add(oracle);
                    oracle.cubeList = patched.cubeList; // immutable fixture geometry, no re-encoding of UV/normals
                    oracle.textureWidth = patched.textureWidth; oracle.textureHeight = patched.textureHeight;
                    require(patched.cubeList.size() == 1, "One original cube per fixture part");
                    for (int pose = 0; pose < 3; pose++) {
                        configure(patched, pose); configure(oracle, pose);
                        // Warm both the class's own list and vanilla postRender's separate inherited list.
                        pass(fb, texture, oracle); pass(fb, texture, patched);
                        Shot a = pass(fb, texture, oracle);
                        CustomNpcs.RwModelVbo = 0;
                        Shot off = pass(fb, texture, patched);
                        CustomNpcs.RwModelVbo = 1;
                        Shot b = pass(fb, texture, patched);
                        Shot a2 = pass(fb, texture, oracle);
                        String key = "model" + m + "-part" + p + "-pose" + pose;
                        JsonObject row = compare(key, a, b, a2);
                        row.addProperty("offEqual", samePixels(a, off) && a.state.equals(off.state));
                        row.addProperty("vboDraws", b.vboDraws);
                        row.addProperty("cubes", patched.cubeList.size());
                        row.addProperty("part", patched.part.name());
                        rows.add(row); // Keep the exact failed comparison in the proof as well.
                        require(patched.isCompiled && GL11.glIsList(patched.displayList), "Compiled patch list " + key);
                        require(original.getField("isCompiled").getBoolean(oracle)
                                && GL11.glIsList(original.getField("displayList").getInt(oracle)), "Compiled original list " + key);
                        row.addProperty("listsValid", true);
                        // Every individual part must produce fragments: no empty/dropped model can pass.
                        require(a.covered > 0, "Visible geometry " + key);
                        require(row.get("equal").getAsBoolean(), "RGBA/depth/state comparison " + key);
                        require(row.get("offEqual").getAsBoolean() && off.vboDraws == 0, "Original at zero " + key);
                        require(b.vboDraws == 1 && a.vboDraws == 0 && a2.vboDraws == 0, "One real VBO draw " + key);
                        calls++;
                        if (pose == 1 && p < 2) { png(b, key); }
                        if (m == 0 && p == 0 && pose == 0) {
                            patched.rotateAngleY += 0.73f;
                            Shot wrongPose = pass(fb, texture, patched);
                            require(!samePixels(a, wrongPose), "Wrong pose must be rejected");
                            proof.addProperty("wrongPoseRejected", true);
                            configure(patched, pose);
                            Shot wrongUv = pass(fb, texture, patched, 0.25f);
                            require(!samePixels(a, wrongUv), "Wrong UV must be rejected");
                            proof.addProperty("wrongUvRejected", true);
                            require(samePixels(a, pass(fb, texture, patched)), "Sensitivity controls restore baseline");
                            // Out-of-context drawing and mutable geometry must really use the old list.
                            authorize = false;
                            Shot outside = pass(fb, texture, patched);
                            authorize = true;
                            require(outside.vboDraws == 0 && samePixels(a, outside) && a.state.equals(outside.state), "Outside context fallback");
                            proof.addProperty("outsideFallback", true);
                            patched.mirror = !patched.mirror;
                            Shot changed = pass(fb, texture, patched);
                            require(changed.vboDraws == 0 && samePixels(a, changed) && a.state.equals(changed.state), "Mutation uses frozen original list");
                            patched.mirror = !patched.mirror;
                            proof.addProperty("mutationFallback", true);
                            Shot restored = pass(fb, texture, patched);
                            require(restored.vboDraws == 1 && samePixels(a, restored) && ModelVbo.live() > 0, "Mutation restore rebuilds VBO");
                            long uploads = ModelVbo.uploads;
                            long deletes = ModelVbo.deletes;
                            mc.refreshResources(); // Invoke the real resource-manager listener, not a test reset hook.
                            require(ModelVbo.live() == 0 && ModelVbo.deletes > deletes, "Reload released every VBO");
                            Shot reloadA = pass(fb, texture, oracle);
                            Shot reloaded = pass(fb, texture, patched);
                            Shot reloadA2 = pass(fb, texture, oracle);
                            require(ModelVbo.uploads == uploads + 1 && reloaded.vboDraws == 1
                                    && samePixels(a, reloadA) && samePixels(reloadA, reloaded) && samePixels(reloadA, reloadA2)
                                    && reloadA.state.equals(reloaded.state) && reloadA.state.equals(reloadA2.state),
                                    "Reload rebuilds exact frozen geometry");
                            proof.addProperty("reloadEqual", true);
                            int testProgram = program();
                            try {
                                GL20.glUseProgram(testProgram);
                                Shot shaderA = pass(fb, texture, oracle);
                                Shot shaderB = pass(fb, texture, patched);
                                require(shaderB.vboDraws == 0 && shaderA.covered > 0 && samePixels(shaderA, shaderB)
                                        && shaderA.state.equals(shaderB.state), "Active program uses original list");
                                proof.addProperty("programFallback", true);
                            } finally { GL20.glUseProgram(0); GL20.glDeleteProgram(testProgram); }
                        }
                    }
                }
            }
            require(parts == 31 && calls == 93, "Expected 31 real parts / 93 comparisons");
            proof.addProperty("modelFixtures", models.length);
            proof.addProperty("distinctParts", parts); proof.addProperty("comparisons", calls);
            proof.addProperty("nonemptyPartDraws", calls * 3);
            proof.addProperty("vboDraws", ModelVbo.draws);
            proof.addProperty("uploads", ModelVbo.uploads);
            proof.add("fallbacks", new GsonBuilder().create().toJsonTree(ModelVbo.fallbacks()));
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "No GL errors");
            proof.addProperty("glErrors", 0);
        } finally {
            ModelVbo.reset();
            proof.addProperty("liveAfterRelease", ModelVbo.live());
            proof.addProperty("deletes", ModelVbo.deletes);
            CustomNpcs.RwModelVbo = oldSwitch;
            // These lists exist only in the diagnostic fixtures; never delete live game model lists.
            for (ModelRenderer part : owned) {
                Field ownList = part.getClass().getField("displayList");
                int id = ownList.getInt(part);
                if (id != 0) { GL11.glDeleteLists(id, 1); }
                int inherited = ((Integer) ObfuscationReflectionHelper.getPrivateValue(ModelRenderer.class, part, 11)).intValue();
                if (inherited != 0 && inherited != id) { GL11.glDeleteLists(inherited, 1); }
            }
            if (texture != null) { texture.deleteGlTexture(); }
            if (fb != null) { fb.deleteFramebuffer(); }
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPopMatrix();
            GL11.glPopClientAttrib(); GL11.glPopAttrib();
            GL11.glMatrixMode(matrixMode);
            if (GLContext.getCapabilities().OpenGL20) { GL20.glUseProgram(program); }
            // Raw diagnostic bindings must not leave GlStateManager's texture cache out of sync.
            GlStateManager.setActiveTexture(active);
            GlStateManager.bindTexture(0); GlStateManager.bindTexture(binding);
            // Framebuffer's constructor uses these cached setters inside the raw attribute guard.
            if (depthTest) { GlStateManager.enableDepth(); } else { GlStateManager.disableDepth(); }
            GlStateManager.depthMask(depthMask);
            GlStateManager.clearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
            GlStateManager.clearDepth(clearDepth);
            mc.getFramebuffer().bindFramebuffer(true);
        }
    }

    private Class<?> originalClass() throws Exception {
        byte[] bytes;
        try (ZipFile jar = new ZipFile(job.getProperty("mappedJar"));
             InputStream in = jar.getInputStream(jar.getEntry(CLASS.replace('.', '/') + ".class"));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int n;
            while ((n = in.read(buffer)) >= 0) { out.write(buffer, 0, n); }
            bytes = out.toByteArray();
        }
        final byte[] original = bytes;
        return new ClassLoader(ModelVboCheck.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!CLASS.equals(name)) { return super.loadClass(name, resolve); }
                Class<?> found = findLoadedClass(name);
                if (found == null) { found = defineClass(name, original, 0, original.length); }
                if (resolve) { resolveClass(found); }
                return found;
            }
        }.loadClass(CLASS);
    }

    private static List<ModelScaleRenderer> parts(ModelBiped model) {
        List<ModelRenderer> list = new ArrayList<ModelRenderer>(Arrays.asList(model.bipedHead, model.bipedBody,
                model.bipedLeftArm, model.bipedRightArm, model.bipedLeftLeg, model.bipedRightLeg, model.bipedHeadwear));
        if (model instanceof ModelPlayer) {
            ModelPlayer p = (ModelPlayer) model;
            list.addAll(Arrays.asList(p.bipedBodyWear, p.bipedLeftArmwear, p.bipedRightArmwear, p.bipedLeftLegwear, p.bipedRightLegwear));
        }
        List<ModelScaleRenderer> result = new ArrayList<ModelScaleRenderer>();
        for (ModelRenderer part : list) { result.add((ModelScaleRenderer) part); }
        return result;
    }

    private static int program() {
        require(GLContext.getCapabilities().OpenGL20, "Program fallback test needs GL2");
        int program = GL20.glCreateProgram();
        int vertex = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        int fragment = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        boolean linked = false;
        try {
            GL20.glShaderSource(vertex, "#version 120\nvoid main(){gl_Position=ftransform();gl_TexCoord[0]=gl_MultiTexCoord0;gl_FrontColor=gl_Color;}");
            GL20.glShaderSource(fragment, "#version 120\nuniform sampler2D skin;void main(){gl_FragColor=texture2D(skin,gl_TexCoord[0].xy)*gl_Color;}");
            GL20.glCompileShader(vertex); GL20.glCompileShader(fragment);
            require(GL20.glGetShaderi(vertex, GL20.GL_COMPILE_STATUS) != 0, "Test vertex shader compiles");
            require(GL20.glGetShaderi(fragment, GL20.GL_COMPILE_STATUS) != 0, "Test fragment shader compiles");
            GL20.glAttachShader(program, vertex); GL20.glAttachShader(program, fragment); GL20.glLinkProgram(program);
            require(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != 0, "Test program links");
            linked = true; return program;
        } finally {
            GL20.glDeleteShader(vertex); GL20.glDeleteShader(fragment);
            if (!linked) { GL20.glDeleteProgram(program); }
        }
    }

    private static void configure(ModelRenderer part, int pose) throws Exception {
        part.rotationPointX = 1; part.rotationPointY = -2; part.rotationPointZ = 3;
        part.rotateAngleX = pose == 0 ? 0 : 0.31f * pose;
        part.rotateAngleY = pose == 0 ? 0 : -0.47f * pose;
        part.rotateAngleZ = pose == 0 ? 0 : 0.19f * pose;
        ModelPartConfig config = null;
        if (pose == 2) {
            config = new ModelPartConfig();
            config.scaleX = 0.65f; config.scaleY = 1.4f; config.scaleZ = 1.15f;
            config.transX = 0.13f; config.transY = -0.17f; config.transZ = 0.09f;
        }
        part.getClass().getField("config").set(part, config);
    }

    private Shot pass(Framebuffer fb, DynamicTexture texture, ModelRenderer part) {
        return pass(fb, texture, part, 0);
    }

    private Shot pass(Framebuffer fb, DynamicTexture texture, ModelRenderer part, float uvShift) {
        fb.bindFramebuffer(true);
        GL11.glClearColor(0, 0, 0, 0); GL11.glClearDepth(1);
        GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_LEQUAL); GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
        GL11.glDisable(GL11.GL_SCISSOR_TEST); GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG); GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
        GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glLoadIdentity(); GL11.glTranslatef(uvShift, 0, 0);
        GL11.glColor4f(1, 1, 1, 1); GL11.glNormal3f(0, 0, 1); GL11.glTexCoord2f(0, 0);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(-1.3, 1.3, -1.3, 1.3, -8, 8);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
        GL11.glRotatef(17, 1, 0, 0); GL11.glRotatef(-23, 0, 1, 0);
        GL11.glTranslatef(0, -0.2f, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        String matrix = floats(GL11.GL_MODELVIEW_MATRIX, 16);
        long draws = ModelVbo.draws;
        ModelVbo.Scope scope = authorize ? ModelVbo.beginParts(fixture) : null;
        try { part.render(SCALE); }
        finally { if (scope != null) { scope.close(); } }
        require(matrix.equals(floats(GL11.GL_MODELVIEW_MATRIX, 16)), "Part restores modelview matrix");
        Shot shot = new Shot();
        shot.vboDraws = ModelVbo.draws - draws;
        shot.state = state();
        ByteBuffer rgba = BufferUtils.createByteBuffer(W * H * 4);
        FloatBuffer depth = BufferUtils.createFloatBuffer(W * H);
        GL11.glReadPixels(0, 0, W, H, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);
        GL11.glReadPixels(0, 0, W, H, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
        shot.rgba = new byte[W * H * 4]; rgba.get(shot.rgba);
        shot.depth = new int[W * H];
        for (int i = 0; i < shot.depth.length; i++) {
            shot.depth[i] = Float.floatToRawIntBits(depth.get(i));
            if (depth.get(i) < 1) { shot.covered++; }
        }
        require(GL11.glGetError() == GL11.GL_NO_ERROR, "Clean GL pass");
        return shot;
    }

    private static Map<String, String> state() {
        Map<String, String> state = new TreeMap<String, String>();
        for (int key : new int[] {GL11.GL_MATRIX_MODE, GL11.GL_MODELVIEW_STACK_DEPTH, GL11.GL_PROJECTION_STACK_DEPTH,
                GL11.GL_DEPTH_FUNC, GL11.GL_DEPTH_WRITEMASK, GL11.GL_TEXTURE_BINDING_2D,
                GL13.GL_ACTIVE_TEXTURE, GL13.GL_CLIENT_ACTIVE_TEXTURE,
                GL11.GL_VERTEX_ARRAY_SIZE, GL11.GL_VERTEX_ARRAY_TYPE, GL11.GL_VERTEX_ARRAY_STRIDE,
                GL11.GL_NORMAL_ARRAY_TYPE, GL11.GL_NORMAL_ARRAY_STRIDE,
                GL11.GL_TEXTURE_COORD_ARRAY_SIZE, GL11.GL_TEXTURE_COORD_ARRAY_TYPE, GL11.GL_TEXTURE_COORD_ARRAY_STRIDE}) {
            state.put("int-" + key, Integer.toString(GL11.glGetInteger(key)));
        }
        if (GLContext.getCapabilities().OpenGL15) {
            state.put("arrayBuffer", Integer.toString(GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)));
            state.put("elementBuffer", Integer.toString(GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING)));
        }
        if (GLContext.getCapabilities().OpenGL20) { state.put("program", Integer.toString(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM))); }
        for (int key : new int[] {GL11.GL_DEPTH_TEST, GL11.GL_BLEND, GL11.GL_ALPHA_TEST, GL11.GL_CULL_FACE,
                GL11.GL_LIGHTING, GL11.GL_FOG, GL11.GL_TEXTURE_2D, GL11.GL_VERTEX_ARRAY, GL11.GL_NORMAL_ARRAY,
                GL11.GL_COLOR_ARRAY, GL11.GL_TEXTURE_COORD_ARRAY}) { state.put("enable-" + key, Boolean.toString(GL11.glIsEnabled(key))); }
        state.put("modelview", floats(GL11.GL_MODELVIEW_MATRIX, 16));
        state.put("projection", floats(GL11.GL_PROJECTION_MATRIX, 16));
        state.put("textureMatrix", floats(GL11.GL_TEXTURE_MATRIX, 16));
        state.put("currentColor", floats(GL11.GL_CURRENT_COLOR, 4));
        state.put("currentNormal", floats(GL11.GL_CURRENT_NORMAL, 3));
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        for (int unit : new int[] {OpenGlHelper.defaultTexUnit, OpenGlHelper.lightmapTexUnit}) {
            GL13.glActiveTexture(unit);
            state.put("uv-" + unit, floats(GL11.GL_CURRENT_TEXTURE_COORDS, 4));
            state.put("texture-" + unit, Integer.toString(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)));
            state.put("textureEnabled-" + unit, Boolean.toString(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)));
        }
        GL13.glActiveTexture(active);
        return state;
    }

    private static String floats(int key, int length) {
        FloatBuffer values = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(key, values);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < length; i++) { out.append(Integer.toHexString(Float.floatToRawIntBits(values.get(i)))).append(','); }
        return out.toString();
    }

    private static JsonObject compare(String key, Shot a, Shot b, Shot a2) throws Exception {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("covered", a.covered);
        row.addProperty("repeatEqual", samePixels(a, a2) && a.state.equals(a2.state));
        row.addProperty("rgbaEqual", Arrays.equals(a.rgba, b.rgba));
        row.addProperty("depthEqual", Arrays.equals(a.depth, b.depth));
        row.addProperty("stateEqual", a.state.equals(b.state));
        row.add("originalState", new GsonBuilder().create().toJsonTree(a.state));
        row.add("patchState", new GsonBuilder().create().toJsonTree(b.state));
        row.addProperty("rgbaSha256", hex(MessageDigest.getInstance("SHA-256").digest(a.rgba)));
        ByteBuffer depth = ByteBuffer.allocate(a.depth.length * 4);
        for (int n : a.depth) { depth.putInt(n); }
        row.addProperty("depthSha256", hex(MessageDigest.getInstance("SHA-256").digest(depth.array())));
        row.addProperty("equal", samePixels(a, b) && samePixels(a, a2) && a.state.equals(b.state) && a.state.equals(a2.state));
        return row;
    }

    private void png(Shot shot, String key) throws IOException {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < H; y++) { for (int x = 0; x < W; x++) {
            int at = ((H - 1 - y) * W + x) * 4;
            image.setRGB(x, y, (shot.rgba[at + 3] & 255) << 24 | (shot.rgba[at] & 255) << 16
                    | (shot.rgba[at + 1] & 255) << 8 | (shot.rgba[at + 2] & 255));
        } }
        File file = new File(job.getProperty("out") + "-" + key + ".png");
        require(ImageIO.write(image, "png", file), "PNG writer"); shots.add(file.getAbsolutePath());
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(); for (byte b : bytes) { out.append(String.format("%02x", b & 255)); } return out.toString();
    }
    private static boolean samePixels(Shot a, Shot b) { return Arrays.equals(a.rgba, b.rgba) && Arrays.equals(a.depth, b.depth); }
    private static void require(boolean ok, String text) { if (!ok) { throw new IllegalStateException(text); } }
    private static final class Shot {
        byte[] rgba; int[] depth; int covered; Map<String, String> state; long vboDraws;
    }
}
