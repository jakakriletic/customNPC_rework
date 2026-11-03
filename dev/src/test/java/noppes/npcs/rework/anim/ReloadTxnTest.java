package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import java.util.UUID;
import org.junit.Test;

/** P7c: a reload commits only after every player confirmed this revision and hash. */
public class ReloadTxnTest {
    private static final byte[] HASH = ReloadCodec.hash(new byte[]{1}), OTHER = ReloadCodec.hash(new byte[]{2});
    private final UUID a = new UUID(0, 1), b = new UUID(0, 2);

    private ReloadTxn txn() {
        ReloadTxn t = new ReloadTxn(3, HASH, 100);
        t.expect(a, "A"); t.expect(b, "B");
        return t;
    }

    @Test public void noPlayersCommitsAtOnce() { assertEquals(ReloadTxn.State.COMMIT, new ReloadTxn(1, HASH, 100).state(0)); }

    @Test public void commitOnlyAfterEveryConfirmation() {
        ReloadTxn t = txn();
        assertEquals(ReloadTxn.State.WAITING, t.state(0));
        t.ack(a, 3, true, HASH);
        assertEquals(ReloadTxn.State.WAITING, t.state(0));
        t.ack(b, 3, true, HASH);
        assertEquals(ReloadTxn.State.COMMIT, t.state(0));
    }

    @Test public void refusalOrWrongHashAborts() {
        ReloadTxn t = txn();
        t.ack(a, 3, false, HASH);
        assertEquals(ReloadTxn.State.ABORT, t.state(0));
        assertTrue(t.failure(), t.failure().contains("A"));
        ReloadTxn u = txn();
        u.ack(b, 3, true, OTHER);
        assertEquals(ReloadTxn.State.ABORT, u.state(0));
    }

    @Test public void staleRevisionAndStrangersChangeNothing() {
        ReloadTxn t = txn();
        t.ack(a, 2, false, OTHER);
        t.ack(new UUID(0, 9), 3, false, OTHER);
        assertEquals(ReloadTxn.State.WAITING, t.state(0));
    }

    @Test public void deadlineAbortsAndNamesWhoIsMissing() {
        ReloadTxn t = txn();
        t.ack(a, 3, true, HASH);
        assertEquals(ReloadTxn.State.WAITING, t.state(99));
        assertEquals(ReloadTxn.State.ABORT, t.state(100));
        assertTrue(t.failure(), t.failure().contains("B") && !t.failure().contains("A"));
    }

    @Test public void aPlayerWhoLeavesIsNoLongerWaitedFor() {
        ReloadTxn t = txn();
        t.ack(a, 3, true, HASH);
        t.left(b);
        assertEquals(ReloadTxn.State.COMMIT, t.state(0));
    }
}
