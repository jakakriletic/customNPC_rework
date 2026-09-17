package noppes.npcs.rework.diag;

import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.WorldServer;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Pometanje sonde: vsak merjeni NPC poisce pot do istega cilja (M2.7).
 *
 * <p><b>Zakaj sonda in ne opazovanje AI-ja.</b> Iz opazovanja AI-ja se ne da prebrati, ali
 * je bila pot cela ali delna: NPC, ki obstane, izgleda enako kot NPC, ki je prisel do
 * konca delne poti. Sonda zato sama sprozi iskanje z znanim ciljem, izmeri cas in primerja
 * vrnjeno pot s ciljem. Ker je cilj znan, je odgovor stevilka in ne vtis.
 *
 * <p><b>Zakaj lasten {@link PathFinder} in ne {@code navigator.getPathToXYZ}.</b> Vanilla
 * {@code PathNavigate.getPathToPos} (vrstica 111 dekompiliranega izpisa) si cilj
 * <i>zapomni</i> v polju {@code targetPos} in ob naslednjem klicu z istim ciljem vrne ze
 * izracunano pot namesto nove. Klic sonde bi torej (a) spremenil stanje navigatorja, ki ga
 * meri, in (b) pri ponovitvah meril branje predpomnilnika namesto iskanja poti. Sonda si
 * zato vzame {@link PathFinder} nad <b>istim</b> {@code NodeProcessor}, ki ga uporablja
 * navigator entitete ({@code getNodeProcessor()} je javen), in postavi {@link ChunkCache}
 * z istim polmerom kot vanilla. Izmerjena koda je s tem ista, stanje navigatorja pa ostane
 * nedotaknjeno.
 *
 * <p><b>Zakaj to sme teci samo na server niti.</b> {@code NodeProcessor} je deljen z
 * navigatorjem entitete in ga {@code findPath} med iskanjem inicializira in na koncu
 * pocisti. Dokler sonda tece iz ukaza (server nit) in se vsako iskanje konca znotraj
 * klica, prepletanja z AI iskanjem ni. Iz druge niti bi bila to okvara in ne meritev.
 *
 * <p><b>Cesa sonda ne izmeri.</b> Vanilla pred iskanjem preveri {@code canNavigate()}
 * (zascitena metoda), ki za kopenski navigator zahteva {@code onGround}. Sonda te preverbe
 * ne more poklicati, zato v vrstico zapise, koliko merjenih NPC-jev je bilo na tleh; ce je
 * to stevilo majhno, AI teh poti sploh ni iskal, ceprav jih sonda najde.
 */
public final class NavSweep {
    /** Koliko iskanj na NPC; prvo je vedno vkljuceno v cas, ostala merijo ogret JIT. */
    public static final int DEFAULT_REPEATS = 3;

    /** Zgornja meja merjenih NPC-jev na pometanje. */
    public static final int DEFAULT_MAX_NPCS = 64;

    private NavSweep() {
    }

    /**
     * Opravi eno pometanje.
     *
     * @param server strezniku pripadajoci svetovi
     * @param gx cilj; pretvori se v vozlisce z istim zaokrozevanjem kot v vanilli
     * @param repeats koliko iskanj na NPC; kakovost poti se belezi samo pri prvem
     * @param maxNpcs zgornja meja merjenih NPC-jev
     * @param namePrefix meri samo NPC-je, katerih ime se zacne s tem nizom; prazno = vsi
     * @return vrstica z markerjem {@code RWNAV-POMET}, primerna za log in za skripto
     */
    public static synchronized String sweep(MinecraftServer server, double gx, double gy,
            double gz, int repeats, int maxNpcs, String namePrefix) {
        if (!Diag.isEnabled()) {
            // Namerno odklonjeno in ne tiho izvedeno: NavProbe se ob `rwdiag on` pocisti,
            // zato bi pometanje pred vklopom izginilo in scenarij bi bral prazno meritev.
            return "RWNAV-NAPAKA merjenje ni vklopljeno; najprej `rwdiag on`";
        }
        if (server == null || server.worlds == null) {
            return "RWNAV-NAPAKA server ni na voljo";
        }
        int passes = repeats < 1 ? 1 : repeats;
        int limit = maxNpcs < 1 ? 1 : maxNpcs;
        BlockPos goal = new BlockPos(gx, gy, gz);
        String goalText = goal.getX() + "," + goal.getY() + "," + goal.getZ();
        String prefix = namePrefix == null ? "" : namePrefix;
        Diag.nav().beginSweep(goalText);
        // Poleg skupne sonde tece se sonda tega pometanja. Scenarij meri dve progi z
        // razlicnima ciljema; brez locene sonde bi merilo bralo njuno mesanico.
        NavProbe sweepProbe = new NavProbe();
        sweepProbe.beginSweep(goalText);

        int measured = 0;
        int onGround = 0;
        int skipped = 0;
        try {
            WorldServer[] worlds = server.worlds;
            for (int w = 0; w < worlds.length && measured < limit; w++) {
                WorldServer world = worlds[w];
                if (world == null) {
                    continue;
                }
                for (int i = 0; i < world.loadedEntityList.size() && measured < limit; i++) {
                    Entity entity = (Entity) world.loadedEntityList.get(i);
                    if (!(entity instanceof EntityNPCInterface)) {
                        continue;
                    }
                    EntityNPCInterface npc = (EntityNPCInterface) entity;
                    if (npc.isKilled()) {
                        continue;
                    }
                    if (!prefix.isEmpty()) {
                        String name = npc.getName();
                        if (name == null || !name.startsWith(prefix)) {
                            continue;
                        }
                    }
                    PathNavigate navigator = npc.getNavigator();
                    NodeProcessor processor = navigator == null ? null : navigator.getNodeProcessor();
                    if (processor == null) {
                        skipped++;
                        continue;
                    }
                    measure(world, npc, navigator, processor, goal, passes, sweepProbe);
                    measured++;
                    if (npc.onGround) {
                        onGround++;
                    }
                }
            }
        } catch (Exception problem) {
            LogWriter.error("RWNAV pometanje ni uspelo", problem);
            return "RWNAV-NAPAKA " + problem;
        }
        return sweepProbe.markerLine("RWNAV-POMET")
                + " npc=" + measured
                + " preskocenih=" + skipped
                + " naTleh=" + onGround
                + " predpona=" + (prefix.isEmpty() ? "-" : prefix)
                + " chunkiForced=" + DiagChunkLoader.forcedChunks();
    }

    /**
     * Izmeri en NPC. Kakovost poti (celost, razmerje, doseg) se zabelezi <b>samo pri prvem
     * iskanju</b>: ponovitve so tam zaradi casa in bi sicer isto pot presteli veckrat.
     */
    private static void measure(WorldServer world, EntityNPCInterface npc,
            PathNavigate navigator, NodeProcessor processor, BlockPos goal, int passes,
            NavProbe sweepProbe) {
        float range = navigator.getPathSearchRange();
        PathFinder finder = new PathFinder(processor);
        int reach = (int) (range + 8.0F);
        for (int pass = 0; pass < passes; pass++) {
            BlockPos origin = new BlockPos(npc);
            long started = System.nanoTime();
            // ChunkCache je del merjenega dela: vanilla ga postavi znotraj odseka
            // profilerja "pathfind" ob vsakem iskanju, torej ga NPC placa vsakic.
            ChunkCache cache = new ChunkCache(world, origin.add(-reach, -reach, -reach),
                    origin.add(reach, reach, reach), 0);
            Path path = finder.findPath(cache, npc, goal, range);
            long elapsed = System.nanoTime() - started;
            if (elapsed < 0L) {
                elapsed = 0L;
            }
            if (pass > 0) {
                // Ponovitev meri samo cas. Ce bi sla skozi record(), bi isto pot presteli
                // veckrat in delez celih poti bi bil odvisen od stevila ponovitev.
                Diag.nav().recordTimeOnly(elapsed);
                sweepProbe.recordTimeOnly(elapsed);
                continue;
            }
            if (path == null || path.getCurrentPathLength() == 0) {
                double straightFromEntity = distance(origin, goal);
                Diag.nav().record(straightFromEntity, 0.0, 0.0, false, elapsed);
                sweepProbe.record(straightFromEntity, 0.0, 0.0, false, elapsed);
                continue;
            }
            PathPoint start = path.getPathPointFromIndex(0);
            PathPoint end = path.getFinalPathPoint();
            double straight = distance(start, goal);
            double length = length(path);
            double endToGoal = end == null ? straight : distance(end, goal);
            Diag.nav().record(straight, length, endToGoal, true, elapsed);
            sweepProbe.record(straight, length, endToGoal, true, elapsed);
        }
    }

    /**
     * Geometrijska dolzina poti: vsota razdalj med zaporednimi vozlisci.
     *
     * <p>Meri se v prostoru vozlisc (cela blokovna koordinata), ne v sredinah blokov, da je
     * popolna diagonalna pot razmerje 1,0 in ne 1,0 plus ostanek zaradi zamika izhodisca.
     */
    static double length(Path path) {
        int count = path.getCurrentPathLength();
        if (count <= 1) {
            return 0.0;
        }
        double sum = 0.0;
        PathPoint previous = path.getPathPointFromIndex(0);
        for (int i = 1; i < count; i++) {
            PathPoint current = path.getPathPointFromIndex(i);
            sum += distance(previous, current);
            previous = current;
        }
        return sum;
    }

    private static double distance(PathPoint a, PathPoint b) {
        double dx = (double) a.x - (double) b.x;
        double dy = (double) a.y - (double) b.y;
        double dz = (double) a.z - (double) b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double distance(PathPoint a, BlockPos b) {
        double dx = (double) a.x - (double) b.getX();
        double dy = (double) a.y - (double) b.getY();
        double dz = (double) a.z - (double) b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double distance(BlockPos a, BlockPos b) {
        double dx = (double) a.getX() - (double) b.getX();
        double dy = (double) a.getY() - (double) b.getY();
        double dz = (double) a.getZ() - (double) b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
