package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.client.renderer.entity.RenderLiving;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.Test;

/**
 * Baseline {@code RenderNPCInterface} ob prevzemu v {@code src/patch} (M5-K K4). Razred je prenesen
 * 1:1 (bajtkoda 26 metod: 18 enakih, 8 razlik v obliki, 0 za pregled,
 * {@code audit/m5k4-bytecode-primerjava.txt}); izris rabi GL, zato test dokumentira samo vstopno
 * tocko, na kateri stoji K4: napis imena in naslova risu {@code renderLivingLabel}.
 */
public class RenderNPCInterfaceBaselineTest {
    @Test
    public void napisRiseRenderLivingLabel() throws Exception {
        assertEquals(RenderLiving.class, RenderNPCInterface.class.getSuperclass());
        Method m = RenderNPCInterface.class.getDeclaredMethod("renderLivingLabel", EntityNPCInterface.class,
                float.class, float.class, float.class, int.class, String.class, String.class);
        assertTrue(Modifier.isProtected(m.getModifiers()));
        assertEquals(void.class, m.getReturnType());
    }
}
