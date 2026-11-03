package si.ladja.xaerofleet.client;

/**
 * The map marker: the captain's own face under a tricorn in his faction's colour.
 *
 * <p>Pure pixels, no OpenGL, so that it can be tested and written out as a
 * preview without a game. The face is the one {@code generate-captain-assets.py}
 * paints on the skin - same skin, brow, eyes, moustache and beard - squeezed
 * into seven rows so it still reads at twelve GUI pixels. The hat is what tells
 * the factions apart, as it does on deck: green for a friend, red for an enemy,
 * black for a pirate. The silhouette gets a dark outline so the marker stays
 * readable over snow, sand and open water alike.</p>
 */
public final class CaptainIcon {

    public static final int SIZE = 16;

    public static final int ALLY_HAT = 0xFF3FB950;
    public static final int ENEMY_HAT = 0xFFD73A3A;
    public static final int PIRATE_HAT = 0xFF2A2A30;
    public static final int UNKNOWN_HAT = 0xFF8A8A8A;

    private static final int SKIN = 0xFFC89670;
    private static final int SKIN_DARK = 0xFFAA7C5C;
    private static final int EYE = 0xFF1C1816;
    private static final int HAIR = 0xFF3A281E;
    private static final int GOLD = 0xFFE0B84A;
    private static final int OUTLINE = 0xE0101014;

    /**
     * Hat, five rows, fourteen wide: C crown, L crown lit from above, T the
     * turned-up corners of the tricorn, B brim, D band, G the gold cockade.
     */
    private static final String[] HAT = {
            "....LLLLLL....",
            "T..CCCCCCCC..T",
            "TT.DDDGGDDD.TT",
            "BTTBBBBBBBBTTB",
            ".BBBBBBBBBBBB.",
    };

    /** Face, seven rows, eight wide: S skin, K shade, E eye, H hair and beard. */
    private static final String[] FACE = {
            "SSSSSSSS",
            "SESSSSES",
            "SSSKKSSS",
            "HSSHHSSH",
            "HHSSSSHH",
            "HHHHHHHH",
            ".HHHHHH.",
    };

    private static final int HAT_LEFT = 1;
    private static final int HAT_TOP = 2;
    private static final int FACE_LEFT = 4;
    private static final int FACE_TOP = HAT_TOP + HAT.length;

    private CaptainIcon() {}

    /** The hat colour for a faction id as the snapshot sends it. */
    public static int hatColour(String faction) {
        if ("ALLY".equalsIgnoreCase(faction)) return ALLY_HAT;
        if ("ENEMY".equalsIgnoreCase(faction)) return ENEMY_HAT;
        if ("PIRATE".equalsIgnoreCase(faction)) return PIRATE_HAT;
        return UNKNOWN_HAT;
    }

    /** A lighter tint of the hat colour, for rings and lines that belong to him. */
    public static int accent(String faction) {
        int hat = hatColour(faction);
        return hat == PIRATE_HAT ? 0xFFBFBFBF : shade(hat, 1.25F);
    }

    /** ARGB pixels, row by row, {@link #SIZE} x {@link #SIZE}. */
    public static int[] pixels(int hat) {
        int[] image = new int[SIZE * SIZE];
        int crown = hat | 0xFF000000;
        int lit = shade(crown, 1.35F);
        int tip = shade(crown, 1.15F);
        int brim = shade(crown, 0.62F);
        int band = shade(crown, 0.42F);

        for (int row = 0; row < FACE.length; row++) {
            String line = FACE[row];
            for (int col = 0; col < line.length(); col++) {
                int colour;
                switch (line.charAt(col)) {
                    case 'S': colour = SKIN; break;
                    case 'K': colour = SKIN_DARK; break;
                    case 'E': colour = EYE; break;
                    case 'H': colour = HAIR; break;
                    default: continue;
                }
                image[(FACE_TOP + row) * SIZE + FACE_LEFT + col] = colour;
            }
        }
        for (int row = 0; row < HAT.length; row++) {
            String line = HAT[row];
            for (int col = 0; col < line.length(); col++) {
                int colour;
                switch (line.charAt(col)) {
                    case 'C': colour = crown; break;
                    case 'L': colour = lit; break;
                    case 'T': colour = tip; break;
                    case 'B': colour = brim; break;
                    case 'D': colour = band; break;
                    case 'G': colour = GOLD; break;
                    default: continue;
                }
                image[(HAT_TOP + row) * SIZE + HAT_LEFT + col] = colour;
            }
        }
        return outlined(image);
    }

    /** One dark pixel around everything that is not transparent. */
    private static int[] outlined(int[] image) {
        int[] result = image.clone();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (image[y * SIZE + x] != 0) continue;
                if (opaque(image, x - 1, y) || opaque(image, x + 1, y)
                        || opaque(image, x, y - 1) || opaque(image, x, y + 1)) {
                    result[y * SIZE + x] = OUTLINE;
                }
            }
        }
        return result;
    }

    private static boolean opaque(int[] image, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && image[y * SIZE + x] != 0;
    }

    /** The same colour, lighter above one and darker below it. */
    static int shade(int argb, float factor) {
        int a = argb >>> 24;
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((argb & 0xFF) * factor));
        return a << 24 | r << 16 | g << 8 | b;
    }
}
