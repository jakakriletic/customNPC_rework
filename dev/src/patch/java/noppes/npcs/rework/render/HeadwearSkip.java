package noppes.npcs.rework.render;

import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.model.Model2DRenderer;
import noppes.npcs.client.model.ModelScaleRenderer;

/**
 * M5-K K14b: pokrivalo ({@code ModelHeadwear}, {@code HeadWearType=1}) brez stirikotnikov, ki
 * vzorcijo samo teksle z alfo 0.
 *
 * <p>Original za vsakega od 6 delov nariše 34 stirikotnikov (816 oglisc na NPC, 19-26 % render
 * niti v M5-K.1), tudi ko je obmocje klobuka v skinu prozorno (41 % skinov CNPC, tudi skin
 * testnega sveta) in alfa test zavrze vse fragmente. Za vsako teksturo se enkrat prebere slika
 * ({@link HeadwearMask}, brez tekslov tocno na robu obsega) in za vsak del doloci: nic (del se ne narise), vse (original list dela)
 * ali nekateri (lasten list z izbranimi stirikotniki). Izpusceni stirikotniki v originalu ne pustijo
 * fragmenta, razen fragmenta tocno na robni crti, ki lahko vzame sosednji (neprozoren) teksel: ta
 * izjema ni dokazano enaka (poskus z robom, ki jo pokrije, ni dal dobitka nad sumom, M5-K K14b), v
 * preverbi pikslov 7 serij × 200 NPC-jev 0 razlik. Zato privzeto 0 in vprasanje Q30.
 *
 * <p>Original se uporabi ({@link #render} vrne false), kadar ta pogoj ni dokazan: OptiFine
 * (nakljucne teksture in senčilniki lahko zamenjajo vezano teksturo), tekstura ni natanko
 * {@code SimpleTexture} (URL in skini igralcev), vir ima metapodatke (blur/clamp), slika se ne da
 * prebrati, otrok ni {@code Model2DRenderer}, ali alfa test po {@code GlStateManager} ni vklopljen z
 * {@code GL_GREATER}, ref ≥ 0 (ali {@code GL_GEQUAL}, ref &gt; 0). Ob ponovnem nalaganju virov
 * (F3+T, paketi virov) se predpomnilnik pobrise.
 */
@SideOnly(Side.CLIENT)
public final class HeadwearSkip {
    /** Statistika za harness (render nit). */
    public static long draws;
    public static long fallbacks;
    public static long stateFallbacks;
    public static long analyses;
    public static long partsNone;
    public static long partsAll;
    public static long partsSome;

    private static final int MAX_TEXTURES = 256;
    private static final int GL_GREATER = 516;
    private static final int GL_GEQUAL = 518;

    private static final MethodHandle ALPHA_STATE;
    private static final MethodHandle ALPHA_TEST;
    private static final MethodHandle ALPHA_FUNC;
    private static final MethodHandle ALPHA_REF;
    private static final MethodHandle BOOL_CURRENT;
    private static final MethodHandle TEXTURE_SOURCE;
    private static final boolean AVAILABLE;

    static {
        MethodHandle as = null, at = null, af = null, ar = null, bc = null, ts = null;
        boolean ok = false;
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            Field fState = ReflectionHelper.findField(GlStateManager.class, "alphaState", "field_179160_a");
            as = l.unreflectGetter(fState);
            Class<?> alpha = fState.getType();
            Field fTest = ReflectionHelper.findField(alpha, "alphaTest", "field_179208_a");
            at = l.unreflectGetter(fTest);
            af = l.unreflectGetter(ReflectionHelper.findField(alpha, "func", "field_179206_b"));
            ar = l.unreflectGetter(ReflectionHelper.findField(alpha, "ref", "field_179207_c"));
            bc = l.unreflectGetter(ReflectionHelper.findField(fTest.getType(), "currentState", "field_179201_b"));
            ts = l.unreflectGetter(ReflectionHelper.findField(SimpleTexture.class, "textureLocation", "field_110568_b"));
            ok = true;
        } catch (Throwable e) {
            System.out.println("RwHeadwearSkip: refleksija ni uspela, uporabljen bo original: " + e);
        }
        ALPHA_STATE = as;
        ALPHA_TEST = at;
        ALPHA_FUNC = af;
        ALPHA_REF = ar;
        BOOL_CURRENT = bc;
        TEXTURE_SOURCE = ts;
        AVAILABLE = ok;
    }

    /** Odlocitev za en par (pokrivalo, tekstura). */
    private static final class Entry {
        final boolean fallback;
        /** po otroku: 0 nic, 1 nekateri, 2 vsi */
        final int[] kinds;
        final boolean[][] keep;
        final int[] lists;
        float listScale = Float.NaN;

        Entry(int[] kinds, boolean[][] keep) {
            this.fallback = kinds == null;
            this.kinds = kinds;
            this.keep = keep;
            this.lists = kinds == null ? null : new int[kinds.length];
            if (this.lists != null) {
                java.util.Arrays.fill(this.lists, -1);
            }
        }

        void deleteLists() {
            if (this.lists == null) {
                return;
            }
            for (int i = 0; i < this.lists.length; i++) {
                if (this.lists[i] >= 0) {
                    GLAllocation.deleteDisplayLists(this.lists[i]);
                    this.lists[i] = -1;
                }
            }
        }
    }

    private static final Entry FALLBACK = new Entry(null, null);

    /** Predpomnilnik enega pokrivala (vsak renderer ima svoj primerek). */
    private static final class PerHeadwear {
        ITextureObject lastTexture;
        Entry lastEntry;
        final LinkedHashMap<ITextureObject, Entry> map = new LinkedHashMap<ITextureObject, Entry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ITextureObject, Entry> e) {
                if (size() > MAX_TEXTURES) {
                    e.getValue().deleteLists();
                    return true;
                }
                return false;
            }
        };
    }

    private static final Map<ModelScaleRenderer, PerHeadwear> CACHE = new IdentityHashMap<ModelScaleRenderer, PerHeadwear>();
    /** Harness: alfa testnih tekstur, ki niso SimpleTexture (DynamicTexture v preverbi). */
    private static final Map<ITextureObject, int[]> TEST_IMAGES = new IdentityHashMap<ITextureObject, int[]>();
    private static boolean listenerRegistered;
    private static Boolean optifine;

    private HeadwearSkip() {
    }

    /**
     * Nariše pokrivalo {@code hw} s teksturo {@code loc}, kot {@code ModelScaleRenderer.render}.
     * @return false, ce mora klicatelj narisati original (nic ni bilo narisano)
     */
    public static boolean render(ModelScaleRenderer hw, ResourceLocation loc, float scale) {
        if (!AVAILABLE || loc == null || optifine()) {
            fallbacks++;
            return false;
        }
        if (!alphaDiscardsZero()) {
            stateFallbacks++;
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ITextureObject t = mc.getTextureManager().getTexture(loc);
        if (t == null) {
            fallbacks++;
            return false;
        }
        PerHeadwear p = CACHE.get(hw);
        if (p == null) {
            registerListener(mc);
            p = new PerHeadwear();
            CACHE.put(hw, p);
        }
        Entry e;
        if (t == p.lastTexture) {
            e = p.lastEntry;
        } else {
            e = p.map.get(t);
            if (e == null) {
                e = analyze(hw, t);
                p.map.put(t, e);
            }
            p.lastTexture = t;
            p.lastEntry = e;
        }
        if (e.fallback) {
            fallbacks++;
            return false;
        }
        draws++;
        // ModelScaleRenderer.render
        if (!hw.showModel || hw.isHidden) {
            return true;
        }
        if (!hw.isCompiled) {
            hw.compile(scale);
        }
        if (Float.floatToIntBits(scale) != Float.floatToIntBits(e.listScale)) {
            e.deleteLists();
            e.listScale = scale;
        }
        List<ModelRenderer> children = hw.childModels;
        GlStateManager.pushMatrix();
        hw.postRender(scale);
        GlStateManager.callList(hw.displayList);
        for (int i = 0; i < children.size(); i++) {
            Model2DRenderer c = (Model2DRenderer)children.get(i);
            int kind = e.kinds[i];
            if (kind == 2) {
                c.render(scale);
                partsAll++;
            } else if (kind == 1) {
                if (e.lists[i] < 0) {
                    e.lists[i] = c.rwCompileFiltered(scale, e.keep[i]);
                }
                c.rwRenderList(scale, e.lists[i]);
                partsSome++;
            } else {
                partsNone++;
            }
        }
        GlStateManager.popMatrix();
        return true;
    }

    private static Entry analyze(ModelScaleRenderer hw, ITextureObject t) {
        analyses++;
        List<ModelRenderer> children = hw.childModels;
        if (children == null || children.isEmpty()) {
            return FALLBACK;
        }
        for (ModelRenderer c : children) {
            if (!(c instanceof Model2DRenderer)) {
                return FALLBACK;
            }
        }
        int[] argb;
        int w;
        int h;
        int[] test = TEST_IMAGES.get(t);
        try {
            if (test != null) {
                w = test[0];
                h = test[1];
                argb = new int[w * h];
                System.arraycopy(test, 2, argb, 0, w * h);
            } else if (t.getClass() == SimpleTexture.class) {
                ResourceLocation src = (ResourceLocation)TEXTURE_SOURCE.invoke((SimpleTexture)t);
                IResource r = Minecraft.getMinecraft().getResourceManager().getResource(src);
                try {
                    if (r.hasMetadata()) {
                        return FALLBACK;
                    }
                    BufferedImage img = TextureUtil.readBufferedImage(r.getInputStream());
                    w = img.getWidth();
                    h = img.getHeight();
                    argb = img.getRGB(0, 0, w, h, null, 0, w);
                } finally {
                    r.close();
                }
            } else {
                return FALLBACK;
            }
        } catch (Throwable ex) {
            return FALLBACK;
        }
        int n = children.size();
        int[] kinds = new int[n];
        boolean[][] keep = new boolean[n][];
        for (int i = 0; i < n; i++) {
            Model2DRenderer c = (Model2DRenderer)children.get(i);
            keep[i] = HeadwearMask.keep(argb, w, h,
                    HeadwearQuads.uvBounds(c.rwU1(), c.rwV1(), c.rwU2(), c.rwV2(), c.rwWidth(), c.rwHeight()), true);
            kinds[i] = HeadwearMask.kind(keep[i]);
        }
        return new Entry(kinds, keep);
    }

    /** Ali alfa test (po stanju GlStateManager) zavrze vsak fragment z alfo 0. */
    static boolean alphaDiscardsZero() {
        try {
            Object state = ALPHA_STATE.invoke();
            if (!(boolean)BOOL_CURRENT.invoke(ALPHA_TEST.invoke(state))) {
                return false;
            }
            int func = (int)ALPHA_FUNC.invoke(state);
            float ref = (float)ALPHA_REF.invoke(state);
            return (func == GL_GREATER && ref >= 0.0f) || (func == GL_GEQUAL && ref > 0.0f);
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean optifine() {
        if (optifine == null) {
            optifine = FMLClientHandler.instance().hasOptifine();
        }
        return optifine;
    }

    private static void registerListener(Minecraft mc) {
        if (listenerRegistered) {
            return;
        }
        listenerRegistered = true;
        IResourceManager rm = mc.getResourceManager();
        if (rm instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager)rm).registerReloadListener(new IResourceManagerReloadListener() {
                @Override
                public void onResourceManagerReload(IResourceManager m) {
                    clear();
                }
            });
        }
    }

    /** Pobrise vse odlocitve in liste (ponovno nalaganje virov, harness). */
    public static void clear() {
        for (PerHeadwear p : CACHE.values()) {
            for (Entry e : p.map.values()) {
                e.deleteLists();
            }
            p.map.clear();
            p.lastTexture = null;
            p.lastEntry = null;
        }
    }

    /** Harness: tekstura {@code t} (npr. DynamicTexture) ima to sliko ARGB. */
    public static void setTestImage(ITextureObject t, int[] argb, int w, int h) {
        int[] d = new int[2 + w * h];
        d[0] = w;
        d[1] = h;
        System.arraycopy(argb, 0, d, 2, w * h);
        TEST_IMAGES.put(t, d);
        clear();
    }

    public static void clearTestImages() {
        TEST_IMAGES.clear();
        clear();
    }
}
