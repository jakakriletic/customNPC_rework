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

    /** Trajanje server ticka. Isti kljuc polni {@link Diag#tick(long)}. */
    public static final Distribution SERVER_TICK_NANOS = Diag.distribution("server.tick.ns", "ns");

    private DiagKeys() {
    }

    /** Poskrbi, da so kljuci registrirani tudi, ce jih se nihce ni uporabil. */
    public static void touch() {
    }
}
