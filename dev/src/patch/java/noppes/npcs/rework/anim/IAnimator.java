package noppes.npcs.rework.anim;

/** Optional server-side action animator for customnpcentities 0.7.0. No client locomotion/blending.
 * Use from the server thread. Queues are transient, shared by all handles/tabs for one entity,
 * and discarded on stop, successful play, death or unload; they are not saved in NBT. */
public interface IAnimator {
    /** True for a live EntityAnimatedNpc with the supported optional API. */
    boolean isAvailable();
    /** Starts a known clip immediately and clears pending clips on success. False leaves playback unchanged. */
    boolean play(String clip);
    /** Starts now if idle, otherwise appends FIFO (at most 32 pending). Unknown/null clips,
     * unavailable or off-thread targets and a full queue return false without changes.
     * External actions may interrupt playback; pending clips wait until the entity is idle. */
    boolean queue(String clip);
    /** Clears pending clips and stops the current action; false for an unavailable/off-thread target. */
    boolean stop();
    /** Current action clip, or the empty string when idle/unavailable. */
    String getClip();
    /** Age of the action in server ticks, or 0 when idle/unavailable. */
    int getTick();
    /** Sorted clip names as a fresh array; empty when unavailable. Names are case-sensitive. */
    String[] getClips();
}
