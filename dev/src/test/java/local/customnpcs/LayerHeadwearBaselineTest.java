package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import noppes.npcs.client.layer.LayerHeadwear;
import noppes.npcs.client.layer.LayerInterface;
import noppes.npcs.client.layer.LayerPreRender;
import noppes.npcs.client.model.part.head.ModelHeadwear;
import org.junit.Test;

/**
 * Baseline {@code LayerHeadwear} ob prevzemu v {@code src/patch} (M5-K K14b). Razred je prenesen
 * 1:1 (bajtkoda 5/5 metod enakih, {@code audit/m5k14b-bytecode-primerjava.txt}); izris rabi GL in
 * pravi {@code RenderLiving}, zato test dokumentira samo zgradbo, na kateri stoji K14b: sloj ima
 * en zasebni primerek pokrivala in tekstura je tekstura NPC-ja ({@code LayerInterface.npc}).
 */
public class LayerHeadwearBaselineTest {
    @Test
    public void slojImaEnoZasebnoPokrivalo() throws Exception {
        assertEquals(LayerInterface.class, LayerHeadwear.class.getSuperclass());
        assertTrue(LayerPreRender.class.isAssignableFrom(LayerHeadwear.class));
        int pokrival = 0;
        for (Field f : LayerHeadwear.class.getDeclaredFields()) {
            if (f.getType() == ModelHeadwear.class) {
                pokrival++;
                assertTrue("pokrivalo je zasebno, nihce drug ga ne spreminja", Modifier.isPrivate(f.getModifiers()));
            }
        }
        assertEquals(1, pokrival);
        assertEquals("tekstura sloja je tekstura NPC-ja",
                "noppes.npcs.entity.EntityCustomNpc", LayerInterface.class.getDeclaredField("npc").getType().getName());
    }
}
