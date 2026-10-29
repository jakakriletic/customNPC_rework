package noppes.npcs.rework.anim;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Deterministic queue policy, independent of Minecraft and reflection. */
final class ClipQueue {
    interface Driver {
        boolean writable();
        boolean playing();
        String[] clips();
        boolean play(String name);
        void stop();
    }
    private final Driver driver;
    private final Deque<String> pending = new ArrayDeque<>();
    ClipQueue(Driver driver) { this.driver = driver; }
    private boolean accepts(String name) {
        return name != null && driver.writable() && Arrays.asList(driver.clips()).contains(name);
    }
    boolean play(String name) {
        if (!accepts(name) || !driver.play(name)) { return false; }
        clear();
        return true;
    }
    boolean queue(String name) {
        if (!accepts(name) || pending.size() >= 32) { return false; }
        if (pending.isEmpty() && !driver.playing()) { return driver.play(name); }
        pending.addLast(name);
        return true;
    }
    boolean stop() {
        if (!driver.writable()) { return false; }
        driver.stop();
        clear();
        return true;
    }
    void tick() {
        if (!driver.writable()) { clear(); return; }
        if (hasPending() && !driver.playing()) {
            String name = pending.removeFirst();
            if (!driver.play(name)) { clear(); }
        }
    }
    boolean hasPending() { return !pending.isEmpty(); }
    void clear() { pending.clear(); }
}
