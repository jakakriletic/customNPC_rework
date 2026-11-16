package local.customnpcs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.constants.EnumParts;
import org.junit.Test;
import static org.junit.Assert.*;

/** Characterization shared by testOriginal and test; no OpenGL context required. */
public class ModelScaleRendererTest {
    @Test public void constructorKeepsPartAndStartsUncompiled() {
        ModelBase base = new ModelBase() {};
        ModelScaleRenderer part = new ModelScaleRenderer(base, 8, 16, EnumParts.BODY);
        assertSame(EnumParts.BODY, part.part);
        assertTrue(base.boxList.contains(part));
        assertFalse(part.isCompiled);
        assertEquals(0, part.displayList);
        assertNull(part.config);
        part.addBox(-4, 0, -2, 8, 12, 4);
        assertEquals(1, part.cubeList.size());
    }

    @Test public void setRotationWritesTheSuppliedRendererOnly() {
        ModelBase base = new ModelBase() {};
        ModelScaleRenderer part = new ModelScaleRenderer(base, EnumParts.HEAD);
        ModelRenderer target = new ModelRenderer(base);
        part.setRotation(target, 0.25f, -0.5f, 1.5f);
        assertEquals(0.25f, target.rotateAngleX, 0);
        assertEquals(-0.5f, target.rotateAngleY, 0);
        assertEquals(1.5f, target.rotateAngleZ, 0);
        assertEquals(0, part.rotateAngleX, 0);
    }

    @Test public void invisiblePartsDoNotCompileOrTouchOpenGl() {
        ModelScaleRenderer part = new ModelScaleRenderer(new ModelBase() {}, EnumParts.HEAD);
        part.showModel = false;
        part.render(1f / 16);
        assertFalse(part.isCompiled);
        part.showModel = true;
        part.isHidden = true;
        part.render(1f / 16);
        assertFalse(part.isCompiled);
        assertEquals(0, part.displayList);
    }

    @Test public void configurationAndChildAreKeptByReference() {
        ModelBase base = new ModelBase() {};
        ModelScaleRenderer part = new ModelScaleRenderer(base, EnumParts.BODY);
        ModelPartConfig config = new ModelPartConfig();
        config.setTranslate(0.1f, -0.2f, 0.3f);
        part.config = config;
        ModelRenderer child = new ModelRenderer(base);
        part.addChild(child);
        assertSame(config, part.config);
        assertSame(child, part.childModels.get(0));
        config.scaleX = 1.25f;
        assertEquals(1.25f, part.config.scaleX, 0);
    }
}
