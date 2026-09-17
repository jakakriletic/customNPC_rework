package noppes.npcs.rework.diag;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Zbiralnik meritev, ki tece na Forge dogodkih.
 *
 * <p>Zakaj na dogodkih in ne v {@code EntityNPCInterface}: ta razred se v M2 se ni
 * prenesen v {@code src/patch/java}. Prenese se v M3.1, skupaj s celotnim {@code ai}
 * paketom. Do takrat Forge dogodki dajo dovolj za osnovni posnetek: koliko NPC-jev tika,
 * koliko traja server tick, kaksen delez casa gre v NPC-je in kaj sploh je v svetu.
 *
 * <p>Zbiralnik je na event bus prijavljen <b>samo dokler je merjenje vklopljeno</b>.
 * Izklopljena instrumentacija zato ne stane niti enega klica na entiteto na tick.
 */
public final class DiagEventCollector {
    /** Kako pogosto se presteje vsebina svetov. 20 tickov = enkrat na sekundo. */
    private static final int SAMPLE_EVERY_TICKS = 20;

    private static DiagEventCollector registered;

    private long serverTickStart;
    private long windowStart;
    private DiagKey windowKey;
    private int npcsThisTick;

    private long tickIndex;
    private long lastTickWithNpcs = -1L;

    // Kontekst tekocega ticka. Nastavi se na 0 na zacetku ticka in se ob koncu preda
    // tabeli najpocasnejsih tickov, zato je iz posnetka razvidno, kaj je v pocasnem ticku
    // tekla poleg NPC-jev.
    private int chunkLoadsThisTick;
    private int chunkUnloadsThisTick;
    private int savesThisTick;

    // M2.7: opazovanje dodelitev poti. Kljuc je entiteta sama in ne njen id, ker
    // IdentityHashMap za to ne alocira (id bi bilo treba zaviti v Integer na vsak NPC na
    // tick). Mapa zivi samo, dokler je merjenje vklopljeno - vklop naredi nov zbiralnik.
    private final Map<Entity, Path> lastPath = new IdentityHashMap<Entity, Path>();
    private int newPathsThisTick;
    private int navigatingThisTick;

    private int sampleCountdown = 1;
    private boolean sampleNow;
    private int sampleEntities;
    private int sampleNpcs;
    private int sampleKilled;
    private int samplePlayers;
    private int sampleForcedChunks;

    private DiagEventCollector() {
    }

    public static synchronized boolean isEnabled() {
        return registered != null;
    }

    /** Vklopi merjenje in prijavi zbiralnik na event bus. Stevci se ob vklopu pocistijo. */
    public static synchronized void enable() {
        if (registered != null) {
            Diag.reset();
            return;
        }
        DiagKeys.touch();
        DiagEventCollector collector = new DiagEventCollector();
        MinecraftForge.EVENT_BUS.register(collector);
        registered = collector;
        Diag.setEnabled(true);
    }

    /** Izklopi merjenje in odjavi zbiralnik. Zadnji posnetek ostane berljiv do vklopa. */
    public static synchronized void disable() {
        Diag.setEnabled(false);
        if (registered == null) {
            return;
        }
        MinecraftForge.EVENT_BUS.unregister(registered);
        registered = null;
    }

    /** Ce je bil zahtevan {@code -Drwdiag=on}, vklopi merjenje ob zagonu serverja. */
    public static void enableIfRequestedByProperty() {
        if (Diag.requestedByProperty()) {
            enable();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.side != Side.SERVER) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            closeWindow();
            this.serverTickStart = System.nanoTime();
            this.npcsThisTick = 0;
            this.chunkLoadsThisTick = 0;
            this.chunkUnloadsThisTick = 0;
            this.savesThisTick = 0;
            this.newPathsThisTick = 0;
            this.navigatingThisTick = 0;
            // Vzorcenje se odloci na zacetku ticka, da ga vsi svetovi v istem ticku
            // uporabijo, prestete vrednosti pa se seste v en vzorec na koncu ticka.
            if (--this.sampleCountdown <= 0) {
                this.sampleCountdown = SAMPLE_EVERY_TICKS;
                this.sampleNow = true;
                this.sampleEntities = 0;
                this.sampleNpcs = 0;
                this.sampleKilled = 0;
                this.samplePlayers = 0;
                this.sampleForcedChunks = 0;
            }
            return;
        }

        closeWindow();
        Diag.record(DiagKeys.NPCS_PER_TICK, this.npcsThisTick);
        Diag.record(DiagKeys.NAV_PATHS_PER_TICK, this.newPathsThisTick);
        Diag.record(DiagKeys.NAV_NAVIGATING, this.navigatingThisTick);
        if (this.npcsThisTick > 0) {
            if (this.lastTickWithNpcs >= 0L) {
                Diag.record(DiagKeys.NPC_TICK_GAP, this.tickIndex - this.lastTickWithNpcs);
            }
            this.lastTickWithNpcs = this.tickIndex;
        }
        long index = this.tickIndex++;
        if (this.sampleNow) {
            this.sampleNow = false;
            DiagKeys.WORLD_ENTITIES.record(this.sampleEntities);
            DiagKeys.WORLD_NPCS.record(this.sampleNpcs);
            DiagKeys.WORLD_NPCS_KILLED.record(this.sampleKilled);
            DiagKeys.WORLD_PLAYERS.record(this.samplePlayers);
            DiagKeys.WORLD_CHUNKS_FORCED.record(this.sampleForcedChunks);
        }
        if (this.serverTickStart != 0L) {
            Diag.tick(System.nanoTime() - this.serverTickStart, index, this.npcsThisTick,
                    this.chunkLoadsThisTick, this.chunkUnloadsThisTick, this.savesThisTick);
            this.serverTickStart = 0L;
        }
    }

    /**
     * Presteje vsebino sveta enkrat na sekundo.
     *
     * <p>Brez tega se iz stevca posodobitev ne da lociti med "entitete so v svetu, a ne
     * tikajo" in "entitet v svetu sploh ni". Zanka cez {@code loadedEntityList} je dovolj
     * poceni enkrat na 20 tickov in tece samo, dokler je merjenje vklopljeno.
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }
        World world = event.world;
        if (world == null || world.isRemote) {
            return;
        }
        DiagKeys.WORLD_TICK.increment();
        if (!this.sampleNow) {
            return;
        }
        for (int i = 0; i < world.loadedEntityList.size(); i++) {
            Entity entity = (Entity) world.loadedEntityList.get(i);
            this.sampleEntities++;
            if (!(entity instanceof EntityNPCInterface)) {
                continue;
            }
            this.sampleNpcs++;
            if (((EntityNPCInterface) entity).isKilled()) {
                this.sampleKilled++;
            }
        }
        this.samplePlayers += world.playerEntities.size();
        // Pogoj meritve se zapise v sam posnetek. Brez tega se iz posnetka ne da lociti
        // meritve, ki je tekla pod pravim pogojem, od take, ki je merila prazen tek.
        this.sampleForcedChunks += world.getPersistentChunks().keySet().size();
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null) {
            return;
        }
        World world = entity.world;
        if (world == null || world.isRemote) {
            return;
        }
        // Okno prejsnje entitete se zapre sele tu, ker je razmik do naslednje posodobitve
        // najboljsa ocena trajanja prejsnje, ki je na voljo brez prenosa entitete.
        closeWindow();
        if (entity instanceof EntityNPCInterface) {
            DiagKeys.NPC_UPDATE.increment();
            this.npcsThisTick++;
            observePath((EntityNPCInterface) entity);
            this.windowKey = DiagKeys.NPC_UPDATE_WINDOW;
            this.windowStart = System.nanoTime();
        } else {
            DiagKeys.OTHER_LIVING_UPDATE.increment();
        }
    }

    /**
     * Steje entitete, ki jim je vanilla zavrnila posodobitev, ker okolica ni nalozena.
     * Dogodka ne spreminja - instrumentacija ne sme spremeniti obnasanja.
     */
    /**
     * Steje nalaganje chunkov, tako skupno kot v tekocem ticku.
     *
     * <p>Chunk se na server strani nalozi sinhrono v ticku, zato je to prvi kandidat za
     * razlago posameznih dolgih tickov. Dogodek se ne spreminja.
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (isRemote(event)) {
            return;
        }
        DiagKeys.CHUNK_LOAD.increment();
        this.chunkLoadsThisTick++;
    }

    /** Isto za odlaganje chunkov. */
    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (isRemote(event)) {
            return;
        }
        DiagKeys.CHUNK_UNLOAD.increment();
        this.chunkUnloadsThisTick++;
    }

    /**
     * Steje shranjevanje svetov. Autosave je edino delo v ticku, ki se ga da vnaprej
     * napovedati (vsakih 900 tickov), zato mora biti v posnetku locljivo od ostalega.
     */
    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (isRemote(event)) {
            return;
        }
        DiagKeys.WORLD_SAVE.increment();
        this.savesThisTick++;
    }

    private static boolean isRemote(WorldEvent event) {
        World world = event.getWorld();
        return world == null || world.isRemote;
    }

    @SubscribeEvent
    public void onCanUpdate(EntityEvent.CanUpdate event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null || entity.world.isRemote) {
            return;
        }
        if (entity instanceof EntityNPCInterface) {
            DiagKeys.NPC_UPDATE_BLOCKED.increment();
        } else {
            DiagKeys.OTHER_UPDATE_BLOCKED.increment();
        }
    }

    /**
     * Opazi, ali je NPC dobil novo pot (M2.7).
     *
     * <p>Primerja se <b>identiteta</b> objekta poti in ne njena vsebina: vanilla ob vsakem
     * uspesnem iskanju ustvari nov {@code Path} ({@code PathFinder.createPath}), medtem ko
     * hoja po ze dodeljeni poti isti objekt samo premika naprej. Primerjava vsebine bi
     * zato stela iste poti veckrat ali pa jih zgresila.
     *
     * <p>Zapise se tudi {@code null}: brez tega bi pot, ki je bila pocisena in cez nekaj
     * tickov znova dodeljena, ostala neopazna.
     */
    private void observePath(EntityNPCInterface npc) {
        PathNavigate navigator = npc.getNavigator();
        if (navigator == null) {
            return;
        }
        Path path = navigator.getPath();
        Path previous = this.lastPath.put(npc, path);
        if (path == null) {
            return;
        }
        this.navigatingThisTick++;
        if (path != previous) {
            this.newPathsThisTick++;
            DiagKeys.NAV_PATH_NEW.increment();
        }
    }

    private void closeWindow() {
        if (this.windowKey == null) {
            return;
        }
        long elapsed = System.nanoTime() - this.windowStart;
        this.windowKey.record(elapsed < 0L ? 0L : elapsed);
        this.windowKey = null;
    }
}
