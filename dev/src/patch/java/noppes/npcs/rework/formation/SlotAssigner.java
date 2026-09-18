package noppes.npcs.rework.formation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Dodelitev mest clanom tako, da se poti ne krizajo.
 *
 * <p>Skripta je vsakemu mestu dodelila najblizjega se prostega NPC-ja. To je pozresno in
 * pri vecjih skupinah daje krizanja: prvi NPC-ji poberejo blizja mesta, zadnji pa tecejo
 * cez celo formacijo. Tukaj se clani in mesta uredijo v istem lokalnem sistemu formacije:
 * sprednji clani dobijo sprednjo vrsto, znotraj vrste pa gredo po vrsti od leve proti
 * desni. Taka dodelitev je monotona in se v vrsti ne more krizati.
 *
 * <p>Pri krogu (vsa mesta {@link Slot#faceOutward}) se clani razdelijo po oddaljenosti od
 * svojega sredisca na kroge, znotraj kroga pa po kotu, z zasukom, ki minimizira vsoto
 * kvadratov razdalj.
 */
public final class SlotAssigner {
    private static final double ROW_EPS = 0.5;

    private SlotAssigner() {
    }

    /**
     * @return {@code result[i]} = indeks mesta za clana {@code i}; vsako mesto najvec enkrat.
     *         Clanov je lahko vec kot mest; odvec clani dobijo {@code -1}.
     */
    public static int[] assign(double[] xs, double[] zs, Slot[] slots, double yaw) {
        int n = xs.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        if (n == 0 || slots.length == 0) {
            return result;
        }
        double cx = 0;
        double cz = 0;
        for (int i = 0; i < n; i++) {
            cx += xs[i];
            cz += zs[i];
        }
        cx /= n;
        cz /= n;
        final double[] ms = new double[n];
        final double[] mb = new double[n];
        for (int i = 0; i < n; i++) {
            ms[i] = FormationMath.localSide(xs[i] - cx, zs[i] - cz, yaw);
            mb[i] = FormationMath.localBack(xs[i] - cx, zs[i] - cz, yaw);
        }
        if (allOutward(slots)) {
            assignRings(ms, mb, slots, result);
        } else {
            assignRows(ms, mb, slots, result);
        }
        return result;
    }

    private static boolean allOutward(Slot[] slots) {
        for (Slot s : slots) {
            if (!s.faceOutward) {
                return false;
            }
        }
        return true;
    }

    private static void assignRows(final double[] ms, final double[] mb, final Slot[] slots, int[] result) {
        Integer[] slotOrder = boxedRange(slots.length);
        Arrays.sort(slotOrder, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int c = Double.compare(slots[a].back, slots[b].back);
                return c != 0 ? c : Double.compare(slots[a].side, slots[b].side);
            }
        });
        Integer[] memberOrder = boxedRange(ms.length);
        Arrays.sort(memberOrder, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int c = Double.compare(mb[a], mb[b]);
                return c != 0 ? c : Double.compare(ms[a], ms[b]);
            }
        });
        int s = 0;
        int m = 0;
        while (s < slotOrder.length && m < memberOrder.length) {
            int rowEnd = s + 1;
            while (rowEnd < slotOrder.length
                    && Math.abs(slots[slotOrder[rowEnd]].back - slots[slotOrder[s]].back) < ROW_EPS) {
                rowEnd++;
            }
            int take = Math.min(rowEnd - s, memberOrder.length - m);
            Integer[] rowMembers = Arrays.copyOfRange(memberOrder, m, m + take);
            Arrays.sort(rowMembers, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Double.compare(ms[a], ms[b]);
                }
            });
            // Nepolna zadnja vrsta: clani zasedejo sredinska mesta vrste.
            int offset = (rowEnd - s - take) / 2;
            for (int k = 0; k < take; k++) {
                result[rowMembers[k]] = slotOrder[s + offset + k];
            }
            s = rowEnd;
            m += take;
        }
    }

    private static void assignRings(final double[] ms, final double[] mb, final Slot[] slots, int[] result) {
        Integer[] slotOrder = boxedRange(slots.length);
        Arrays.sort(slotOrder, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(radius(slots[a].side, slots[a].back), radius(slots[b].side, slots[b].back));
            }
        });
        Integer[] memberOrder = boxedRange(ms.length);
        Arrays.sort(memberOrder, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(radius(ms[a], mb[a]), radius(ms[b], mb[b]));
            }
        });
        int s = 0;
        int m = 0;
        while (s < slotOrder.length && m < memberOrder.length) {
            double r0 = radius(slots[slotOrder[s]].side, slots[slotOrder[s]].back);
            int ringEnd = s + 1;
            while (ringEnd < slotOrder.length
                    && Math.abs(radius(slots[slotOrder[ringEnd]].side, slots[slotOrder[ringEnd]].back) - r0) < ROW_EPS) {
                ringEnd++;
            }
            int take = Math.min(ringEnd - s, memberOrder.length - m);
            List<Integer> ringSlots = new ArrayList<Integer>(Arrays.asList(slotOrder).subList(s, ringEnd));
            List<Integer> ringMembers = new ArrayList<Integer>(Arrays.asList(memberOrder).subList(m, m + take));
            sortByAngle(ringSlots, slots);
            sortMembersByAngle(ringMembers, ms, mb);
            int size = ringSlots.size();
            int bestShift = 0;
            double bestCost = Double.MAX_VALUE;
            for (int shift = 0; shift < size; shift++) {
                double cost = 0;
                for (int k = 0; k < take; k++) {
                    Slot sl = slots[ringSlots.get((k + shift) % size)];
                    int mi = ringMembers.get(k);
                    double dx = sl.side - ms[mi];
                    double dz = sl.back - mb[mi];
                    cost += dx * dx + dz * dz;
                }
                if (cost < bestCost) {
                    bestCost = cost;
                    bestShift = shift;
                }
            }
            for (int k = 0; k < take; k++) {
                result[ringMembers.get(k)] = ringSlots.get((k + bestShift) % size);
            }
            s = ringEnd;
            m += take;
        }
    }

    private static void sortByAngle(List<Integer> idx, final Slot[] slots) {
        java.util.Collections.sort(idx, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(Math.atan2(slots[a].back, slots[a].side), Math.atan2(slots[b].back, slots[b].side));
            }
        });
    }

    private static void sortMembersByAngle(List<Integer> idx, final double[] ms, final double[] mb) {
        java.util.Collections.sort(idx, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(Math.atan2(mb[a], ms[a]), Math.atan2(mb[b], ms[b]));
            }
        });
    }

    private static double radius(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }

    private static Integer[] boxedRange(int n) {
        Integer[] out = new Integer[n];
        for (int i = 0; i < n; i++) {
            out[i] = i;
        }
        return out;
    }
}
