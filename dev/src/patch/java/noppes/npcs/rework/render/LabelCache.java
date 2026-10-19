package noppes.npcs.rework.render;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * M5-K K4: napis NPC-ja (ime, naslov) iz display lista namesto znak za znakom.
 *
 * <p>{@code FontRenderer.drawString} nariše vsak znak v takojsnjem nacinu ({@code glBegin},
 * 4 × tekstura + oglisce, {@code glEnd}); profil M5-K.1: ~8 us na napis, 20 % render niti pri
 * 200 NPC-jih v dometu imen. Za isti niz, barvo in pisavo so ukazi vedno isti (polozaj
 * {@code x = -sirina/2} je v ogliscih), zato se ob drugem izrisu niza posnamejo v list
 * ({@code GL_COMPILE_AND_EXECUTE}) in nato predvajajo z enim klicem. Display list shrani vrednosti
 * ukazov, zato gonilnik dobi iste ukaze z istimi vrednostmi.
 *
 * <p>{@code GlStateManager} ukaze, ki ne spremenijo njegovega predpomnjenega stanja, izpusti.
 * Pred snemanjem se zato predpomnilnik alfe, barve in teksture izenaci z dejanskim stanjem tako,
 * da se vsi trije ukazi niza res posnamejo; po predvajanju se predpomnilnik uskladi s koncnim
 * stanjem lista (alfa vklopljena, barva neznana, vezana zadnja tekstura niza).
 *
 * <p>Original se uporabi za: prvi izris niza (nalozi teksture znakov; {@code glTexImage2D} ne sme v
 * list), podrazrede {@code FontRenderer} (modi pisav), OptiFine, nize s {@code §k} (nakljucni
 * znaki), {@code §n} in {@code §m} (crta brez teksture). Ob ponovnem nalaganju virov se vsi listi
 * pobrisejo.
 */
@SideOnly(Side.CLIENT)
public final class LabelCache {
    /** Statistika za harness (render nit). */
    public static long drawsList;
    public static long drawsOriginal;
    public static long compiles;

    private static final int MAX = 1024;
    private static final MethodHandle TEXTURE_STATE;
    private static final MethodHandle ACTIVE_UNIT;
    private static final MethodHandle TEXTURE_NAME;
    private static final boolean AVAILABLE;

    static {
        MethodHandle ts = null, au = null, tn = null;
        boolean ok = false;
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            Field fState = ReflectionHelper.findField(GlStateManager.class, "textureState", "field_179174_p");
            ts = l.unreflectGetter(fState);
            au = l.unreflectGetter(ReflectionHelper.findField(GlStateManager.class, "activeTextureUnit", "field_179162_o"));
            tn = l.unreflectGetter(ReflectionHelper.findField(fState.getType().getComponentType(), "textureName", "field_179059_b"));
            ok = true;
        } catch (Throwable e) {
            System.out.println("RwLabelCache: refleksija ni uspela, uporabljen bo original: " + e);
        }
        TEXTURE_STATE = ts;
        ACTIVE_UNIT = au;
        TEXTURE_NAME = tn;
        AVAILABLE = ok;
    }

    private static final class Key {
        final String text;
        final int color;
        final boolean unicode;
        final boolean bidi;
        final int hash;

        Key(String text, int color, boolean unicode, boolean bidi) {
            this.text = text;
            this.color = color;
            this.unicode = unicode;
            this.bidi = bidi;
            this.hash = (text.hashCode() * 31 + color) * 4 + (unicode ? 2 : 0) + (bidi ? 1 : 0);
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
            return this.hash == k.hash && this.color == k.color && this.unicode == k.unicode && this.bidi == k.bidi
                    && this.text.equals(k.text);
        }
    }

    private static final class Entry {
        FontRenderer font;
        int list = -1;
        int x;
        int lastTexture;
    }

    private static final LinkedHashMap<Key, Entry> CACHE = new LinkedHashMap<Key, Entry>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Entry> e) {
            if (size() > MAX) {
                delete(e.getValue());
                return true;
            }
            return false;
        }
    };
    private static boolean listenerRegistered;
    private static Boolean optifine;

    private LabelCache() {
    }

    /** Kot {@code fr.drawString(text, -fr.getStringWidth(text) / 2, 0, color)}. */
    public static void drawCentered(FontRenderer fr, String text, int color) {
        if (!usable(fr, text)) {
            drawsOriginal++;
            fr.drawString(text, -fr.getStringWidth(text) / 2, 0, color);
            return;
        }
        Key k = new Key(text, color, fr.getUnicodeFlag(), fr.getBidiFlag());
        Entry e = CACHE.get(k);
        if (e == null || e.font != fr) {
            if (e != null) {
                delete(e);
            }
            registerListener();
            e = new Entry();
            e.font = fr;
            e.x = -fr.getStringWidth(text) / 2;
            CACHE.put(k, e);
            // Prvi izris: original (nalozi teksture znakov zunaj lista).
            drawsOriginal++;
            fr.drawString(text, e.x, 0, color);
            return;
        }
        if (e.list < 0) {
            // Izenaci predpomnilnik GlStateManager, da se vsi trije ukazi stanja posnamejo.
            GlStateManager.disableAlpha();
            GlStateManager.resetColor();
            GlStateManager.bindTexture(0);
            e.list = GLAllocation.generateDisplayLists(1);
            GlStateManager.glNewList(e.list, GL11.GL_COMPILE_AND_EXECUTE);
            fr.drawString(text, e.x, 0, color);
            GlStateManager.glEndList();
            e.lastTexture = boundTexture();
            compiles++;
            if (e.lastTexture < 0) {
                delete(e);
                CACHE.remove(k);
            }
            return;
        }
        GlStateManager.callList(e.list);
        // Uskladi predpomnilnik s koncnim stanjem lista.
        GlStateManager.enableAlpha();
        GlStateManager.resetColor();
        GlStateManager.bindTexture(e.lastTexture);
        drawsList++;
    }

    static boolean usable(FontRenderer fr, String text) {
        return AVAILABLE && fr.getClass() == FontRenderer.class && !optifine() && cacheableText(text);
    }

    /** Niz brez {@code §k} (nakljucni znaki), {@code §n} in {@code §m} (crta brez teksture). */
    public static boolean cacheableText(String text) {
        if (text == null) {
            return false;
        }
        for (int i = text.indexOf('§'); i >= 0 && i + 1 < text.length(); i = text.indexOf('§', i + 1)) {
            char c = Character.toLowerCase(text.charAt(i + 1));
            if (c == 'k' || c == 'n' || c == 'm') {
                return false;
            }
        }
        return true;
    }

    private static int boundTexture() {
        try {
            Object states = TEXTURE_STATE.invoke();
            int unit = (int)ACTIVE_UNIT.invoke();
            return (int)TEXTURE_NAME.invoke(Array.get(states, unit));
        } catch (Throwable e) {
            return -1;
        }
    }

    private static void delete(Entry e) {
        if (e.list >= 0) {
            GLAllocation.deleteDisplayLists(e.list);
            e.list = -1;
        }
    }

    private static boolean optifine() {
        if (optifine == null) {
            optifine = FMLClientHandler.instance().hasOptifine();
        }
        return optifine;
    }

    private static void registerListener() {
        if (listenerRegistered) {
            return;
        }
        listenerRegistered = true;
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if (rm instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager)rm).registerReloadListener(new IResourceManagerReloadListener() {
                @Override
                public void onResourceManagerReload(IResourceManager m) {
                    clear();
                }
            });
        }
    }

    /** Pobrise vse liste (ponovno nalaganje virov, harness). */
    public static void clear() {
        for (Entry e : CACHE.values()) {
            delete(e);
        }
        CACHE.clear();
    }
}
