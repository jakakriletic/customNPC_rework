package local.customnpcs;

import static org.junit.Assert.assertEquals;

import noppes.npcs.rework.diag.FarProbe;
import org.junit.Test;

/** M5-V.1: povzetek velikosti spawn NBT ({@code /rwdiag spawnnbt}), percentil z rangom ceil(p * n). */
public class FarProbeTest {
    @Test
    public void praznoJeVseNic() {
        assertEquals("n=0 min=0 p50=0 p95=0 max=0 vsota=0", FarProbe.sizeSummary(new int[0]));
    }

    @Test
    public void percentiliNeurejenegaVzorca() {
        int[] s = new int[20];
        for (int i = 0; i < 20; i++) {
            s[i] = 20 - i; // 20..1
        }
        assertEquals("n=20 min=1 p50=10 p95=19 max=20 vsota=210", FarProbe.sizeSummary(s));
        assertEquals("vhod ostane nespremenjen", 20, s[0]);
    }

    @Test
    public void enVzorec() {
        assertEquals("n=1 min=812 p50=812 p95=812 max=812 vsota=812", FarProbe.sizeSummary(new int[] {812}));
    }
}
