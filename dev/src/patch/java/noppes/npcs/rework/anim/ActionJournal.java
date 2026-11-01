package noppes.npcs.rework.anim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** P7a (D-098, Q277): exact per-entity log of what the entities mod did on the server, written by
 * the instrumented EntityAnimatedNpc (AnimJournal) at the moment it happens and drained once per
 * world tick END. Unlike the M7.5 END observer (HookTracker) it keeps every transition of a tick in
 * order (start, event, stop, start), the events of the step a stop follows and the difference
 * between the natural end and a stop at length-1. No Minecraft types. */
public final class ActionJournal {
    /** Steps kept per entity between two drains; more means a runaway script loop. */
    public static final int LIMIT = 256;

    public enum Kind { START, EVENT, END }

    /** One hook step. {@code source} is action, idle, walk or death. */
    public static final class Step {
        public final Kind kind;
        public final String clip, source;
        public final int tick;
        /** END only. */
        public final boolean finished;
        /** EVENT only. */
        public final String type, data;
        public final float time;
        Step(Kind kind, String source, String clip, int tick, boolean finished, String type, float time, String data) {
            this.kind = kind; this.source = source; this.clip = clip; this.tick = tick; this.finished = finished;
            this.type = type; this.time = time; this.data = data;
        }
        static Step start(String source, String clip) { return new Step(Kind.START, source, clip, 0, false, null, 0, null); }
        static Step end(String source, String clip, int tick, boolean finished) { return new Step(Kind.END, source, clip, tick, finished, null, 0, null); }
        static Step event(String source, String clip, int tick, String type, float time, String data) {
            return new Step(Kind.EVENT, source, clip, tick, false, type, time, data);
        }
        @Override public String toString() {
            switch (kind) {
                case START: return "S:" + clip + ":" + tick + ":" + source;
                case END: return "X:" + clip + ":" + tick + ":" + finished + ":" + source;
                default: return "E:" + clip + ":" + type + ":" + tick + ":" + source;
            }
        }
    }

    private List<Step> steps = new ArrayList<>();
    private int dropped;

    public void start(String clip) { add(Step.start(AnimationEvent.ACTION, clip)); }

    /** A stop or replacement of the running action at its current age; first assumed not finished. */
    public void end(String clip, int age) { add(Step.end(AnimationEvent.ACTION, clip, age, false)); }

    /** The entities mod's own end of a clip: the stop just logged is the natural one. */
    public void natural() {
        int last = steps.size() - 1;
        if (last < 0) { return; }
        Step s = steps.get(last);
        if (s.kind == Kind.END && !s.finished && AnimationEvent.ACTION.equals(s.source)) {
            steps.set(last, Step.end(s.source, s.clip, s.tick, true));
        }
    }

    public void event(String clip, int age, String type, float time, String data) {
        add(Step.event(AnimationEvent.ACTION, clip, age, type, time, data));
    }

    void add(Step step) {
        if (steps.size() >= LIMIT) { dropped++; return; }
        steps.add(step);
    }

    /** Steps since the last drain in order; steps logged while they are dispatched go to the next drain. */
    public List<Step> drain() {
        if (steps.isEmpty()) { return Collections.emptyList(); }
        List<Step> out = steps;
        steps = new ArrayList<>();
        return out;
    }

    /** Steps lost to {@link #LIMIT} since the last call (reported as an error by the caller). */
    public int takeDropped() { int d = dropped; dropped = 0; return d; }
}
