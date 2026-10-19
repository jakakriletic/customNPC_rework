package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import noppes.npcs.ModelEyeData;
import noppes.npcs.rework.render.EyesList;
import org.junit.Test;

/**
 * M5-K K3: kljuc display lista oci zajame vse, od cesar so v {@code LayerEyes} odvisna oglisca
 * zunaj utripa (tip, vzorec, barva, sijaj pri zivem NPC-ju, debelina in barva obrvi), in nic drugega.
 */
public class EyesListTest {
    private static ModelEyeData oci() {
        ModelEyeData e = new ModelEyeData();
        e.color = 0x4040FF;
        e.type = 0;
        e.pattern = 0;
        return e;
    }

    @Test
    public void vsakParameterGeometrijeSpremeniKljuc() {
        Object k = EyesList.key(oci(), true);
        assertEquals(k, EyesList.key(oci(), true));
        assertEquals(k.hashCode(), EyesList.key(oci(), true).hashCode());

        ModelEyeData e = oci();
        e.type = 1;
        assertNotEquals(k, EyesList.key(e, true));
        e = oci();
        e.pattern = 2;
        assertNotEquals(k, EyesList.key(e, true));
        e = oci();
        e.color = 0x4040FE;
        assertNotEquals(k, EyesList.key(e, true));
        e = oci();
        e.glint = false;
        assertNotEquals(k, EyesList.key(e, true));
        e = oci();
        e.browThickness = 0;
        assertNotEquals(k, EyesList.key(e, true));
        e = oci();
        e.browColor = 0;
        assertNotEquals(k, EyesList.key(e, true));
    }

    @Test
    public void sijajMrtvegaNpcJaJeKotBrezSijaja() {
        ModelEyeData brez = oci();
        brez.glint = false;
        assertEquals(EyesList.key(brez, true), EyesList.key(oci(), false));
    }

    @Test
    public void polozajBarvaKozeInStanjeUtripaNeVplivajoNaOglisca() {
        Object k = EyesList.key(oci(), true);
        ModelEyeData e = oci();
        e.eyePos = 3;
        e.skinColor = 0x123456;
        e.blinkStart = -15L;
        e.colorPattern = 0;
        assertEquals(k, EyesList.key(e, true));
    }

    @Test
    public void utripGrePoOriginalu() {
        ModelEyeData e = oci();
        assertFalse(EyesList.blinks(e, true, 0));
        e.blinkStart = System.currentTimeMillis();
        assertTrue(EyesList.blinks(e, true, 0));
        assertFalse("mrtev NPC ne utripa", EyesList.blinks(e, false, 0));
        assertFalse("umirajoc NPC ne utripa", EyesList.blinks(e, true, 3));
        assertFalse("odstevanje do utripa", EyesList.blinks(oci(), true, 0));
        e.blinkStart = -20L;
        assertFalse(EyesList.blinks(e, true, 0));
    }
}
