package noppes.npcs.rework.anim;

import java.util.ArrayList;
import java.util.List;
import noppes.npcs.rework.anim.ActionJournal.Step;

/** P7a (D-098, Q278): server view of the locomotion layer of one animated NPC with RwAnimHooks=2,
 * observed once per world tick END after the entity's update. State priority death > action > walk >
 * idle; entering a state with a clip gives a start step, leaving it an end step (a loop wrap gives no
 * extra start). Clocks are the entities mod's own: walk = limbSwing / 4 / stride * length with the
 * client's threshold limbSwingAmount > 0.25 and window rule, idle = ticksExisted / 20, death =
 * deathTime / 20. Script hooks only: no sound, particle or damage of the client events is repeated.
 * The action state itself produces nothing here; the ActionJournal reports it. No Minecraft types. */
public final class Locomotion {
    public interface Clip {
        String name();
        float length();
        int lengthTicks();
        float stride();
        boolean loop();
        /** The entities mod's AnimClip.eventsBetween (from &lt; time &lt;= to, a loop wraps). */
        void eventsBetween(float from, float to, List<HookTracker.Fired> out);
    }

    /** Clips of the entity; null when the clip does not exist. */
    public interface Clips {
        Clip walk();
        Clip idle();
        Clip death();
    }

    private String state;
    private Clip stateClip;
    private int stateAge;
    private float lastWalkTime = Float.NaN;
    private int lastDeathTime;
    private int lastTicks;
    private final List<HookTracker.Fired> fired = new ArrayList<>();

    public String state() { return state; }

    /** @param action the entity runs an action (any, also one started by a script) */
    public void observe(Clips clips, boolean action, int deathTime, float limbSwing, float limbSwingAmount, int ticksExisted, List<Step> out) {
        Clip walk = clips.walk();
        boolean walkable = walk != null && walk.stride() > 0;
        float walkTime = walkable ? limbSwing / 4F / walk.stride() * walk.length() : Float.NaN;
        String next;
        Clip clip;
        if (deathTime > 0) { next = AnimationEvent.DEATH; clip = clips.death(); }
        else if (action) { next = AnimationEvent.ACTION; clip = null; }
        else if (walkable && limbSwingAmount > 0.25F) { next = AnimationEvent.WALK; clip = walk; }
        else { next = AnimationEvent.IDLE; clip = clips.idle(); }
        String previous = state;
        if (!next.equals(state) || clip != stateClip) {
            if (stateClip != null) {
                out.add(Step.end(state, stateClip.name(), stateAge, !stateClip.loop() && stateAge >= stateClip.lengthTicks()));
            }
            state = next; stateClip = clip; stateAge = 0;
            if (clip != null) { out.add(Step.start(next, clip.name())); }
        } else {
            stateAge++;
        }
        if (stateClip != null) {
            fired.clear();
            if (AnimationEvent.WALK.equals(state)) {
                // Same condition and window as EntityAnimatedNpc.tickClientLocomotion.
                if (!Float.isNaN(lastWalkTime) && walkTime - lastWalkTime < walk.length()) { walk.eventsBetween(lastWalkTime, walkTime, fired); }
            } else if (AnimationEvent.IDLE.equals(state)) {
                float from = AnimationEvent.IDLE.equals(previous) ? lastTicks / 20F : (ticksExisted - 1) / 20F;
                stateClip.eventsBetween(from, ticksExisted / 20F, fired);
            } else if (AnimationEvent.DEATH.equals(state) && deathTime > lastDeathTime) {
                stateClip.eventsBetween(lastDeathTime == 0 ? -1 : lastDeathTime / 20F, deathTime / 20F, fired);
            }
            for (HookTracker.Fired f : fired) { out.add(Step.event(state, stateClip.name(), stateAge, f.type, f.time, f.data)); }
        }
        lastWalkTime = walkTime;
        lastDeathTime = deathTime;
        lastTicks = ticksExisted;
    }
}
