package noppes.npcs.rework.entity;

/**
 * M3.3 — enoten vir resnice o jahanju: vloga entitete in kdo krmili nosilca.
 *
 * <p>Zakaj obstaja: M3.2 je pokazal, da vanilla {@code EntityLiving.updateEntityActionState}
 * (metoda je {@code final}) na koncu AI ticka <b>vsakega</b> jahaca, ki sedi na zivi
 * entiteti, naredi {@code nosilec.getNavigator().setPath(jahac.getPath(), 1.5)} in
 * {@code nosilec.getMoveHelper().read(jahac.getMoveHelper())} — vanilla spider jockey.
 * Jahac brez cilja nosilcu tako vsak tick izbrise pot (R1: {@code navig=0/8}). Kdo krmili,
 * je bilo doslej implicitno odloceno v vanilla kodi; tu je odloceno eksplicitno in pod
 * stikalom.
 *
 * <p>Nacini (stikalo {@code RwMountSteering} v {@code CustomNpcs.cfg}, med tekom
 * {@code /rwmount}):
 * <ul>
 * <li>{@link #ORIGINAL} (0, privzeto, D-007): vedno krmili jahac, kot v originalu.</li>
 * <li>{@link #MOUNT_WHEN_OWN_PATH} (1, popravek R1): ce je nosilec NPC, krmili sam; jahac
 *     prevzame samo, ko ima pot on in nosilec ne (osnova za M3.10, konjenica s
 *     poveljnikom).</li>
 * <li>{@link #MOUNT_ALWAYS} (2): nosilec-NPC krmili vedno; jahac ga nikoli ne vodi.</li>
 * </ul>
 * Nosilec, ki ni NPC (konj, zombi ...), je v vseh nacinih voden po vanilla pravilu —
 * reworku ne pripada odlocanje o tujih entitetah.
 *
 * <p>Razred je namenoma brez Minecraft tipov, da je enotsko testljiv.
 */
public final class RiderState {
    public static final int ORIGINAL = 0;
    public static final int MOUNT_WHEN_OWN_PATH = 1;
    public static final int MOUNT_ALWAYS = 2;

    /** Vloga entitete v jahanju. */
    public enum Role {
        NONE, RIDER, MOUNT, RIDER_AND_MOUNT
    }

    /** Kdo ta tick doloca pot in move helper nosilca. */
    public enum Steering {
        /** Vanilla: jahac nosilcu prepise pot in move helper. */
        RIDER,
        /** Nosilec obdrzi svojo pot in move helper; prepis jahaca se razveljavi. */
        MOUNT
    }

    private static volatile int mode = ORIGINAL;

    private RiderState() {
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
        return m == ORIGINAL || m == MOUNT_WHEN_OWN_PATH || m == MOUNT_ALWAYS;
    }

    public static String describe(int m) {
        switch (m) {
            case MOUNT_WHEN_OWN_PATH:
                return "nosilec krmili, jahac samo ce ima pot le on";
            case MOUNT_ALWAYS:
                return "nosilec krmili vedno";
            case ORIGINAL:
                return "original (jahac krmili)";
            default:
                return "neveljaven";
        }
    }

    public static Role role(boolean ridesSomething, boolean carriesSomething) {
        if (ridesSomething) {
            return carriesSomething ? Role.RIDER_AND_MOUNT : Role.RIDER;
        }
        return carriesSomething ? Role.MOUNT : Role.NONE;
    }

    /**
     * Kdo krmili nosilca ta tick.
     *
     * @param m            nacin (neveljaven se obravnava kot {@link #ORIGINAL})
     * @param mountIsNpc   nosilec je CustomNPCs NPC
     * @param mountHasPath nosilec ima pred tickom jahaca svojo pot
     * @param riderHasPath jahac ima pred svojim tickom pot
     */
    public static Steering decide(int m, boolean mountIsNpc, boolean mountHasPath,
            boolean riderHasPath) {
        if (!mountIsNpc) {
            return Steering.RIDER;
        }
        switch (m) {
            case MOUNT_ALWAYS:
                return Steering.MOUNT;
            case MOUNT_WHEN_OWN_PATH:
                return riderHasPath && !mountHasPath ? Steering.RIDER : Steering.MOUNT;
            default:
                return Steering.RIDER;
        }
    }
}
