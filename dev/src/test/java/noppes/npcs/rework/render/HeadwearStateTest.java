package noppes.npcs.rework.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import noppes.npcs.client.model.part.head.ModelHeadwear;
import org.junit.Test;

/** M5-K K14: list pokrivala velja natanko, dokler se spremljano stanje otrok ne spremeni. */
public class HeadwearStateTest {
    private static final float S = 0.0625f;

    private static ModelHeadwear headwear() {
        return new ModelHeadwear(new ModelBase() { });
    }

    @Test
    public void brezPosnetkaNeVelja() {
        HeadwearState st = new HeadwearState();
        assertFalse(st.matches(headwear().childModels, S));
    }

    @Test
    public void nespremenjenoStanjeVelja() {
        ModelHeadwear hw = headwear();
        HeadwearState st = new HeadwearState();
        st.capture(hw.childModels, S);
        for (int i = 0; i < 1000; i++) {
            assertTrue(st.matches(hw.childModels, S));
        }
        assertEquals(0, st.rebuilds());
    }

    @Test
    public void vsakaSpremljanaSpremembaRazveljavi() {
        String[] polja = {"showModel", "isHidden", "rpX", "rpY", "rpZ", "kotX", "kotY", "kotZ"};
        for (int otrok = 0; otrok < 6; otrok++) {
            for (String polje : polja) {
                ModelHeadwear hw = headwear();
                HeadwearState st = new HeadwearState();
                st.capture(hw.childModels, S);
                ModelRenderer c = hw.childModels.get(otrok);
                spremeni(c, polje);
                assertFalse(polje + " otroka " + otrok, st.matches(hw.childModels, S));
                st.capture(hw.childModels, S);
                assertTrue(st.matches(hw.childModels, S));
            }
        }
    }

    @Test
    public void meriloZaporedjeInSteviloOtrok() {
        ModelHeadwear hw = headwear();
        HeadwearState st = new HeadwearState();
        st.capture(hw.childModels, S);
        assertFalse("drugo merilo", st.matches(hw.childModels, S * 2f));

        List<ModelRenderer> zamenjana = new ArrayList<ModelRenderer>(hw.childModels);
        ModelRenderer prvi = zamenjana.get(0);
        zamenjana.set(0, zamenjana.get(1));
        zamenjana.set(1, prvi);
        assertFalse("drug vrstni red", st.matches(zamenjana, S));

        List<ModelRenderer> krajsa = new ArrayList<ModelRenderer>(hw.childModels.subList(0, 5));
        assertFalse("manj otrok", st.matches(krajsa, S));

        List<ModelRenderer> drug = new ArrayList<ModelRenderer>(headwear().childModels);
        assertFalse("isti podatki, drugi primerki", st.matches(drug, S));
    }

    @Test
    public void ponovnoNastavljenaIstaVrednostVelja() {
        ModelHeadwear hw = headwear();
        HeadwearState st = new HeadwearState();
        st.capture(hw.childModels, S);
        ModelRenderer c = hw.childModels.get(3);
        c.rotateAngleY = (float)Math.PI;
        c.showModel = true;
        assertTrue(st.matches(hw.childModels, S));
    }

    @Test
    public void prevezaveSoOmejene() {
        HeadwearState st = new HeadwearState();
        assertTrue("prva izdelava ni prevezava", st.allowRebuild());
        st.capture(headwear().childModels, S);
        for (int i = 0; i < HeadwearState.MAX_REBUILDS; i++) {
            assertTrue(st.allowRebuild());
        }
        assertFalse(st.allowRebuild());
        assertEquals(HeadwearState.MAX_REBUILDS + 1, st.rebuilds());
    }

    private static void spremeni(ModelRenderer c, String polje) {
        if ("showModel".equals(polje)) {
            c.showModel = !c.showModel;
        } else if ("isHidden".equals(polje)) {
            c.isHidden = !c.isHidden;
        } else if ("rpX".equals(polje)) {
            c.rotationPointX += 0.001f;
        } else if ("rpY".equals(polje)) {
            c.rotationPointY += 0.001f;
        } else if ("rpZ".equals(polje)) {
            c.rotationPointZ += 0.001f;
        } else if ("kotX".equals(polje)) {
            c.rotateAngleX += 0.001f;
        } else if ("kotY".equals(polje)) {
            c.rotateAngleY += 0.001f;
        } else {
            c.rotateAngleZ += 0.001f;
        }
    }
}
