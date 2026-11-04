package noppes.npcs.rework.linked;

/**
 * M13.2 (Q391): at most so many group transfers per server tick. A member over the budget keeps its old data and
 * asks again at its next check, so nothing has to be queued. Server thread only. The counters are evidence for
 * {@code /rwlinked info}.
 */
public final class LinkedBudget {
    private long tick = Long.MIN_VALUE;
    private int used;
    public int maxPerTick;
    public long applied;
    public long deferred;
    public long nanos;

    /** @param limit transfers per tick; 0 or less means no limit */
    public boolean tryAcquire(long now, int limit) {
        if (now != tick) {
            tick = now;
            used = 0;
        }
        if (limit > 0 && used >= limit) {
            deferred++;
            return false;
        }
        used++;
        applied++;
        if (used > maxPerTick) {
            maxPerTick = used;
        }
        return true;
    }

    public long microsPerTransfer() {
        return applied == 0 ? 0 : nanos / applied / 1000;
    }
}
