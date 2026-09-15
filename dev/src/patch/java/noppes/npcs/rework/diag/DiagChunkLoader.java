package noppes.npcs.rework.diag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Pogoj meritve: chunki z merjenimi NPC-ji so prisilno nalozeni (M2.1d).
 *
 * <p><b>Zakaj to obstaja.</b> Meritev 15. 9. je pokazala, da NPC-ji na dedicated serverju
 * brez igralca tikajo natanko 300 tickov in nato nikoli vec. Vzrok ni v modu, ampak v
 * {@code WorldServer.updateEntities()}:
 *
 * <pre>
 *   if (this.playerEntities.isEmpty() &amp;&amp; getPersistentChunks().isEmpty()) {
 *       if (this.updateEntityTick++ &gt;= 300) { return; }
 *   } else { this.resetUpdateEntityTick(); }
 * </pre>
 *
 * <p>Vsaka meritev brez igralca in brez prisilno nalozenih chunkov zato meri prvih 15 sekund
 * in nato prazen tek. Ista vrstica je tudi resitev: dovolj je, da {@code getPersistentChunks()}
 * ni prazen, in stevec se resetira vsak tick.
 *
 * <p><b>Zakaj ticket in ne igralec.</b> Ticket je strozje jamstvo, ker deluje na obeh
 * vratarjih hkrati: odklene zanko {@code updateEntities()} (pogoj na ravni sveta) in v
 * {@code World.updateEntityWithOptionalForce} postavi {@code range = 0}, s cimer odpade
 * preverba obmocja 32 blokov (pogoj na ravni posameznega chunka). Poleg tega ne potrebuje
 * cloveka pred zaslonom, zato je scenarij ponovljiv.
 *
 * <p><b>Zakaj ne uporabimo obstojecega {@code ChunkController}.</b> Mod ze ima svoj
 * chunkloader za NPC-je z opravilom "Chunk Loader" (job 8). Merilni pogoj ne sme biti
 * odvisen od kode, ki je predmet meritve — in v tistem razredu sta dve napaki iz originala
 * (glej {@code docs/04-STANJE.md}). Ta razred si zato vzame svoje tickete tipa
 * {@code NORMAL} in se obstojecega ne dotakne.
 *
 * <p><b>Privzeto izklopljeno (D-007).</b> Dokler ni izrecnega {@code /rwdiag chunks on},
 * ta razred ne poklice nicesar iz {@code ForgeChunkManager} in ni prijavljen na event bus.
 * Registracija {@code LoadingCallback}, ki jo {@code requestTicket} zahteva, ze obstaja v
 * originalu ({@code CustomNpcs.load}), zato je ta razred ne dodaja in ne spreminja.
 */
public final class DiagChunkLoader {
    /** Privzeto en obroc okoli chunka vsakega NPC-ja: NPC sme malo zatavati, pa je se pokrit. */
    public static final int DEFAULT_RADIUS = 1;

    /** Zgornja meja na svet. Pri 25 chunkih na ticket je to 16 ticketov od 200 dovoljenih. */
    public static final int MAX_CHUNKS_PER_WORLD = 400;

    /** Ponovni pregled enkrat na sekundo: NPC-ji se premikajo in lahko zapustijo pokrit chunk. */
    private static final int REFRESH_EVERY_TICKS = 20;

    private static final List<WorldTickets> WORLDS = new ArrayList<WorldTickets>();

    private static DiagChunkLoader registered;
    private static int radius = DEFAULT_RADIUS;
    private static int refused;

    private DiagChunkLoader() {
    }

    public static synchronized boolean isEnabled() {
        return registered != null;
    }

    public static synchronized int forcedChunks() {
        int total = 0;
        for (int i = 0; i < WORLDS.size(); i++) {
            total += WORLDS.get(i).forced.size();
        }
        return total;
    }

    public static synchronized int tickets() {
        int total = 0;
        for (int i = 0; i < WORLDS.size(); i++) {
            total += WORLDS.get(i).tickets.size();
        }
        return total;
    }

    /**
     * Prisilno nalozi chunke okoli vseh trenutno nalozenih NPC-jev in se prijavi na tick,
     * da nabor sledi premikajocim se NPC-jem.
     *
     * @param server strezniku pripadajoci svetovi; ukaz ga dobi od {@code CommandBase}
     * @param requestedRadius koliko obrocev chunkov okoli vsakega NPC-ja
     * @return vrstica z markerjem {@code RWDIAG-CHUNKS}, primerna za log in za skripto
     */
    public static synchronized String enable(MinecraftServer server, int requestedRadius) {
        disable();
        if (server == null || server.worlds == null) {
            return "RWDIAG-CHUNKS-NAPAKA server ni na voljo";
        }
        radius = requestedRadius < 0 ? 0 : Math.min(requestedRadius, DiagChunkPlan.MAX_RADIUS);
        int npcs = 0;
        try {
            WorldServer[] worlds = server.worlds;
            for (int i = 0; i < worlds.length; i++) {
                npcs += scan(worlds[i], false);
            }
        } catch (Exception problem) {
            LogWriter.error("RWDIAG-CHUNKS chunkov ni bilo mogoce prisilno nalozit", problem);
            disable();
            return "RWDIAG-CHUNKS-NAPAKA " + problem;
        }
        if (forcedChunks() == 0) {
            disable();
            return "RWDIAG-CHUNKS stanje=off chunki=0 tiketi=0 npc=" + npcs
                    + " razlog=v svetovih ni nalozenih NPC-jev";
        }
        registered = new DiagChunkLoader();
        MinecraftForge.EVENT_BUS.register(registered);
        return status(npcs);
    }

    /** Sprosti vse tickete in se odjavi. Varno tudi, ce ni bilo nic vklopljeno. */
    public static synchronized void disable() {
        if (registered != null) {
            MinecraftForge.EVENT_BUS.unregister(registered);
            registered = null;
        }
        for (int i = 0; i < WORLDS.size(); i++) {
            List<ForgeChunkManager.Ticket> tickets = WORLDS.get(i).tickets;
            for (int j = 0; j < tickets.size(); j++) {
                try {
                    ForgeChunkManager.releaseTicket(tickets.get(j));
                } catch (Exception problem) {
                    LogWriter.error("RWDIAG-CHUNKS ticketa ni bilo mogoce sprostiti", problem);
                }
            }
        }
        WORLDS.clear();
        refused = 0;
    }

    public static synchronized String status(int npcs) {
        return "RWDIAG-CHUNKS stanje=" + (isEnabled() ? "on" : "off")
                + " chunki=" + forcedChunks()
                + " tiketi=" + tickets()
                + " obroc=" + radius
                + " zavrnjeni=" + refused
                + (npcs < 0 ? "" : " npc=" + npcs);
    }

    public static synchronized String status() {
        return status(-1);
    }

    /**
     * Prestej NPC-je enega sveta in poskrbi, da je chunk vsakega med prisilno nalozenimi.
     *
     * @param countAdded ce je {@code true}, se novi chunki stejejo v {@code diag.chunks.added}
     * @return koliko NPC-jev je v tem svetu
     */
    private static int scan(WorldServer world, boolean countAdded) {
        if (world == null) {
            return 0;
        }
        int npcs = 0;
        LinkedHashSet<Long> centers = new LinkedHashSet<Long>();
        List<Entity> loaded = world.loadedEntityList;
        for (int i = 0; i < loaded.size(); i++) {
            Entity entity = loaded.get(i);
            if (!(entity instanceof EntityNPCInterface)) {
                continue;
            }
            npcs++;
            centers.add(DiagChunkPlan.pack(
                    DiagChunkPlan.chunkOf(entity.posX), DiagChunkPlan.chunkOf(entity.posZ)));
        }
        if (centers.isEmpty()) {
            return npcs;
        }
        WorldTickets holder = holderFor(world);
        LinkedHashSet<Long> wanted = DiagChunkPlan.expand(centers, radius, MAX_CHUNKS_PER_WORLD);
        for (Long packed : wanted) {
            if (holder.forced.contains(packed)) {
                continue;
            }
            if (holder.forced.size() >= MAX_CHUNKS_PER_WORLD) {
                refused++;
                break;
            }
            if (!force(holder, world, packed.longValue())) {
                refused++;
                break;
            }
            if (countAdded) {
                DiagKeys.DIAG_CHUNKS_ADDED.increment();
            }
        }
        return npcs;
    }

    private static boolean force(WorldTickets holder, WorldServer world, long packed) {
        int maxDepth = Math.max(1, ForgeChunkManager.getMaxChunkDepthFor(CustomNpcs.MODID));
        if (holder.current == null || holder.onCurrent >= maxDepth) {
            ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(
                    CustomNpcs.instance, world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                return false;
            }
            holder.tickets.add(ticket);
            holder.current = ticket;
            holder.onCurrent = 0;
        }
        ForgeChunkManager.forceChunk(holder.current,
                new ChunkPos(DiagChunkPlan.unpackX(packed), DiagChunkPlan.unpackZ(packed)));
        holder.onCurrent++;
        holder.forced.add(Long.valueOf(packed));
        return true;
    }

    private static WorldTickets holderFor(WorldServer world) {
        for (int i = 0; i < WORLDS.size(); i++) {
            if (WORLDS.get(i).world == world) {
                return WORLDS.get(i);
            }
        }
        WorldTickets created = new WorldTickets(world);
        WORLDS.add(created);
        return created;
    }

    /**
     * Enkrat na sekundo preveri, ali je kateri NPC odtaval iz pokritega obmocja.
     *
     * <p>Dogodek je {@code WorldTickEvent} in ne {@code ServerTickEvent}, ker prvi prinese
     * svet s sabo. Tako ta razred ne potrebuje nobene poti do {@code MinecraftServer}
     * izven ukaza, ki ga vklopi.
     *
     * <p>Chunki se ne sproscajo nazaj. Merilni zagon traja minute, ne ure, zato je nekaj
     * chunkov vec cenejse od odklapljanja in ponovnega priklapljanja, ki bi sredi meritve
     * spremenilo prav tisto, kar merimo.
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        World world = event.world;
        if (!(world instanceof WorldServer) || world.isRemote) {
            return;
        }
        try {
            synchronized (DiagChunkLoader.class) {
                if (registered != this) {
                    return;
                }
                WorldTickets holder = holderFor((WorldServer) world);
                if (--holder.refreshCountdown > 0) {
                    return;
                }
                holder.refreshCountdown = REFRESH_EVERY_TICKS;
                scan((WorldServer) world, true);
            }
        } catch (Exception problem) {
            LogWriter.error("RWDIAG-CHUNKS osvezitev ni uspela", problem);
        }
    }

    /** Ticketi in ze pokriti chunki enega sveta. Chunk koordinate se med svetovi ponavljajo. */
    private static final class WorldTickets {
        private final WorldServer world;
        private final List<ForgeChunkManager.Ticket> tickets =
                new ArrayList<ForgeChunkManager.Ticket>();
        private final LinkedHashSet<Long> forced = new LinkedHashSet<Long>();
        private ForgeChunkManager.Ticket current;
        private int onCurrent;
        private int refreshCountdown = REFRESH_EVERY_TICKS;

        private WorldTickets(WorldServer world) {
            this.world = world;
        }
    }
}
