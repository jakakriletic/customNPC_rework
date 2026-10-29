package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class HookTrackerTest {
    /** Simulated clip with the entity's event window: events with from < t <= to. */
    private static HookTracker.Clip clip(String name, float length, float... times) {
        return new HookTracker.Clip() {
            public String name() { return name; }
            public int lengthTicks() { return Math.round(length * 20); }
            public void fired(int age, List<HookTracker.Fired> out) {
                float from = age - 1 == 0 ? -1 : (age - 1) / 20F, to = Math.min(age / 20F, length);
                for (float t : times) { if (t > from && t <= to) { out.add(new HookTracker.Fired("e" + t, t, "{}")); } }
            }
        };
    }

    /** Runs the entity's server rule (age++, stop at lengthTicks) and observes after every tick. */
    private static List<String> play(HookTracker tracker, HookTracker.Clip c, int raw, int ticks) {
        List<String> out = new ArrayList<>();
        for (int age = 1; age <= ticks; age++) {
            boolean done = age >= c.lengthTicks();
            for (HookTracker.Step s : tracker.observe(done ? 0 : raw, done ? null : c, done ? 0 : age)) { out.add(s.toString()); }
            if (done) { break; }
        }
        return out;
    }

    @Test public void naturalFinishFiresEveryEventAtItsAgeThenEnd() {
        // bite: 0.95 s, events at 0.05, 0.25, 0.48 -> ages 1, 5, 10; ends at 19.
        HookTracker t = new HookTracker();
        assertEquals("[S:bite, E:e0.05:1, E:e0.25:5, E:e0.48:10, X:bite:19:true]",
                play(t, clip("bite", 0.95F, 0.05F, 0.25F, 0.48F), 0x101, 40).toString());
        assertTrue("idle stays quiet", t.observe(0, null, 0).isEmpty());
    }

    @Test public void eventInTheLastStepFiresBeforeEnd() {
        HookTracker t = new HookTracker();
        assertEquals("[S:c, E:e0.0:1, E:e0.2:4, X:c:4:true]", play(t, clip("c", 0.2F, 0F, 0.2F), 0x101, 10).toString());
    }

    @Test public void startAfterTheEntityUpdatedFiresEventsNextTick() {
        HookTracker t = new HookTracker();
        HookTracker.Clip c = clip("bite", 0.95F, 0.05F);
        assertEquals("[S:bite]", t.observe(0x101, c, 0).toString());
        assertEquals("[E:e0.05:1]", t.observe(0x101, c, 1).toString());
        assertTrue(t.observe(0x101, c, 1).isEmpty());
    }

    @Test public void interruptionAndRestartOfTheSameClip() {
        HookTracker t = new HookTracker();
        HookTracker.Clip bite = clip("bite", 0.95F, 0.48F), hurt = clip("hurt", 0.45F);
        t.observe(0x101, bite, 1); t.observe(0x101, bite, 2);
        // Same clip again: a new sequence number in the synced value.
        assertEquals("[X:bite:2:false, S:bite]", t.observe(0x201, bite, 1).toString());
        assertEquals("[X:bite:1:false, S:hurt]", t.observe(0x302, hurt, 1).toString());
        assertEquals("[X:hurt:1:false]", t.observe(0, null, 0).toString());
        assertEquals("unknown clip is idle", "[]", t.observe(0x403, null, 1).toString());
    }

    @Test public void skippedObservationsReplayEachStepInOrder() {
        HookTracker t = new HookTracker();
        HookTracker.Clip c = clip("charge", 1.5F, 0.3F, 0.3F, 0.45F, 1.05F);
        assertEquals("[S:charge, E:e0.3:6, E:e0.3:6, E:e0.45:9]", t.observe(0x105, c, 9).toString());
        assertEquals("stopped early", "[X:charge:9:false]", t.observe(0, null, 0).toString());
        HookTracker u = new HookTracker();
        assertEquals("[S:charge, E:e0.3:6, E:e0.3:6, E:e0.45:9, E:e1.05:21]", u.observe(0x105, c, 29).toString());
        assertEquals("[X:charge:30:true]", u.observe(0, null, 0).toString());
    }
}
