package noppes.npcs.rework.anim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClipQueueTest {
    static final class Driver implements ClipQueue.Driver {
        boolean writable = true, busy, reject;
        final List<String> started = new ArrayList<>();
        public boolean writable() { return writable; }
        public boolean playing() { return busy; }
        public String[] clips() { return new String[]{"a", "b", "c"}; }
        public boolean play(String name) { if (reject) { return false; } busy = true; started.add(name); return true; }
        public void stop() { busy = false; }
    }
    @Test public void idleStartsNowAndBusyRunsFifoIncludingRepeatedClip() {
        Driver d = new Driver(); ClipQueue q = new ClipQueue(d);
        assertTrue(q.queue("a")); assertTrue(q.queue("b")); assertTrue(q.queue("b"));
        q.tick(); assertEquals(Arrays.asList("a"), d.started);
        d.busy = false; q.tick(); assertEquals(Arrays.asList("a", "b"), d.started);
        d.busy = false; q.tick(); assertEquals(Arrays.asList("a", "b", "b"), d.started);
        assertFalse(q.hasPending());
    }
    @Test public void invalidPlayDoesNotCancelCurrentOrPendingButValidPlayDoes() {
        Driver d = new Driver(); ClipQueue q = new ClipQueue(d);
        q.play("a"); q.queue("b");
        assertFalse(q.play("missing")); assertFalse(q.play(null)); assertFalse(q.queue("A"));
        assertTrue(q.hasPending()); assertTrue(d.busy);
        assertTrue(q.play("c")); assertFalse(q.hasPending());
        d.busy = false; q.tick(); assertEquals(Arrays.asList("a", "c"), d.started);
    }
    @Test public void boundsQueueWithoutSilentlyDroppingExistingClips() {
        Driver d = new Driver(); ClipQueue q = new ClipQueue(d); q.play("a");
        for (int i = 0; i < 32; i++) { assertTrue(q.queue("b")); }
        assertFalse(q.queue("c"));
        for (int i = 0; i < 32; i++) { d.busy = false; q.tick(); }
        assertEquals(33, d.started.size()); assertFalse(d.started.contains("c")); assertFalse(q.hasPending());
    }
    @Test public void stopAndUnloadClearQueueWithoutLaterResurrection() {
        Driver d = new Driver(); ClipQueue q = new ClipQueue(d); q.play("a"); q.queue("b");
        assertTrue(q.stop()); assertFalse(d.busy); q.tick(); assertEquals(1, d.started.size());
        q.play("a"); q.queue("b"); q.clear(); d.busy = false; q.tick();
        assertEquals(2, d.started.size());
    }
    @Test public void externalActionKeepsQueueWaitingAndUnavailableCancelsIt() {
        Driver d = new Driver(); ClipQueue q = new ClipQueue(d); q.play("a"); q.queue("b");
        for (int i = 0; i < 100; i++) { q.tick(); }
        assertEquals(1, d.started.size());
        d.writable = false; assertFalse(q.stop()); assertFalse(q.play("c")); assertFalse(q.queue("c"));
        q.tick(); assertFalse(q.hasPending());
        d.writable = true; d.busy = false; q.tick(); assertEquals(1, d.started.size());
    }
    @Test public void rejectedImmediatePlayPreservesQueueAndFailedDeferredStartCancelsRest() {
        Driver d = new Driver(); ClipQueue q = new ClipQueue(d); q.play("a"); q.queue("b"); q.queue("c");
        d.reject = true; assertFalse(q.play("c")); assertTrue(q.hasPending());
        d.busy = false; q.tick(); assertFalse(q.hasPending()); assertEquals(Arrays.asList("a"), d.started);
    }
}
