package noppes.npcs.rework.render;

/**
 * Kateri stirikotniki pokrivala lahko pustijo kak fragment (M5-K K14b). Brez GL, testljivo.
 *
 * <p>Tekstura skina NPC-ja je {@code SimpleTexture} brez metapodatkov: filter GL_NEAREST, ovijanje
 * GL_REPEAT, brez mipmap. Fragment vzame teksel {@code floor(u·W)}, {@code floor(v·H)}; u in v
 * fragmenta lezita v obsegu uv stirikotnika (do napake interpolacije). Stirikotnik, katerega vsi
 * mozni teksli imajo alfo 0, da fragmente z alfo 0 (barva oglisca alfe ne poveca), alfa test
 * ({@code GL_GREATER}, ref ≥ 0) jih zavrze vse: ne spremeni barve ne globine. Tak stirikotnik se
 * sme izpustiti. Obseg se razsiri za {@link #ROB} teksla na vsako stran (napaka interpolacije,
 * rob obraza tocno na meji teksla, HD skin s sredino teksla modela na meji tekslov slike).
 */
public final class HeadwearMask {
    /** Razsiritev obsega v tekslih slike. */
    public static final float ROB = 0.01f;

    private HeadwearMask() {
    }

    /**
     * @param argb slika (vrstica za vrstico, ARGB kot {@code BufferedImage.getRGB})
     * @param bounds {@link HeadwearQuads#uvBounds}
     * @return keep[q]: stirikotnik q lahko vzorci teksel z alfo &gt; 0
     */
    public static boolean[] keep(int[] argb, int w, int h, float[][] bounds) {
        return keep(argb, w, h, bounds, false);
    }

    /**
     * @param inner samo teksli znotraj obsega, brez tekslov tocno na robu in brez ovijanja cez rob
     *              ({@code RwHeadwearSkip=1}). Fragment na sami robni crti, ki v originalu vzame
     *              sosednji teksel, se lahko razlikuje — ni dokazano enako. Brez {@code inner}
     *              (z robom) je dokazano enako, a pri obicajnih skinih ne izpusti skoraj nicesar
     *              (M5-K K14b: brez dobitka nad sumom), zato se v igri ne uporablja.
     */
    public static boolean[] keep(int[] argb, int w, int h, float[][] bounds, boolean inner) {
        boolean[] keep = new boolean[bounds.length];
        float rob = inner ? -ROB : ROB;
        for (int q = 0; q < bounds.length; q++) {
            float[] b = bounds[q];
            int x0 = (int)Math.floor(b[0] * w - rob), x1 = (int)Math.floor(b[1] * w + rob);
            int y0 = (int)Math.floor(b[2] * h - rob), y1 = (int)Math.floor(b[3] * h + rob);
            if (inner) {
                // trak: sredina teksla, obseg je en teksel
                x1 = Math.max(x0, x1);
                y1 = Math.max(y0, y1);
            }
            keep[q] = anyVisible(argb, w, h, x0, x1, y0, y1);
        }
        return keep;
    }

    private static boolean anyVisible(int[] argb, int w, int h, int x0, int x1, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            int row = Math.floorMod(y, h) * w;
            for (int x = x0; x <= x1; x++) {
                if ((argb[row + Math.floorMod(x, w)] >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 0 = noben stirikotnik, 2 = vsi, 1 = nekateri. */
    public static int kind(boolean[] keep) {
        int n = 0;
        for (boolean k : keep) {
            if (k) {
                n++;
            }
        }
        return n == 0 ? 0 : (n == keep.length ? 2 : 1);
    }
}
