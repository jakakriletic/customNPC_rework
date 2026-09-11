package noppes.npcs.rework.data;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.LogWriter;
import noppes.npcs.util.NBTJsonUtil;

/** Owns asynchronous data writes for exactly one server-world session. */
public final class WorldSaveSession {
    private static final Object SESSION_LOCK = new Object();
    private static volatile WorldSaveSession current;

    private final File worldRoot;
    private final ExecutorService executor;
    private final AtomicReference<Throwable> firstFailure = new AtomicReference<Throwable>();
    private boolean accepting = true;

    private WorldSaveSession(File worldRoot) {
        if (worldRoot == null) {
            throw new NullPointerException("worldRoot");
        }
        this.worldRoot = worldRoot.getAbsoluteFile();
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "CustomNPCs World Save");
                thread.setDaemon(false);
                return thread;
            }
        });
    }

    public static void begin(File worldRoot) {
        synchronized (SESSION_LOCK) {
            if (current != null) {
                throw new IllegalStateException("A CustomNPCs world save session is already active");
            }
            current = new WorldSaveSession(worldRoot);
        }
    }

    public static WorldSaveSession current() {
        return current;
    }

    public static boolean end(long timeout, TimeUnit unit) {
        WorldSaveSession session;
        synchronized (SESSION_LOCK) {
            session = current;
            if (session == null) {
                return true;
            }
            session.stopAccepting();
        }

        boolean clean = session.drain(timeout, unit);
        synchronized (SESSION_LOCK) {
            if (current == session) {
                current = null;
            }
        }
        return clean;
    }

    /** Captures both the destination and an independent NBT snapshot before queueing. */
    public boolean savePlayerData(String filename, NBTTagCompound compound) {
        if (filename == null || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Player data filename must not contain a path");
        }
        final File target = new File(new File(worldRoot, "playerdata"), filename).getAbsoluteFile();
        final NBTTagCompound snapshot = compound.copy();
        return submit(new Runnable() {
            @Override
            public void run() {
                try {
                    NBTJsonUtil.SaveFile(target, snapshot);
                } catch (Exception failure) {
                    firstFailure.compareAndSet(null, failure);
                    LogWriter.except(failure);
                }
            }
        });
    }

    public File getWorldRoot() {
        return worldRoot;
    }

    private synchronized boolean submit(Runnable task) {
        if (!accepting) {
            return false;
        }
        try {
            executor.execute(task);
            return true;
        } catch (RejectedExecutionException rejected) {
            firstFailure.compareAndSet(null, rejected);
            return false;
        }
    }

    private synchronized void stopAccepting() {
        accepting = false;
        executor.shutdown();
    }

    private boolean drain(long timeout, TimeUnit unit) {
        boolean terminated = false;
        try {
            terminated = executor.awaitTermination(timeout, unit);
            if (!terminated) {
                executor.shutdownNow();
                terminated = executor.awaitTermination(timeout, unit);
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            firstFailure.compareAndSet(null, interrupted);
            Thread.currentThread().interrupt();
        }
        return terminated && firstFailure.get() == null;
    }
}
