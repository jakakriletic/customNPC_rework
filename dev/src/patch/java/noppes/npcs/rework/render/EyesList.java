package noppes.npcs.rework.render;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.ModelEyeData;
import org.lwjgl.opengl.GL11;

/**
 * M5-K K3: geometrija oci NPC-ja iz display lista namesto vsakic znova iz {@code Tessellator}.
 *
 * <p>{@code LayerEyes} vsako slicico napolni {@code Tessellator} z 8-12 pravokotniki in ga narise z
 * odjemalskimi polji ({@code glVertexPointer}, {@code glColorPointer}, {@code glDrawArrays}); profil
 * M5-K.1: 80 % casa oci je v tem izrisu (gonilnik kopira polja ob vsakem klicu). Oglisca so odvisna
 * samo od tipa, vzorca, barve oci, sijaja (vklopljen in NPC zivi), debeline in barve obrvi, zato
 * se isti izris ({@code Tessellator.draw}) ob prvem srecanju kljuca posname v list in nato predvaja.
 * {@code glDrawArrays} se v list posname z razresenimi vrednostmi polj (tako vanilla prevaja chunke
 * brez VBO), zato gonilnik dobi ista oglisca in barve.
 *
 * <p>Okoli izrisa je v listu {@code glPushAttrib(GL_CURRENT_BIT)}/{@code glPopAttrib}: predvajan list
 * bi sicer trenutno barvo pustil na barvi zadnjega oglisca, izris s polji pa je ne spremeni.
 * {@code GlStateManager} se uskladi kot v originalu: pred izrisom {@code color(1,1,1,1)} (klic iz
 * {@code drawRect}), po izrisu {@code resetColor()} (klic iz {@code WorldVertexBufferUploader}).
 *
 * <p>Original ostane za: utrip (obrvi se premikajo s casom), OptiFine (spremeni pot izrisa).
 * LRU {@value #MAX} kljucev.
 */
@SideOnly(Side.CLIENT)
public final class EyesList {
    /** Statistika za harness (render nit). */
    public static long drawsList;
    public static long drawsOriginal;
    public static long compiles;

    static final int MAX = 256;

    /** Vse, od cesar so odvisna oglisca oci zunaj utripa. */
    static final class Key {
        final int type;
        final int pattern;
        final int color;
        final boolean glint;
        final int browThickness;
        final int browColor;
        final int hash;

        Key(int type, int pattern, int color, boolean glint, int browThickness, int browColor) {
            this.type = type;
            this.pattern = pattern;
            this.color = color;
            this.glint = glint;
            this.browThickness = browThickness;
            this.browColor = browColor;
            int h = color;
            h = h * 31 + browColor;
            h = h * 31 + browThickness;
            h = h * 31 + type;
            h = h * 31 + pattern;
            this.hash = h * 2 + (glint ? 1 : 0);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) {
                return false;
            }
            Key k = (Key)o;
            return this.hash == k.hash && this.color == k.color && this.browColor == k.browColor
                    && this.browThickness == k.browThickness && this.type == k.type && this.pattern == k.pattern
                    && this.glint == k.glint;
        }
    }

    private static final LinkedHashMap<Key, Integer> CACHE = new LinkedHashMap<Key, Integer>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Integer> e) {
            if (size() > MAX) {
                GLAllocation.deleteDisplayLists(e.getValue());
                return true;
            }
            return false;
        }
    };
    private static Boolean optifine;

    private EyesList() {
    }

    /**
     * Ali sme izris iti iz lista. Utrip ({@code drawBrows}: {@code blinkStart > 0}, NPC zivi in ne
     * umira) premika obrvi s casom, zato gre po originalu.
     */
    public static boolean usable(ModelEyeData eyes, boolean alive, int deathTime) {
        return !blinks(eyes, alive, deathTime) && !optifine();
    }

    /** Pogoj utripa iz {@code LayerEyes.drawBrows}. */
    public static boolean blinks(ModelEyeData eyes, boolean alive, int deathTime) {
        return eyes.blinkStart > 0L && alive && deathTime == 0;
    }

    /** Kljuc geometrije oci zunaj utripa (enaka kljuca = enaka oglisca). */
    public static Object key(ModelEyeData eyes, boolean alive) {
        return new Key(eyes.type, eyes.pattern, eyes.color, eyes.glint && alive, eyes.browThickness, eyes.browColor);
    }

    /**
     * Narise oci iz lista; ob prvem srecanju kljuca {@code geometry} (polnjenje {@code Tessellator}
     * in {@code Tessellator.draw()}, kot v originalu) posname v list.
     */
    public static void draw(ModelEyeData eyes, boolean alive, Runnable geometry) {
        // drawRect: GlStateManager.color(1,1,1,1) pred izrisom.
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        Key k = (Key)key(eyes, alive);
        Integer list = CACHE.get(k);
        if (list == null) {
            int id = GLAllocation.generateDisplayLists(1);
            GlStateManager.glNewList(id, GL11.GL_COMPILE);
            GL11.glPushAttrib(GL11.GL_CURRENT_BIT);
            geometry.run();
            GL11.glPopAttrib();
            GlStateManager.glEndList();
            compiles++;
            list = id;
            CACHE.put(k, list);
        }
        GlStateManager.callList(list);
        // WorldVertexBufferUploader po izrisu z barvami: GlStateManager.resetColor().
        GlStateManager.resetColor();
        drawsList++;
    }

    private static boolean optifine() {
        if (optifine == null) {
            optifine = FMLClientHandler.instance().hasOptifine();
        }
        return optifine;
    }

    /** Pobrise vse liste (harness). */
    public static void clear() {
        for (Integer id : CACHE.values()) {
            GLAllocation.deleteDisplayLists(id);
        }
        CACHE.clear();
    }
}
