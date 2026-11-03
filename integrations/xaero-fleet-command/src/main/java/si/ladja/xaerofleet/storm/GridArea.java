package si.ladja.xaerofleet.storm;

/** A bounded rectangle of 64-block map cells. Upper world edges are exclusive. */
public final class GridArea {
    public static final int CELL = 64, MAX_SIDE = 16, WORLD_LIMIT = 29999984;
    public final int minX, minZ, maxX, maxZ;
    public GridArea(int x0, int z0, int x1, int z1) {
        minX = Math.min(x0,x1); minZ = Math.min(z0,z1);
        maxX = Math.max(x0,x1); maxZ = Math.max(z0,z1);
        if ((long) maxX - minX >= MAX_SIDE || (long) maxZ - minZ >= MAX_SIDE
                || (long) minX * CELL < -WORLD_LIMIT || (long) minZ * CELL < -WORLD_LIMIT
                || ((long) maxX + 1) * CELL > WORLD_LIMIT || ((long) maxZ + 1) * CELL > WORLD_LIMIT) {
            throw new IllegalArgumentException("Storm area is outside the map or larger than 16 x 16 cells");
        }
    }
    public static int cell(double world) {
        if (!Double.isFinite(world) || Math.abs(world) > WORLD_LIMIT) throw new IllegalArgumentException("Invalid world coordinate");
        return (int) Math.floor(world / CELL);
    }
    public int width() { return maxX - minX + 1; }
    public int depth() { return maxZ - minZ + 1; }
    public int left() { return minX * CELL; }
    public int top() { return minZ * CELL; }
    public int right() { return (maxX + 1) * CELL; }
    public int bottom() { return (maxZ + 1) * CELL; }
    public boolean contains(double x,double z) { return x >= left() && x < right() && z >= top() && z < bottom(); }
    public boolean overlaps(GridArea other) { return minX <= other.maxX && maxX >= other.minX && minZ <= other.maxZ && maxZ >= other.minZ; }
    public float edge(double x,double z) {
        if (!contains(x,z)) return 0;
        double distance = Math.min(Math.min(x-left(),right()-x),Math.min(z-top(),bottom()-z));
        float n = (float) Math.min(1,distance / 16);
        return n*n*(3-2*n);
    }
}
