package noppes.npcs.rework.formation;

import java.util.ArrayList;
import java.util.List;

/**
 * Generatorji oblik formacij. Vsaka oblika vrne mesta, sredinjena okoli (0, 0), da je
 * sidro na poti res sredisce formacije.
 *
 * <p>Oblike so namenoma prevod tistega, kar je uporabnikova skripta ze delala (legija,
 * obramba, march), da se obnasanje ukazov ne spremeni, spremeni se samo gibanje do tja.
 */
public final class FormationShape {
    /** Razmik med mesti v legiji, kot v skripti ({@code x * 2}). */
    public static final double LEGION_SPACING = 2.0;
    /** Skripta je vsakih 5 stolpcev dodala en blok razmika (manipli). */
    public static final int LEGION_GROUP = 5;
    public static final double LEGION_GROUP_GAP = 1.0;
    /** Razmik po obodu kroga, kot v skripti ({@code DEFENSE_SPACING}). */
    public static final double RING_SPACING = 2.3;
    /** Razmik med zaporednimi krogi, kot v skripti ({@code r += 3}). */
    public static final double RING_STEP = 3.0;
    public static final double COLUMN_SIDE_SPACING = 1.5;
    public static final double COLUMN_BACK_SPACING = 2.0;

    private FormationShape() {
    }

    /**
     * Pravokotna formacija: {@code width} stolpcev, vrstic kolikor je treba. Zadnja, nepolna
     * vrstica je sredinjena.
     */
    public static Slot[] block(int n, int width, double spacing, int groupSize, double groupGap) {
        return block(n, width, spacing, spacing, groupSize, groupGap);
    }

    /** Kot zgoraj, z locenim razmikom med stolpci in med vrstami. */
    public static Slot[] block(int n, int width, double sideSpacing, double backSpacing, int groupSize,
            double groupGap) {
        if (n <= 0) {
            return new Slot[0];
        }
        int w = Math.max(1, Math.min(width, n));
        double[] columns = new double[w];
        for (int c = 0; c < w; c++) {
            double gaps = groupSize > 0 ? Math.floor(c / (double) groupSize) * groupGap : 0.0;
            columns[c] = c * sideSpacing + gaps;
        }
        double mid = (columns[0] + columns[w - 1]) / 2.0;
        for (int c = 0; c < w; c++) {
            columns[c] -= mid;
        }
        int rows = (n + w - 1) / w;
        Slot[] slots = new Slot[n];
        int k = 0;
        for (int r = 0; r < rows; r++) {
            int inRow = Math.min(w, n - k);
            for (int c = 0; c < inRow; c++) {
                double side = inRow == w ? columns[c] : (c - (inRow - 1) / 2.0) * sideSpacing;
                slots[k++] = new Slot(side, r * backSpacing);
            }
        }
        return centerBack(slots);
    }

    /** Legija kot v skripti: razmik 2, vsakih 5 stolpcev en blok vec. */
    public static Slot[] legion(int n, int width) {
        return block(n, width, LEGION_SPACING, LEGION_GROUP, LEGION_GROUP_GAP);
    }

    /** Kolona: ozka formacija za pohod skozi ozka grla. */
    public static Slot[] column(int n, int files) {
        return block(n, files, COLUMN_SIDE_SPACING, COLUMN_BACK_SPACING, 0, 0.0);
    }

    /**
     * Krogi okoli sredisca, kot {@code obramba} v skripti: prvi krog ima polmer
     * {@code radius}, vsak naslednji {@link #RING_STEP} vec, zadnji krog razporedi ostanek
     * enakomerno. NPC-ji ob prihodu gledajo ven.
     */
    public static Slot[] ring(int n, double radius) {
        List<Slot> out = new ArrayList<Slot>();
        double r = Math.max(1.0, radius);
        int left = n;
        while (left > 0) {
            int capacity = Math.max(1, (int) Math.floor(r * 2.0 * Math.PI / RING_SPACING));
            int here = Math.min(capacity, left);
            for (int c = 0; c < here; c++) {
                double phi = (c + 1) / (double) here * 2.0 * Math.PI;
                out.add(new Slot(Math.cos(phi) * r, Math.sin(phi) * r, true));
            }
            left -= here;
            r += RING_STEP;
        }
        return out.toArray(new Slot[out.size()]);
    }

    /**
     * Ohrani trenutne medsebojne odmike (march v skripti). Odmiki se preberejo v sistemu
     * formacije z yawom {@code yaw}, zato se formacija na zavojih zasuka kot celota, namesto
     * da bi drsela bocno.
     */
    public static Slot[] keep(double[] xs, double[] zs, double yaw) {
        int n = xs.length;
        Slot[] slots = new Slot[n];
        if (n == 0) {
            return slots;
        }
        double cx = 0;
        double cz = 0;
        for (int i = 0; i < n; i++) {
            cx += xs[i];
            cz += zs[i];
        }
        cx /= n;
        cz /= n;
        for (int i = 0; i < n; i++) {
            double dx = xs[i] - cx;
            double dz = zs[i] - cz;
            slots[i] = new Slot(FormationMath.localSide(dx, dz, yaw), FormationMath.localBack(dx, dz, yaw));
        }
        return slots;
    }

    /** Sredinji samo po globini; bocno so oblike simetricne ze po konstrukciji. */
    private static Slot[] centerBack(Slot[] slots) {
        double sum = 0;
        for (Slot s : slots) {
            sum += s.back;
        }
        double mean = sum / slots.length;
        Slot[] out = new Slot[slots.length];
        for (int i = 0; i < slots.length; i++) {
            out[i] = new Slot(slots[i].side, slots[i].back - mean, slots[i].faceOutward);
        }
        return out;
    }
}
