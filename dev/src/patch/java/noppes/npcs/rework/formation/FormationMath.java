package noppes.npcs.rework.formation;

/**
 * Pretvorbe med lokalnim sistemom formacije in svetom, v Minecraftovi konvenciji za yaw.
 *
 * <p>Yaw 0 gleda proti +Z (jug), yaw 90 proti -X (zahod). Vektor naprej je
 * {@code (-sin yaw, cos yaw)}, vektor desno {@code (-cos yaw, -sin yaw)}. Prav desno je
 * pomembno: pri yaw 0 (pogled proti jugu) je desna roka na zahodu, torej -X.
 */
public final class FormationMath {
    private FormationMath() {
    }

    public static double forwardX(double yawDeg) {
        return -Math.sin(Math.toRadians(yawDeg));
    }

    public static double forwardZ(double yawDeg) {
        return Math.cos(Math.toRadians(yawDeg));
    }

    public static double rightX(double yawDeg) {
        return -Math.cos(Math.toRadians(yawDeg));
    }

    public static double rightZ(double yawDeg) {
        return -Math.sin(Math.toRadians(yawDeg));
    }

    /** Yaw smeri (dx, dz); za nicelni vektor vrne NaN. */
    public static double yawOf(double dx, double dz) {
        if (dx * dx + dz * dz < 1.0E-12) {
            return Double.NaN;
        }
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Kot v obmocje (-180, 180]. */
    public static double wrap(double deg) {
        double d = deg % 360.0;
        if (d <= -180.0) {
            d += 360.0;
        } else if (d > 180.0) {
            d -= 360.0;
        }
        return d;
    }

    /** Zasuk iz {@code from} proti {@code to} za najvec {@code maxStep} stopinj. */
    public static double turnToward(double from, double to, double maxStep) {
        if (Double.isNaN(to)) {
            return from;
        }
        if (Double.isNaN(from)) {
            return wrap(to);
        }
        double diff = wrap(to - from);
        if (Math.abs(diff) <= maxStep) {
            return wrap(to);
        }
        return wrap(from + Math.signum(diff) * maxStep);
    }

    /** Svetovni X mesta pri danem sidru in yawu. */
    public static double worldX(double anchorX, double yawDeg, Slot slot) {
        return anchorX + slot.side * rightX(yawDeg) - slot.back * forwardX(yawDeg);
    }

    /** Svetovni Z mesta pri danem sidru in yawu. */
    public static double worldZ(double anchorZ, double yawDeg, Slot slot) {
        return anchorZ + slot.side * rightZ(yawDeg) - slot.back * forwardZ(yawDeg);
    }

    /** Lokalni {@code side} tocke glede na sidro. */
    public static double localSide(double dx, double dz, double yawDeg) {
        return dx * rightX(yawDeg) + dz * rightZ(yawDeg);
    }

    /** Lokalni {@code back} tocke glede na sidro. */
    public static double localBack(double dx, double dz, double yawDeg) {
        return -(dx * forwardX(yawDeg) + dz * forwardZ(yawDeg));
    }
}
