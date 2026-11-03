package si.ladja.xaerofleet.network;

public final class CommandFeedback {
    public final boolean accepted;
    public final int acceptedCount;
    public final int rejectedCount;
    public final String reason;

    public CommandFeedback(boolean accepted, int acceptedCount, int rejectedCount, String reason) {
        this.accepted = accepted;
        this.acceptedCount = Math.max(0, acceptedCount);
        this.rejectedCount = Math.max(0, rejectedCount);
        String safe = reason == null ? "" : reason;
        this.reason = safe.length() <= 64 ? safe : safe.substring(0, 64);
    }
}
