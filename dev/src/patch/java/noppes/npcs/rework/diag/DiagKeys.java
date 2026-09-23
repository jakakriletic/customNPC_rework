package noppes.npcs.rework.diag;

/**
 * Imena merjenih velicin na enem mestu.
 *
 * <p>Kljuci so tu zato, da klicna mesta v razlicnih paketih ne izumljajo vsak svojega
 * imena za isto stvar in da je ob branju izpisa jasno, kaj manjka. Kljuci, ki jih se
 * nihce ne polni, se v izpisu ne pokazejo (stevec 0), so pa ze rezervirani z imenom in
 * enoto, da bo prehod v M3/M5 samo dodajanje klicev.
 */
public final class DiagKeys {
    /** Koliko NPC-jev je tiknilo (server stran, en dogodek na NPC na tick). */
    public static final DiagKey NPC_UPDATE = Diag.key("npc.update", "tick");

    /**
     * Priblizen cas, porabljen v posodobitvah NPC-jev.
     *
     * <p>Meri se kot razmik med zacetkom posodobitve NPC-ja in zacetkom posodobitve
     * naslednje zive entitete v istem svetu. Svetovi tikajo entitete zaporedno na server
     * niti, zato je to dobra ocena, ni pa tocna meritev: vkljucuje tudi rezijo zanke.
     * Tocno meritev dobimo, ko je {@code EntityNPCInterface} prenesen (M3.1).
     */
    public static final DiagKey NPC_UPDATE_WINDOW = Diag.key("npc.update.window", "tick");

    /** Posodobitve zivih entitet, ki niso NPC — za primerjavo, koliksen delez so NPC-ji. */
    public static final DiagKey OTHER_LIVING_UPDATE = Diag.key("entity.living.update", "tick");

    /**
     * NPC, ki mu je vanilla zavrnila posodobitev.
     *
     * <p>Forge posije {@code EntityEvent.CanUpdate} natanko takrat, ko
     * {@code World.updateEntityWithOptionalForce} ugotovi, da obmocje 32 blokov okoli
     * entitete ni nalozeno, in bi posodobitev preskocil. Ce ta stevec tece, medtem ko
     * {@code npc.per.tick} kaze 0, je vzrok v nalaganju obmocja. Ce ne tece, entitet sploh
     * ne poskusa posodobiti nekaj visje - zanka sveta.
     *
     * <p>Zbiralnik dogodka nikoli ne spremeni; samo steje.
     */
    public static final DiagKey NPC_UPDATE_BLOCKED = Diag.key("npc.update.blocked", "tick");

    /** Isto za entitete, ki niso NPC. */
    public static final DiagKey OTHER_UPDATE_BLOCKED = Diag.key("entity.update.blocked", "tick");

    /** Se ne polni: zahteve po izracunu poti. Klicna mesta pridejo z M3.1/M4. */
    public static final DiagKey PATH_REQUEST = Diag.key("ai.path.request", "poizvedba");

    /** Se ne polni: neuspeli izracuni poti (navigator vrne null ali prazno pot). */
    public static final DiagKey PATH_FAILED = Diag.key("ai.path.failed", "poizvedba");

    /** Se ne polni: klici skript. Klicno mesto pride s prenosom ScriptContainer (M5.1/M6). */
    public static final DiagKey SCRIPT_RUN = Diag.key("script.run", "klic");

    /** Se ne polni: cakanje na globalni script lock — glavni sum za M5. */
    public static final DiagKey SCRIPT_LOCK_WAIT = Diag.key("script.lock.wait", "klic");

    /** Stevilo NPC-jev, ki so tiknili v posameznem ticku. */
    public static final Distribution NPCS_PER_TICK = Diag.distribution("npc.per.tick", "npc");

    /**
     * Razmik v server tickih med dvema tickoma, v katerih je tiknil vsaj en NPC.
     *
     * <p>Obstaja zaradi meritve z 15. 9.: `npc.per.tick` je bil binaren (0 ali 8), NPC-ji so
     * tiknili priblizno vsak peti tick. Ta porazdelitev pove, ali je razmik enakomeren
     * (nekaj v vanilli ali v modu posodablja entitete redkeje) ali razmetan (chunki se
     * nalagajo in odlagajo).
     */
    public static final Distribution NPC_TICK_GAP = Diag.distribution("npc.tick.gap", "tick");

    /** Server tick za posamezen svet; vsota cez vse svetove. */
    public static final DiagKey WORLD_TICK = Diag.key("world.tick", "tick");

    /** Vzorcenje na sekundo: koliko entitet je v `loadedEntityList` posameznega sveta. */
    public static final Distribution WORLD_ENTITIES = Diag.distribution("world.entities.loaded", "entiteta");

    /** Vzorcenje na sekundo: koliko od teh je NPC-jev. */
    public static final Distribution WORLD_NPCS = Diag.distribution("world.npc.loaded", "npc");

    /**
     * Vzorcenje na sekundo: koliko NPC-jev je v stanju killed.
     *
     * <p>Ubit CustomNPC ostane v svetu in caka na respawn (`RespawnTime = 20`,
     * `SpawnCycle = 0`). Ce se meritev dela nad svetom, polnim mrtvih NPC-jev, so stevilke
     * o necem drugem, kot mislimo.
     */
    public static final Distribution WORLD_NPCS_KILLED = Diag.distribution("world.npc.killed", "npc");

    /** Vzorcenje na sekundo: koliko igralcev je v svetu. Vpliva na to, kaj sploh tika. */
    public static final Distribution WORLD_PLAYERS = Diag.distribution("world.players", "igralec");

    /**
     * Vzorcenje na sekundo: koliko chunkov je prisilno nalozenih (vsi modi skupaj).
     *
     * <p>To ni zanimivost, ampak <b>zapisan pogoj meritve</b>. Ce je ta vrednost 0 in je
     * {@code world.players} prav tako 0, je meritev po 300 tickih merila prazen tek in
     * njeni percentili ne pomenijo nicesar (glej {@link DiagChunkLoader}). Ker se vzorec
     * jemlje vso meritev, se iz porazdelitve vidi tudi, ali je pogoj veljal ves cas:
     * {@code min} in {@code max} morata biti enaka.
     */
    public static final Distribution WORLD_CHUNKS_FORCED =
            Diag.distribution("world.chunks.forced", "chunk");

    /**
     * Koliko chunkov je {@link DiagChunkLoader} dodal <b>po</b> zacetnem naboru.
     *
     * <p>Raste, kadar NPC-ji tavajo iz pokritega obmocja. Ce je ob koncu meritve velik, je
     * bil obroc za ta scenarij premajhen in meritev je lahko del casa tekla nad NPC-ji,
     * ki niso bili pokriti.
     */
    public static final DiagKey DIAG_CHUNKS_ADDED = Diag.key("diag.chunks.added", "chunk");

    /**
     * Nalozeni chunki. Steje se {@code ChunkEvent.Load} na server strani.
     *
     * <p>Kandidat za razlago prepada med p95 in p99: nalaganje chunka je sinhrono delo v
     * server ticku (branje z diska ali generiranje), zato se pokaze kot posamezen dolg
     * tick, ne kot dvig celotne porazdelitve.
     */
    public static final DiagKey CHUNK_LOAD = Diag.key("chunk.load", "chunk");

    /** Odlozeni chunki; {@code ChunkEvent.Unload} na server strani. */
    public static final DiagKey CHUNK_UNLOAD = Diag.key("chunk.unload", "chunk");

    /**
     * Shranjeni svetovi; {@code WorldEvent.Save} na server strani.
     *
     * <p>Autosave tece v server ticku vsakih 900 tickov (45 s,
     * {@code MinecraftServer.tick():762}). V 60-sekundni meritvi sta to najvec dva ticka,
     * zato sam po sebi ne more razloziti ducata pocasnih tickov - ta stevec to trditev
     * spremeni iz sklepanja v podatek.
     */
    public static final DiagKey WORLD_SAVE = Diag.key("world.save", "svet");

    /**
     * Nove poti, dodeljene NPC-jem, kot jih vidi opazovalec (M2.7, sesta velicina).
     *
     * <p><b>Kaj to je in kaj ni.</b> Vanilla ne poslje dogodka ob iskanju poti, klicnih
     * mest pa v M2 se ne moremo instrumentirati ({@code EntityNPCInterface} in {@code ai}
     * paket se nista prenesena, M3.1). Zbiralnik zato enkrat na tick primerja, ali ima NPC
     * <b>drug objekt</b> {@code Path} kot prejsnji tick. To steje <b>uspesne dodelitve
     * poti</b>, ne vseh iskanj: iskanje, ki vrne {@code null} ali ki ga
     * {@code canNavigate()} zavrne, ne dodeli nicesar in je za ta stevec neviden.
     *
     * <p>Zato je ta stevec spodnja meja stevila iskanj in ne njihovo stevilo. Za ceno
     * iskanja je namenjena sonda ({@link NavSweep}), ki meri cas enega iskanja; zmnozek
     * obeh je ocena cene navigacije na tick. Tocno stevilo iskanj pride z M3.1.
     */
    public static final DiagKey NAV_PATH_NEW = Diag.key("nav.ai.path.new", "pot");

    /** Koliko novih poti je bilo dodeljenih v posameznem ticku. */
    public static final Distribution NAV_PATHS_PER_TICK =
            Diag.distribution("nav.ai.paths.per.tick", "pot");

    /**
     * Koliko NPC-jev je v ticku imelo pot.
     *
     * <p>Brez tega je stevilo novih poti neberljivo: ena nova pot na tick pri osmih
     * navigirajocih NPC-jih pomeni nekaj drugega kot ena pri dvesto.
     */
    public static final Distribution NAV_NAVIGATING = Diag.distribution("nav.ai.navigating", "npc");

    /** Trajanje server ticka. Isti kljuc polni {@link Diag#tick(long)}. */
    public static final Distribution SERVER_TICK_NANOS = Diag.distribution("server.tick.ns", "ns");

    /** Zbirke smeti v oknu meritve; nanos = cas zbiranja (M2.6, {@link JvmProbe}). */
    public static final DiagKey JVM_GC = Diag.key("jvm.gc", "zbirka");

    /** Samo zbirke stare generacije. */
    public static final DiagKey JVM_GC_OLD = Diag.key("jvm.gc.old", "zbirka");

    /** Bajti, ki jih je v oknu alocirala server nit. */
    public static final DiagKey JVM_ALLOC_SERVER = Diag.key("jvm.alloc.server", "bajt");

    /** 1, ce JVM podpira stetje alokacij po niti; sicer je jvm.alloc.server brez pomena. */
    public static final DiagKey JVM_ALLOC_SUPPORTED = Diag.key("jvm.alloc.podprto", "da");

    /** Zasedenost stare generacije ob zadnji zbirki v oknu; 0, ce je ni bilo. */
    public static final DiagKey JVM_HEAP_OLD_AFTER_GC = Diag.key("jvm.heap.old.poGc", "bajt");

    /** Najvecji heap (-Xmx). */
    public static final DiagKey JVM_HEAP_MAX = Diag.key("jvm.heap.max", "bajt");

    private DiagKeys() {
    }

    /** Poskrbi, da so kljuci registrirani tudi, ce jih se nihce ni uporabil. */
    public static void touch() {
    }
}
