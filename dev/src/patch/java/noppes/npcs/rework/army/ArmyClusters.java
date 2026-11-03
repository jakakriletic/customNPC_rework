package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * M11.2 (research 3.1): members of one army grouped by horizontal distance, without game state. Single link: two
 * members within the radius are one cluster, so a long column stays one point. The largest cluster is the main one
 * also below the threshold, so an army never drops off the map; smaller ones are groups or, below the threshold,
 * stragglers. The order does not depend on the input order.
 */
public final class ArmyClusters {
    public static final int DEFAULT_RADIUS = 48;
    public static final int MAX_RADIUS = 512;
    /** One company (Q298). */
    public static final int DEFAULT_MIN_MEMBERS = 8;
    public static final int MAX_MIN_MEMBERS = 1024;

    public enum Kind {
        MAIN, GROUP, STRAGGLER;

        private static final Kind[] VALUES = values();

        static Kind of(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
        }

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Position of one member. */
    public static final class Point {
        final int dimension;
        final double x, y, z;

        public Point(int dimension, double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** One marker: rounded mean of the positions of its members. */
    public static final class Cluster {
        public final Kind kind;
        public final int dimension, x, y, z, size;

        Cluster(Kind kind, int dimension, int x, int y, int z, int size) {
            this.kind = kind;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
        }
    }

    private ArmyClusters() {
    }

    public static int clampRadius(int radius) {
        return Math.max(1, Math.min(MAX_RADIUS, radius));
    }

    public static int clampMinMembers(int min) {
        return Math.max(1, Math.min(MAX_MIN_MEMBERS, min));
    }

    /** Clusters sorted by size (largest first), then dimension, x and z; the first one is the main one. */
    public static List<Cluster> cluster(List<Point> points, int radius, int minMembers) {
        int[] parent = new int[points.size()];
        Map<Integer, List<Integer>> dimensions = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
            int dimension = points.get(i).dimension;
            List<Integer> list = dimensions.get(dimension);
            if (list == null) {
                dimensions.put(dimension, list = new ArrayList<Integer>());
            }
            list.add(i);
        }
        for (List<Integer> members : dimensions.values()) {
            linkDimension(points, members, radius, parent);
        }
        Map<Integer, List<Point>> roots = new HashMap<Integer, List<Point>>();
        for (int i = 0; i < parent.length; i++) {
            int root = find(parent, i);
            List<Point> list = roots.get(root);
            if (list == null) {
                roots.put(root, list = new ArrayList<Point>());
            }
            list.add(points.get(i));
        }
        List<int[]> rows = new ArrayList<int[]>();
        for (List<Point> list : roots.values()) {
            double x = 0, y = 0, z = 0;
            for (Point p : list) {
                x += p.x;
                y += p.y;
                z += p.z;
            }
            int n = list.size();
            rows.add(new int[] {n, list.get(0).dimension, (int) Math.round(x / n), (int) Math.round(y / n),
                    (int) Math.round(z / n)});
        }
        Collections.sort(rows, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                if (a[0] != b[0]) {
                    return Integer.compare(b[0], a[0]);
                }
                if (a[1] != b[1]) {
                    return Integer.compare(a[1], b[1]);
                }
                return a[2] != b[2] ? Integer.compare(a[2], b[2]) : Integer.compare(a[4], b[4]);
            }
        });
        List<Cluster> out = new ArrayList<Cluster>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            int[] r = rows.get(i);
            Kind kind = i == 0 ? Kind.MAIN : r[0] >= minMembers ? Kind.GROUP : Kind.STRAGGLER;
            out.add(new Cluster(kind, r[1], r[2], r[3], r[4], r[0]));
        }
        return out;
    }

    /**
     * The side of a cell is radius/sqrt(2): members of one cell are always within the radius, and a member within
     * the radius is at most two cells away, so a pair of cells is settled by its first close pair.
     */
    private static void linkDimension(List<Point> points, List<Integer> members, int radius, int[] parent) {
        double side = radius / Math.sqrt(2);
        double limit = (double) radius * radius;
        Map<Long, List<Integer>> cells = new HashMap<Long, List<Integer>>();
        for (int i : members) {
            Point p = points.get(i);
            long key = cell((int) Math.floor(p.x / side), (int) Math.floor(p.z / side));
            List<Integer> list = cells.get(key);
            if (list == null) {
                cells.put(key, list = new ArrayList<Integer>());
            }
            if (!list.isEmpty()) {
                union(parent, list.get(0), i);
            }
            list.add(i);
        }
        for (Map.Entry<Long, List<Integer>> e : cells.entrySet()) {
            long key = e.getKey();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            for (int dx = 0; dx <= 2; dx++) {
                for (int dz = dx == 0 ? 1 : -2; dz <= 2; dz++) {
                    List<Integer> other = cells.get(cell(cx + dx, cz + dz));
                    if (other != null) {
                        linkCells(points, e.getValue(), other, limit, parent);
                    }
                }
            }
        }
    }

    private static void linkCells(List<Point> points, List<Integer> a, List<Integer> b, double limit, int[] parent) {
        if (find(parent, a.get(0)) == find(parent, b.get(0))) {
            return;
        }
        for (int i : a) {
            Point p = points.get(i);
            for (int j : b) {
                Point q = points.get(j);
                double dx = p.x - q.x, dz = p.z - q.z;
                if (dx * dx + dz * dz <= limit) {
                    union(parent, i, j);
                    return;
                }
            }
        }
    }

    private static long cell(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        parent[find(parent, a)] = find(parent, b);
    }
}
