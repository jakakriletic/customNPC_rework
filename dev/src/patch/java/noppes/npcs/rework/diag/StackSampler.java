package noppes.npcs.rework.diag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vzorcevalnik sklada ene niti (M5-K.1), za profil render niti klienta.
 *
 * <p>JFR v JDK 8 na periodo vzorci le nekaj niti, nativne niti pa si delijo en vzorec na 20 ms;
 * render nit je vecino casa v JNI (GL gonilnik, {@code Display.update}), zato je v posnetku
 * 60 s dobila samo nekaj deset vzorcev. Ta vzorcevalnik gleda samo eno nit, ne glede na stanje
 * (Java ali JNI). {@code Thread.getStackTrace} cilja ustavi na varni tocki, zato so Java okvirji
 * pristranski k varnim tockam (kot pri vsakem Java vzorcevalniku), nativni pa ne; strosek je
 * zapisan ({@link #avgCostMicros()}) in zagon z vzorcevalnikom ni baseline.
 *
 * <p>Izhod: "zlozeni skladi" (koren → list, okvirji {@code razred.metoda}, locilo {@code ;}),
 * ena vrstica na razlicen sklad: {@code stevilo<TAB>sklad}. Brez Minecraft tipov.
 */
public final class StackSampler implements Runnable {
    private final Thread target;
    private final long intervalMs;
    private final int maxDepth;
    private final Map<String, int[]> folded = new HashMap<String, int[]>();
    private final int[] states = new int[Thread.State.values().length];
    private volatile boolean running;
    private Thread worker;
    private int samples;
    private long costNs;

    public StackSampler(Thread target, int intervalMs, int maxDepth) {
        if (intervalMs < 1 || maxDepth < 1) {
            throw new IllegalArgumentException("interval in globina morata biti >= 1");
        }
        this.target = target;
        this.intervalMs = intervalMs;
        this.maxDepth = maxDepth;
    }

    public synchronized void start() {
        if (this.worker != null) {
            return;
        }
        this.running = true;
        this.worker = new Thread(this, "RwHarness-vzorcevalnik");
        this.worker.setDaemon(true);
        this.worker.setPriority(Thread.MAX_PRIORITY);
        this.worker.start();
    }

    /** Ustavi in pocaka; po vrnitvi so stevci varno berljivi (join). */
    public synchronized void stop() {
        if (this.worker == null) {
            return;
        }
        this.running = false;
        try {
            this.worker.join(5000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        while (this.running) {
            long t0 = System.nanoTime();
            StackTraceElement[] st = this.target.getStackTrace();
            Thread.State s = this.target.getState();
            if (st.length > 0) {
                record(st, s);
            }
            this.costNs += System.nanoTime() - t0;
            try {
                Thread.sleep(this.intervalMs);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    void record(StackTraceElement[] st, Thread.State s) {
        this.samples++;
        this.states[s.ordinal()]++;
        String k = fold(st, this.maxDepth);
        int[] c = this.folded.get(k);
        if (c == null) {
            this.folded.put(k, new int[] {1});
        } else {
            c[0]++;
        }
    }

    /** {@code st[0]} je vrh (list); izhod od korena proti listu, najvec {@code maxDepth} okvirjev ob listu. */
    static String fold(StackTraceElement[] st, int maxDepth) {
        int n = Math.min(st.length, maxDepth);
        StringBuilder b = new StringBuilder(n * 48);
        if (st.length > n) {
            b.append("(odrezano);");
        }
        for (int i = n - 1; i >= 0; i--) {
            b.append(st[i].getClassName()).append('.').append(st[i].getMethodName());
            if (i > 0) {
                b.append(';');
            }
        }
        return b.toString();
    }

    public int samples() {
        return this.samples;
    }

    public int distinctStacks() {
        return this.folded.size();
    }

    public int samplesInState(Thread.State s) {
        return this.states[s.ordinal()];
    }

    public double avgCostMicros() {
        return this.samples == 0 ? 0.0 : this.costNs / 1000.0 / this.samples;
    }

    public void writeFolded(File f) throws Exception {
        List<Map.Entry<String, int[]>> e = new ArrayList<Map.Entry<String, int[]>>(this.folded.entrySet());
        Collections.sort(e, new Comparator<Map.Entry<String, int[]>>() {
            @Override
            public int compare(Map.Entry<String, int[]> a, Map.Entry<String, int[]> b) {
                return b.getValue()[0] - a.getValue()[0];
            }
        });
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
        try {
            for (Map.Entry<String, int[]> x : e) {
                w.write(Integer.toString(x.getValue()[0]));
                w.write('\t');
                w.write(x.getKey());
                w.write('\n');
            }
        } finally {
            w.close();
        }
    }
}
