package noppes.npcs.rework.nav;

/**
 * M4.17 (D-085): preverba vidne crte pri sledenju poti letecega NPC-ja.
 *
 * <p>Vanilla 1.12 {@code PathNavigateFlying.isDirectPathBetweenPoints} prehodi celice med tockama,
 * a nobene ne prebere in vedno vrne {@code true} [K]. {@code PathNavigate.pathFollow} zato leteci
 * NPC vsak tick preskoci na zadnjo tocko poti na isti visini, tudi za vogalom in skozi steno;
 * {@code FlyingMoveHelper.isNotColliding} nato da {@code WAIT}. Izid M4.16 [M]: proga vogal 0/6
 * (NPC-ji obstanejo ob steni), labirint 0/6 brez premika.
 *
 * <p>Pri vklopljenem stikalu je tocka neposredno dosegljiva samo, ce je od NPC-ja najvec
 * {@link #MAX_SKIP} blokov (CustomNPC+ {@code trimPath}, M4.0 §4) in ce hitbox, premaknjen od
 * trenutnega polozaja do tocke v korakih {@link #step}, ne zadane nobenega bloka. Hitbox je
 * zmanjsan za {@link #SHRINK}, da NPC, ki se steni ali tlom dotika ali jih po drsenju malo
 * zadane, ne izgubi vseh kandidatov. Brez tock, ki jih preverba zavrne, ostane trenutna tocka
 * (vanilla zanka v {@code pathFollow} indeksa ne spremeni) [K].
 *
 * <p>To je <b>sprememba obnasanja</b> (D-007): NPC leti ob poti okoli ovir namesto naravnost v
 * oviro. Zato stikalo {@code RwFlyLineOfSight}, privzeto original. Stikalo in stevci so staticni,
 * bere jih samo strezniska nit. Razred nima odvisnosti od sveta, da je pravilo preverjeno z JUnit.
 */
public final class FlightFollow {
    public static final int ORIGINAL = 0;
    public static final int ON = 1;
    public static final int MODES = 2;

    /** Najdaljsi preskok po poti (bloki, evklidsko) - CustomNPC+ {@code trimPath}. */
    public static final double MAX_SKIP = 6.0D;
    /** Zmanjsanje hitboxa na vsaki strani (bloki). */
    public static final double SHRINK = 0.05D;
    /** Najvecji korak premika hitboxa (bloki); manjsi od sirine, da poln blok ne zdrsne vmes. */
    public static final double MAX_STEP = 0.5D;
    /** Najmanjsi korak (bloki), da drobni NPC ne preverja stokrat na blok. */
    public static final double MIN_STEP = 0.1D;

    /** Ali je kvader (koordinate sveta) v trku z bloki. */
    public interface BoxTest {
        boolean blocked(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }

    /**
     * M4.17 poskus 3: celice sveta za {@link #scan}. {@code blocked} doda trkalne skatle celice v
     * seznam (kot zanka {@code World.getCollisionBoxes}) in vrne, ali seznam ni vec prazen.
     */
    public interface Cells {
        boolean loaded(int x, int z);

        boolean blocked(int x, int y, int z);
    }

    private static int mode = ORIGINAL;

    /** Preverbe tocke (klici isDirectPathBetweenPoints v nacinu 1). */
    static long calls;
    /** Zavrnjene, ker so dlje od {@link #MAX_SKIP}. */
    static long tooFar;
    /** Zavrnjene, ker hitbox na poti zadane blok. */
    static long blockedCount;
    /** Proste. */
    static long clear;
    /** M4.17 poskus 2: proste ze po preverbi unije (brez korakov). */
    static long unionClear;
    /** M4.17 poskus 3: prebrane celice v {@link #scan}. */
    static long cellCount;
    /** Q455: zaceti komolci ({@link #elbow}). */
    static long elbowCount;

    /**
     * Q455: za koliko blokov cez oglisce komolca NPC cilja. {@code FlyingMoveHelper} ob razdalji
     * do 0,5 preneha pospesevati (WAIT); brez prestrela NPC obstane 0,5 pod robom.
     */
    public static final double ELBOW_OVERSHOOT = 0.6D;
    /** Q455: najvec tickov enega komolca; potem spet original (zastoj, vanilla {@code clearPath}). */
    public static final int ELBOW_TICKS = 60;
    /** {@code FlyingMoveHelper}: pod to razdaljo do cilja NPC ne pospesuje. */
    static final double MOVE_MIN = 0.5D;

    private FlightFollow() {
    }

    public static boolean isValidMode(int m) {
        return m >= ORIGINAL && m < MODES;
    }

    public static void setMode(int m) {
        mode = isValidMode(m) ? m : ORIGINAL;
    }

    public static int mode() {
        return mode;
    }

    public static boolean on() {
        return mode == ON;
    }

    public static void reset() {
        calls = 0L;
        tooFar = 0L;
        blockedCount = 0L;
        clear = 0L;
        unionClear = 0L;
        cellCount = 0L;
        elbowCount = 0L;
    }

    /** Q455: stevilo zacetih komolcev od zadnjega {@link #reset}. */
    public static long elbows() {
        return elbowCount;
    }

    public static String counters() {
        return "vidKlicev=" + calls + " vidDalec=" + tooFar + " vidZaprtih=" + blockedCount + " vidProstih=" + clear
                + " vidUnija=" + unionClear + " vidCelic=" + cellCount;
    }

    /**
     * M4.17 poskus 3 (Q85): zanka {@code World.getCollisionBoxes(null, aabb, true, list)} (Forge
     * 14.23.5, World:1413-1486) brez {@code GetCollisionBoxesEvent} v vsaki celici. Isti obseg
     * (kvader, razsirjen za 1; brez kotnih stolpcev; brez zgornje plasti na robu), isti pogoj
     * nalozenega stolpca ({@code y = 64}), isti izhod pri meji sveta +-30.000.000 in isti zgodnji
     * izhod ob prvi skatli. Dogodek poklice klicatelj enkrat pred zanko, kot original v vrstici 1427.
     *
     * <p>Enakost [K]: original dogodek objavi pred zanko in po vsaki celici, vsakic z istim svetom,
     * {@code entity = null}, istim kvadrom in praznim seznamom (ob prvi skatli zanka konca). Poslusalec,
     * ki je funkcija svojih argumentov in sveta (svet se med zanko ne spremeni), zato po prvi objavi
     * nicesar ne doda. Brez poslusalcev (dev, testni svet) je objava brez ucinka.
     */
    public static boolean scan(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Cells c) {
        int i = floor(minX) - 1;
        int j = ceil(maxX) + 1;
        int k = floor(minY) - 1;
        int l = ceil(maxY) + 1;
        int i1 = floor(minZ) - 1;
        int j1 = ceil(maxZ) + 1;
        for (int x = i; x < j; ++x) {
            for (int z = i1; z < j1; ++z) {
                boolean edgeX = x == i || x == j - 1;
                boolean edgeZ = z == i1 || z == j1 - 1;
                if ((!edgeX || !edgeZ) && c.loaded(x, z)) {
                    for (int y = k; y < l; ++y) {
                        if (!edgeX && !edgeZ || y != l - 1) {
                            if (x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) {
                                return true;
                            }
                            cellCount++;
                            if (c.blocked(x, y, z)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /** {@code MathHelper.floor}. */
    static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    /** {@code MathHelper.ceil}. */
    static int ceil(double v) {
        int i = (int) v;
        return v > i ? i + 1 : i;
    }

    /** Korak premika za hitbox sirine {@code width}. */
    static double step(double width) {
        return Math.max(MIN_STEP, Math.min(MAX_STEP, width));
    }

    /**
     * Ali je tocka {@code (dx, dy, dz)} od NPC-ja s hitboxom {@code box} (minX, minY, minZ, maxX,
     * maxY, maxZ) neposredno dosegljiva. Steje v stevce.
     */
    public static boolean direct(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double dx, double dy, double dz, BoxTest test) {
        calls++;
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 < 1.0E-8D) {
            // Kot vanilla: tocka na mestu NPC-ja ni kandidat.
            blockedCount++;
            return false;
        }
        if (len2 > MAX_SKIP * MAX_SKIP) {
            tooFar++;
            return false;
        }
        if (sweep(minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz, test)) {
            clear++;
            return true;
        }
        blockedCount++;
        return false;
    }

    /**
     * Premakne zmanjsan hitbox od zacetka do {@code (dx, dy, dz)} v enakih korakih, najvec
     * {@link #step} dolgih, in vrne {@code true}, ce noben polozaj (brez zacetnega, vkljucno s
     * koncnim) ne zadane bloka.
     *
     * <p>M4.17 poskus 2 (Q83): najprej en kvader, ki objame zmanjsan hitbox na zacetku in na koncu
     * (unija vseh polozajev). Vsak polozaj koraka lezi v njem, zato prosta unija pomeni prost vsak
     * korak in isti izid kot koraki; ce unija zadane blok (tudi samo zacetni polozaj, ki ga koraki
     * ne preverijo), odlocijo nespremenjeni koraki. En klic namesto ~12 na prosti preverbi.
     */
    static boolean sweep(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double dx, double dy, double dz, BoxTest test) {
        double width = Math.min(maxX - minX, maxZ - minZ);
        double x0 = minX + SHRINK;
        double y0 = minY + SHRINK;
        double z0 = minZ + SHRINK;
        double x1 = Math.max(x0, maxX - SHRINK);
        double y1 = Math.max(y0, maxY - SHRINK);
        double z1 = Math.max(z0, maxZ - SHRINK);
        if (!test.blocked(Math.min(x0, x0 + dx), Math.min(y0, y0 + dy), Math.min(z0, z0 + dz),
                Math.max(x1, x1 + dx), Math.max(y1, y1 + dy), Math.max(z1, z1 + dz))) {
            unionClear++;
            return true;
        }
        return steps(x0, y0, z0, x1, y1, z1, width, dx, dy, dz, test);
    }

    /**
     * Q455: {@code FlyingMoveHelper.isNotColliding} (original, nespremenjen): poln hitbox, premaknjen
     * za 1, 2, ... blokov (manj od dolzine) proti {@code (dx, dy, dz)}; koncni polozaj se ne preveri.
     * {@code false} pomeni, da helper da {@code WAIT} in NPC obmiruje.
     */
    public static boolean moveClear(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double dx, double dy, double dz, BoxTest test) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-8D) {
            return true;
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        for (int i = 1; i < len; i++) {
            if (test.blocked(minX + ux * i, minY + uy * i, minZ + uz * i, maxX + ux * i, maxY + uy * i, maxZ + uz * i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Q455: obvoznica za NPC, ki mu {@link #moveClear} do trenutne tocke poti zapre rob ovire (izid
     * {@code q455-zid-sled-v1} [M]: NPC proge Z pred robom zida obmiruje, po ~100 tickih vanilla
     * {@code checkForStuck} poti izbrise in M4.10 nima vec cesa nadaljevati). Komolec je tocka, kjer
     * se premik razdeli na navpicni in vodoravni del: pri cilju visje najprej navpicno, sicer najprej
     * vodoravno; drugi vrstni red je rezerva. Komolec velja, ce sta hitbox na oglisce in na cilj
     * NPC-ja prosta ter ce {@link #moveClear} prepusti oba kraka. NPC cilja
     * {@link #ELBOW_OVERSHOOT} cez oglisce, ce je tam prosto.
     *
     * @return odmik tocke, ki jo NPC cilja, od trenutnega polozaja, ali {@code null}
     */
    public static double[] elbow(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double dx, double dy, double dz, BoxTest test) {
        double[][] corners = dy > 0.0D
                ? new double[][] { { 0.0D, dy, 0.0D }, { dx, 0.0D, dz } }
                : new double[][] { { dx, 0.0D, dz }, { 0.0D, dy, 0.0D } };
        for (double[] c : corners) {
            double len = Math.sqrt(c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
            double rx = dx - c[0];
            double ry = dy - c[1];
            double rz = dz - c[2];
            if (len < 1.0E-8D || rx * rx + ry * ry + rz * rz < 1.0E-8D) {
                continue;
            }
            if (test.blocked(minX + c[0], minY + c[1], minZ + c[2], maxX + c[0], maxY + c[1], maxZ + c[2])
                    || !moveClear(minX, minY, minZ, maxX, maxY, maxZ, c[0], c[1], c[2], test)
                    || !moveClear(minX + c[0], minY + c[1], minZ + c[2], maxX + c[0], maxY + c[1], maxZ + c[2], rx, ry, rz, test)) {
                continue;
            }
            double f = (len + ELBOW_OVERSHOOT) / len;
            double[] aim = { c[0] * f, c[1] * f, c[2] * f };
            if (test.blocked(minX + aim[0], minY + aim[1], minZ + aim[2], maxX + aim[0], maxY + aim[1], maxZ + aim[2])
                    || !moveClear(minX, minY, minZ, maxX, maxY, maxZ, aim[0], aim[1], aim[2], test)) {
                aim = c;
            }
            if (Math.sqrt(aim[0] * aim[0] + aim[1] * aim[1] + aim[2] * aim[2]) <= MOVE_MIN) {
                continue;
            }
            elbowCount++;
            return new double[] { aim[0], aim[1], aim[2] };
        }
        return null;
    }

    /** Premik po korakih (poskus 1), brez unije; {@code x0..z1} je ze zmanjsan hitbox. */
    static boolean steps(double x0, double y0, double z0, double x1, double y1, double z1, double width,
            double dx, double dy, double dz, BoxTest test) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int n = Math.max(1, (int) Math.ceil(len / step(width)));
        for (int i = 1; i <= n; i++) {
            double f = (double) i / n;
            double ox = dx * f;
            double oy = dy * f;
            double oz = dz * f;
            if (test.blocked(x0 + ox, y0 + oy, z0 + oz, x1 + ox, y1 + oy, z1 + oz)) {
                return false;
            }
        }
        return true;
    }
}
