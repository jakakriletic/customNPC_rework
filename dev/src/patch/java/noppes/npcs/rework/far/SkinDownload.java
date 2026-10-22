package noppes.npcs.rework.far;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.ImageDownloadAlt;

/**
 * M5-V.7 (D-050): ali tekstura skina se prenasa (URL skin CNPC prek {@link ImageDownloadAlt}, skin igralca prek
 * vanilla {@link ThreadDownloadImageData}).
 *
 * <p>{@code RenderNPCInterface.getEntityTexture} postavi {@code textureLocation} URL skina takoj, ko zacne prenos;
 * do konca prenosa tekstura kaze privzeti skin. Peka, ki caka samo na {@code textureLocation}, bi zato trajno
 * spekla privzeti skin. Prenos tece, dokler nit prenosa zivi in slike se ni; neuspel prenos (nit koncana brez
 * slike) ni "v teku" - renderer ostane pri privzetem skinu in impostor ga sme speci, kot ga rise model.
 * Obe polji sta zasebni, zato refleksija; ce je ni mogoce brati, prenos ni v teku (vedenje pred M5-V.7).
 */
@SideOnly(Side.CLIENT)
public final class SkinDownload {
    private static final Map<Class<?>, Field[]> FIELDS = new HashMap<Class<?>, Field[]>();

    private SkinDownload() {
    }

    /** {@code true}, ce se tekstura se prenasa (slike se ni, nit prenosa zivi). */
    public static boolean pending(ITextureObject tex) {
        if (tex == null) {
            return false;
        }
        Field[] f = fields(tex.getClass());
        if (f == null) {
            return false;
        }
        try {
            Object image = f[0].get(tex);
            Object thread = f[1].get(tex);
            return image == null && thread instanceof Thread && ((Thread) thread).isAlive();
        } catch (IllegalAccessException | RuntimeException e) {
            FIELDS.put(tex.getClass(), null);
            return false;
        }
    }

    /** {bufferedImage, imageThread} za razred teksture ali {@code null}, ce ni prenos. */
    private static Field[] fields(Class<?> c) {
        if (FIELDS.containsKey(c)) {
            return FIELDS.get(c);
        }
        Field[] f = null;
        try {
            if (ImageDownloadAlt.class.isAssignableFrom(c)) {
                // Razred originala CNPC (ni obfuskiran): imena polj so ista v razvoju in produkciji.
                f = new Field[] {ImageDownloadAlt.class.getDeclaredField("bufferedImage"),
                        ImageDownloadAlt.class.getDeclaredField("imageThread")};
                f[0].setAccessible(true);
                f[1].setAccessible(true);
            } else if (ThreadDownloadImageData.class.isAssignableFrom(c)) {
                f = new Field[] {ReflectionHelper.findField(ThreadDownloadImageData.class, "bufferedImage", "field_110560_d"),
                        ReflectionHelper.findField(ThreadDownloadImageData.class, "imageThread", "field_110561_e")};
            }
        } catch (NoSuchFieldException | RuntimeException e) {
            f = null;
        }
        FIELDS.put(c, f);
        return f;
    }
}
