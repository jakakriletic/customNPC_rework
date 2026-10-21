package noppes.npcs.rework.far;

/**
 * M5-V.5: ciste funkcije izrisa L3 brez GL (raziskava M5-V.0 §5.3, §5.5, D-047) - interpolacija nadomestka
 * med zadnjima posnetkoma in geometrija cilindricnega billboarda.
 *
 * <p>Streznik nadomestek osvezi najvec enkrat na sekundo na igralca (krozna kadenca {@code FarServer}), zato
 * klient polozaj premakne od prikazanega do novega v {@link #DURATION_MS}; brez tega bi hodec NPC na 384
 * blokih skakal. Cas je v milisekundah kot {@code int} (razlika je pravilna tudi cez preliv).
 */
public final class FarInterp {
    /** Trajanje premika med posnetkoma (ms) = kadenca streznika. */
    public static final int DURATION_MS = 1000;

    /** Indeksi vnosa shrambe {@code FarClient} za M5-V.5 (0..5 so x, y, z, yaw, videz, zastavice). */
    public static final int E_PX = 6;
    public static final int E_PY = 7;
    public static final int E_PZ = 8;
    public static final int E_T = 9;
    public static final int E_LIGHT = 10;
    /** Skok nad 16 blokov (v 1/8 bloka) se ne interpolira (teleport). */
    public static final int TELEPORT_Q = 16 * 8;

    private FarInterp() {
    }

    /** Delez premika 0..1 ob casu {@code now} za posnetek, prejet ob {@code t0}. */
    public static float fraction(int t0, int now, int duration) {
        if (duration <= 0) {
            return 1.0f;
        }
        int dt = now - t0;
        if (dt <= 0) {
            return 0.0f;
        }
        return dt >= duration ? 1.0f : dt / (float) duration;
    }

    /** Interpoliran polozaj v 1/8 bloka (za nov zacetek premika). */
    public static int lerpQ(int prev, int cur, float a) {
        return prev + Math.round((cur - prev) * a);
    }

    /**
     * M5-V.5: nov vnos shrambe. Premik se zacne pri trenutno prikazanem (interpoliranem) polozaju starega
     * vnosa; pri skoku nad {@link #TELEPORT_Q} ali novem NPC-ju brez premika. Svetloba se ohrani, dokler
     * NPC ostane v istem bloku.
     */
    public static int[] entryOf(int[] old, int x, int y, int z, int yaw, int look, int flags, int now) {
        int px = x;
        int py = y;
        int pz = z;
        int light = -1;
        if (old != null) {
            float a = fraction(old[E_T], now, DURATION_MS);
            int ox = lerpQ(old[E_PX], old[0], a);
            int oy = lerpQ(old[E_PY], old[1], a);
            int oz = lerpQ(old[E_PZ], old[2], a);
            if (Math.abs(ox - x) <= TELEPORT_Q && Math.abs(oy - y) <= TELEPORT_Q && Math.abs(oz - z) <= TELEPORT_Q) {
                px = ox;
                py = oy;
                pz = oz;
            }
            if ((old[0] >> 3) == (x >> 3) && (old[1] >> 3) == (y >> 3) && (old[2] >> 3) == (z >> 3)) {
                light = old[E_LIGHT];
            }
        }
        return new int[] {x, y, z, yaw, look, flags, px, py, pz, now, light};
    }

    /** Interpoliran polozaj v blokih. */
    public static double lerpBlocks(int prev, int cur, float a) {
        return (prev + (cur - prev) * (double) a) / 8.0;
    }

    /** Yaw (stopinje) iz bajta posnetka. */
    public static float yawOf(int yawByte) {
        return (yawByte & 0xFF) * (360.0f / 256.0f);
    }

    /**
     * Desni vektor ploskve {@code {rx, rz}} (enotski, vodoraven) za smer kamera -&gt; NPC {@code (dx, dz)}: na
     * zaslonu je to desno, kot pri peki ({@code ImpostorCheck}: kamera gleda proti -Z, desno je +X). Pri
     * nicelni smeri vrne desno za pogled proti +Z.
     */
    public static float[] right(double dx, double dz) {
        double l = Math.sqrt(dx * dx + dz * dz);
        if (l < 1.0e-6) {
            return new float[] {-1.0f, 0.0f};
        }
        return new float[] {(float) (-dz / l), (float) (dx / l)};
    }

    /**
     * Projekcija tocke (koordinate glede na kamero, kot pri izrisu) z matrikama OpenGL (stolpcno) v piksle
     * okna {@code {x, y}} z izvorom spodaj levo; {@code null}, ce je tocka za kamero.
     */
    public static float[] project(float[] mv, float[] pr, double x, double y, double z, int width, int height) {
        double ex = mv[0] * x + mv[4] * y + mv[8] * z + mv[12];
        double ey = mv[1] * x + mv[5] * y + mv[9] * z + mv[13];
        double ez = mv[2] * x + mv[6] * y + mv[10] * z + mv[14];
        double ew = mv[3] * x + mv[7] * y + mv[11] * z + mv[15];
        double cx = pr[0] * ex + pr[4] * ey + pr[8] * ez + pr[12] * ew;
        double cy = pr[1] * ex + pr[5] * ey + pr[9] * ez + pr[13] * ew;
        double cw = pr[3] * ex + pr[7] * ey + pr[11] * ez + pr[15] * ew;
        if (cw <= 1.0e-9) {
            return null;
        }
        return new float[] {(float) ((cx / cw + 1.0) * 0.5 * width), (float) ((cy / cw + 1.0) * 0.5 * height)};
    }
}
