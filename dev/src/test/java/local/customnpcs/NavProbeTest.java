package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import noppes.npcs.rework.diag.Diag;
import noppes.npcs.rework.diag.NavProbe;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Merila kakovosti navigacije (M2.7) so podlaga za A/B primerjave v M4.10-M4.13 in M5.6.
 * Napaka tukaj ni vidna kot okvara: stevilka se izpise, samo o necem drugem govori, kot
 * mislimo. Testi zato preverjajo predvsem, <b>kaj se v katero velicino ne sme steti</b>.
 */
public class NavProbeTest {

    private static final double EPS = 0.0005;

    @Before
    public void cleanSlate() {
        Diag.setEnabled(false);
        Diag.reset();
    }

    @After
    public void leaveItOff() {
        Diag.setEnabled(false);
    }

    /** Cela pot: zadnja tocka je na cilju. */
    private static void whole(NavProbe probe, double straight, double length, long nanos) {
        probe.record(straight, length, 0.0, true, nanos);
    }

    /** Delna pot: zadnja tocka je od cilja oddaljena {@code endToGoal}. */
    private static void partial(NavProbe probe, double straight, double length, double endToGoal,
            long nanos) {
        probe.record(straight, length, endToGoal, true, nanos);
    }

    @Test
    public void emptyProbeStillWritesAMarkerLineWithZeros() {
        NavProbe probe = new NavProbe();
        String line = probe.markerLine();
        assertTrue(line.startsWith("RWNAV-SONDA "));
        assertTrue(line.contains(" iskanj=0 "));
        assertTrue(line.contains(" delezCelih=0.000 "));
        // Prazna meritev ne sme dobiti razdelka v posnetku, vrstica pa mora obstajati:
        // merilo mora lociti "sonda ni nicesar nasla" od "sonda sploh ni tekla".
        assertEquals("", probe.toText());
    }

    @Test
    public void wholePathCountsAsCompleteAndGivesRatioOne() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, 150000L);
        assertEquals(1L, probe.searches());
        assertEquals(1L, probe.complete());
        assertEquals(0L, probe.partial());
        assertEquals(1.0, probe.completeShare(), EPS);
        assertEquals(1.0, probe.ratioPercentile(0.50), 0.01);
        assertEquals(1.0, probe.reachPercentile(0.50), 0.01);
    }

    @Test
    public void ratioIsPathLengthOverStraightDistance() {
        NavProbe probe = new NavProbe();
        whole(probe, 10.0, 25.0, 1000L);
        assertEquals(2.5, probe.ratioPercentile(0.50), 0.02);
    }

    /**
     * Najpomembnejsi test tega razreda. Razmerje delne poti primerja dolzino poti, ki
     * nikamor ne pride, z razdaljo do cilja, ki ga ni dosegla - cim prej bi iskanje
     * obupalo, tem "boljse" bi bilo razmerje. Delna pot zato v razmerje ne sme.
     */
    @Test
    public void partialPathIsExcludedFromTheRatioButNotFromTheShare() {
        NavProbe probe = new NavProbe();
        partial(probe, 40.0, 4.0, 36.0, 1000L);
        assertEquals(1L, probe.searches());
        assertEquals(0L, probe.complete());
        assertEquals(1L, probe.partial());
        assertEquals(0L, probe.ratioMilli().count());
        assertEquals(0.0, probe.completeShare(), EPS);
        // Doseg pa jo mora zajeti: pot je zaprla desetino vrzeli.
        assertEquals(1L, probe.reachMilli().count());
        assertEquals(0.1, probe.reachPercentile(0.50), 0.01);
    }

    @Test
    public void searchWithoutAPathCountsInTheDenominator() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, 1000L);
        probe.record(20.0, 0.0, 0.0, false, 1000L);
        assertEquals(2L, probe.searches());
        assertEquals(1L, probe.notFound());
        // Iskanje brez poti je za navigacijo enak neuspeh kot delna pot in ga delez
        // celih poti ne sme skriti tako, da ga izpusti iz imenovalca.
        assertEquals(0.5, probe.completeShare(), EPS);
        // Neuspelo iskanje ne prispeva razmerja: razmerje ima samo cela pot.
        assertEquals(1L, probe.ratioMilli().count());
    }

    @Test
    public void goalAtArmsLengthIsCountedButKeptOutOfRatioAndReach() {
        NavProbe probe = new NavProbe();
        whole(probe, 0.5, 3.0, 1000L);
        assertEquals(1L, probe.searches());
        assertEquals(1L, probe.complete());
        assertEquals(1L, probe.tooClose());
        // Razmerje 6,0 pri ciljem pol bloka stran ne pove nicesar o kakovosti navigacije.
        assertEquals(0L, probe.ratioMilli().count());
        assertEquals(0L, probe.reachMilli().count());
    }

    @Test
    public void toleranceBoundaryCountsAsComplete() {
        NavProbe probe = new NavProbe(2.0);
        partial(probe, 30.0, 30.0, 2.0, 1000L);
        assertEquals(1L, probe.complete());
        assertEquals(0L, probe.partial());
        NavProbe strict = new NavProbe(1.0);
        partial(strict, 30.0, 30.0, 2.0, 1000L);
        assertEquals(0L, strict.complete());
        assertEquals(1L, strict.partial());
    }

    @Test
    public void pathThatEndsFurtherThanItStartedHasReachZeroAndNotNegative() {
        NavProbe probe = new NavProbe();
        partial(probe, 10.0, 12.0, 14.0, 1000L);
        assertEquals(1L, probe.reachMilli().count());
        assertEquals(0.0, probe.reachPercentile(0.50), EPS);
    }

    @Test
    public void repeatsCountOnlyTowardsTime() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, 100000L);
        probe.recordTimeOnly(90000L);
        probe.recordTimeOnly(80000L);
        assertEquals(1L, probe.searches());
        assertEquals(1L, probe.complete());
        assertEquals(2L, probe.repeats());
        // Delez celih poti ne sme biti odvisen od stevila ponovitev.
        assertEquals(1.0, probe.completeShare(), EPS);
        assertEquals(3L, probe.searchNanos().count());
    }

    @Test
    public void negativeDurationIsClampedToZero() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, -5L);
        assertEquals(0L, probe.searchNanos().min());
    }

    @Test
    public void microsPercentileReadsNanosAsMicroseconds() {
        NavProbe probe = new NavProbe();
        for (int i = 0; i < 10; i++) {
            whole(probe, 20.0, 20.0, 200000L);
        }
        // 200 000 ns = 200 us; kos porazdelitve je zgornja meja, zato dovolimo 3,2 %.
        assertEquals(200.0, probe.microsPercentile(0.50), 7.0);
    }

    @Test
    public void sweepCounterAndGoalEndUpInTheMarkerLine() {
        NavProbe probe = new NavProbe();
        probe.beginSweep("-30,4,-32");
        whole(probe, 20.0, 22.0, 1000L);
        String line = probe.markerLine();
        assertTrue(line.contains(" pometanj=1 "));
        assertTrue(line.contains(" cilj=-30,4,-32 "));
        assertFalse("vrstica mora biti ena sama", line.contains("\n"));
    }

    @Test
    public void markerLineCarriesEveryQuantityTheScriptReads() {
        NavProbe probe = new NavProbe();
        probe.beginSweep("0,4,0");
        whole(probe, 20.0, 24.0, 300000L);
        partial(probe, 40.0, 10.0, 25.0, 400000L);
        probe.record(20.0, 0.0, 0.0, false, 50000L);
        String line = probe.markerLine();
        String[] needed = {"iskanj=3", "celih=1", "delnih=1", "brezPoti=1", "preblizu=0",
            "delezCelih=0.333", "razmerjeN=1", "dosegN=2"};
        for (int i = 0; i < needed.length; i++) {
            assertTrue("manjka " + needed[i] + " v: " + line, line.contains(needed[i]));
        }
    }

    /**
     * Pometanje ima svoj marker, ker scenarij meri dve progi z razlicnima ciljema: ce bi
     * obe vrstici nosili isti marker, bi bralnik prebral prvo in mislil, da je njegova.
     */
    @Test
    public void markerLineCanCarryAnotherMarker() {
        NavProbe probe = new NavProbe();
        probe.beginSweep("0,4,0");
        whole(probe, 20.0, 20.0, 1000L);
        String line = probe.markerLine("RWNAV-POMET");
        assertTrue(line, line.startsWith("RWNAV-POMET pometanj=1 "));
        assertTrue(line.contains(" iskanj=1 "));
        assertFalse(line.contains("RWNAV-SONDA"));
    }

    @Test
    public void textSectionAppearsOnlyAfterTheFirstSearch() {
        NavProbe probe = new NavProbe();
        assertEquals("", probe.toText());
        whole(probe, 20.0, 20.0, 1000L);
        String text = probe.toText();
        assertTrue(text.contains("kakovost navigacije"));
        assertTrue(text.contains("celih poti"));
    }

    @Test
    public void jsonCarriesTheSameNumbersAsTheMarkerLine() {
        NavProbe probe = new NavProbe();
        probe.beginSweep("1,2,3");
        whole(probe, 20.0, 20.0, 1000L);
        partial(probe, 20.0, 5.0, 15.0, 1000L);
        String json = probe.toJson();
        assertTrue(json.contains("\"searches\":2"));
        assertTrue(json.contains("\"complete\":1"));
        assertTrue(json.contains("\"partial\":1"));
        assertTrue(json.contains("\"goal\":\"1,2,3\""));
        assertTrue(json.contains("\"completeShare\":0.500"));
    }

    @Test
    public void resetClearsCountersAndDistributions() {
        NavProbe probe = new NavProbe();
        probe.beginSweep("0,0,0");
        whole(probe, 20.0, 20.0, 1000L);
        partial(probe, 20.0, 5.0, 15.0, 1000L);
        probe.recordTimeOnly(1000L);
        probe.reset();
        assertEquals(0L, probe.searches());
        assertEquals(0L, probe.complete());
        assertEquals(0L, probe.partial());
        assertEquals(0L, probe.repeats());
        assertEquals(0L, probe.sweeps());
        assertEquals(0L, probe.searchNanos().count());
        assertEquals(0L, probe.ratioMilli().count());
        assertEquals(0L, probe.reachMilli().count());
        assertTrue(probe.markerLine().contains(" cilj=- "));
    }

    @Test
    public void copyIsASnapshotAndDoesNotFollowLaterSearches() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, 1000L);
        NavProbe copy = probe.copy();
        whole(probe, 20.0, 20.0, 1000L);
        assertEquals(2L, probe.searches());
        assertEquals(1L, copy.searches());
        assertEquals(1L, copy.searchNanos().count());
    }

    @Test
    public void diagOwnsOneProbeAndResetClearsIt() {
        Diag.nav().beginSweep("5,6,7");
        whole(Diag.nav(), 20.0, 20.0, 1000L);
        assertEquals(1L, Diag.nav().searches());
        Diag.reset();
        assertEquals(0L, Diag.nav().searches());
        assertEquals(0L, Diag.nav().sweeps());
    }

    /**
     * Posnetek mora nositi kopijo in ne zive sonde, sicer se med izpisom tabela lahko
     * spremeni in posnetek je notranje neskladen.
     */
    @Test
    public void snapshotCarriesACopyOfTheProbe() {
        whole(Diag.nav(), 20.0, 20.0, 1000L);
        NavProbe fromSnapshot = Diag.snapshot().nav();
        whole(Diag.nav(), 20.0, 20.0, 1000L);
        assertEquals(1L, fromSnapshot.searches());
        assertEquals(2L, Diag.nav().searches());
    }

    @Test
    public void snapshotTextContainsTheProbeMarkerLineOnceThereIsData() {
        String before = Diag.snapshot().toText();
        assertFalse(before.contains("RWNAV-SONDA"));
        Diag.nav().beginSweep("0,4,0");
        whole(Diag.nav(), 20.0, 20.0, 1000L);
        String after = Diag.snapshot().toText();
        assertTrue(after.contains("kakovost navigacije"));
        assertTrue(after.contains("RWNAV-SONDA"));
    }

    // --- M2.7b: hladna in ogreta iskanja sta dve populaciji ------------------------------

    /**
     * Jedro M2.7b. Prvo iskanje na NPC placa nalaganje razredov in hladen JIT, ponovitev
     * meri algoritem; percentil cez njuno mesanico ni percentil nicesar. Test to preveri s
     * stevilkami, ki sta si tako narazen, da mesanica ne more dati pravilnega odgovora.
     */
    @Test
    public void firstSearchesAndRepeatsLandInSeparateDistributions() {
        NavProbe probe = new NavProbe();
        for (int i = 0; i < 4; i++) {
            whole(probe, 20.0, 20.0, 1000000L);   // 1000 us, hladno
        }
        for (int i = 0; i < 16; i++) {
            probe.recordTimeOnly(50000L);         // 50 us, ogreto
        }
        assertEquals(1000.0, probe.firstMicrosPercentile(0.50), 0.5);
        assertEquals(50.0, probe.repeatMicrosPercentile(0.50), 0.5);
        // Skupna porazdelitev ostane in je mesanica obeh - prav zato ni merilo za A/B.
        assertEquals(50.0, probe.microsPercentile(0.50), 0.5);
        assertEquals(4L, probe.firstNanos().count());
        assertEquals(16L, probe.repeatNanos().count());
        assertEquals(20L, probe.searchNanos().count());
    }

    /** Ponovitev ne sme steti v hladno porazdelitev, tudi ce je edina meritev. */
    @Test
    public void aRepeatAloneLeavesTheFirstSearchDistributionEmpty() {
        NavProbe probe = new NavProbe();
        probe.recordTimeOnly(70000L);
        assertEquals(0L, probe.firstNanos().count());
        assertEquals(0.0, probe.firstMicrosPercentile(0.50), EPS);
        assertEquals(70.0, probe.repeatMicrosPercentile(0.50), 0.5);
        // Prazna porazdelitev vrne 0 in ne vrze; merilo mora dobiti stevilko.
        assertEquals(1L, probe.searchNanos().count());
    }

    /** Prvo iskanje ne sme steti med ponovitve. */
    @Test
    public void aFirstSearchAloneLeavesTheRepeatDistributionEmpty() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, 250000L);
        assertEquals(0L, probe.repeatNanos().count());
        assertEquals(0.0, probe.repeatMicrosPercentile(0.95), EPS);
        assertEquals(250.0, probe.firstMicrosPercentile(0.50), 0.5);
    }

    /**
     * Vsota je tista stevilka, ki jo proracun ticka dejansko placa, in je za razliko od
     * percentila cez majhen vzorec stabilna. Steti mora vse - tudi iskanja brez poti in
     * tista s preblizu postavljenim ciljem, ki iz razmerja in dosega izpadejo.
     */
    @Test
    public void totalMicrosSumsEverySearchIncludingTheOnesExcludedElsewhere() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, 100000L);                  // 100 us
        probe.record(0.5, 0.0, 0.0, true, 30000L);          // preblizu: 30 us
        probe.record(20.0, 0.0, 0.0, false, 20000L);        // brez poti: 20 us
        probe.recordTimeOnly(50000L);                       // ponovitev: 50 us
        assertEquals(200.0, probe.totalMicros(), 0.5);
        assertEquals(1L, probe.tooClose());
        assertEquals(1L, probe.notFound());
    }

    /** Nova polja morajo biti v vrstici sonde, sicer jih nav-run.ps1 ne more prebrati. */
    @Test
    public void markerLineCarriesTheNewFieldsAtTheEnd() {
        NavProbe probe = new NavProbe();
        probe.beginSweep("0,4,0");
        whole(probe, 20.0, 20.0, 1000000L);
        probe.recordTimeOnly(50000L);
        String line = probe.markerLine();
        assertTrue(line.contains(" prviN=1 "));
        assertTrue(line.contains(" ponN=1 "));
        assertTrue(line.contains(" usSkupaj="));
        // Stara polja morajo ostati pred novimi: ze zapisane meritve in regex berejo po
        // zaporedju, zato novo polje ne sme pristati sredi vrstice.
        assertTrue(line.indexOf(" usMax=") < line.indexOf(" prviN="));
        assertTrue(line.indexOf(" prviN=") < line.indexOf(" usSkupaj="));
    }

    /** Reset in kopija morata zajeti tudi novi porazdelitvi, sicer bi podatki pusceli naprej. */
    @Test
    public void resetAndCopyCoverTheNewDistributions() {
        NavProbe probe = new NavProbe();
        whole(probe, 20.0, 20.0, 1000000L);
        probe.recordTimeOnly(50000L);
        NavProbe copy = probe.copy();
        probe.reset();
        assertEquals(0L, probe.firstNanos().count());
        assertEquals(0L, probe.repeatNanos().count());
        assertEquals(0.0, probe.totalMicros(), EPS);
        // Kopija je posnetek in je reset izvirnika ne sme prizadeti.
        assertEquals(1L, copy.firstNanos().count());
        assertEquals(1L, copy.repeatNanos().count());
        assertEquals(1050.0, copy.totalMicros(), 0.5);
    }
}
