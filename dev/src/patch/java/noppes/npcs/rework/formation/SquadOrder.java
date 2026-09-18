package noppes.npcs.rework.formation;

import java.util.Locale;

/**
 * Ukaz enoti: oblika, cilj, smer in nacin. Skupen za ukaz {@code /rwsquad} in za
 * {@link FormationApi}, da oba pomenita isto.
 */
public final class SquadOrder {

    public enum Shape {
        /** Pravokotnik, parameter = sirina (stevilo stolpcev). */
        LEGIJA,
        /** Krogi okoli cilja, parameter = polmer prvega kroga. */
        OBRAMBA,
        /** Ozka kolona, parameter = stevilo vrst (stolpcev). */
        KOLONA,
        /** Ohrani trenutne medsebojne odmike (march v skripti). */
        MARCH;

        public static Shape parse(String s) {
            String k = s.toLowerCase(Locale.ROOT);
            if (k.equals("legija") || k.equals("legion")) {
                return LEGIJA;
            }
            if (k.equals("obramba") || k.equals("ring") || k.equals("krog")) {
                return OBRAMBA;
            }
            if (k.equals("kolona") || k.equals("column")) {
                return KOLONA;
            }
            if (k.equals("march") || k.equals("pohod")) {
                return MARCH;
            }
            return null;
        }
    }

    /**
     * Hitrost je v istih enotah kot {@code nav_speed} v uporabnikovi skripti: skripta je
     * klicala {@code navigateTo(..., nav_speed)}, ta pa {@code tryMoveToXYZ(..., speed * 0.7)}.
     */
    public static final double SCRIPT_SPEED_FACTOR = 0.7;
    public static final double MIN_SPEED = 0.5;
    public static final double MAX_SPEED = 6.0;

    public final Shape shape;
    public final double param;
    public final double x;
    public final double y;
    public final double z;
    /** Smer formacije na cilju; NaN = smer zadnjega dela poti. */
    public final double yaw;
    public final double speed;
    /** Ob stiku s sovraznikom clan zapusti formacijo in se bori (privzeto). */
    public final boolean engage;
    /** Ob prihodu postavi dom (startPos) in orientacijo na novo mesto (privzeto). */
    public final boolean anchor;
    /** Pri daljsi poti se enota najprej postroji (privzeto). */
    public final boolean formFirst;

    public SquadOrder(Shape shape, double param, double x, double y, double z, double yaw, double speed,
            boolean engage, boolean anchor, boolean formFirst) {
        this.shape = shape;
        this.param = param;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
        this.engage = engage;
        this.anchor = anchor;
        this.formFirst = formFirst;
    }

    /** Privzeti parameter oblike, ce ga ukaz ne poda. */
    public static double defaultParam(Shape shape) {
        switch (shape) {
            case LEGIJA:
                return 5;
            case OBRAMBA:
                return 5;
            case KOLONA:
                return 2;
            default:
                return 0;
        }
    }

    /** Privzeta hitrost: kot v skripti, 3 za razpored in 2 za march. */
    public static double defaultSpeed(Shape shape) {
        return shape == Shape.MARCH ? 2.0 : 3.0;
    }

    /** Zastavice iz besedila: {@code drzi}, {@code brezsidra}, {@code takoj}. */
    public static boolean[] parseFlags(String flags) {
        boolean engage = true;
        boolean anchor = true;
        boolean formFirst = true;
        if (flags != null) {
            for (String f : flags.toLowerCase(Locale.ROOT).split("[\\s,]+")) {
                if (f.equals("drzi") || f.equals("hold")) {
                    engage = false;
                } else if (f.equals("brezsidra") || f.equals("noanchor")) {
                    anchor = false;
                } else if (f.equals("takoj") || f.equals("direct")) {
                    formFirst = false;
                }
            }
        }
        return new boolean[] {engage, anchor, formFirst};
    }

    public static boolean isFlag(String token) {
        String f = token.toLowerCase(Locale.ROOT);
        return f.equals("drzi") || f.equals("hold") || f.equals("brezsidra") || f.equals("noanchor")
                || f.equals("takoj") || f.equals("direct");
    }
}
