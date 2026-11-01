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
import noppes.npcs.rework.core.AnimatedNpcPatch;

/** Explicit disposable P7a fixture only. Called each server tick by the controller's JS NPC (after the
 * two bears in the same tick). Both bears get the same hook script in JS and typed Java; action steps
 * go to tempdata "h", idle/walk/death steps to "l". Pinned: natural bite, several transitions in one
 * tick, stop in the tick of the hit step, stop at length-1, and with RwAnimHooks=2 walk -> idle and
 * death. hooks=0: class untouched, no listener and no hook call. */
public final class AnimationJournalProbe {
    private static final String JS = "function add(e,s){var t=e.npc.getTempdata();var k=e.source=='action'?'h':'l';t.put(k,(t.has(k)?t.get(k):'')+s+'|');}\n"
            + "function animationStart(e){add(e,'S:'+e.clip+':'+e.tick+':'+e.source);}\n"
            + "function animationEvent(e){add(e,'E:'+e.clip+':'+e.type+':'+e.tick+':'+e.source);}\n"
            + "function animationEnd(e){add(e,'X:'+e.clip+':'+e.tick+':'+e.finished+':'+e.source);}\n";
    private static final String JAVA = "package p7;\nimport noppes.npcs.api.entity.data.IData;\n"
            + "import noppes.npcs.rework.anim.*;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class HookTab extends NpcScript {\n"
            + " private static void add(AnimationEvent e,String s){IData t=e.npc.getTempdata();String k=e.source.equals(\"action\")?\"h\":\"l\";t.put(k,(t.has(k)?t.get(k):\"\")+s+\"|\");}\n"
            + " @Override public void animationStart(AnimationEvent.Started e){add(e,\"S:\"+e.clip+\":\"+e.tick+\":\"+e.source);}\n"
            + " @Override public void animationEvent(AnimationEvent.ClipEvent e){add(e,\"E:\"+e.clip+\":\"+e.type+\":\"+e.tick+\":\"+e.source);}\n"
            + " @Override public void animationEnd(AnimationEvent.Ended e){add(e,\"X:\"+e.clip+\":\"+e.tick+\":\"+e.finished+\":\"+e.source);}\n}\n";
    static final String[] BITE_EVENTS = {"E:bite:sound:1:action|", "E:bite:lock:5:action|", "E:bite:lunge:5:action|", "E:bite:hit:10:action|", "E:bite:sound:10:action|"};
    static final int[] BITE_AGES = {1, 5, 5, 10, 10};
    static final String BITE = "S:bite:0:action|" + String.join("", BITE_EVENTS) + "X:bite:19:true:action|";
    static final String BURST = "S:bite:0:action|X:bite:0:false:action|S:hurt:0:action|X:hurt:0:false:action|" + BITE;
    /** Leaves idle, walks with at least one step on the walk clock, rests; may walk again (return home). */
    static final String WALK = "X:idle:\\d+:false:idle\\|S:walk:0:walk\\|.*E:walk:step:\\d+:walk\\|.*X:walk:\\d+:false:walk\\|S:idle:0:idle\\|.*";
    /** Death after idle or walk: both 0.8 s events at death age 15 (deathTime 16), once. */
    static final String DEATH = "X:(idle:\\d+:false:idle|walk:\\d+:false:walk)\\|S:death:0:death\\|E:death:sound:15:death\\|E:death:dust:15:death\\|.*";
    private final String output;
    private final int hooks;
    private final WorldServer world;
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final Map<String, Object> proof = new LinkedHashMap<>();
    private final EntityNPCInterface[] pair = new EntityNPCInterface[2];
    private final IAnimator[] animators = new IAnimator[2];
    private final int[] stoppedAt = new int[2];
    private final ICustomNpc<?> control;
    private int tick, phase, phaseTick;
    private boolean finished;

    public AnimationJournalProbe(ICustomNpc<?> control, int hooks, String output) throws Exception {
        this.control = control; this.hooks = hooks; this.output = output; world = (WorldServer) control.getWorld().getMCWorld();
        proof.put("success", false); proof.put("hooks", hooks); proof.put("checks", checks);
        try {
            check(checks, "switch-read", CustomNpcs.RwAnimHooks == hooks);
            check(checks, "listener-only-with-switch", (installed() != null) == (hooks >= 1));
            proof.put("animpatch", System.getProperty(AnimatedNpcPatch.PROP_PATCH));
            check(checks, "class-patch-only-with-switch", (hooks >= 1 ? "1" : "off").equals(System.getProperty(AnimatedNpcPatch.PROP_PATCH)));
            List<EntityNPCInterface> bears = new ArrayList<>();
            for (Object raw : world.loadedEntityList) {
                if (raw instanceof EntityNPCInterface && raw.getClass().getName().endsWith(".EntityCaveBear")) { bears.add((EntityNPCInterface) raw); }
            }
            if (bears.size() != 2) { throw new IllegalStateException("need exactly two bears"); }
            pair[0] = bears.get(0); pair[1] = bears.get(1);
            configure(pair[0], "ECMAScript", JS); configure(pair[1], "Java", JAVA);
            check(checks, "tracks-only-animated", hooks == 0 || tracked() == 2);
            for (int i = 0; i < 2; i++) {
                animators[i] = Animators.of(pair[i].wrappedNPC);
                check(checks, "available-" + i, animators[i].isAvailable());
            }
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
    private String trace(int i, String key) {
        Object value = pair[i].wrappedNPC.getTempdata().get(key);
        return value == null ? "" : value.toString();
    }
    private void clear() { for (EntityNPCInterface e : pair) { e.wrappedNPC.getTempdata().remove("h"); } }
    private boolean idle() { return animators[0].getClip().isEmpty() && animators[1].getClip().isEmpty(); }
    private boolean healthy() { return !pair[0].script.getScripts().get(0).errored && !pair[1].script.getScripts().get(0).errored; }
    private void all(String clip) { for (IAnimator a : animators) { if (clip == null) { a.stop(); } else { a.play(clip); } } }
    private void next() { clear(); phase++; phaseTick = 0; }
    /** Bite trace stopped at age {@code age} (0 < age < 19) by a script. */
    static String stopped(int age) {
        StringBuilder s = new StringBuilder("S:bite:0:action|");
        for (int k = 0; k < BITE_EVENTS.length; k++) { if (BITE_AGES[k] <= age) { s.append(BITE_EVENTS[k]); } }
        return s.append("X:bite:").append(age).append(":false:action|").toString();
    }
    private void pinned(String key, String expected) {
        String js = trace(0, "h"), java = trace(1, "h");
        proof.put(key + "Js", js); proof.put(key + "Java", java);
        check(checks, key + "-js", js.equals(hooks >= 1 ? expected : ""));
        check(checks, key + "-java-equals-js", java.equals(js));
    }
    /** Stops each bear in the tick its action age reaches {@code age} (the bears tick before this NPC). */
    private boolean stopAt(int age) {
        for (int i = 0; i < 2; i++) {
            if (stoppedAt[i] == 0 && animators[i].getTick() >= age) { stoppedAt[i] = animators[i].getTick(); animators[i].stop(); }
        }
        return stoppedAt[0] != 0 && stoppedAt[1] != 0;
    }

    /** true means the JSON proof was written (success is a separate pinned field). */
    public boolean step() throws Exception {
        if (finished) { return true; }
        try {
            tick++; phaseTick++;
            if (tick > 900) { throw new IllegalStateException("P7 phase timeout " + phase); }
            switch (phase) {
                case 0:
                    if (phaseTick < 25 || !idle()) { return false; }
                    clear(); all("bite"); next(); break;
                case 1:
                    if (phaseTick < 30 || !idle()) { return false; }
                    pinned("bite", BITE);
                    // Several transitions inside one tick: the old END observer saw only the last start.
                    clear(); all("bite"); all("hurt"); all(null); all("bite"); next(); break;
                case 2:
                    if (phaseTick < 30 || !idle()) { return false; }
                    pinned("burst", BURST);
                    clear(); all("bite"); stoppedAt[0] = stoppedAt[1] = 0; next(); break;
                case 3:
                    // Stop in the tick the hit fired (age 10): the old observer lost that step.
                    if (!stopAt(10) && phaseTick < 40) { return false; }
                    if (phaseTick < 25 || !idle()) { return false; }
                    proof.put("hitStopAges", stoppedAt[0] + "," + stoppedAt[1]);
                    check(checks, "hit-stop-age", hooks == 0 || (stoppedAt[0] == 10 && stoppedAt[1] == 10));
                    pinned("hitStop", stopped(10));
                    clear(); all("bite"); stoppedAt[0] = stoppedAt[1] = 0; next(); break;
                case 4:
                    // Stop at length-1 (age 18): not a natural end.
                    if (!stopAt(18) && phaseTick < 40) { return false; }
                    if (phaseTick < 25 || !idle()) { return false; }
                    proof.put("lastStopAges", stoppedAt[0] + "," + stoppedAt[1]);
                    check(checks, "last-stop-age", hooks == 0 || (stoppedAt[0] == 18 && stoppedAt[1] == 18));
                    pinned("lastStop", stopped(18));
                    check(checks, "tabs-healthy", healthy());
                    if (hooks >= 1) {
                        // One measured batch of the real END observer on idle tracked bears, data only.
                        Object observer = installed();
                        TickEvent.WorldTickEvent end = new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world);
                        long start = System.nanoTime();
                        for (int i = 0; i < 1000; i++) { ((AnimationHooks) observer).tick(end); }
                        proof.put("idleObserveMicrosPerWorldTick", (System.nanoTime() - start) / 1000000.0);
                        proof.put("trackedNpcs", tracked());
                        check(checks, "idle-observe-silent", trace(0, "h").equals(proof.get("lastStopJs")) && trace(1, "h").equals(proof.get("lastStopJava")));
                    }
                    // No walk back home: a return walk right after arrival never rests (attempt 1 run 1).
                    for (EntityNPCInterface e : pair) { e.wrappedNPC.getAi().setReturnsHome(false); e.wrappedNPC.getTempdata().remove("l"); e.wrappedNPC.navigateTo(e.posX + 6, e.posY, e.posZ, 1.0); }
                    next(); break;
                case 5:
                    // Walk 6 blocks, then rest: wait for the rest on both bears (at most 300 ticks).
                    if (phaseTick < 300 && !(trace(0, "l").matches(WALK) && trace(1, "l").matches(WALK))) { return false; }
                    proof.put("walkJs", trace(0, "l")); proof.put("walkJava", trace(1, "l"));
                    check(checks, "walk-layer", hooks == 2 ? trace(0, "l").matches(WALK) && trace(1, "l").matches(WALK)
                            : trace(0, "l").isEmpty() && trace(1, "l").isEmpty());
                    check(checks, "walk-no-action-steps", trace(0, "h").isEmpty() && trace(1, "h").isEmpty());
                    for (EntityNPCInterface e : pair) { e.setHealth(0F); }
                    next(); break;
                default:
                    if (phaseTick < 30) { return false; }
                    proof.put("layerJs", trace(0, "l")); proof.put("layerJava", trace(1, "l"));
                    check(checks, "death-layer", hooks == 2 ? trace(0, "l").matches(WALK + DEATH) && trace(1, "l").matches(WALK + DEATH)
                            : trace(0, "l").isEmpty() && trace(1, "l").isEmpty());
                    check(checks, "death-no-action-steps", trace(0, "h").isEmpty() && trace(1, "h").isEmpty());
                    check(checks, "control-npc-unchanged", control.getTempdata().get("h") == null && control.getTempdata().get("l") == null);
                    finish();
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
        System.out.println("P7 DONE success=" + success + " hooks=" + hooks);
    }
}
