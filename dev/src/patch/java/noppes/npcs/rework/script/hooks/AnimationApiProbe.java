package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.*;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import noppes.npcs.EventHooks;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.anim.Animators;
import noppes.npcs.rework.anim.IAnimator;

/** Explicit disposable M7.9 fixture only. Called each server tick by the controller's JS NPC.
 * Uses real script dispatch on two live bears (JS and typed Java) and independently samples
 * the entities mod's raw action API. No dependency on optional mod classes, even in the probe. */
public final class AnimationApiProbe {
    private static final String JS = "var A=Java.type('noppes.npcs.rework.anim.Animators');\n"
            + "function timer(e){var a=A.of(e.npc),s='';\n"
            + "if(e.id==1){s=''+a.isAvailable()+':'+a.play('hurt')+':'+a.queue('idle')+':'+a.queue('hurt')+':'+a.play('missing')+':'+a.queue(null);}\n"
            + "if(e.id==2){s=''+a.play('hurt')+':'+a.queue('idle')+':'+a.stop()+':'+a.getClip()+':'+a.getTick();}\n"
            + "e.npc.getTempdata().put('t',s);}\n";
    private static final String JAVA = "package m79;\nimport noppes.npcs.rework.anim.*;\n"
            + "import noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class AnimationTab extends NpcScript {\n"
            + " @Override public void timer(NpcEvent.TimerEvent e){IAnimator a=Animators.of(e.npc);String s=\"\";\n"
            + " if(e.id==1){s=\"\"+a.isAvailable()+\":\"+a.play(\"hurt\")+\":\"+a.queue(\"idle\")+\":\"+a.queue(\"hurt\")+\":\"+a.play(\"missing\")+\":\"+a.queue(null);}\n"
            + " if(e.id==2){s=\"\"+a.play(\"hurt\")+\":\"+a.queue(\"idle\")+\":\"+a.stop()+\":\"+a.getClip()+\":\"+a.getTick();}\n"
            + " e.npc.getTempdata().put(\"t\",s); }\n}\n";
    private final String output;
    private final boolean present;
    private final WorldServer world;
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final Map<String, Object> proof = new LinkedHashMap<>();
    private final List<List<String>> sequences = Arrays.asList(new ArrayList<>(), new ArrayList<>());
    private final List<List<String>> samples = Arrays.asList(new ArrayList<>(), new ArrayList<>());
    private final EntityNPCInterface[] pair = new EntityNPCInterface[2];
    private final IAnimator[] animators = new IAnimator[2];
    private final String[] previous = {"", ""};
    private int tick, phase, phaseTick;
    private boolean finished;
    private boolean samplesOk = true, hitboxesOk = true;
    private final float[] widths = new float[2], heights = new float[2];

    public AnimationApiProbe(ICustomNpc<?> control, boolean present, String output) throws Exception {
        this.present = present; this.output = output; world = (WorldServer) control.getWorld().getMCWorld();
        proof.put("success", false); proof.put("entities", present); proof.put("checks", checks);
        proof.put("sequences", sequences); proof.put("samples", samples);
        try {
            check(checks, "initially-no-queue-listener", listener() == null);
            IAnimator none = Animators.of(control);
            check(checks, "ordinary-and-null-inert", inert(none) && inert(Animators.of(null)));
            NBTTagCompound before = new NBTTagCompound();
            ((EntityNPCInterface) control.getMCEntity()).writeEntityToNBT(before);
            for (int i = 0; i < 100; i++) { inert(Animators.of(control)); }
            NBTTagCompound after = new NBTTagCompound();
            ((EntityNPCInterface) control.getMCEntity()).writeEntityToNBT(after);
            check(checks, "ordinary-nbt-unchanged", before.equals(after));
            if (!present) {
                boolean absent = false;
                try { Class.forName("dev.jakak.customnpcentities.entity.EntityAnimatedNpc", false, getClass().getClassLoader()); }
                catch (ClassNotFoundException expected) { absent = true; }
                check(checks, "optional-mod-actually-absent", absent);
                pair[0] = npc(world, "ECMAScript", JS); pair[1] = npc(world, "Java", JAVA);
            } else {
                List<EntityNPCInterface> bears = new ArrayList<>();
                boolean golem = false;
                for (Object raw : world.loadedEntityList) {
                    if (!(raw instanceof EntityNPCInterface)) { continue; }
                    EntityNPCInterface e = (EntityNPCInterface) raw;
                    if (e.getClass().getName().endsWith(".EntityCaveBear")) { bears.add(e); }
                    if (e.getClass().getName().endsWith(".EntityIceGolem")) { golem = inert(Animators.of(e.wrappedNPC)); }
                }
                check(checks, "two-bears-and-inert-golem", bears.size() == 2 && golem);
                if (bears.size() != 2) { throw new IllegalStateException("need exactly two bears"); }
                pair[0] = bears.get(0); pair[1] = bears.get(1);
                configure(pair[0], "ECMAScript", JS); configure(pair[1], "Java", JAVA);
            }
            for (int i = 0; i < 2; i++) {
                animators[i] = Animators.of(pair[i].wrappedNPC);
                widths[i] = pair[i].width; heights[i] = pair[i].height;
                check(checks, "availability-" + i, animators[i].isAvailable() == present);
                if (present) {
                    String[] clips = animators[i].getClips();
                    check(checks, "known-sorted-clips-" + i, Arrays.equals(clips, new String[]{"bite", "charge", "death", "hurt", "idle", "maul", "rear_roar", "swipe", "walk"}));
                    clips[0] = "damaged snapshot";
                    check(checks, "defensive-clips-" + i, "bite".equals(animators[i].getClips()[0]));
                    check(checks, "shared-handle-" + i, Animators.of(pair[i].wrappedNPC) == animators[i]);
                }
                EventHooks.onNPCTimer(pair[i], 1);
            }
            String golden = present ? "true:true:true:true:false:false" : "false:false:false:false:false:false";
            check(checks, "js-golden", golden.equals(trace(pair[0])));
            check(checks, "java-equals-js", golden.equals(trace(pair[1])));
            check(checks, "tabs-healthy", healthy());
            if (!present) {
                check(checks, "no-listener-without-mod", listener() == null);
                finish();
            } else {
                check(checks, "listener-only-for-pending", listener() != null);
                AtomicBoolean rejected = new AtomicBoolean();
                Thread thread = new Thread(() -> rejected.set(!animators[0].play("hurt") && !animators[0].queue("hurt") && !animators[0].stop()), "m79-offthread-check");
                thread.start(); thread.join(2000);
                check(checks, "off-thread-mutations-rejected", !thread.isAlive() && rejected.get());
                sample();
            }
        } catch (Throwable error) { fail(error); }
    }

    private static void configure(EntityNPCInterface npc, String language, String source) {
        NBTTagCompound data = new NBTTagCompound(), tab = new NBTTagCompound(); NBTTagList tabs = new NBTTagList();
        tab.setString("Script", source); tab.setTag("Console", new NBTTagList()); tab.setTag("ScriptList", new NBTTagList());
        tabs.appendTag(tab); data.setTag("Scripts", tabs); data.setString("ScriptLanguage", language); data.setBoolean("ScriptEnabled", true);
        npc.script.readFromNBT(data);
    }
    private boolean healthy() { return !pair[0].script.getScripts().get(0).errored && !pair[1].script.getScripts().get(0).errored; }
    private static boolean inert(IAnimator a) {
        return !a.isAvailable() && !a.play("hurt") && !a.queue("hurt") && !a.stop()
                && a.getClip().isEmpty() && a.getTick() == 0 && a.getClips().length == 0;
    }
    private static Object listener() throws Exception {
        Field f = Animators.class.getDeclaredField("listener"); f.setAccessible(true); return f.get(null);
    }
    private void sample() throws Exception {
        for (int i = 0; i < 2; i++) {
            EntityNPCInterface e = pair[i]; IAnimator a = animators[i];
            String clip = a.getClip(); int age = a.getTick();
            int action = ((Number) e.getClass().getMethod("getAction").invoke(e)).intValue();
            int rawAge = ((Number) e.getClass().getMethod("getActionAge").invoke(e)).intValue();
            String rawClip = action == 0 ? "" : (String) e.getClass().getMethod("clipForAction", int.class).invoke(e, action);
            samplesOk &= clip.equals(rawClip) && age == rawAge && age >= 0;
            hitboxesOk &= e.width == widths[i] && e.height == heights[i];
            if (!clip.isEmpty() && !clip.equals(previous[i])) { sequences.get(i).add(clip); }
            previous[i] = clip;
            samples.get(i).add(tick + ":" + clip + ":" + age);
        }
    }
    /** true means the JSON proof was written (success is a separate pinned field). */
    public boolean step() throws Exception {
        if (finished) { return true; }
        try {
            tick++;
            if (tick > 600) { throw new IllegalStateException("M7.9 phase timeout " + phase); }
            if (phase == 0) {
                sample();
                if (sequences.get(0).size() < 3 || sequences.get(1).size() < 3
                        || !animators[0].getClip().isEmpty() || !animators[1].getClip().isEmpty()) { return false; }
                check(checks, "fifo-js", sequences.get(0).equals(Arrays.asList("hurt", "idle", "hurt")));
                check(checks, "fifo-java", sequences.get(1).equals(sequences.get(0)));
                check(checks, "raw-action-samples", samplesOk && samples.get(0).size() > 10);
                check(checks, "hitbox-held", hitboxesOk);
                for (EntityNPCInterface e : pair) { EventHooks.onNPCTimer(e, 2); }
                check(checks, "script-stop-golden", "true:true:true::0".equals(trace(pair[0])) && trace(pair[0]).equals(trace(pair[1])));
                check(checks, "listener-detaches-after-stop", listener() == null);
                // Successful play cancels pending; invalid play/queue did not cancel the FIFO above.
                for (IAnimator a : animators) { a.play("hurt"); a.queue("idle"); a.play("hurt"); }
                check(checks, "successful-play-clears-pending", listener() == null);
                phase = 1; phaseTick = tick;
            } else if (phase == 1 && tick - phaseTick > 100) {
                check(checks, "no-resurrection-after-play", animators[0].getClip().isEmpty() && animators[1].getClip().isEmpty() && listener() == null);
                // One measured read batch, data only; no extra server or benchmark repetition.
                long start = System.nanoTime();
                for (int i = 0; i < 1000; i++) { animators[0].getClip(); animators[0].getTick(); animators[0].getClips(); }
                proof.put("readTripletMicros", (System.nanoTime() - start) / 1000000.0);
                for (IAnimator a : animators) { a.play("hurt"); a.queue("idle"); }
                // Actual world removal/death, then let the real world END listener cancel both queues.
                world.removeEntity(pair[0]); pair[1].setDead();
                phase = 2; phaseTick = tick;
            } else if (phase == 2 && tick - phaseTick >= 2) {
                check(checks, "death-and-removal-detach-listener", listener() == null);
                check(checks, "dead-target-inert", inert(animators[1]));
                check(checks, "tabs-still-healthy", healthy());
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
        proof.put("jsTrace", pair[0] == null ? "" : trace(pair[0])); proof.put("javaTrace", pair[1] == null ? "" : trace(pair[1]));
        Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        finished = true;
        System.out.println("M79 DONE success=" + success + " entities=" + present);
    }
}
