package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** P7a: server locomotion layer (RwAnimHooks=2) with the entities mod's clip rule (AnimClip.eventsBetween). */
public class LocomotionTest {
    /** Copy of AnimClip.eventsBetween / collect from customnpcentities 0.7.0. */
    static Locomotion.Clip clip(String name, float length, float stride, boolean loop, float... times) {
        return new Locomotion.Clip() {
            public String name() { return name; }
            public float length() { return length; }
            public int lengthTicks() { return Math.max(1, Math.round(length * 20)); }
            public float stride() { return stride; }
            public boolean loop() { return loop; }
            public void eventsBetween(float from, float to, List<HookTracker.Fired> out) {
                if (to <= from) { return; }
                if (loop && length > 0 && to - from < length) {
                    float a = from % length, b = to % length;
                    if (a < 0) { a += length; }
                    if (b < 0) { b += length; }
                    if (a <= b) { collect(a, b, out); } else { collect(a, length, out); collect(-1, b, out); }
                    return;
                }
                collect(from, to, out);
            }
            void collect(float from, float to, List<HookTracker.Fired> out) {
                for (float t : times) { if (t > from && t <= to) { out.add(new HookTracker.Fired("step", t, "{}")); } }
            }
        };
    }

    static final Locomotion.Clip WALK = clip("walk", 0.8F, 3.2F, true, 0F, 0.4F);
    static final Locomotion.Clip IDLE = clip("idle", 2F, 0, true);
    static final Locomotion.Clip DEATH = clip("death", 1.2F, 0, false, 0.8F);
    static final Locomotion.Clips CLIPS = new Locomotion.Clips() {
        public Locomotion.Clip walk() { return WALK; }
        public Locomotion.Clip idle() { return IDLE; }
        public Locomotion.Clip death() { return DEATH; }
    };

    static final class Npc {
        final Locomotion loco = new Locomotion();
        float limbSwing, amount; int ticks = 100, death; boolean action;
        final List<String> out = new ArrayList<>();
        void tick() {
            ticks++;
            List<ActionJournal.Step> steps = new ArrayList<>();
            loco.observe(CLIPS, action, death, limbSwing, amount, ticks, steps);
            for (ActionJournal.Step s : steps) { out.add(s.toString()); }
        }
    }

    @Test public void idleStartsOnceAndALoopWrapGivesNoNewStart() {
        Npc n = new Npc();
        for (int i = 0; i < 90; i++) { n.tick(); }
        assertEquals("[S:idle:0:idle]", n.out.toString());
    }

    @Test public void walkUsesTheDistanceClockAndThreshold() {
        Npc n = new Npc();
        n.tick();                                  // idle
        n.amount = 1F;
        // 0.8 s clip over stride 3.2: one cycle per 12.8 limb swing; steps at 0 and 0.4 s = swing 0 and 6.4.
        for (int i = 0; i < 16; i++) { n.limbSwing += 1F; n.tick(); }
        n.amount = 0.2F;                           // below 0.25: idle again
        n.tick();
        assertEquals("[S:idle:0:idle, X:idle:0:false:idle, S:walk:0:walk, E:walk:step:6:walk, E:walk:step:12:walk, X:walk:15:false:walk, S:idle:0:idle]",
                n.out.toString());
    }

    @Test public void restingDoesNotReplayOldStepsOnTheNextWalk() {
        Npc n = new Npc();
        n.amount = 1F;
        n.limbSwing = 1F; n.tick();               // walk starts, clock 0.0625
        n.amount = 0F;
        for (int i = 0; i < 5; i++) { n.limbSwing += 1F; n.tick(); }   // swing grows while idle (clock moves on)
        n.amount = 1F;
        n.limbSwing += 0.1F; n.tick();
        assertEquals("[S:walk:0:walk, X:walk:0:false:walk, S:idle:0:idle, X:idle:4:false:idle, S:walk:0:walk]", n.out.toString());
    }

    @Test public void actionHasPriorityOverWalkAndGivesNoLayerSteps() {
        Npc n = new Npc();
        n.amount = 1F;
        n.tick();
        n.action = true;
        for (int i = 0; i < 3; i++) { n.limbSwing += 1F; n.tick(); }
        n.action = false; n.amount = 0F;
        n.tick();
        assertEquals("[S:walk:0:walk, X:walk:0:false:walk, S:idle:0:idle]", n.out.toString());
    }

    @Test public void deathFiresItsEventOnceAndEndsOnRespawn() {
        Npc n = new Npc();
        n.tick();
        for (int d = 1; d <= 40; d++) { n.death = d; n.tick(); }
        n.death = 0;
        n.tick();
        assertEquals("[S:idle:0:idle, X:idle:0:false:idle, S:death:0:death, E:death:step:15:death, X:death:39:true:death, S:idle:0:idle]",
                n.out.toString());
    }
}
