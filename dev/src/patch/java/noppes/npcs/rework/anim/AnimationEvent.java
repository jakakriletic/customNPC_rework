package noppes.npcs.rework.anim;

import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.NpcEvent;

/** M7.5 (D-095, Q194), P7a (D-098): payload of the optional animation hooks animationStart, animationEnd
 * and animationEvent. Dispatched only with RwAnimHooks=1 (actions) or 2 (actions and the idle, walk and
 * death layers) for an EntityAnimatedNpc of customnpcentities 0.7.0, on the server thread at world tick
 * END of the tick the transition happened in, in the exact order it happened.
 * Script hooks only: these events are not posted to the CustomNPCs API event bus. */
public abstract class AnimationEvent extends NpcEvent {
    /** Values of {@link #source}. */
    public static final String ACTION = "action", IDLE = "idle", WALK = "walk", DEATH = "death";
    /** Clip name (case-sensitive). */
    public final String clip;
    /** Age in server ticks of the action or layer state: 0 at start, the firing age for a clip event,
     * the last age at end. Not the live Animators.getTick(), which is 0 once the action stopped. */
    public final int tick;
    /** action (a clip started by AI or a script), idle, walk or death (the last three only with RwAnimHooks=2). */
    public final String source;

    AnimationEvent(ICustomNpc<?> npc, String clip, int tick, String source) {
        super(npc);
        this.clip = clip;
        this.tick = tick;
        this.source = source;
    }

    /** animationStart: a new action (also the same clip again) or layer state started. */
    public static final class Started extends AnimationEvent {
        public Started(ICustomNpc<?> npc, String clip) { this(npc, clip, ACTION); }
        public Started(ICustomNpc<?> npc, String clip, String source) { super(npc, clip, 0, source); }
    }

    /** animationEnd: the action or layer state is gone. */
    public static final class Ended extends AnimationEvent {
        /** True when the entities mod ended the clip at its length; false when stopped (also at length-1),
         * replaced or cut by death, and when an idle/walk loop is left. */
        public final boolean finished;
        public Ended(ICustomNpc<?> npc, String clip, int tick, boolean finished) { this(npc, clip, tick, finished, ACTION); }
        public Ended(ICustomNpc<?> npc, String clip, int tick, boolean finished, String source) { super(npc, clip, tick, source); this.finished = finished; }
    }

    /** animationEvent: one timed event from the clip's events list. For an action it is the event the
     * server fired; for idle/walk/death it is the event on the entities mod's own clock (hook only). */
    public static final class ClipEvent extends AnimationEvent {
        /** Event type from the clip (hit, sound, shake, step, ...). */
        public final String type;
        /** Event time in seconds from the clip start. */
        public final float time;
        /** The event's JSON object as written in the clip. */
        public final String data;
        public ClipEvent(ICustomNpc<?> npc, String clip, int tick, String type, float time, String data) {
            this(npc, clip, tick, type, time, data, ACTION);
        }
        public ClipEvent(ICustomNpc<?> npc, String clip, int tick, String type, float time, String data, String source) {
            super(npc, clip, tick, source); this.type = type; this.time = time; this.data = data;
        }
    }
}
