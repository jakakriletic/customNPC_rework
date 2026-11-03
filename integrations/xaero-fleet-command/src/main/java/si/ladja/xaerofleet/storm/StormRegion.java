package si.ladja.xaerofleet.storm;

public final class StormRegion {
    public static final int MAX_ACTIVE = 32, MAX_DURATION = 36000;
    public final int id, dimension;
    public final GridArea area;
    public final long started, expires, seed;
    public StormRegion(int id,int dimension,GridArea area,long started,long expires,long seed) {
        if (id < 1 || area == null || started < 0 || expires <= started || expires - started > MAX_DURATION)
            throw new IllegalArgumentException("Invalid winter storm");
        this.id=id; this.dimension=dimension; this.area=area; this.started=started; this.expires=expires; this.seed=seed;
    }
    public float intensity(double x,double z,long now) {
        if (now < started || now >= expires) return 0;
        return area.edge(x,z) * Math.min(1,Math.min((now-started)/100f,(expires-now)/100f));
    }
    public float gust(long now) { return (float) (0.95 + 0.25*Math.sin(now*0.041 + (seed & 255)) + 0.13*Math.sin(now*0.107)); }
    public double windX(long now) { return Math.cos((seed & 65535) / 65535.0 * Math.PI*2) * gust(now); }
    public double windZ(long now) { return Math.sin((seed & 65535) / 65535.0 * Math.PI*2) * gust(now); }
}
