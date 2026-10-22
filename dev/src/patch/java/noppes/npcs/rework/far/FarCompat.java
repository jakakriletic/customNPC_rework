package noppes.npcs.rework.far;

import java.lang.reflect.Field;

/**
 * M5-V.7: zdruzljivost izrisa L2/L3 z OptiFine senčilniki (raziskava M5-V.0 §7, D-050).
 *
 * <p>OptiFine 1.12.2 HD U G5 (bajtkoda {@code RenderGlobal} po popravku, analiza M5-V.7) ima sidro
 * {@code "blockentities"} v {@code renderEntities} se vedno natanko enkrat, a s senčilniki tik pred njim poklice
 * {@code Shaders.endEntities()} in {@code Shaders.beginBlockEntities()}: impostorji bi sli skozi program blocnih
 * entitet. {@code ShadersRender.renderShadowMap} postavi {@code Shaders.isShadowPass} in {@code renderEntities}
 * poklice se dvakrat (senčni prehod). Zato:
 * <ul>
 * <li>{@link #OFF}: aktiven paket senčil in {@code RwFarShaders=0} (privzeto) - klient rise original (brez
 * preklica L2, brez impostorjev);</li>
 * <li>{@link #SKIP}: senčni prehod - brez impostorjev; NPC, ki je v L2, se v senčni mapi ne narise;</li>
 * <li>{@link #DRAW}: sicer.</li>
 * </ul>
 * Ce je OptiFine prisoten, a polj {@code Shaders} ni mogoce brati, velja paket senčil za aktiven (varno).
 * Odlocitev {@link #mode(boolean, boolean, int)} je brez Minecraft tipov in testljiva z JUnit.
 */
public final class FarCompat {
    public static final int DRAW = 0;
    public static final int SKIP = 1;
    public static final int OFF = 2;

    static final String SHADERS = "net.optifine.shaders.Shaders";

    /** Slicice brez izrisa zaradi senčil ({@link #OFF}) in preskoceni senčni prehodi ({@link #SKIP}). */
    public static long offFrames;
    public static long shadowSkips;

    private static boolean probed;
    private static boolean optifine;
    private static boolean unreadable;
    private static Field packLoaded;
    private static Field shadowPass;

    private FarCompat() {
    }

    /** Nacin izrisa: aktiven paket senčil, senčni prehod in stikalo {@code RwFarShaders}. */
    public static int mode(boolean shaderPack, boolean inShadowPass, int setting) {
        if (shaderPack && setting == 0) {
            return OFF;
        }
        return inShadowPass ? SKIP : DRAW;
    }

    /** Nacin za trenutni klic (bere stanje OptiFine prek refleksije; brez OptiFine vedno {@link #DRAW}). */
    public static int mode(int setting) {
        probe();
        if (!optifine) {
            return DRAW;
        }
        return mode(shaderPack(), shadowPass(), setting);
    }

    public static boolean optifine() {
        probe();
        return optifine;
    }

    /** Ali je paket senčil aktiven (z neberljivimi polji: da). */
    public static boolean shaderPack() {
        probe();
        if (!optifine) {
            return false;
        }
        if (unreadable) {
            return true;
        }
        try {
            return packLoaded.getBoolean(null);
        } catch (IllegalAccessException e) {
            unreadable = true;
            return true;
        }
    }

    static boolean shadowPass() {
        if (unreadable || shadowPass == null) {
            return false;
        }
        try {
            return shadowPass.getBoolean(null);
        } catch (IllegalAccessException e) {
            unreadable = true;
            return false;
        }
    }

    private static void probe() {
        if (probed) {
            return;
        }
        probed = true;
        Class<?> c;
        try {
            c = Class.forName(SHADERS, false, FarCompat.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return;
        }
        optifine = true;
        try {
            packLoaded = c.getField("shaderPackLoaded");
            shadowPass = c.getField("isShadowPass");
            if (packLoaded.getType() != boolean.class || shadowPass.getType() != boolean.class) {
                unreadable = true;
            }
        } catch (NoSuchFieldException | SecurityException e) {
            unreadable = true;
        }
    }

    /** Del vrstice {@code /rwimpostor}. */
    public static String status(int setting) {
        return "compat: optifine=" + (optifine() ? 1 : 0) + " sencila=" + (shaderPack() ? 1 : 0)
                + (unreadable ? " (polja Shaders neberljiva)" : "") + " RwFarShaders=" + setting
                + " izklopSlicic=" + offFrames + " sencnihPreskokov=" + shadowSkips;
    }
}
