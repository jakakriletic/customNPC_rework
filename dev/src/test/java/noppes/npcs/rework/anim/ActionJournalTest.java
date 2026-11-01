package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** P7a: the exact journal against the M7.5 END observer (HookTracker) on the same simulated entity.
 * The simulator follows EntityAnimatedNpc 0.7.0: startAction/stopAction, and tickAnimation with
 * age++, events of the step (from = age 0 ? -1 : age/20), natural stop at lengthTicks. */
public class ActionJournalTest {
    static final float[] BITE = {0.05F, 0.25F, 0.48F};

    /** Simulated entity that writes the journal as the instrumented class does. */
    static final class Sim {
        final ActionJournal journal = new ActionJournal();
        final HookTracker old = new HookTracker();
        String clip; float length; float[] events; int raw, seq, age;
        void start(String name, float len, float... ev) {
            if (clip != null) { journal.end(clip, age); }
            clip = name; length = len; events = ev; seq++; raw = (seq << 8) | 1; age = 0;
            journal.start(name);
        }
        void stop() { if (clip != null) { journal.end(clip, age); } clip = null; raw = 0; age = 0; }
        int lengthTicks() { return Math.round(length * 20); }
        void tick() {
            if (clip == null) { return; }
            float from = age == 0 ? -1 : age / 20F;
            age++;
            float to = Math.min(age / 20F, length);
            for (float t : events) { if (t > from && t <= to) { journal.event(clip, age, "e" + t, t, "{}"); } }
            if (age >= lengthTicks()) { stop(); journal.natural(); }
        }
        /** World tick END: both observers. */
        String[] end() {
            List<String> a = new ArrayList<>(), b = new ArrayList<>();
            for (ActionJournal.Step s : journal.drain()) { a.add(s.toString()); }
            final String name = clip; final float len = length; final float[] ev = events;
            HookTracker.Clip view = name == null ? null : new HookTracker.Clip() {
                public String name() { return name; }
                public int lengthTicks() { return Math.round(len * 20); }
                public void fired(int at, List<HookTracker.Fired> out) {
                    float from = at - 1 == 0 ? -1 : (at - 1) / 20F, to = Math.min(at / 20F, len);
                    for (float t : ev) { if (t > from && t <= to) { out.add(new HookTracker.Fired("e" + t, t, "{}")); } }
                }
            };
            for (HookTracker.Step s : old.observe(raw, view, age)) { b.add(s.toString()); }
            return new String[]{String.join("|", a), String.join("|", b)};
        }
    }

    @Test public void naturalEndKeepsEveryEventAndFinished() {
        Sim s = new Sim();
        s.start("bite", 0.95F, BITE);
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < 25; i++) { s.tick(); String j = s.end()[0]; if (!j.isEmpty()) { all.append(j).append('|'); } }
        assertEquals("S:bite:0:action|E:bite:e0.05:1:action|E:bite:e0.25:5:action|E:bite:e0.48:10:action|X:bite:19:true:action|", all.toString());
    }

    @Test public void stopAfterTheEventStepKeepsItsEventsTheOldObserverLosesThem() {
        Sim s = new Sim();
        s.start("bite", 0.95F, BITE);
        for (int i = 0; i < 9; i++) { s.tick(); s.end(); }
        s.tick();      // age 10: hit fires
        s.stop();      // a script stops it in the same tick, after the entity's update
        String[] r = s.end();
        assertEquals("E:bite:e0.48:10:action|X:bite:10:false:action", r[0]);
        assertEquals("old model drops the fired step", "X:bite:9:false", r[1]);
    }

    @Test public void stopAtLengthMinusOneIsNotFinishedTheOldObserverSaysFinished() {
        Sim s = new Sim();
        s.start("bite", 0.95F, BITE);
        for (int i = 0; i < 18; i++) { s.tick(); s.end(); }
        s.stop();      // before the entity's tick to age 19 (e.g. a world tick START handler)
        String[] r = s.end();
        assertEquals("X:bite:18:false:action", r[0]);
        assertEquals("old model reports a false natural end", "X:bite:19:true", r[1]);
    }

    @Test public void severalTransitionsInOneTickKeepTheirOrder() {
        Sim s = new Sim();
        s.start("bite", 0.95F, BITE);
        s.tick();
        s.start("hurt", 0.45F);
        s.stop();
        s.start("bite", 0.95F, BITE);
        String[] r = s.end();
        assertEquals("S:bite:0:action|E:bite:e0.05:1:action|X:bite:1:false:action|S:hurt:0:action|X:hurt:0:false:action|S:bite:0:action", r[0]);
        assertEquals("old model only sees the last start", "S:bite", r[1]);
    }

    @Test public void stepsWrittenWhileDispatchingGoToTheNextDrain() {
        ActionJournal j = new ActionJournal();
        j.start("bite");
        List<ActionJournal.Step> first = j.drain();
        j.end("bite", 0);          // a hook stops the clip while the first batch is dispatched
        assertEquals(1, first.size());
        assertEquals("[X:bite:0:false:action]", j.drain().toString());
        assertTrue(j.drain().isEmpty());
    }

    @Test public void naturalOnlyMarksAnUnfinishedActionEnd() {
        ActionJournal j = new ActionJournal();
        j.natural();
        j.start("a");
        j.natural();
        assertEquals("[S:a:0:action]", j.drain().toString());
    }

    @Test public void overflowIsBoundedAndCounted() {
        ActionJournal j = new ActionJournal();
        for (int i = 0; i < ActionJournal.LIMIT + 5; i++) { j.start("a"); }
        assertEquals(ActionJournal.LIMIT, j.drain().size());
        assertEquals(5, j.takeDropped());
        assertEquals(0, j.takeDropped());
    }
}
