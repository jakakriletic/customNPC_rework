package noppes.npcs.rework.ai;

/**
 * M3.6 — prioriteta napada proti gibalnemu tasku, ki mu sledi.
 *
 * <p><b>Napaka v originalu</b> (bytecode {@code EntityNPCInterface.setResponse}, odmik 585-601):
 * {@code EntityAIAttackTarget} se doda s {@code tasks.addTask(this.taskCount, ...)} brez
 * {@code taskCount++}. Naslednji task dobi zato <b>isto</b> prioriteto. Pri NPC-ju brez
 * projektila je to gibalni task iz {@code setMoveType}: {@code EntityAIWander} (tavanje) ali
 * {@code EntityAIMovingPath}. Vsi trije imajo mutex bit 1 (vanilla MOVE; CustomNPCs ga
 * imenuje {@code AiMutex.PASSIVE}).
 *
 * <p>Vanilla {@code EntityAITasks.canUse} dovoli taskom z enako ali nizjo prioriteto zagon
 * samo, ce so zdruzljivi z vsemi, ki ze tecejo ({@code taskEntry.priority >= other.priority}
 * in {@code areTasksCompatible}). Prekinitev dovoli samo taskom z <b>visjo</b> prioriteto
 * (manjse stevilo). Napad zato tavanja ne more prekiniti: NPC, ki tava, ko dobi tarco,
 * tava naprej do konca poti, ker {@code EntityAIWander.shouldContinueExecuting} ne preverja
 * {@code isAttacking()}. {@code EntityAIMovingPath} to preverja in se umakne sam, zato tam
 * napake ni videti.
 *
 * <p>Z NPC-jem s projektilom napake ni: {@code EntityAIRangedAttack} dobi isto prioriteto kot
 * napad in sele on poveca {@code taskCount}, zato gibalni task ze v originalu dobi nizjo
 * prioriteto. Enaka prioriteta napada in strelskega napada ostane nespremenjena.
 *
 * <p>Premisa iz {@code 02-ZAHTEVE.md} §R1 („napad ne rezervira {@code PATHING}, zato z njim
 * hkrati tecejo Wander, Follow, Return, MovingPath“) ne drzi: {@code AiMutex} 1/2/4 so vanilla
 * MOVE/LOOK/JUMP in napad ima 1+2 = 3, enako kot vanilla {@code EntityAIAttackMelee}. Z njim
 * hkrati ne tece noben gibalni task. Zapis v {@code docs/meritve/2026-09-24-M3.6-prioriteta-napada.md}.
 *
 * <p>Razred je namenoma brez Minecraft tipov, da je enotsko testljiv.
 */
public final class AttackPriority {
    /** Original: gibalni task ima isto prioriteto kot napad. */
    public static final int ORIGINAL = 0;
    /** Popravek: gibalni task dobi prioriteto za napadom, napad ga lahko prekine. */
    public static final int BEFORE_MOVEMENT = 1;

    private static volatile int mode = ORIGINAL;

    private AttackPriority() {
    }

    public static int mode() {
        return mode;
    }

    /** Neveljavna vrednost (tudi iz configa) pade na {@link #ORIGINAL}; vrne uporabljeno. */
    public static int setMode(int requested) {
        mode = isValidMode(requested) ? requested : ORIGINAL;
        return mode;
    }

    public static boolean isValidMode(int m) {
        return m == ORIGINAL || m == BEFORE_MOVEMENT;
    }

    public static String describe(int m) {
        switch (m) {
            case ORIGINAL:
                return "original (napad in gibanje na isti prioriteti)";
            case BEFORE_MOVEMENT:
                return "napad pred gibanjem";
            default:
                return "neveljaven";
        }
    }

    /**
     * Prioriteta za prvi task, ki se doda za bojnimi taski.
     *
     * @param m              nacin
     * @param taskCount      trenutni {@code EntityNPCInterface.taskCount} po bojnih taskih
     * @param attackPriority prioriteta, s katero je bil dodan {@code EntityAIAttackTarget}
     * @return {@code taskCount} v originalu; v popravku najmanj {@code attackPriority + 1}
     */
    public static int nextPriority(int m, int taskCount, int attackPriority) {
        if (m == BEFORE_MOVEMENT && taskCount <= attackPriority) {
            return attackPriority + 1;
        }
        return taskCount;
    }
}
