package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import noppes.npcs.client.model.Model2DRenderer;
import noppes.npcs.client.model.part.head.ModelHeadwear;
import noppes.npcs.constants.EnumParts;
import org.junit.Test;

/**
 * Baseline razredov {@code ModelHeadwear} in {@code Model2DRenderer} ob prevzemu v {@code src/patch}
 * (M5-K K14, pokrivalo v enem display listu).
 *
 * <p>Test dokumentira zgradbo pokrivala izdaje 01Oct19: 6 otrok {@code Model2DRenderer} s
 * konstantnimi tockami vrtenja, koti, merili in debelino. Na teh vrednostih stoji K14 (zapece jih
 * v en list), zato jih naslednji commit ne sme spremeniti po nesreci. Izris rabi GL in je preverjen
 * v klientu, ne tukaj.
 */
public class ModelHeadwearBaselineTest {
    private static final float HALF_PI = 1.5707964f;

    // tocka vrtenja x, y, z, kot x, y, z, merilo, uv x1, y1, x2, y2 (tekstura 64 x 32)
    private static final float[][] OTROCI = {
        {-4.641f, 0.8f, 4.64f, 0f, HALF_PI, 0f, 0.58f, 32f, 8f},
        {4.639f, 0.8f, -4.64f, 0f, -HALF_PI, 0f, 0.58f, 48f, 8f},
        {-4.64f, 0.801f, -4.641f, 0f, 0f, 0f, 0.58f, 40f, 8f},
        {4.64f, 0.801f, 4.639f, 0f, (float)Math.PI, 0f, 0.58f, 56f, 8f},
        {-4.64f, -8.5f, -4.64f, -HALF_PI, 0f, 0f, 0.5799f, 40f, 0f},
        {-4.64f, 0f, -4.64f, -HALF_PI, 0f, 0f, 0.5799f, 48f, 0f},
    };

    @Test
    public void sestOtrokSKonstantnoGeometrijo() throws Exception {
        ModelBase base = new ModelBase() { };
        ModelHeadwear hw = new ModelHeadwear(base);

        assertEquals(EnumParts.HEAD, hw.part);
        assertNull(hw.config);
        assertFalse(hw.isCompiled);
        assertTrue(hw.cubeList.isEmpty());
        assertEquals(0f, hw.rotationPointX, 0f);
        assertEquals(0f, hw.rotationPointY, 0f);
        assertEquals(0f, hw.rotationPointZ, 0f);
        assertEquals(0f, hw.rotateAngleX, 0f);
        assertEquals(0f, hw.rotateAngleY, 0f);
        assertEquals(0f, hw.rotateAngleZ, 0f);
        assertEquals(6, hw.childModels.size());
        // ModelRenderer(ModelBase) se doda v base.boxList: starš in 6 otrok
        assertEquals(7, base.boxList.size());

        for (int i = 0; i < OTROCI.length; i++) {
            float[] o = OTROCI[i];
            ModelRenderer c = hw.childModels.get(i);
            assertEquals(Model2DRenderer.class, c.getClass());
            String m = "otrok " + i;
            assertEquals(m, o[0], c.rotationPointX, 0f);
            assertEquals(m, o[1], c.rotationPointY, 0f);
            assertEquals(m, o[2], c.rotationPointZ, 0f);
            assertEquals(m, o[3], c.rotateAngleX, 0f);
            assertEquals(m, o[4], c.rotateAngleY, 0f);
            assertEquals(m, o[5], c.rotateAngleZ, 0f);
            assertTrue(m, c.showModel);
            assertFalse(m, c.isHidden);
            assertFalse(m, c.mirror);
            assertTrue(m, c.cubeList.isEmpty());
            assertNull(m, c.childModels);
            assertEquals(m, 0f, polje(c, "rotationOffsetX"), 0f);
            assertEquals(m, 0f, polje(c, "rotationOffsetY"), 0f);
            assertEquals(m, 0f, polje(c, "rotationOffsetZ"), 0f);
            assertEquals(m, o[6], polje(c, "scaleX"), 0f);
            assertEquals(m, o[6], polje(c, "scaleY"), 0f);
            assertEquals(m, 0.65f, polje(c, "thickness"), 0f);
            assertEquals(m, o[7] / 64f, polje(c, "x1"), 0f);
            assertEquals(m, o[8] / 32f, polje(c, "y1"), 0f);
            assertEquals(m, (o[7] + 8f) / 64f, polje(c, "x2"), 0f);
            assertEquals(m, (o[8] + 8f) / 32f, polje(c, "y2"), 0f);
            assertEquals(m, 8, ((Number)surovo(c, "width")).intValue());
            assertEquals(m, 8, ((Number)surovo(c, "height")).intValue());
            assertEquals(m, Boolean.FALSE, surovo(c, "isCompiled"));
        }
    }

    private static float polje(Object o, String ime) throws Exception {
        return ((Number)surovo(o, ime)).floatValue();
    }

    private static Object surovo(Object o, String ime) throws Exception {
        Field f = Model2DRenderer.class.getDeclaredField(ime);
        f.setAccessible(true);
        return f.get(o);
    }
}
