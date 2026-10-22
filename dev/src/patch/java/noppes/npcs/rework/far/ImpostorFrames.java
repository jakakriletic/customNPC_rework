package noppes.npcs.rework.far;

/**
 * M5-V.4: postavitev in obdelava slik impostorjev (raziskava M5-V.0 §5.4, D-046). Brez GL in
 * Minecraft tipov, testljivo z JUnit; slike so {@code int[]} ARGB (straight alpha), vrstica 0 zgoraj.
 *
 * <p>Videz zaseda v atlasu en <b>trak</b> {@link #ANGLES} celic {@link #CELL_W} x {@link #CELL_H}: okvir
 * najvec {@link #FRAME_W} x {@link #FRAME_H} tekslov, sredinsko vodoravno in poravnan na dno celice, okoli
 * njega najmanj {@link #PAD} prozornih tekslov. Celice in trakovi so poravnani na {@code 2^(LEVELS-1)},
 * zato vsaka mipmap stopnja celice ostane znotraj celice ({@code 40 x 72 -> 20 x 36 -> 10 x 18 -> 5 x 9}).
 *
 * <p>Okvir {@code k} je model z yaw {@code k * 45} stopinj, gledan od strani {@code +Z} (kamera gleda proti
 * {@code -Z}); {@link #frameFor} iz smeri kamera -&gt; NPC in yaw NPC-ja izbere okvir (0 = NPC gleda v kamero).
 */
public final class ImpostorFrames {
    public static final int ANGLES = 8;
    public static final int FRAME_W = 32;
    public static final int FRAME_H = 64;
    public static final int PAD = 4;
    public static final int CELL_W = FRAME_W + 2 * PAD;
    public static final int CELL_H = FRAME_H + 2 * PAD;
    public static final int STRIP_W = ANGLES * CELL_W;
    /** Stopnje mipmap (0 .. LEVELS-1); celica 40 x 72 se deli brez ostanka do 5 x 9. */
    public static final int LEVELS = 4;
    /** Alfa test vanille {@code GL_GREATER 0.1}: teksel je viden pri alfi &gt;= 26 (od 255). */
    public static final int ALPHA_REF = 26;

    private ImpostorFrames() {
    }

    /** Stevilo trakov (videzov) v kvadratnem atlasu stranice {@code atlas}. */
    public static int slots(int atlas) {
        return (atlas / STRIP_W) * (atlas / CELL_H);
    }

    public static int slotX(int slot, int atlas) {
        return (slot % (atlas / STRIP_W)) * STRIP_W;
    }

    public static int slotY(int slot, int atlas) {
        return (slot / (atlas / STRIP_W)) * CELL_H;
    }

    /** Levi rob okvirja sirine {@code tw} v celici. */
    public static int frameX(int tw) {
        return PAD + (FRAME_W - tw) / 2;
    }

    /** Zgornji rob okvirja visine {@code th} v celici (okvir na dnu). */
    public static int frameY(int th) {
        return PAD + FRAME_H - th;
    }

    /**
     * Okvir za risanje: {@code cameraToNpcYaw} je yaw (stopinje, konvencija Minecrafta: 0 = +Z, 90 = -X)
     * smeri od kamere do NPC-ja, {@code npcYaw} yaw telesa NPC-ja.
     */
    public static int frameFor(float cameraToNpcYaw, float npcYaw) {
        double rel = npcYaw - cameraToNpcYaw + 180.0;
        int k = (int) Math.floor(rel / 45.0 + 0.5);
        return ((k % ANGLES) + ANGLES) % ANGLES;
    }

    /** Yaw (stopinje) smeri {@code (dx, dz)} v konvenciji Minecrafta. */
    public static float yawOf(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Pravokotnik pikslov z alfo &gt; 0: {@code {minX, minY, maxX, maxY}} (vkljucno) ali {@code null}. */
    public static int[] alphaBounds(int[] img, int w, int h) {
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; ++y) {
            int row = y * w;
            for (int x = 0; x < w; ++x) {
                if ((img[row + x] >>> 24) != 0) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    maxY = y;
                }
            }
        }
        return maxX < 0 ? null : new int[] {minX, minY, maxX, maxY};
    }

    /** Unija pravokotnikov {@link #alphaBounds}; {@code null} je prazen. */
    /**
     * M5-V.7 (harness): povprecna barva {@code 0xRRGGBB} tekslov, vidnih po alfa testu ({@link #ALPHA_REF}),
     * ali -1 brez vidnih tekslov.
     */
    public static int averageOpaque(int[] img) {
        long r = 0L, g = 0L, b = 0L, n = 0L;
        for (int p : img) {
            if ((p >>> 24) >= ALPHA_REF) {
                r += (p >> 16) & 0xFF;
                g += (p >> 8) & 0xFF;
                b += p & 0xFF;
                ++n;
            }
        }
        if (n == 0L) {
            return -1;
        }
        return (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
    }

    public static int[] union(int[] a, int[] b) {
        if (a == null) {
            return b == null ? null : b.clone();
        }
        if (b == null) {
            return a.clone();
        }
        return new int[] {Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[2], b[2]), Math.max(a[3], b[3])};
    }

    /** Ali se pravokotnik dotika roba slike (izrez je morda odrezan). */
    public static boolean touchesEdge(int[] r, int w, int h) {
        return r != null && (r[0] <= 0 || r[1] <= 0 || r[2] >= w - 1 || r[3] >= h - 1);
    }

    /**
     * Izrez, simetricen okoli navpicne osi entitete (meja med stolpcema {@code centerX - 1} in
     * {@code centerX}): {@code {x0, y0, sirina, visina}} v pikslih vira.
     */
    public static int[] symmetricCrop(int[] bounds, int centerX) {
        int half = Math.max(centerX - bounds[0], bounds[2] + 1 - centerX);
        half = Math.max(1, half);
        return new int[] {centerX - half, bounds[1], 2 * half, bounds[3] + 1 - bounds[1]};
    }

    /** Velikost okvirja {@code {tw, th}}: izrez sorazmerno pomanjsan v FRAME_W x FRAME_H, nikoli povecan. */
    public static int[] frameSize(int srcW, int srcH) {
        double s = Math.min(1.0, Math.min(FRAME_W / (double) srcW, FRAME_H / (double) srcH));
        int tw = Math.max(1, Math.min(FRAME_W, (int) Math.round(srcW * s)));
        int th = Math.max(1, Math.min(FRAME_H, (int) Math.round(srcH * s)));
        return new int[] {tw, th};
    }

    /**
     * Ploskovno povprecenje izreza {@code (x0, y0, w, h)} slike {@code src} ({@code sw x sh}) v {@code tw x th}
     * s premnozeno alfo (barva prozornih pikslov ne obarva roba). Piksli izven vira so prozorni.
     */
    public static int[] resample(int[] src, int sw, int sh, int x0, int y0, int w, int h, int tw, int th) {
        int[] out = new int[tw * th];
        double fx = w / (double) tw;
        double fy = h / (double) th;
        for (int ty = 0; ty < th; ++ty) {
            double sy0 = y0 + ty * fy;
            double sy1 = sy0 + fy;
            for (int tx = 0; tx < tw; ++tx) {
                double sx0 = x0 + tx * fx;
                double sx1 = sx0 + fx;
                double a = 0, r = 0, g = 0, b = 0, sum = 0;
                for (int y = (int) Math.floor(sy0); y < sy1; ++y) {
                    double wy = Math.min(y + 1, sy1) - Math.max(y, sy0);
                    if (wy <= 0) {
                        continue;
                    }
                    for (int x = (int) Math.floor(sx0); x < sx1; ++x) {
                        double wx = Math.min(x + 1, sx1) - Math.max(x, sx0);
                        if (wx <= 0) {
                            continue;
                        }
                        double wt = wx * wy;
                        sum += wt;
                        if (x < 0 || y < 0 || x >= sw || y >= sh) {
                            continue;
                        }
                        int p = src[y * sw + x];
                        double pa = (p >>> 24) * wt;
                        a += pa;
                        r += ((p >> 16) & 0xFF) * pa;
                        g += ((p >> 8) & 0xFF) * pa;
                        b += (p & 0xFF) * pa;
                    }
                }
                out[ty * tw + tx] = pack(a, r, g, b, sum);
            }
        }
        return out;
    }

    private static int pack(double a, double r, double g, double b, double sum) {
        if (a <= 0 || sum <= 0) {
            return 0;
        }
        int ia = clamp((int) Math.round(a / sum));
        if (ia == 0) {
            return 0;
        }
        return ia << 24 | clamp((int) Math.round(r / a)) << 16 | clamp((int) Math.round(g / a)) << 8
                | clamp((int) Math.round(b / a));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** Celica CELL_W x CELL_H z okvirjem {@code tw x th} na mestu {@link #frameX}, {@link #frameY}. */
    public static int[] cell(int[] frame, int tw, int th) {
        int[] c = new int[CELL_W * CELL_H];
        int ox = frameX(tw);
        int oy = frameY(th);
        for (int y = 0; y < th; ++y) {
            System.arraycopy(frame, y * tw, c, (oy + y) * CELL_W + ox, tw);
        }
        return c;
    }

    /** Polovicna stopnja (2 x 2 povprecje s premnozeno alfo); {@code w}, {@code h} sodi. */
    public static int[] half(int[] img, int w, int h) {
        int hw = w / 2;
        int hh = h / 2;
        int[] out = new int[hw * hh];
        for (int y = 0; y < hh; ++y) {
            for (int x = 0; x < hw; ++x) {
                double a = 0, r = 0, g = 0, b = 0;
                for (int k = 0; k < 4; ++k) {
                    int p = img[(2 * y + (k >> 1)) * w + 2 * x + (k & 1)];
                    double pa = p >>> 24;
                    a += pa;
                    r += ((p >> 16) & 0xFF) * pa;
                    g += ((p >> 8) & 0xFF) * pa;
                    b += (p & 0xFF) * pa;
                }
                out[y * hw + x] = pack(a, r, g, b, 4.0);
            }
        }
        return out;
    }

    /**
     * Stevilo tekslov, ki jih alfa test obdrzi, ko je alfa pomnozena s {@code scale} in zaokrozena kot v
     * {@link #scaleAlpha} (sicer bi bisekcija na meji izbrala faktor, ki po zaokrozitvi da drugo pokritost).
     */
    public static int coverage(int[] img, double scale) {
        int n = 0;
        for (int p : img) {
            int a = p >>> 24;
            if (a != 0 && clamp((int) Math.round(a * scale)) >= ALPHA_REF) {
                ++n;
            }
        }
        return n;
    }

    /**
     * Ohranjanje pokritosti alfe (Castano, <i>Computing Alpha Mipmaps</i>): faktor alfe, pri katerem je delez
     * vidnih tekslov stopnje najblizje deležu {@code target} osnovne stopnje. Bisekcija, pokritost raste s faktorjem.
     */
    public static double coverageScale(int[] img, double target) {
        double lo = 0.0;
        double hi = 16.0;
        double best = 1.0;
        double bestErr = Math.abs(coverage(img, 1.0) / (double) img.length - target);
        for (int i = 0; i < 24; ++i) {
            double mid = 0.5 * (lo + hi);
            double c = coverage(img, mid) / (double) img.length;
            double err = Math.abs(c - target);
            if (err < bestErr) {
                bestErr = err;
                best = mid;
            }
            if (c < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return best;
    }

    /** Alfa pomnozena s {@code scale} (omejena na 255); barva ostane. */
    public static void scaleAlpha(int[] img, double scale) {
        for (int i = 0; i < img.length; ++i) {
            int p = img[i];
            int a = p >>> 24;
            if (a == 0) {
                continue;
            }
            img[i] = clamp((int) Math.round(a * scale)) << 24 | (p & 0xFFFFFF);
        }
    }

    /**
     * Prozornim tekslom da barvo povprecja vidnih sosedov ({@code passes} krogov), da linearno mesanje
     * med stopnjami ne potemni roba silhuete. Alfa ostane 0.
     */
    public static void bleed(int[] img, int w, int h, int passes) {
        for (int pass = 0; pass < passes; ++pass) {
            int[] src = img.clone();
            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    int i = y * w + x;
                    if ((src[i] >>> 24) != 0 || (src[i] & 0xFFFFFF) != 0) {
                        continue;
                    }
                    int n = 0, r = 0, g = 0, b = 0;
                    for (int dy = -1; dy <= 1; ++dy) {
                        for (int dx = -1; dx <= 1; ++dx) {
                            int xx = x + dx, yy = y + dy;
                            if ((dx | dy) == 0 || xx < 0 || yy < 0 || xx >= w || yy >= h) {
                                continue;
                            }
                            int p = src[yy * w + xx];
                            if ((p >>> 24) != 0 || (p & 0xFFFFFF) != 0) {
                                ++n;
                                r += (p >> 16) & 0xFF;
                                g += (p >> 8) & 0xFF;
                                b += p & 0xFF;
                            }
                        }
                    }
                    if (n > 0) {
                        img[i] = (r / n) << 16 | (g / n) << 8 | (b / n);
                    }
                }
            }
        }
    }

    /**
     * Vse stopnje celice: {@code [0]} je celica, vsaka naslednja polovica prejsnje z ohranjeno pokritostjo
     * alfe; nazadnje {@link #bleed} na vsaki stopnji.
     */
    public static int[][] mipChain(int[] cell) {
        int[][] levels = new int[LEVELS][];
        levels[0] = cell.clone();
        double target = coverage(levels[0], 1.0) / (double) levels[0].length;
        int w = CELL_W;
        int h = CELL_H;
        // Vsaka stopnja iz nepopravljene prejsnje, da se faktorji alfe ne sestevajo.
        int[] raw = levels[0];
        for (int l = 1; l < LEVELS; ++l) {
            raw = half(raw, w, h);
            w /= 2;
            h /= 2;
            int[] next = raw.clone();
            if (target > 0) {
                scaleAlpha(next, coverageScale(next, target));
            }
            levels[l] = next;
        }
        w = CELL_W;
        h = CELL_H;
        for (int l = 0; l < LEVELS; ++l) {
            bleed(levels[l], w, h, 2);
            w /= 2;
            h /= 2;
        }
        return levels;
    }
}
