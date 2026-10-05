package noppes.npcs.rework.entity;

/**
 * M3.8 (R6): kako se vanilla potisk ob trku dveh entitet razdeli med njiju.
 *
 * <p>Vanilla {@code Entity.applyEntityCollision} odrine obe entiteti z enako silo (delez 1 in 1).
 * Nacini hitboxa NPC-ja ta delez spremenijo:
 * <ul>
 *   <li>{@link #ORIGINAL} - kot vanilla (privzeto, NBT kljuca ni);</li>
 *   <li>{@link #SOLID} - NPC-ja odrivanje ne premakne; drugo entiteto odrine kot vanilla;
 *       dva SOLID NPC-ja se razdelita po masi, sicer bi se prekrila;</li>
 *   <li>{@link #SMART} - delez je obratno sorazmeren masi (prostornina hitboxa, s scitom vec):
 *       enako velika entiteta se odriva kot vanilla, NPC velikosti 1 velikana skoraj ne premakne.</li>
 * </ul>
 * Brez Minecraft tipov, da je testno brez igre.
 */
public final class HitboxWeights {
    public static final int ORIGINAL = 0;
    public static final int SOLID = 1;
    public static final int SMART = 2;

    /** Najmanjsa masa, da delitev ne deli z nic (hitbox brez tlorisa ima sirino 1.0E-5). */
    static final double MIN_MASS = 1.0E-6;

    private HitboxWeights() {
    }

    public static int sanitize(int mode) {
        return mode == SOLID || mode == SMART ? mode : ORIGINAL;
    }

    public static String name(int mode) {
        switch (sanitize(mode)) {
            case SOLID: return "solid";
            case SMART: return "smart";
            default: return "original";
        }
    }

    /** -1, ce ime ni znano. */
    public static int parse(String name) {
        if ("original".equals(name) || "0".equals(name)) {
            return ORIGINAL;
        }
        if ("solid".equals(name) || "1".equals(name)) {
            return SOLID;
        }
        if ("smart".equals(name) || "2".equals(name)) {
            return SMART;
        }
        return -1;
    }

    /** Masa hitboxa: sirina^2 * visina, pomnozena z utezjo scita (1, ce ga ni). */
    public static double mass(double width, double height, double shieldWeight) {
        double m = width * width * height * Math.max(shieldWeight, 0.0);
        return m > MIN_MASS ? m : MIN_MASS;
    }

    /**
     * Delez vanilla potiska za entiteto A in B, {@code {a, b}}. Vanilla je {@code {1, 1}}.
     * Vsota delezev je vedno 2, razen ko je eden SOLID (takrat se ne odrine nihce bolj kot vanilla).
     */
    public static double[] shares(int modeA, double massA, int modeB, double massB) {
        int a = sanitize(modeA);
        int b = sanitize(modeB);
        if (a == ORIGINAL && b == ORIGINAL) {
            return new double[] {1.0, 1.0};
        }
        if (a == SOLID && b != SOLID) {
            return new double[] {0.0, 1.0};
        }
        if (b == SOLID && a != SOLID) {
            return new double[] {1.0, 0.0};
        }
        double ma = Math.max(massA, MIN_MASS);
        double mb = Math.max(massB, MIN_MASS);
        double sum = ma + mb;
        return new double[] {2.0 * mb / sum, 2.0 * ma / sum};
    }
}
