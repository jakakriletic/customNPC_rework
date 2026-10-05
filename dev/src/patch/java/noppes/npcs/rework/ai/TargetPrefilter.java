package noppes.npcs.rework.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.constants.EnumCompanionJobs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobGuard;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.companion.CompanionGuard;

/**
 * M5-S S1 — predzavrnitev kandidata v iskalniku tarc, preden se izvede raytrace vidnosti.
 *
 * <p><b>Strosek v originalu</b> ({@code NPCAttackSelector.isEntityApplicable}): predikat najprej
 * preveri doseg, nato pri {@code directLOS} (privzeto vklopljen) klice
 * {@code getEntitySenses().canSee}, ki ob zgresenem predpomnilniku izvede
 * {@code world.rayTraceBlocks}. Sele na koncu preveri, ali je NPC do kandidata sploh
 * sovrazen (strazar, spremljevalec-strazar, frakcija do igralca, frakcija do NPC-ja).
 * Prijazen NPC tako raytrace-a vsako zivo bitje v {@code aggroRange} in ga nato zavrne.
 * Model v {@code docs/09-PERFORMANCE-RAZISKAVA.md} temu pripise vecino alokacij pri idle-500.
 *
 * <p><b>Predzavrnitev</b> vrne {@code true} samo, ko je iz preverjanj <b>brez stranskih
 * ucinkov</b> gotovo, da bi original vrnil {@code false} (vsi pogoji sovraznosti so v originalu
 * AND-ani z vidnostjo). Ce ni gotovo, tece original nespremenjen. Namenoma ostanejo v originalnem
 * vrstnem redu:
 * <ul>
 * <li>igralec, ki ni v kreativnem nacinu in nima {@code disableDamage}:
 * {@code Faction.isAggressiveToPlayer} ob prvem stiku s frakcijo sprozi skriptni dogodek
 * {@code FactionUpdateEvent} in zapise tocke ({@code PlayerFactionData.getFactionPoints}),
 * original pa to naredi sele za vidnim kandidatom;</li>
 * <li>neskladno stanje ({@code job == 3} brez {@code JobGuard}, vloga 6 brez
 * {@code RoleCompanion}, frakcija {@code null}): original tu vrze izjemo, a samo za vidnega
 * kandidata, zato predzavrnitev o njem ne odloca.</li>
 * </ul>
 * Edina razlika je vsebina predpomnilnika {@code EntitySenses}, ki se brise vsak tick; poznejsi
 * {@code canSee} istega kandidata v istem ticku izracuna enak rezultat sam.
 *
 * <p>Privzeto je nacin {@link #ORIGINAL} (D-007), takrat se predzavrnitev ne izvede.
 */
public final class TargetPrefilter {
    /** Original: vrstni red predikata nespremenjen. */
    public static final int ORIGINAL = 0;
    /** Predzavrnitev nesovraznih kandidatov pred raytraceom. */
    public static final int HOSTILITY_FIRST = 1;

    /** Tristanje za pogoj, ki ga ni mogoce oceniti brez stranskih ucinkov ali izjeme. */
    public static final int NO = 0;
    public static final int YES = 1;
    public static final int UNKNOWN = 2;

    public static final int KIND_OTHER = 0;
    public static final int KIND_PLAYER = 1;
    public static final int KIND_NPC = 2;

    private static volatile int mode = ORIGINAL;
    /** Stevec predzavrnitev; pise samo strezniska nit, bere ukaz. */
    private static long rejected;

    private TargetPrefilter() {
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
        return m == ORIGINAL || m == HOSTILITY_FIRST;
    }

    public static String describe(int m) {
        switch (m) {
            case ORIGINAL:
                return "original (raytrace pred preverjanjem sovraznosti)";
            case HOSTILITY_FIRST:
                return "sovraznost pred raytraceom";
            default:
                return "neveljaven";
        }
    }

    public static long rejected() {
        return rejected;
    }

    public static void resetRejected() {
        rejected = 0L;
    }

    /**
     * Cista odlocitev. Vrne {@code true} samo, ce je vsak sovrazni izid originala izkljucen.
     *
     * @param guard            strazar ({@code job == 3}): NO, YES ali UNKNOWN (neskladno stanje)
     * @param companionGuard   spremljevalec-strazar (vloga 6): NO, YES ali UNKNOWN
     * @param kind             vrsta kandidata
     * @param playerExempt     igralec v kreativnem nacinu ali z {@code disableDamage}
     * @param npcKilled        NPC kandidat je ubit
     * @param attackOtherFactions nastavitev napadalca
     * @param aggressiveToNpc  frakcija napadalca napada frakcijo kandidata: NO, YES ali UNKNOWN
     */
    public static boolean rejects(int guard, int companionGuard, int kind, boolean playerExempt,
            boolean npcKilled, boolean attackOtherFactions, int aggressiveToNpc) {
        if (guard != NO || companionGuard != NO) {
            return false;
        }
        switch (kind) {
            case KIND_PLAYER:
                return playerExempt;
            case KIND_NPC:
                return npcKilled || !attackOtherFactions || aggressiveToNpc == NO;
            default:
                return true;
        }
    }

    /**
     * Klice ga {@code NPCAttackSelector} za poceni preverjanji dosega in zdravja. Vrne
     * {@code false} v nacinu {@link #ORIGINAL}.
     */
    public static boolean rejectsEarly(EntityNPCInterface npc, EntityLivingBase entity) {
        if (mode == ORIGINAL) {
            return false;
        }
        int kind;
        boolean playerExempt = false;
        boolean npcKilled = false;
        int aggressiveToNpc = NO;
        if (entity instanceof EntityPlayerMP) {
            kind = KIND_PLAYER;
            EntityPlayerMP player = (EntityPlayerMP) entity;
            playerExempt = player.capabilities.isCreativeMode || player.capabilities.disableDamage;
        } else if (entity instanceof EntityNPCInterface) {
            kind = KIND_NPC;
            EntityNPCInterface other = (EntityNPCInterface) entity;
            npcKilled = other.isKilled();
            if (npc.faction == null || other.faction == null) {
                aggressiveToNpc = UNKNOWN;
            } else {
                aggressiveToNpc = npc.faction.isAggressiveToNpc(other) ? YES : NO;
            }
        } else {
            kind = KIND_OTHER;
        }
        boolean r = rejects(guard(npc, entity), companionGuard(npc, entity), kind, playerExempt,
                npcKilled, npc.advanced.attackOtherFactions, aggressiveToNpc);
        if (r) {
            rejected++;
        }
        return r;
    }

    private static int guard(EntityNPCInterface npc, EntityLivingBase entity) {
        if (npc.advanced.job != 3) {
            return NO;
        }
        if (!(npc.jobInterface instanceof JobGuard)) {
            return UNKNOWN;
        }
        return ((JobGuard) npc.jobInterface).isEntityApplicable(entity) ? YES : NO;
    }

    private static int companionGuard(EntityNPCInterface npc, EntityLivingBase entity) {
        if (npc.advanced.role != 6) {
            return NO;
        }
        if (!(npc.roleInterface instanceof RoleCompanion)) {
            return UNKNOWN;
        }
        RoleCompanion role = (RoleCompanion) npc.roleInterface;
        if (role.job != EnumCompanionJobs.GUARD) {
            return NO;
        }
        if (!(role.jobInterface instanceof CompanionGuard)) {
            return UNKNOWN;
        }
        return ((CompanionGuard) role.jobInterface).isEntityApplicable(entity) ? YES : NO;
    }
}
