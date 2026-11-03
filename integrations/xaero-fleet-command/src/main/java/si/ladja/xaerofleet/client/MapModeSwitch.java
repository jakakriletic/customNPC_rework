package si.ladja.xaerofleet.client;

/**
 * Where the two-part switch at the top of the map sits, in GUI pixels, and
 * which half a click lands on. Layout only; drawing is in ClientMapEvents.
 */
public final class MapModeSwitch {

    public static final String WORLD_LABEL = "World map";
    public static final String SHIP_LABEL = "Ship's map";
    public static final String STORM_LABEL = "Winter storm";
    public static final String ARMY_LABEL = "Armies";

    static final int TOP = 5;
    static final int HEIGHT = 14;
    static final int PAD = 8;

    public static final int NONE = -1, WORLD = 0, SHIP = 1, STORM = 2, ARMY = 3;

    /** Left half, then right half: {x0, y0, x1, y1} each. */
    final int[][] halves;

    private MapModeSwitch(int[][] halves) {
        this.halves = halves;
    }

    /**
     * Centred at the top of a screen {@code screenWidth} wide, for labels of
     * these widths - but never left of {@code minLeft}, so that on a narrow
     * screen it moves over rather than onto the fleet panel.
     */
    public static MapModeSwitch layout(int screenWidth, int worldLabelWidth, int shipLabelWidth, int minLeft) {
        int w0 = worldLabelWidth + 2 * PAD;
        int w1 = shipLabelWidth + 2 * PAD;
        int x0 = Math.max(minLeft, (screenWidth - w0 - w1) / 2);
        return new MapModeSwitch(new int[][]{
                {x0, TOP, x0 + w0, TOP + HEIGHT},
                {x0 + w0, TOP, x0 + w0 + w1, TOP + HEIGHT}});
    }

    public int[] half(int which) {
        return halves[which];
    }

    public static MapModeSwitch layout(int width,int worldWidth,int shipWidth,int stormWidth,int minLeft) {
        int a=worldWidth+PAD*2,b=shipWidth+PAD*2,c=stormWidth+PAD*2;
        int x=Math.max(minLeft,(width-a-b-c)/2);
        return new MapModeSwitch(new int[][]{{x,TOP,x+a,TOP+HEIGHT},{x+a,TOP,x+a+b,TOP+HEIGHT},{x+a+b,TOP,x+a+b+c,TOP+HEIGHT}});
    }
    public int count() { return halves.length; }

    public static MapModeSwitch layout(int width,int world,int ship,int storm,int army,int minLeft) {
        int[] sizes={world,ship,storm,army}; int total=8*PAD;
        for(int size:sizes)total+=size;
        int x=Math.max(minLeft,(width-total)/2);int[][] boxes=new int[4][];
        for(int i=0;i<4;i++){int right=x+sizes[i]+2*PAD;boxes[i]=new int[]{x,TOP,right,TOP+HEIGHT};x=right;}
        return new MapModeSwitch(boxes);
    }

    public int left() {
        return halves[0][0];
    }

    public int right() {
        return halves[halves.length-1][2];
    }

    public int bottom() {
        return TOP + HEIGHT;
    }

    /** WORLD, SHIP or NONE for a point on the screen. */
    public int at(double x, double y) {
        for (int i = 0; i < halves.length; i++) {
            int[] h = halves[i];
            if (x >= h[0] && x < h[2] && y >= h[1] && y < h[3]) return i;
        }
        return NONE;
    }
}
