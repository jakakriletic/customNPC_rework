package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.check;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.anim.AnimationHooks;
import noppes.npcs.rework.anim.Animators;
import noppes.npcs.rework.anim.IAnimator;

/** Explicit disposable M7.5 fixture only. Called each server tick by the controller's JS NPC.
 * Two live bears get the same hook script in JS and typed Java; the probe drives clips through the
 * M7.9 API and compares both hook traces with pinned goldens. hooks=0 must show no listener and no call. */
public final class AnimationHooksProbe {
    private static final String JS = "var A=Java.type('noppes.npcs.rework.anim.Animators');\n"
            + "function add(e,s){var t=e.npc.getTempdata();t.put('h',(t.has('h')?t.get('h'):'')+s+'|');}\n"
            + "function animationStart(e){add(e,'S:'+e.clip+':'+e.tick+':'+A.of(e.npc).getClip());}\n"
            + "function animationEvent(e){add(e,'E:'+e.clip+':'+e.type+':'+e.tick+':'+A.of(e.npc).getTick());}\n"
            + "function animationEnd(e){add(e,'X:'+e.clip+':'+e.tick+':'+e.finished);}\n";
    private static final String JAVA = "package m75;\nimport noppes.npcs.api.entity.data.IData;\n"
            + "import noppes.npcs.rework.anim.*;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class HookTab extends NpcScript {\n"
            + " private static void add(AnimationEvent e,String s){IData t=e.npc.getTempdata();t.put(\"h\",(t.has(\"h\")?t.get(\"h\"):\"\")+s+\"|\");}\n"
            + " @Override public void animationStart(AnimationEvent.Started e){add(e,\"S:\"+e.clip+\":\"+e.tick+\":\"+Animators.of(e.npc).getClip());}\n"
            + " @Override public void animationEvent(AnimationEvent.ClipEvent e){add(e,\"E:\"+e.clip+\":\"+e.type+\":\"+e.tick+\":\"+Animators.of(e.npc).getTick());}\n"
            + " @Override public void animationEnd(AnimationEvent.Ended e){add(e,\"X:\"+e.clip+\":\"+e.tick+\":\"+e.finished);}\n}\n";
    /** bite 0.95 s: sound 0.05, lock/lunge 0.25, hit/sound 0.48 -> ages 1, 5, 10; ends at age 19. */
    static final String BITE = "S:bite:0:bite|E:bite:sound:1:1|E:bite:lock:5:5|E:bite:lunge:5:5|E:bite:hit:10:10|E:bite:sound:10:10|X:bite:19:true|";
    /** Replace by hurt (9 ticks, no events), stop, then the same clip again; cuts happen before age 5. */
    static final String CUTS = "S:bite:0:bite\\|E:bite:sound:1:1\\|X:bite:[1-4]:false\\|S:hurt:0:hurt\\|X:hurt:9:true\\|"
            + "S:bite:0:bite\\|E:bite:sound:1:1\\|X:bite:[1-4]:false\\|"
            + "S:bite:0:bite\\|E:bite:sound:1:1\\|X:bite:[1-4]:false\\|" + BITE.replace("|", "\\|");
    private final String output;
    private final boolean hooks;
    private final WorldServer world;
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final Map<String, Object> proof = new LinkedHashMap<>();
    private final EntityNPCInterface[] pair = new EntityNPCInterface[2];
    private final IAnimator[] animators = new IAnimator[2];
    private final ICustomNpc<?> control;
    private int tick, phase, phaseTick;
    private boolean finished;

    public AnimationHooksProbe(ICustomNpc<?> control, boolean hooks, String output) throws Exception {
        this.control = control; this.hooks = hooks; this.output = output; world = (WorldServer) control.getWorld().getMCWorld();
        proof.put("success", false); proof.put("hooks", hooks); proof.put("checks", checks);
        try {
            check(checks, "switch-read", CustomNpcs.RwAnimHooks == (hooks ? 1 : 0));
            check(checks, "listener-only-with-switch", (installed() != null) == hooks);
            List<EntityNPCInterface> bears = new ArrayList<>();
            for (Object raw : world.loadedEntityList) {
                if (raw instanceof EntityNPCInterface && raw.getClass().getName().endsWith(".EntityCaveBear")) { bears.add((EntityNPCInterface) raw); }
            }
            if (bears.size() != 2) { throw new IllegalStateException("need exactly two bears"); }
            pair[0] = bears.get(0); pair[1] = bears.get(1);
            configure(pair[0], "ECMAScript", JS); configure(pair[1], "Java", JAVA);
            // Only animated NPCs are tracked: two bears, not the golem or this ordinary control NPC.
            check(checks, "tracks-only-animated", !hooks || tracked() == 2);
            for (int i = 0; i < 2; i++) {
                animators[i] = Animators.of(pair[i].wrappedNPC);
                check(checks, "available-" + i, animators[i].isAvailable());
                check(checks, "play-bite-" + i, animators[i].play("bite"));
            }
            phaseTick = 0;
        } catch (Throwable error) { fail(error); }
    }

    private static void configure(EntityNPCInterface npc, String language, String source) {
        NBTTagCompound data = new NBTTagCompound(), tab = new NBTTagCompound(); NBTTagList tabs = new NBTTagList();
        tab.setString("Script", source); tab.setTag("Console", new NBTTagList()); tab.setTag("ScriptList", new NBTTagList());
        tabs.appendTag(tab); data.setTag("Scripts", tabs); data.setString("ScriptLanguage", language); data.setBoolean("ScriptEnabled", true);
        npc.script.readFromNBT(data);
    }
    private static Object installed() throws Exception {
        Field f = AnimationHooks.class.getDeclaredField("installed"); f.setAccessible(true); return f.get(null);
    }
    private static int tracked() throws Exception {
        Object hooks = installed();
        Field f = AnimationHooks.class.getDeclaredField("tracked"); f.setAccessible(true);
        return ((List<?>) f.get(hooks)).size();
    }
    private String trace(int i) {
        Object value = pair[i].wrappedNPC.getTempdata().get("h");
        return value == null ? "" : value.toString();
    }
    private void clear() { for (EntityNPCInterface e : pair) { e.wrappedNPC.getTempdata().remove("h"); } }
    private boolean idle() { return animators[0].getClip().isEmpty() && animators[1].getClip().isEmpty(); }
    private boolean healthy() { return !pair[0].script.getScripts().get(0).errored && !pair[1].script.getScripts().get(0).errored; }
    private void all(String clip) { for (IAnimator a : animators) { if (clip == null) { a.stop(); } else { a.play(clip); } } }

    /** true means the JSON proof was written (success is a separate pinned field). */
    public boolean step() throws Exception {
        if (finished) { return true; }
        try {
            tick++; phaseTick++;
            if (tick > 600) { throw new IllegalStateException("M7.5 phase timeout " + phase); }
            if (phase == 0) {
                if (phaseTick < 25 || !idle()) { return false; }
                proof.put("biteJs", trace(0)); proof.put("biteJava", trace(1));
                check(checks, "bite-js", trace(0).equals(hooks ? BITE : ""));
                check(checks, "bite-java-equals-js", trace(1).equals(trace(0)));
                clear(); all("bite"); phase = 1; phaseTick = 0;
            } else if (phase == 1) {
                // Relative ticks: hurt replaces bite, then bite + stop, then bite restarted, each before age 5.
                if (phaseTick == 2) { all("hurt"); }
                else if (phaseTick == 20) { all("bite"); }
                else if (phaseTick == 22) { all(null); }
                else if (phaseTick == 25) { all("bite"); }
                else if (phaseTick == 28) { all("bite"); }
                else if (phaseTick > 60 && idle()) {
                    proof.put("cutsJs", trace(0)); proof.put("cutsJava", trace(1));
                    check(checks, "cuts-js", hooks ? trace(0).matches(CUTS) : trace(0).isEmpty());
                    check(checks, "cuts-java-equals-js", trace(1).equals(trace(0)));
                    check(checks, "tabs-healthy", healthy());
                    if (hooks) {
                        // One measured batch of the real END observer on idle tracked bears, data only.
                        Object observer = installed();
                        TickEvent.WorldTickEvent end = new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world);
                        long start = System.nanoTime();
                        for (int i = 0; i < 1000; i++) { ((AnimationHooks) observer).tick(end); }
                        proof.put("idleObserveMicrosPerWorldTick", (System.nanoTime() - start) / 1000000.0);
                        proof.put("trackedNpcs", tracked());
                        check(checks, "idle-observe-silent", trace(0).equals(proof.get("cutsJs")) && trace(1).equals(proof.get("cutsJava")));
                    }
                    check(checks, "control-npc-unchanged", control.getTempdata().get("h") == null);
                    finish();
                }
            }
        } catch (Throwable error) { fail(error); }
        return finished;
    }
    private void fail(Throwable error) throws Exception {
        proof.put("error", error.toString()); error.printStackTrace(); check(checks, "unexpected-error", false); finish();
    }
    private void finish() throws Exception {
        boolean success = true; for (Map<String, Object> row : checks) { success &= Boolean.TRUE.equals(row.get("ok")); }
        proof.put("success", success); proof.put("ticks", tick);
        Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        finished = true;
        System.out.println("M75 DONE success=" + success + " hooks=" + hooks);
    }
}
