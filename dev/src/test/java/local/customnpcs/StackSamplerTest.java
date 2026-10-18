package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import noppes.npcs.rework.diag.StackSampler;
import org.junit.Test;

/** M5-K.1: vzorcevalnik render niti — zlaganje sklada in vzorcenje druge niti. */
public class StackSamplerTest {
    private static StackTraceElement el(String c, String m) {
        return new StackTraceElement(c, m, c + ".java", 1);
    }

    @Test
    public void zlaganjeOdKorenaProtiListu() throws Exception {
        java.lang.reflect.Method fold = StackSampler.class.getDeclaredMethod("fold", StackTraceElement[].class, int.class);
        fold.setAccessible(true);
        StackTraceElement[] st = { el("a.List", "vrh"), el("a.Sredina", "klic"), el("a.Koren", "main") };
        assertEquals("a.Koren.main;a.Sredina.klic;a.List.vrh", fold.invoke(null, st, 10));
        assertEquals("globina obdrzi okvirje ob listu", "(odrezano);a.Sredina.klic;a.List.vrh", fold.invoke(null, st, 2));
    }

    private static volatile boolean vrti;

    private static void vrtiSeVZnaniMetodi() {
        long x = 0;
        while (vrti) {
            x += System.nanoTime() & 1;
        }
        if (x == 42) {
            System.out.print("");
        }
    }

    @Test
    public void vzorciDrugoNitInZapiseZlozeneSklade() throws Exception {
        vrti = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                vrtiSeVZnaniMetodi();
            }
        }, "cilj");
        t.start();
        StackSampler s = new StackSampler(t, 1, 64);
        s.start();
        Thread.sleep(300);
        s.stop();
        vrti = false;
        t.join();
        assertTrue("vsaj nekaj vzorcev v 300 ms: " + s.samples(), s.samples() >= 20);
        assertEquals("cilj se ves cas vrti", s.samples(), s.samplesInState(Thread.State.RUNNABLE));
        File f = File.createTempFile("skladi", ".txt");
        try {
            s.writeFolded(f);
            List<String> v = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            int vsota = 0;
            boolean najden = false;
            for (String l : v) {
                String[] d = l.split("\t", 2);
                vsota += Integer.parseInt(d[0]);
                najden |= d[1].contains("StackSamplerTest.vrtiSeVZnaniMetodi");
            }
            assertEquals("vsota vrstic = stevilo vzorcev", s.samples(), vsota);
            assertTrue("znana metoda je v skladih", najden);
        } finally {
            f.delete();
        }
    }
}
