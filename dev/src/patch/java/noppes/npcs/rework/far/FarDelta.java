package noppes.npcs.rework.far;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * M5-V.2: zadnji poslani posnetek pasu za enega igralca in delta do novega (raziskava M5-V.0 §5.1/4).
 *
 * <p>Krog: {@link #begin()}, za vsak NPC v pasu {@link #offer} (vrne, ali ga je treba poslati), nato
 * {@link #end} (id-ji, ki jih v tem krogu ni bilo, so odstranjeni). Poslati je treba nove id-je in
 * spremenjene: premik za vec kot {@link #MOVE_Q} (0,5 bloka v 1/8) po kateri koli osi, yaw za vec kot
 * {@link #YAW_STEPS} (11,25 stopinje), drug videz ali druge zastavice. Primerja se z zadnjim
 * <b>poslanim</b> stanjem, zato se drobni premiki sestevajo, dokler ne presezejo praga. Mirujoci NPC-ji
 * po prvem posnetku ne stanejo nic.
 *
 * <p>Brez Minecraft tipov, testljivo brez sveta. Ni varno za vec niti (bere ga samo strezniska nit).
 */
public final class FarDelta {
    /** Prag premika v 1/8 bloka: vec kot 4 = vec kot 0,5 bloka. */
    public static final int MOVE_Q = 4;
    /** Prag yaw v korakih 1,40625 stopinje: vec kot 8 = vec kot 11,25 stopinje. */
    public static final int YAW_STEPS = 8;

    private static final int QX = 0;
    private static final int QY = 1;
    private static final int QZ = 2;
    private static final int YAW = 3;
    private static final int LOOK = 4;
    private static final int FLAGS = 5;
    private static final int ROUND = 6;

    private final Map<Integer, int[]> sent = new HashMap<Integer, int[]>();
    private int round;
    private int seen;

    public void begin() {
        ++this.round;
        this.seen = 0;
    }

    /**
     * NPC je v tem krogu v pasu. Vrne {@code true}, ce ga je treba poslati (nov ali spremenjen); takrat
     * je novo stanje ze shranjeno kot poslano.
     */
    public boolean offer(int id, int qx, int qy, int qz, int yaw, int look, int flags) {
        ++this.seen;
        int[] s = this.sent.get(id);
        if (s == null) {
            this.sent.put(id, new int[] {qx, qy, qz, yaw, look, flags, this.round});
            return true;
        }
        s[ROUND] = this.round;
        boolean changed = Math.abs(qx - s[QX]) > MOVE_Q || Math.abs(qy - s[QY]) > MOVE_Q
                || Math.abs(qz - s[QZ]) > MOVE_Q || FarCodec.yawDiff(yaw, s[YAW]) > YAW_STEPS
                || look != s[LOOK] || flags != s[FLAGS];
        if (!changed) {
            return false;
        }
        s[QX] = qx;
        s[QY] = qy;
        s[QZ] = qz;
        s[YAW] = yaw;
        s[LOOK] = look;
        s[FLAGS] = flags;
        return true;
    }

    /**
     * Konec kroga: odstrani id-je, ki jih v tem krogu ni bilo, in jih zapise v {@code removedOut}
     * (ce je polje premajhno, se poveca). Vrne novo polje (lahko isto) in stevilo v {@code count[0]}.
     */
    public int[] end(int[] removedOut, int[] count) {
        int[] out = removedOut;
        int n = 0;
        if (this.sent.size() > this.seen) {
            for (Iterator<Map.Entry<Integer, int[]>> it = this.sent.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, int[]> e = it.next();
                if (e.getValue()[ROUND] != this.round) {
                    if (n == out.length) {
                        out = java.util.Arrays.copyOf(out, Math.max(16, n * 2));
                    }
                    out[n++] = e.getKey();
                    it.remove();
                }
            }
        }
        count[0] = n;
        return out;
    }

    /** Stevilo id-jev, ki jih klient po zadnjem krogu pozna. */
    public int size() {
        return this.sent.size();
    }

    /** Naslednji krog poslje vse znova (nov HELLO, menjava dimenzije, ponoven vklop). */
    public void clear() {
        this.sent.clear();
    }
}
