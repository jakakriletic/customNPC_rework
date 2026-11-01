package noppes.npcs.rework.anim;

/** Optional server-side action animator for customnpcentities 0.7.0. No client locomotion.
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
    /** P7b: like {@link #play} (same guards, clears pending clips), and every client tracking the NPC
     * moves linearly from the pose it last drew to the new clip over {@code ticks} (0..200; 0 = play).
     * Props switch at once; death, another action or a new blend end the transition. Needs
     * RwAnimHooks>=1 on the server, otherwise false without changes; a client without RwAnimHooks>=1
     * just plays the clip. Out-of-range ticks, unknown clips and off-thread calls return false. */
    boolean blendTo(String clip, int ticks);
    /** Current action clip, or the empty string when idle/unavailable. */
    String getClip();
    /** Age of the action in server ticks, or 0 when idle/unavailable. */
    int getTick();
    /** Sorted clip names as a fresh array; empty when unavailable. Names are case-sensitive. */
    String[] getClips();
}
