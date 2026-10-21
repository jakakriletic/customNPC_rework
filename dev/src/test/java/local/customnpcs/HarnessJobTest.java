package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Properties;

import noppes.npcs.rework.diag.HarnessJob;

import org.junit.Test;

/** M5-K.0: navodilo harnessa, ki ga zapise k-run.ps1. */
public class HarnessJobTest {

    private static Properties base() {
        Properties p = new Properties();
        p.setProperty("world", "rwk-idle-200");
        p.setProperty("cam", "60.5, 12, -30, 180, 30");
        p.setProperty("out", "C:/tmp/k.json");
        return p;
    }

    @Test
    public void expectNpcMinDovoliPadleVBoju() {
        Properties p = base();
        p.setProperty("expectNpc", "201");
        p.setProperty("expectNpcMin", "180");
        HarnessJob j = HarnessJob.parse(p);
        assertEquals(201, j.expectNpc);
        assertEquals(180, j.expectNpcMin);
    }

    @Test
    public void parsesFullJobInOrder() {
        Properties p = base();
        p.setProperty("cmd.1", "/noppes slay npcs 2000");
        p.setProperty("cmd.2", "noppes clone grid PERF_Idle 1 25 8 48,4,-12");
        p.setProperty("cmd.4", "ignored - vrzel v stevilcenju konca seznam");
        p.setProperty("expectNpc", "200");
        p.setProperty("renderDistance", "12");
        p.setProperty("fov", "90");
        p.setProperty("hideGui", "1");
        p.setProperty("sampleMs", "2");
        p.setProperty("settle", "3");
        p.setProperty("warmup", "30");
        p.setProperty("measure", "60");
        HarnessJob j = HarnessJob.parse(p);
        assertEquals("rwk-idle-200", j.world);
        assertEquals(Arrays.asList("noppes slay npcs 2000", "noppes clone grid PERF_Idle 1 25 8 48,4,-12"), j.commands);
        assertEquals(200, j.expectNpc);
        assertEquals("brez expectNpcMin velja natancno", 200, j.expectNpcMin);
        assertEquals(60.5, j.camX, 0.0);
        assertEquals(12.0, j.camY, 0.0);
        assertEquals(-30.0, j.camZ, 0.0);
        assertEquals(180.0f, j.camYaw, 0.0f);
        assertEquals(30.0f, j.camPitch, 0.0f);
        assertEquals(12, j.renderDistance);
        assertEquals(90.0f, j.fov, 0.0f);
        assertTrue(j.hideGui);
        assertEquals(2, j.sampleMs);
        assertEquals(3, j.settleSeconds);
        assertEquals(30, j.warmupSeconds);
        assertEquals(60, j.measureSeconds);
        assertEquals("C:/tmp/k.json", j.out);
    }

    @Test
    public void defaults() {
        HarnessJob j = HarnessJob.parse(base());
        assertEquals(0, j.commands.size());
        assertEquals(-1, j.expectNpc);
        assertEquals(8, j.renderDistance);
        assertEquals(70.0f, j.fov, 0.0f);
        assertFalse("privzeto GUI viden, da se imena NPC-jev narisejo (M5-K.1)", j.hideGui);
        assertEquals("vzorcevalnik privzeto izklopljen", 0, j.sampleMs);
        assertEquals(5, j.settleSeconds);
        assertEquals(30, j.warmupSeconds);
        assertEquals(60, j.measureSeconds);
    }

    @Test
    public void lodStartIzKonfiguracijeAliZahtevan() {
        assertEquals("M5-K K5: brez kljuca RwLodStart iz konfiguracije", -1, HarnessJob.parse(base()).lodStart);
        Properties p = base();
        p.setProperty("lodStart", "32");
        assertEquals(32, HarnessJob.parse(p).lodStart);
        p.setProperty("lodStart", "0");
        assertEquals(0, HarnessJob.parse(p).lodStart);
    }

    @Test
    public void farViewIzKonfiguracijeAliZahtevan() {
        assertEquals("M5-V.2: brez kljuca RwFarView iz konfiguracije", -1, HarnessJob.parse(base()).farView);
        Properties p = base();
        p.setProperty("farView", "1");
        assertEquals(1, HarnessJob.parse(p).farView);
    }

    @Test
    public void impostorCheckPrivzetoIzklopljen() {
        assertEquals("M5-V.4: brez kljuca ni primerjave impostor - model", false, HarnessJob.parse(base()).impostorCheck);
        Properties p = base();
        p.setProperty("impostorCheck", "1");
        assertEquals(true, HarnessJob.parse(p).impostorCheck);
    }

    @Test
    public void farDrawInSeamPrivzetoIzklopljena() {
        assertEquals("M5-V.5: brez kljuca ni A/B pikslov izrisa L3", false, HarnessJob.parse(base()).farDrawCheck);
        assertEquals("M5-V.5: brez kljuca ni prehoda cez mejo", false, HarnessJob.parse(base()).seamCheck);
        Properties p = base();
        p.setProperty("farDrawCheck", "1");
        p.setProperty("seamCheck", "1");
        assertEquals(true, HarnessJob.parse(p).farDrawCheck);
        assertEquals(true, HarnessJob.parse(p).seamCheck);
    }

    @Test
    public void createSeedMatchesDedicatedServer() {
        Properties p = base();
        assertEquals(null, HarnessJob.parse(p).createSeed);
        p.setProperty("createSeed", " customnpc-rework ");
        assertEquals("customnpc-rework", HarnessJob.parse(p).createSeed);
        assertEquals((long) "customnpc-rework".hashCode(), HarnessJob.seedOf("customnpc-rework"));
        assertEquals(42L, HarnessJob.seedOf("42"));
    }

    @Test
    public void rejectsBadJobs() {
        String[][] bad = {
            {"world", ""},
            {"out", " "},
            {"cam", "1,2,3"},
            {"renderDistance", "40"},
            {"measure", "0"},
            {"warmup", "-1"},
            {"lodStart", "-2"},
            {"lodStart", "513"},
            {"farView", "3"},
            {"farView", "-2"},
            {"connect", ":25565"},
            {"connect", "localhost:0"},
            {"connect", "localhost:x"},
            {"renderWeight", "-1"},
            {"renderWeight", "17"},
        };
        for (String[] kv : bad) {
            Properties p = base();
            p.setProperty(kv[0], kv[1]);
            try {
                HarnessJob.parse(p);
                fail("sprejeto: " + kv[0] + "=" + kv[1]);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    @Test
    public void connectInRenderWeight() {
        HarnessJob brez = HarnessJob.parse(base());
        assertEquals("privzeto integrirani svet", null, brez.connectHost);
        assertEquals(0.0, brez.renderWeight, 0.0);
        Properties p = base();
        p.setProperty("connect", " 127.0.0.1:25599 ");
        p.setProperty("renderWeight", "6");
        HarnessJob j = HarnessJob.parse(p);
        assertEquals("127.0.0.1", j.connectHost);
        assertEquals(25599, j.connectPort);
        assertEquals(6.0, j.renderWeight, 0.0);
        p.setProperty("connect", "localhost");
        assertEquals("brez vrat privzeta 25565", 25565, HarnessJob.parse(p).connectPort);
    }
}
