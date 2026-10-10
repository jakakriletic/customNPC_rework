package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.ModelEyeData;
import org.junit.Test;

/**
 * Baseline razreda {@code ModelEyeData} ob prevzemu v {@code src/patch} (M5.13, S7).
 *
 * <p>Razred je prevzet zato, da lahko utrip oci posilja prek {@code rework/net}; ta test
 * dokumentira obnasanje izdaje 01Oct19, da ga naslednji commit ne spremeni po nesreci.
 * {@code update(npc)} rabi entiteto in svet, zato je merjen v scenariju, ne tukaj.
 */
public class ModelEyeDataBaselineTest {
    @Test
    public void nbtKroznaPot() {
        ModelEyeData eyes = new ModelEyeData();
        eyes.setType(3);
        eyes.color = 0x123456;
        eyes.glint = false;
        eyes.browThickness = 7;
        eyes.eyePos = 2;
        eyes.skinColor = 0x010203;
        eyes.browColor = 0x040506;
        NBTTagCompound saved = eyes.writeToNBT();

        ModelEyeData loaded = new ModelEyeData();
        loaded.readFromNBT(saved);
        assertEquals("eyes", loaded.name);
        assertEquals(3, loaded.type);
        assertEquals(0x123456, loaded.color);
        assertFalse(loaded.glint);
        assertEquals(7, loaded.browThickness);
        assertEquals(2, loaded.eyePos);
        assertEquals(0x010203, loaded.skinColor);
        assertEquals(0x040506, loaded.browColor);
    }

    @Test
    public void vklopljenDoklerTipNiNegativen() {
        ModelEyeData eyes = new ModelEyeData();
        assertTrue("privzeti tip 0 je vklopljen", eyes.isEnabled());
        eyes.setType(-1);
        assertFalse(eyes.isEnabled());
        eyes.setType(0);
        assertTrue(eyes.isEnabled());
    }

    @Test
    public void praznoNbtPustiTipNespremenjen() {
        // Original: readFromNBT ob praznem compoundu vrne takoj, zato tip NE pade na -1
        // (ModelPartData.readFromNBT brez kljuca "Type" ga postavi na -1). Znacilnost izdaje,
        // ne zelena lastnost.
        ModelEyeData eyes = new ModelEyeData();
        eyes.setType(5);
        eyes.readFromNBT(new NBTTagCompound());
        assertEquals(5, eyes.type);
        assertTrue(eyes.isEnabled());
    }

    @Test
    public void blinkStartJePrivzetoNic() {
        assertEquals(0L, new ModelEyeData().blinkStart);
    }
}
