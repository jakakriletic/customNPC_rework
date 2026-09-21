package noppes.npcs.rework.diag;

import java.util.Locale;

/**
 * Merila kakovosti navigacije (M2.7).
 *
 * <p><b>Zakaj obstaja.</b> Paketi M4.10-M4.13 in M5.6 posegajo v iskanje poti. Brez
 * stevilk, izmerjenih na originalu, po {@code docs/05-SEJA-PROTOKOL.md} nobenega od njih
 * ni dovoljeno razglasiti za izboljsavo: "NPC-ji zdaj hodijo lepse" ni meritev. Ta razred
 * hrani tri od sestih velicin, ki jih je mogoce izmeriti na eni sami poizvedbi:
 *
 * <ol>
 *   <li><b>delez celih poti</b> - koliko iskanj vrne pot do cilja in ne delne poti, ki se
 *       konca sredi poti. Vanilla A* se prekine po 200 vozliscih
 *       ({@code PathFinder.findPath:65}) in vrne pot do najblizje dosezene tocke, zato je
 *       delna pot pravilo in ne izjema;</li>
 *   <li><b>razmerje dolzine</b> - dolzina poti proti zracni razdalji. Vanilla racuna ceno
 *       in hevristiko manhattansko ({@code PathPoint.distanceManhattan:86}) tudi pri
 *       osemsmernem gibanju, zato poti sistematicno bezijo od diagonal; razmerje to pove
 *       s stevilko;</li>
 *   <li><b>doseg delne poti</b> - koliksen del vrzeli do cilja pot zapre. Loci delno pot,
 *       ki pride skoraj do cilja, od take, ki obtici takoj za vogalom.</li>
 * </ol>
 *
 * <p>Cetrta velicina, <b>cas iskanja</b>, se meri ob istem klicu in je tu kot porazdelitev
 * v nanosekundah - od M2.7b v <b>treh</b> porazdelitvah: prva iskanja (eno na NPC, hladno),
 * ponovitve (ogreto) in vsota obojega. Locitev je prisla iz M2.5c: percentil cez mesanico
 * hladnih in ogretih iskanj je nihal do 114 % med sicer identicnimi ponovitvami serije,
 * ker je merilo dve populaciji hkrati in nobene dovolj dolgo. Preostali dve (cas skupine do cilja in razpon skupine na ozkem grlu) sta
 * lastnosti scenarija, ne posamezne poizvedbe, in ju meri {@code nav-control.js}.
 *
 * <p>Razred je namenoma brez Minecraft tipov, da je enotsko testljiv; iskanja opravi
 * {@link NavSweep} in rezultate preda sem. Metode so {@code synchronized}: sonda tece na
 * server niti, izpis pa lahko pride s konzole.
 */
public final class NavProbe {
    /**
     * Do te razdalje od cilja se pot se steje za celo.
     *
     * <p>Meja ni 0, ker so tocke poti blokovne koordinate, cilj pa sredina bloka; ista
     * meja je uporabljena v {@code r2-control.js}. Vecja meja bi delne poti prikazala kot
     * cele, manjsa bi kot delne prikazala poti, ki so cilj dosegle.
     */
    public static final double DEFAULT_COMPLETE_TOLERANCE = 2.0;

    /**
     * Pod to zracno razdaljo se razmerje in doseg ne merita.
     *
     * <p>Pri cilju tik pred nosom je razmerje dolzina/razdalja poljubno veliko tudi pri
     * povsem normalni poti (deljenje z majhno stevilko), zato bi take poizvedbe pokvarile
     * porazdelitev. Stejejo se posebej, da je iz izpisa vidno, koliko jih je bilo.
     */
    public static final double MIN_STRAIGHT_BLOCKS = 1.0;

    /** Razmerje in doseg sta v porazdelitvi shranjena kot tisocinke. */
    private static final double MILLI = 1000.0;

    private final double tolerance;

    // Porazdelitve niso final samo zato, ker jih {@link #copy()} zamenja s kopijami;
    // izven kopiranja se referenca ne spremeni.
    private Distribution searchNanos = new Distribution("nav.search.ns", "ns");
    // M2.7b: prvo iskanje na NPC in ponovitve sta dve populaciji, ne ena. Prvo placa
    // nalaganje razredov, hladen JIT in prazen predpomnilnik chunkov; ponovitev meri
    // samo algoritem. Percentil nad njuno mesanico ni percentil nicesar - pri osmih
    // NPC-jih in treh prehodih je bil p95 prakticno "drugo najpocasnejse hladno iskanje
    // od osmih", od tod razpon do 114 % v M2.5c. Porazdelitvi sta zato loceni;
    // searchNanos ostane vsota obeh, ker nanj stojijo ze zapisane meritve.
    private Distribution firstNanos = new Distribution("nav.search.first.ns", "ns");
    private Distribution repeatNanos = new Distribution("nav.search.repeat.ns", "ns");
    private Distribution ratioMilli = new Distribution("nav.path.ratio.milli", "1/1000");
    private Distribution reachMilli = new Distribution("nav.path.reach.milli", "1/1000");

    private long searches;
    private long complete;
    private long partial;
    private long notFound;
    private long tooClose;
    private long repeats;
    private long sweeps;
    private String lastGoal = "-";

    public NavProbe() {
        this(DEFAULT_COMPLETE_TOLERANCE);
    }

    public NavProbe(double completeTolerance) {
        if (completeTolerance < 0.0 || Double.isNaN(completeTolerance)) {
            throw new IllegalArgumentException("toleranca < 0: " + completeTolerance);
        }
        this.tolerance = completeTolerance;
    }

    /** Zabelezi zacetek pometanja; cilj je tu samo zato, da je v izpisu vidno, kam se je merilo. */
    public synchronized void beginSweep(String goal) {
        this.sweeps++;
        this.lastGoal = goal == null || goal.isEmpty() ? "-" : goal;
    }

    /**
     * Zabelezi eno iskanje poti.
     *
     * <p>Razmerje se belezi <b>samo za cele poti</b>. Razmerje delne poti primerja dolzino
     * poti, ki nikamor ne pride, z razdaljo do cilja, ki ga ni dosegla - stevilka bi
     * izgledala tem boljsa, cim prej bi iskanje obupalo.
     *
     * @param straightBlocks zracna razdalja od NPC-ja do cilja
     * @param pathBlocks geometrijska dolzina vrnjene poti (vsota razdalj med tockami)
     * @param endToGoalBlocks razdalja od zadnje tocke poti do cilja
     * @param found ali je iskanje sploh vrnilo pot
     * @param nanos trajanje iskanja
     */
    public synchronized void record(double straightBlocks, double pathBlocks,
            double endToGoalBlocks, boolean found, long nanos) {
        this.searches++;
        long clamped = nanos < 0L ? 0L : nanos;
        this.searchNanos.record(clamped);
        this.firstNanos.record(clamped);
        if (!found) {
            this.notFound++;
            return;
        }
        boolean whole = endToGoalBlocks <= this.tolerance;
        if (whole) {
            this.complete++;
        } else {
            this.partial++;
        }
        if (!(straightBlocks >= MIN_STRAIGHT_BLOCKS)) {
            this.tooClose++;
            return;
        }
        double reach = (straightBlocks - endToGoalBlocks) / straightBlocks;
        if (reach < 0.0) {
            reach = 0.0;
        } else if (reach > 1.0) {
            reach = 1.0;
        }
        this.reachMilli.record(Math.round(reach * MILLI));
        if (whole) {
            this.ratioMilli.record(Math.round(pathBlocks / straightBlocks * MILLI));
        }
    }

    /**
     * Zabelezi ponovitev ze izmerjenega iskanja: steje samo v cas.
     *
     * <p>Ponovitve obstajajo zato, ker je prvo iskanje v JVM-u obremenjeno z nalaganjem
     * razredov in hladnim JIT-om. Skozi {@link #record} ne smejo: ista pot bi bila
     * prestela veckrat in delez celih poti bi bil odvisen od stevila ponovitev, ne od
     * kakovosti navigacije.
     */
    public synchronized void recordTimeOnly(long nanos) {
        this.repeats++;
        long clamped = nanos < 0L ? 0L : nanos;
        this.searchNanos.record(clamped);
        this.repeatNanos.record(clamped);
    }

    public synchronized long searches() {
        return this.searches;
    }

    public synchronized long complete() {
        return this.complete;
    }

    public synchronized long partial() {
        return this.partial;
    }

    /** Iskanja, ki niso vrnila poti (vanilla vrne {@code null}, ce ni premaknila niti ene tocke). */
    public synchronized long notFound() {
        return this.notFound;
    }

    /** Iskanja s preblizu postavljenim ciljem; stejejo v cas, ne pa v razmerje in doseg. */
    public synchronized long tooClose() {
        return this.tooClose;
    }

    /** Ponovitve, ki so stele samo v cas. */
    public synchronized long repeats() {
        return this.repeats;
    }

    public synchronized long sweeps() {
        return this.sweeps;
    }

    public double tolerance() {
        return this.tolerance;
    }

    /**
     * Delez iskanj, ki so vrnila celo pot, med <b>vsemi</b> iskanji.
     *
     * <p>Imenovalec so vsa iskanja in ne samo tista, ki so vrnila pot: iskanje brez poti je
     * za navigacijo enak neuspeh kot delna pot in ga merilo ne sme skriti.
     */
    public synchronized double completeShare() {
        return this.searches == 0L ? 0.0 : (double) this.complete / (double) this.searches;
    }

    public synchronized Distribution searchNanos() {
        return this.searchNanos.copy();
    }

    /** Samo prva iskanja (eno na NPC): cena hladnega iskanja. */
    public synchronized Distribution firstNanos() {
        return this.firstNanos.copy();
    }

    /** Samo ponovitve: cena ogretega iskanja - to je velicina, ki jo A/B v M4.11 in M5.6 primerja. */
    public synchronized Distribution repeatNanos() {
        return this.repeatNanos.copy();
    }

    public synchronized Distribution ratioMilli() {
        return this.ratioMilli.copy();
    }

    public synchronized Distribution reachMilli() {
        return this.reachMilli.copy();
    }

    /** Percentil razmerja dolzina/zracna razdalja; 0, ce se ni bilo cele poti. */
    public synchronized double ratioPercentile(double p) {
        return this.ratioMilli.count() == 0L ? 0.0 : this.ratioMilli.percentile(p) / MILLI;
    }

    /** Percentil dosega (0..1); 0, ce se ni bilo merjene poti. */
    public synchronized double reachPercentile(double p) {
        return this.reachMilli.count() == 0L ? 0.0 : this.reachMilli.percentile(p) / MILLI;
    }

    /** Percentil casa iskanja v mikrosekundah, cez prva iskanja in ponovitve skupaj. */
    public synchronized double microsPercentile(double p) {
        return percentileMicros(this.searchNanos, p);
    }

    /** Percentil casa <b>prvega</b> iskanja na NPC (hladno) v mikrosekundah. */
    public synchronized double firstMicrosPercentile(double p) {
        return percentileMicros(this.firstNanos, p);
    }

    /** Percentil casa <b>ponovitve</b> (ogreto) v mikrosekundah. */
    public synchronized double repeatMicrosPercentile(double p) {
        return percentileMicros(this.repeatNanos, p);
    }

    /**
     * Skupen cas vseh iskanj tega pometanja v mikrosekundah.
     *
     * <p>Vsota je stabilnejsa od percentila cez majhen vzorec in je hkrati tista stevilka,
     * ki jo proracun ticka dejansko placa: percentil pove ceno <i>enega</i> iskanja, vsota
     * pa ceno <i>vseh</i>. M2.5c je pokazal, da so percentili pri osmih vzorcih sumni;
     * vsota to ni, ker sestevanje vzorcev ne izgublja.
     */
    public synchronized double totalMicros() {
        return this.searchNanos.sum() / 1000.0;
    }

    private static double percentileMicros(Distribution d, double p) {
        return d.count() == 0L ? 0.0 : d.percentile(p) / 1000.0;
    }

    public synchronized void reset() {
        this.searchNanos.reset();
        this.firstNanos.reset();
        this.repeatNanos.reset();
        this.ratioMilli.reset();
        this.reachMilli.reset();
        this.searches = 0L;
        this.complete = 0L;
        this.partial = 0L;
        this.notFound = 0L;
        this.tooClose = 0L;
        this.repeats = 0L;
        this.sweeps = 0L;
        this.lastGoal = "-";
    }

    public synchronized NavProbe copy() {
        NavProbe other = new NavProbe(this.tolerance);
        other.searchNanos = this.searchNanos.copy();
        other.firstNanos = this.firstNanos.copy();
        other.repeatNanos = this.repeatNanos.copy();
        other.ratioMilli = this.ratioMilli.copy();
        other.reachMilli = this.reachMilli.copy();
        other.searches = this.searches;
        other.complete = this.complete;
        other.partial = this.partial;
        other.notFound = this.notFound;
        other.tooClose = this.tooClose;
        other.repeats = this.repeats;
        other.sweeps = this.sweeps;
        other.lastGoal = this.lastGoal;
        return other;
    }

    /**
     * Ena vrstica z markerjem, ki jo bere {@code nav-run.ps1}.
     *
     * <p>Vrstica je samostojna in ne razpotegnjena tabela, ker jo skripta bere iz loga
     * dedicated serverja, kjer se vrstice razlicnih niti mesajo. Prazna meritev vrne
     * vrstico s samimi niclami in ne praznega niza: merilo mora lociti "sonda ni nicesar
     * nasla" od "sonda sploh ni tekla".
     */
    public synchronized String markerLine() {
        return markerLine("RWNAV-SONDA");
    }

    /**
     * Ista vrstica pod drugim markerjem.
     *
     * <p>{@link NavSweep} vodi poleg skupne sonde se svojo za posamezno pometanje, ker
     * scenarij meri dve progi z razlicnima ciljema: skupna stevilka bi ju zmesala, merilo
     * pa mora vedeti, katera proga je dala kateri delez celih poti.
     */
    public synchronized String markerLine(String marker) {
        StringBuilder out = new StringBuilder(256);
        out.append(marker).append(" pometanj=").append(this.sweeps)
                .append(" cilj=").append(this.lastGoal)
                .append(" iskanj=").append(this.searches)
                .append(" celih=").append(this.complete)
                .append(" delnih=").append(this.partial)
                .append(" brezPoti=").append(this.notFound)
                .append(" preblizu=").append(this.tooClose)
                .append(" ponovitev=").append(this.repeats)
                .append(" delezCelih=").append(number(this.completeShare()))
                .append(" razmerjeN=").append(this.ratioMilli.count())
                .append(" razmerjeP50=").append(number(this.ratioPercentile(0.50)))
                .append(" razmerjeP95=").append(number(this.ratioPercentile(0.95)))
                .append(" dosegN=").append(this.reachMilli.count())
                .append(" dosegP50=").append(number(this.reachPercentile(0.50)))
                .append(" dosegP05=").append(number(this.reachPercentile(0.05)))
                .append(" usP50=").append(number(this.microsPercentile(0.50)))
                .append(" usP95=").append(number(this.microsPercentile(0.95)))
                .append(" usMax=").append(number(this.searchNanos.max() / 1000.0))
                // M2.7b: od tu naprej so polja dodana na konec vrstice, ker regex v
                // nav-run.ps1 in ze zapisane meritve berejo prejsnji del po zaporedju.
                .append(" prviN=").append(this.firstNanos.count())
                .append(" prviP50=").append(number(this.firstMicrosPercentile(0.50)))
                .append(" prviP95=").append(number(this.firstMicrosPercentile(0.95)))
                .append(" ponN=").append(this.repeatNanos.count())
                .append(" ponP50=").append(number(this.repeatMicrosPercentile(0.50)))
                .append(" ponP95=").append(number(this.repeatMicrosPercentile(0.95)))
                .append(" ponMax=").append(number(this.repeatNanos.max() / 1000.0))
                .append(" usSkupaj=").append(number(this.totalMicros()));
        return out.toString();
    }

    /** Berljiv razdelek posnetka; prazna meritev ne dobi razdelka. */
    public synchronized String toText() {
        if (this.searches == 0L) {
            return "";
        }
        StringBuilder out = new StringBuilder(384);
        out.append("kakovost navigacije (M2.7), pometanj: ").append(this.sweeps)
                .append(", zadnji cilj: ").append(this.lastGoal).append('\n');
        out.append("  iskanj        : ").append(this.searches).append('\n');
        out.append("  celih poti    : ").append(this.complete)
                .append(" (").append(number(this.completeShare() * 100.0)).append(" %)\n");
        out.append("  delnih poti   : ").append(this.partial).append('\n');
        out.append("  brez poti     : ").append(this.notFound).append('\n');
        if (this.tooClose > 0L) {
            out.append("  preblizu cilju: ").append(this.tooClose)
                    .append(" (izloceno iz razmerja in dosega)\n");
        }
        out.append("  razmerje pot/zracna razdalja: p50 ").append(number(this.ratioPercentile(0.50)))
                .append("  p95 ").append(number(this.ratioPercentile(0.95)))
                .append("  (n = ").append(this.ratioMilli.count()).append(")\n");
        out.append("  doseg poti (1 = do cilja)   : p50 ").append(number(this.reachPercentile(0.50)))
                .append("  p05 ").append(number(this.reachPercentile(0.05)))
                .append("  (n = ").append(this.reachMilli.count()).append(")\n");
        out.append("  cas iskanja (us, z ").append(this.repeats).append(" ponovitvami): p50 ").append(number(this.microsPercentile(0.50)))
                .append("  p95 ").append(number(this.microsPercentile(0.95)))
                .append("  max ").append(number(this.searchNanos.max() / 1000.0)).append('\n');
        out.append("    od tega prvo iskanje (hladno): p50 ").append(number(this.firstMicrosPercentile(0.50)))
                .append("  p95 ").append(number(this.firstMicrosPercentile(0.95)))
                .append("  (n = ").append(this.firstNanos.count()).append(")\n");
        out.append("    od tega ponovitve   (ogreto): p50 ").append(number(this.repeatMicrosPercentile(0.50)))
                .append("  p95 ").append(number(this.repeatMicrosPercentile(0.95)))
                .append("  (n = ").append(this.repeatNanos.count()).append(")\n");
        out.append("  cas vseh iskanj skupaj      : ").append(number(this.totalMicros())).append(" us\n");
        return out.toString();
    }

    /** Strojno berljiv izpis; vstavi se v posnetek pod kljuc {@code nav}. */
    public synchronized String toJson() {
        StringBuilder out = new StringBuilder(384);
        out.append("{\"sweeps\":").append(this.sweeps);
        out.append(",\"goal\":\"").append(escape(this.lastGoal)).append('"');
        out.append(",\"searches\":").append(this.searches);
        out.append(",\"complete\":").append(this.complete);
        out.append(",\"partial\":").append(this.partial);
        out.append(",\"notFound\":").append(this.notFound);
        out.append(",\"tooClose\":").append(this.tooClose);
        out.append(",\"repeats\":").append(this.repeats);
        out.append(",\"toleranceBlocks\":").append(number(this.tolerance));
        out.append(",\"completeShare\":").append(number(this.completeShare()));
        out.append(",\"ratio\":{\"n\":").append(this.ratioMilli.count())
                .append(",\"p50\":").append(number(this.ratioPercentile(0.50)))
                .append(",\"p95\":").append(number(this.ratioPercentile(0.95))).append('}');
        out.append(",\"reach\":{\"n\":").append(this.reachMilli.count())
                .append(",\"p50\":").append(number(this.reachPercentile(0.50)))
                .append(",\"p05\":").append(number(this.reachPercentile(0.05))).append('}');
        out.append(",\"searchNanos\":{\"n\":").append(this.searchNanos.count())
                .append(",\"p50\":").append(this.searchNanos.percentile(0.50))
                .append(",\"p95\":").append(this.searchNanos.percentile(0.95))
                .append(",\"max\":").append(this.searchNanos.max())
                .append(",\"sum\":").append(this.searchNanos.sum()).append('}');
        out.append(",\"firstNanos\":{\"n\":").append(this.firstNanos.count())
                .append(",\"p50\":").append(this.firstNanos.percentile(0.50))
                .append(",\"p95\":").append(this.firstNanos.percentile(0.95))
                .append(",\"max\":").append(this.firstNanos.max()).append('}');
        out.append(",\"repeatNanos\":{\"n\":").append(this.repeatNanos.count())
                .append(",\"p50\":").append(this.repeatNanos.percentile(0.50))
                .append(",\"p95\":").append(this.repeatNanos.percentile(0.95))
                .append(",\"max\":").append(this.repeatNanos.max()).append('}');
        out.append('}');
        return out.toString();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", Double.valueOf(value));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public synchronized String toString() {
        return "NavProbe{iskanj=" + this.searches + ", celih=" + this.complete + "}";
    }
}
