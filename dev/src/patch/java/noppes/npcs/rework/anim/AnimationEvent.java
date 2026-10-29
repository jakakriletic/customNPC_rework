package noppes.npcs.rework.anim;

import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.NpcEvent;

/** M7.5 (D-095, Q194): payload of the optional animation hooks animationStart, animationEnd and
 * animationEvent. Dispatched only with RwAnimHooks=1 for an EntityAnimatedNpc of customnpcentities
 * 0.7.0, on the server thread at world tick END of the tick the transition happened in.
 * Script hooks only: these events are not posted to the CustomNPCs API event bus. */
public abstract class AnimationEvent extends NpcEvent {
    /** Clip name of the action (case-sensitive). */
    public final String clip;
    /** Action age in server ticks: 0 at start, the firing age for a clip event, the last age at end. */
    public final int tick;

    AnimationEvent(ICustomNpc<?> npc, String clip, int tick) {
        super(npc);
        this.clip = clip;
        this.tick = tick;
    }

    /** animationStart: a new action (also the same clip again) was observed. */
    public static final class Started extends AnimationEvent {
        public Started(ICustomNpc<?> npc, String clip) { super(npc, clip, 0); }
    }

    /** animationEnd: the observed action is gone. */
    public static final class Ended extends AnimationEvent {
        /** True when the clip reached its length; false when stopped, replaced or cut by death. */
        public final boolean finished;
        public Ended(ICustomNpc<?> npc, String clip, int tick, boolean finished) { super(npc, clip, tick); this.finished = finished; }
    }

    /** animationEvent: one timed event from the clip's events list fired on the server. */
    public static final class ClipEvent extends AnimationEvent {
        /** Event type from the clip (hit, sound, shake, ...). */
        public final String type;
        /** Event time in seconds from the clip start. */
        public final float time;
        /** The event's JSON object as written in the clip. */
        public final String data;
        public ClipEvent(ICustomNpc<?> npc, String clip, int tick, String type, float time, String data) {
            super(npc, clip, tick); this.type = type; this.time = time; this.data = data;
        }
    }
}
