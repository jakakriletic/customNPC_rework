package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.ModelPlayerAlt;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.render.NpcBatch;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** M14.3a controller-only native GL boundary and resource-lifetime proof. One real integrated
 * world/NPC supplies the ordinary main model; FBO A/B/A2 comparisons use frozen poses. This
 * deliberately does not claim whole-renderer, OptiFine, rig, layer or L2 coverage. No cost window.
 * Reflection observes production-owned GL names; it never forces the production decision. */
@SideOnly(Side.CLIENT)
public final class NpcBatchCompatHarness {
    private static final int W = 256, H = 256;
    private static final float SCALE = 1.0f / 16;
    private final Properties job;
    private final JsonArray checks = new JsonArray(), rows = new JsonArray();
    private final File out;
    private String phase = "MENU";
    private int delay, joins;
    private long since = System.nanoTime();
    private boolean finished, allOk = true;
    private volatile boolean serverDone;
    private volatile String serverError;
    private ModelPlayerAlt model;
    private EntityNPCInterface npc;
    private Object renderer;
    private List<int[]> resources;
    private int oldProgram;
    private long builds;

    private NpcBatchCompatHarness(Properties p) { job = p; out = new File(p.getProperty("out")); }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("npcBatchCompat"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete batch compatibility job"); }
        MinecraftForge.EVENT_BUS.register(new NpcBatchCompatHarness(p));
        return true;
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject c = new JsonObject(); c.addProperty("key", key); c.addProperty("ok", ok); c.addProperty("detail", detail);
        checks.add(c); allOk &= ok;
        System.out.println("M143A CHECK " + key + " ok=" + ok + " " + detail);
    }

    private void enter(String next, int wait) { phase = next; delay = wait; since = System.nanoTime(); }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (finished || e.phase != TickEvent.Phase.END) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (serverError != null) { throw new IllegalStateException(serverError); }
            if (System.nanoTime() - since > 180000000000L) { throw new IllegalStateException("Timeout " + phase); }
            if (delay-- > 0) { return; }
            switch (phase) {
                case "MENU":
                case "REJOIN":
                    if (!(mc.currentScreen instanceof GuiMainMenu)) { break; }
                    mc.gameSettings.pauseOnLostFocus = false;
                    CustomNpcs.RwNpcInstancing = 0;
                    joins++;
                    enter("JOIN", 20);
                    mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"),
                            new WorldSettings(HarnessJob.seedOf("customnpc-rework"), GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
                    break;
                case "JOIN":
                    if (mc.world == null || mc.player == null || mc.currentScreen != null) { break; }
                    serverDone = false;
                    final String player = mc.player.getName();
                    mc.getIntegratedServer().addScheduledTask(() -> {
                        try {
                            for (String c : new String[] {"gamerule doMobSpawning false", "gamerule doDaylightCycle false",
                                    "time set 6000", "tp " + player + " 0 6 -5 0 10"}) {
                                if (mc.getIntegratedServer().getCommandManager().executeCommand(mc.getIntegratedServer(), c) < 1) {
                                    throw new IllegalStateException(c);
                                }
                            }
                            if (joins == 1 && mc.getIntegratedServer().getCommandManager().executeCommand(mc.getIntegratedServer(),
                                    "summon customnpcs:customnpc 0 4 2 {Name:\"M143A\",MovingState:0,StandingState:1,Orientation:180}") < 1) {
                                throw new IllegalStateException("summon");
                            }
                        } catch (Exception problem) { serverError = problem.toString(); }
                        serverDone = true;
                    });
                    enter("FIND", 60);
                    break;
                case "FIND":
                    if (!serverDone) { break; }
                    npc = null;
                    for (Entity x : mc.world.loadedEntityList) {
                        if (x instanceof EntityNPCInterface && "M143A".equals(x.getName())) { npc = (EntityNPCInterface) x; }
                    }
                    if (npc == null) { break; }
                    renderer = mc.getRenderManager().getEntityRenderObject(npc);
                    model = new ModelPlayerAlt(0, false);
                    enter(joins == 1 ? "MATRIX" : "AFTER_REJOIN", 0);
                    break;
                case "RELOAD":
                    // Real synchronous resource reload while the optimization is OFF. It must
                    // retire its old IDs even if no candidate model is subsequently drawn.
                    resources = ownedNames(); oldProgram = program(); builds = NpcBatch.programs;
                    check("reload-has-live-resources", !resources.isEmpty() && oldProgram != 0, "meshes=" + resources.size() + " program=" + oldProgram);
                    mc.refreshResources();
                    enter("AFTER_RELOAD", 20);
                    break;
                case "AFTER_RELOAD":
                    retired("reload-off");
                    enter("REBUILD", 0);
                    break;
                case "LEAVE":
                    resources = ownedNames(); oldProgram = program();
                    check("unload-has-live-resources", !resources.isEmpty() && oldProgram != 0, "meshes=" + resources.size() + " program=" + oldProgram);
                    mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); mc.displayGuiScreen(new GuiMainMenu());
                    enter("AFTER_LEAVE", 20);
                    break;
                case "AFTER_LEAVE":
                    retired("world-unload-off");
                    model = null; npc = null; renderer = null;
                    enter("REJOIN", 20);
                    break;
                case "FINISH":
                    finish(mc, "");
                    break;
                default: break;
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, problem.toString()); }
    }

    @SubscribeEvent public void render(RenderWorldLastEvent e) {
        if (finished || !(phase.equals("MATRIX") || phase.equals("REBUILD") || phase.equals("AFTER_REJOIN"))) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer fb = null;
        int savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int savedBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int savedClient = GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        int savedUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int savedMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        SavedMirrors mirrors = new SavedMirrors();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
        for (int mode : new int[] {GL11.GL_PROJECTION, GL11.GL_MODELVIEW}) { GlStateManager.matrixMode(mode); GlStateManager.pushMatrix(); }
        try {
            if (!OpenGlHelper.isFramebufferEnabled()) { throw new IllegalStateException("FBO required"); }
            NpcBatch.checkScope = true;
            fb = new Framebuffer(W, H, true); fb.setFramebufferColor(0, 0, 0, 0);
            if ("1".equals(job.getProperty("wholeRenderer"))) {
                JsonObject scene = NpcBatchSceneCheck.run(mc, fb, (noppes.npcs.entity.EntityCustomNpc) npc,
                        out.getPath().replace(".proof.json", ""));
                for (com.google.gson.JsonElement c : scene.getAsJsonArray("checks")) { checks.add(c); }
                for (com.google.gson.JsonElement r : scene.getAsJsonArray("rows")) { rows.add(r); }
                allOk &= scene.get("success").getAsBoolean();
                enter("FINISH", 0);
                return;
            }
            // Original warmup compiles display lists before state/image comparisons. Candidate
            // warmup pins the production per-frame decision before testing a later program/VAO.
            shot(mc, fb, false, 0, 0);
            shot(mc, fb, true, 0, 0);
            if (phase.equals("MATRIX")) {
                trial(mc, fb, "ordinary", 0, 0, 1, -1, false);
                int vao = createVao();
                try { trial(mc, fb, "foreign-vao-same-frame", 0, vao, 0, 2, true); }
                finally { bindVao(0); deleteVao(vao); }
                int p = foreignProgram();
                try { trial(mc, fb, "foreign-program-same-frame", p, 0, 0, 4, true); }
                finally { GL20.glUseProgram(0); GL20.glDeleteProgram(p); }
                trial(mc, fb, "extra-light", 0, 0, 0, 2, true);
                trial(mc, fb, "extra-texture", 0, 0, 0, 2, true);
                ModelRenderer child = new ModelRenderer(model, 0, 0); child.addBox(0, 0, 0, 1, 1, 1);
                model.bipedHead.addChild(child);
                try { trial(mc, fb, "child-model", 0, 0, 0, 5, true); }
                finally { model.bipedHead.childModels.remove(child); }
                NpcBatch.checkScope = false;
                try { trial(mc, fb, "outside-world-pass", 0, 0, 0, 0, true); }
                finally { NpcBatch.checkScope = true; }
                trial(mc, fb, "ordinary-after-fallbacks", 0, 0, 1, -1, false);
                enter("RELOAD", 0);
            } else if (phase.equals("REBUILD")) {
                trial(mc, fb, "ordinary-after-reload", 0, 0, 1, -1, false);
                check("reload-program-rebuilt", NpcBatch.programs == builds + 1 && program() != 0 && NpcBatch.live() == 1,
                        "programs=" + builds + "->" + NpcBatch.programs + " live=" + NpcBatch.live());
                enter("LEAVE", 0);
            } else {
                trial(mc, fb, "ordinary-after-world-rejoin", 0, 0, 1, -1, false);
                check("rejoined-world", joins == 2 && NpcBatch.live() == 1 && program() != 0, "joins=" + joins + " live=" + NpcBatch.live());
                enter("FINISH", 0);
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, problem.toString()); }
        finally {
            NpcBatch.checkScope = false;
            GL20.glUseProgram(savedProgram); bindVao(savedVao); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedBuffer);
            GL13.glClientActiveTexture(savedClient);
            for (int mode : new int[] {GL11.GL_MODELVIEW, GL11.GL_PROJECTION}) { GlStateManager.matrixMode(mode); GlStateManager.popMatrix(); }
            GlStateManager.matrixMode(savedMode);
            // Raw glPopAttrib alone would restore the driver while leaving Minecraft's
            // mirror stale. Restore the exact incoming mirrored values first.
            mirrors.restore();
            GL11.glPopClientAttrib(); GL11.glPopAttrib(); GL13.glActiveTexture(savedUnit);
            mc.getFramebuffer().bindFramebuffer(true);
            if (fb != null) { fb.deleteFramebuffer(); }
        }
    }

    private String trial = "";
    private void trial(Minecraft mc, Framebuffer fb, String key, int foreign, int vao, long expected, int reason, boolean exact) throws Exception {
        trial = key;
        Shot a = shot(mc, fb, false, foreign, vao);
        long d = NpcBatch.draws; long[] f = NpcBatch.fallbacks();
        Shot b = shot(mc, fb, true, foreign, vao);
        long draws = NpcBatch.draws - d; long[] after = NpcBatch.fallbacks(); long fallbacks = 0;
        for (int i = 0; i < f.length; i++) { fallbacks += after[i] - f[i]; }
        Shot a2 = shot(mc, fb, false, foreign, vao);
        NpcBatchCheck.Cmp cmp = NpcBatchCheck.compare(a.color, a.depth, b.color, b.depth, W * H);
        boolean repeat = same(a, a2) && a.state.equals(a2.state);
        boolean state = a.state.equals(b.state);
        boolean image = exact ? same(a, b) && cmp.union > 0 : cmp.imageEqual();
        boolean counted = draws == expected && fallbacks == (expected == 1 ? 0 : 1) && (reason < 0 || after[reason] - f[reason] == 1);
        JsonObject row = new JsonObject(); row.addProperty("key", key); row.addProperty("exact", exact);
        row.addProperty("repeatEqual", repeat); row.addProperty("stateEqual", state); row.addProperty("imageEqual", image);
        row.addProperty("draws", draws); row.addProperty("fallbacks", fallbacks); row.addProperty("pixels", cmp.toString());
        row.add("originalState", new GsonBuilder().create().toJsonTree(a.state));
        row.add("candidateState", new GsonBuilder().create().toJsonTree(b.state)); rows.add(row);
        check(key, repeat && state && image && counted, "repeat=" + repeat + " state=" + state + " image=" + image + " draws=" + draws
                + " fallbacks=" + fallbacks + " reasons=" + NpcBatch.fallbackText() + " " + cmp);
        check(key + "-gl", GL11.glGetError() == GL11.GL_NO_ERROR, "GL_NO_ERROR");
    }

    private Shot shot(Minecraft mc, Framebuffer fb, boolean candidate, int p, int vao) {
        GL20.glUseProgram(0); bindVao(0); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        fb.framebufferClear(); fb.bindFramebuffer(true);
        GlStateManager.enableDepth(); GlStateManager.depthMask(true); GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.colorMask(true, true, true, true); GlStateManager.disableBlend();
        GlStateManager.disableCull(); GlStateManager.disableAlpha(); GlStateManager.disableLighting(); GlStateManager.disableFog();
        GlStateManager.disableRescaleNormal(); GlStateManager.disableNormalize();
        for (int l = 2; l < 8; l++) { GlStateManager.disableLight(l); }
        if (trial.equals("extra-light")) { GlStateManager.enableLight(2); }
        for (int u = 2; u >= 1; u--) {
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + u); GlStateManager.disableTexture2D();
            if (u == 2 && trial.equals("extra-texture")) { GlStateManager.enableTexture2D(); }
        }
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0); GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(new ResourceLocation("minecraft:textures/entity/steve.png"));
        GlStateManager.color(1, 1, 1, 1); GL11.glNormal3f(0, 0, 1); GL11.glTexCoord2f(0, 0);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity(); GL11.glOrtho(-1.4, 1.4, -1.4, 1.4, -8, 8);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity();
        GlStateManager.rotate(180, 0, 0, 1); GlStateManager.rotate(-23, 0, 1, 0); GlStateManager.translate(0, -0.5, 0);
        model.setRotationAngles(0, 0, 0, 17, -9, SCALE, npc);
        GL20.glUseProgram(p); bindVao(vao);
        if (!candidate || !NpcBatch.accepts(renderer, model, npc) || !NpcBatch.render(model, npc, SCALE)) {
            model.render(npc, 0, 0, 0, 17, -9, SCALE);
        }
        Shot shot = new Shot(); shot.state = state();
        shot.color = BufferUtils.createByteBuffer(W * H * 4); shot.depth = BufferUtils.createFloatBuffer(W * H);
        GL11.glReadPixels(0, 0, W, H, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, shot.color);
        GL11.glReadPixels(0, 0, W, H, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, shot.depth);
        GL20.glUseProgram(0); bindVao(0);
        return shot;
    }

    static Map<String, String> state() {
        Map<String, String> out = new TreeMap<String, String>();
        for (int key : new int[] {GL20.GL_CURRENT_PROGRAM, GL30.GL_VERTEX_ARRAY_BINDING, GL15.GL_ARRAY_BUFFER_BINDING,
                GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING, GL13.GL_ACTIVE_TEXTURE, GL13.GL_CLIENT_ACTIVE_TEXTURE,
                GL11.GL_MATRIX_MODE, GL11.GL_MODELVIEW_STACK_DEPTH, GL11.GL_PROJECTION_STACK_DEPTH,
                GL11.GL_DEPTH_FUNC, GL11.GL_DEPTH_WRITEMASK, GL11.GL_TEXTURE_BINDING_2D}) {
            out.put("int-" + key, Integer.toString(GL11.glGetInteger(key)));
        }
        for (int key : new int[] {GL11.GL_MODELVIEW_MATRIX, GL11.GL_PROJECTION_MATRIX, GL11.GL_CURRENT_NORMAL, GL11.GL_CURRENT_TEXTURE_COORDS}) {
            FloatBuffer b = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(key, b);
            StringBuilder s = new StringBuilder(); int n = key == GL11.GL_CURRENT_NORMAL ? 3 : key == GL11.GL_CURRENT_TEXTURE_COORDS ? 4 : 16;
            for (int i = 0; i < n; i++) { s.append(Integer.toHexString(Float.floatToRawIntBits(b.get(i)))).append(','); }
            out.put("float-" + key, s.toString());
        }
        return out;
    }

    private static boolean same(Shot a, Shot b) {
        for (int i = 0; i < W * H; i++) {
            if (a.color.getInt(i * 4) != b.color.getInt(i * 4)
                    || Float.floatToRawIntBits(a.depth.get(i)) != Float.floatToRawIntBits(b.depth.get(i))) { return false; }
        }
        return true;
    }

    private void retired(String key) throws Exception {
        boolean gone = oldProgram != 0 && !GL20.glIsProgram(oldProgram);
        for (int[] ids : resources) { gone &= !GL15.glIsBuffer(ids[0]) && !isVao(ids[1]); }
        check(key, gone && NpcBatch.live() == 0 && program() == 0,
                "oldNamesGone=" + gone + " live=" + NpcBatch.live() + " program=" + program());
        check(key + "-gl", GL11.glGetError() == GL11.GL_NO_ERROR, "GL_NO_ERROR");
    }

    private static Object field(Object owner, Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    private static int program() throws Exception { return (Integer) field(null, NpcBatch.class, "program"); }
    private static List<int[]> ownedNames() throws Exception {
        List<int[]> out = new ArrayList<int[]>();
        for (Object e : (List<?>) field(null, NpcBatch.class, "OWNED")) {
            int vbo = (Integer) field(e, e.getClass(), "vbo"), vao = (Integer) field(e, e.getClass(), "vao");
            if (vbo != 0 || vao != 0) { out.add(new int[] {vbo, vao}); }
        }
        return out;
    }
    private static boolean coreVao() { return GLContext.getCapabilities().OpenGL30; }
    private static int createVao() { return coreVao() ? GL30.glGenVertexArrays() : ARBVertexArrayObject.glGenVertexArrays(); }
    static void bindVao(int id) { if (coreVao()) { GL30.glBindVertexArray(id); } else { ARBVertexArrayObject.glBindVertexArray(id); } }
    private static void deleteVao(int id) { if (coreVao()) { GL30.glDeleteVertexArrays(id); } else { ARBVertexArrayObject.glDeleteVertexArrays(id); } }
    private static boolean isVao(int id) { return coreVao() ? GL30.glIsVertexArray(id) : ARBVertexArrayObject.glIsVertexArray(id); }
    private static int foreignProgram() {
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER), fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(vs, "#version 120\nvoid main(){gl_Position=ftransform();gl_FrontColor=gl_Color;}");
        GL20.glShaderSource(fs, "#version 120\nvoid main(){gl_FragColor=gl_Color;}");
        GL20.glCompileShader(vs); GL20.glCompileShader(fs);
        int p = GL20.glCreateProgram(); GL20.glAttachShader(p, vs); GL20.glAttachShader(p, fs); GL20.glLinkProgram(p);
        GL20.glDetachShader(p, vs); GL20.glDetachShader(p, fs); GL20.glDeleteShader(vs); GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0) { throw new IllegalStateException("Foreign program link: " + GL20.glGetProgramInfoLog(p, 4096)); }
        return p;
    }

    private void finish(Minecraft mc, String error) {
        finished = true;
        try {
            CustomNpcs.RwNpcInstancing = 0;
            JsonObject proof = new JsonObject(); proof.addProperty("success", allOk && error.isEmpty()); proof.addProperty("error", error);
            proof.addProperty("joins", joins); proof.addProperty("width", W); proof.addProperty("height", H);
            proof.add("checks", checks); proof.add("rows", rows);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer); }
            System.out.println("M143A DONE success=" + (allOk && error.isEmpty()));
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
    private static final class Shot { ByteBuffer color; FloatBuffer depth; Map<String, String> state; }

    private static final class SavedMirrors {
        final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND),
                cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                lighting = GL11.glIsEnabled(GL11.GL_LIGHTING), fog = GL11.glIsEnabled(GL11.GL_FOG),
                rescale = GL11.glIsEnabled(GL12_RESCALE_NORMAL), normalize = GL11.glIsEnabled(GL11.GL_NORMALIZE),
                depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC), unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final boolean[] lights = new boolean[8], textures = new boolean[3];
        final int[] bindings = new int[3];
        final FloatBuffer color = BufferUtils.createFloatBuffer(16), clear = BufferUtils.createFloatBuffer(16), clearDepth = BufferUtils.createFloatBuffer(16);
        final ByteBuffer mask = BufferUtils.createByteBuffer(16);
        private static final int GL12_RESCALE_NORMAL = 0x803A;

        SavedMirrors() {
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color); GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clear);
            GL11.glGetFloat(GL11.GL_DEPTH_CLEAR_VALUE, clearDepth); GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, mask);
            for (int i = 2; i < 8; i++) { lights[i] = GL11.glIsEnabled(GL11.GL_LIGHT0 + i); }
            for (int i = 0; i < 3; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                textures[i] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D); bindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GL13.glActiveTexture(unit);
        }

        void restore() {
            if (depth) { GlStateManager.enableDepth(); } else { GlStateManager.disableDepth(); }
            if (blend) { GlStateManager.enableBlend(); } else { GlStateManager.disableBlend(); }
            if (cull) { GlStateManager.enableCull(); } else { GlStateManager.disableCull(); }
            if (alpha) { GlStateManager.enableAlpha(); } else { GlStateManager.disableAlpha(); }
            if (lighting) { GlStateManager.enableLighting(); } else { GlStateManager.disableLighting(); }
            if (fog) { GlStateManager.enableFog(); } else { GlStateManager.disableFog(); }
            if (rescale) { GlStateManager.enableRescaleNormal(); } else { GlStateManager.disableRescaleNormal(); }
            if (normalize) { GlStateManager.enableNormalize(); } else { GlStateManager.disableNormalize(); }
            for (int i = 2; i < 8; i++) { if (lights[i]) { GlStateManager.enableLight(i); } else { GlStateManager.disableLight(i); } }
            for (int i = 0; i < 3; i++) {
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + i); GlStateManager.bindTexture(bindings[i]);
                if (textures[i]) { GlStateManager.enableTexture2D(); } else { GlStateManager.disableTexture2D(); }
            }
            GlStateManager.setActiveTexture(unit);
            GlStateManager.depthFunc(depthFunc); GlStateManager.depthMask(depthMask);
            GlStateManager.colorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
            GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
            GlStateManager.clearColor(clear.get(0), clear.get(1), clear.get(2), clear.get(3)); GlStateManager.clearDepth(clearDepth.get(0));
        }
    }
}
