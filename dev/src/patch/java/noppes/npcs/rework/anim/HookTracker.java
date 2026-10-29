package noppes.npcs.rework.anim;

import java.util.ArrayList;
import java.util.List;

/** M7.5: turns one observation per server tick of an animated NPC's action into hook steps.
 * No Minecraft types; the caller supplies the raw synced action (sequence and index, 0 = none),
 * the action age and the clip. Clip events for age a are those the entity fired when its age went
 * from a-1 to a, so the steps repeat exactly what the entities mod fired on the server. */
public final class HookTracker {
    /** Read-only view of one clip. */
    public interface Clip {
        String name();
        int lengthTicks();
        /** Events the entity fires on the step to {@code age} (from = age 1 ? -1 : (age-1)/20, to = min(age/20, length)). */
        void fired(int age, List<Fired> out);
    }

    public static final class Fired {
        public final String type, data;
        public final float time;
        public Fired(String type, float time, String data) { this.type = type; this.time = time; this.data = data; }
    }

    public enum Kind { START, EVENT, END }

    public static final class Step {
        public final Kind kind;
        public final String clip;
        public final int tick;
        /** END only. */
        public final boolean finished;
        /** EVENT only. */
        public final Fired event;
        Step(Kind kind, String clip, int tick, boolean finished, Fired event) {
            this.kind = kind; this.clip = clip; this.tick = tick; this.finished = finished; this.event = event;
        }
        @Override public String toString() {
            switch (kind) {
                case START: return "S:" + clip;
                case END: return "X:" + clip + ":" + tick + ":" + finished;
                default: return "E:" + event.type + ":" + tick;
            }
        }
    }

    private int lastRaw;
    private Clip lastClip;
    private int lastAge;
    private final List<Fired> fired = new ArrayList<>();

    /** @param raw synced action value (a new start changes it, also for the same clip); 0 = idle
     * @param clip clip of the action, null when idle or unknown (the entity drops an unknown action)
     * @param age action age after this tick's update
     * @return steps in dispatch order: end of the previous action, start, then clip events */
    public List<Step> observe(int raw, Clip clip, int age) {
        List<Step> out = new ArrayList<>(2);
        if (clip == null) { raw = 0; }
        if (lastRaw != 0 && raw != lastRaw) {
            // The entity stops a clip on the step to lengthTicks, after firing that step's events.
            int length = lastClip.lengthTicks();
            boolean finished = lastAge + 1 >= length;
            if (finished) { events(lastClip, lastAge + 1, length, out); }
            out.add(new Step(Kind.END, lastClip.name(), finished ? Math.max(length, lastAge) : lastAge, finished, null));
        }
        if (raw != 0) {
            int from = raw == lastRaw ? lastAge + 1 : 1;
            if (raw != lastRaw) { out.add(new Step(Kind.START, clip.name(), 0, false, null)); }
            events(clip, from, age, out);
        }
        lastRaw = raw; lastClip = raw == 0 ? null : clip; lastAge = raw == 0 ? 0 : age;
        return out;
    }

    private void events(Clip clip, int from, int to, List<Step> out) {
        for (int age = Math.max(1, from); age <= to; age++) {
            fired.clear();
            clip.fired(age, fired);
            for (Fired f : fired) { out.add(new Step(Kind.EVENT, clip.name(), age, false, f)); }
        }
    }
}
