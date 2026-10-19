package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import noppes.npcs.ModelEyeData;
import noppes.npcs.client.layer.LayerEyes;
import noppes.npcs.client.layer.LayerInterface;
import org.junit.Test;

/**
 * Baseline {@code LayerEyes} ob prevzemu v {@code src/patch} (M5-K K3). Razred je prenesen 1:1
 * ({@code audit/m5k3-bytecode-primerjava.txt}); izris rabi GL in pravi {@code RenderLiving}, zato test
 * dokumentira zgradbo, na kateri stoji K3: oči so en sloj s tremi zasebnimi deli izrisa (levo,
 * desno, obrvi) in javnim {@code drawRect}, privzete oči pa so vklopljene, s sijajem in obrvmi.
 */
public class LayerEyesBaselineTest {
    @Test
    public void slojInDeliIzrisa() throws Exception {
        assertEquals(LayerInterface.class, LayerEyes.class.getSuperclass());
        for (String ime : new String[] {"drawLeft", "drawRight", "drawBrows"}) {
            Method m = LayerEyes.class.getDeclaredMethod(ime);
            assertTrue(ime + " je zaseben", Modifier.isPrivate(m.getModifiers()));
        }
        Method rect = LayerEyes.class.getDeclaredMethod("drawRect", double.class, double.class, double.class,
                double.class, int.class, double.class);
        assertTrue(Modifier.isPublic(rect.getModifiers()));
    }

    @Test
    public void privzeteOci() {
        ModelEyeData oci = new ModelEyeData();
        assertTrue(oci.glint);
        assertEquals(4, oci.browThickness);
        assertEquals(0L, oci.blinkStart);
    }
}
