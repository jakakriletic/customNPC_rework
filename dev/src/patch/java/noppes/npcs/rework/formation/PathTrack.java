package noppes.npcs.rework.formation;

/**
 * Lomljena crta, po kateri se premika sidro formacije, parametrizirana z dolzino loka.
 *
 * <p>Tocke so v svetovnih koordinatah, {@code y} je visina stopal. Pot pride iz enega
 * samega iskanja poti (vodja) in jo uporabljajo vsi clani: to je deljenje poti iz M5.6,
 * omejeno na eno enoto.
 */
public final class PathTrack {
    private final double[] xs;
    private final double[] ys;
    private final double[] zs;
    private final double[] cumulative;

    public PathTrack(double[] xs, double[] ys, double[] zs) {
        if (xs.length == 0 || xs.length != ys.length || xs.length != zs.length) {
            throw new IllegalArgumentException("pot mora imeti vsaj eno tocko in enake dolzine tabel");
        }
        this.xs = xs.clone();
        this.ys = ys.clone();
        this.zs = zs.clone();
        this.cumulative = new double[xs.length];
        for (int i = 1; i < xs.length; i++) {
            double dx = xs[i] - xs[i - 1];
            double dy = ys[i] - ys[i - 1];
            double dz = zs[i] - zs[i - 1];
            cumulative[i] = cumulative[i - 1] + Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    /** Ravna crta od A do B; nadomestek, ko iskanje poti ne vrne nicesar. */
    public static PathTrack straight(double ax, double ay, double az, double bx, double by, double bz) {
        return new PathTrack(new double[] {ax, bx}, new double[] {ay, by}, new double[] {az, bz});
    }

    /**
     * Pot, ki se zacne v sidru: iz tock poti odstrani zacetni del do tocke, ki je sidru
     * najblizja, in na zacetek postavi sidro. Brez tega bi se sidro ob ponovnem iskanju
     * vracalo do polozaja vodje, ki je lahko za sidrom.
     */
    public static PathTrack fromAnchor(double ax, double ay, double az, double[] xs, double[] ys, double[] zs) {
        int nearest = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            double dx = xs[i] - ax;
            double dz = zs[i] - az;
            double d = dx * dx + dz * dz;
            if (d < best) {
                best = d;
                nearest = i;
            }
        }
        int count = xs.length - nearest;
        double[] nx = new double[count + 1];
        double[] ny = new double[count + 1];
        double[] nz = new double[count + 1];
        nx[0] = ax;
        ny[0] = ay;
        nz[0] = az;
        for (int i = 0; i < count; i++) {
            nx[i + 1] = xs[nearest + i];
            ny[i + 1] = ys[nearest + i];
            nz[i + 1] = zs[nearest + i];
        }
        return new PathTrack(nx, ny, nz);
    }

    public double length() {
        return cumulative[cumulative.length - 1];
    }

    public int size() {
        return xs.length;
    }

    public double x(double s) {
        return interpolate(xs, s);
    }

    public double y(double s) {
        return interpolate(ys, s);
    }

    public double z(double s) {
        return interpolate(zs, s);
    }

    public double endX() {
        return xs[xs.length - 1];
    }

    public double endY() {
        return ys[ys.length - 1];
    }

    public double endZ() {
        return zs[zs.length - 1];
    }

    /**
     * Smer poti okoli {@code s}: od tocke malo zadaj do tocke malo naprej. Gledanje naprej
     * zgladi stopnicaste poti vanilla A*, da se formacija ne zvija na vsakem bloku.
     */
    public double yawAt(double s, double behind, double ahead) {
        double a = Math.max(0, s - behind);
        double b = Math.min(length(), s + ahead);
        if (b - a < 1.0E-6) {
            if (xs.length < 2) {
                return Double.NaN;
            }
            return FormationMath.yawOf(xs[xs.length - 1] - xs[0], zs[zs.length - 1] - zs[0]);
        }
        return FormationMath.yawOf(x(b) - x(a), z(b) - z(a));
    }

    private double interpolate(double[] values, double s) {
        if (values.length == 1 || s <= 0) {
            return values[0];
        }
        if (s >= length()) {
            return values[values.length - 1];
        }
        int lo = 0;
        int hi = cumulative.length - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (cumulative[mid] <= s) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double seg = cumulative[hi] - cumulative[lo];
        double t = seg < 1.0E-9 ? 0 : (s - cumulative[lo]) / seg;
        return values[lo] + (values[hi] - values[lo]) * t;
    }
}
