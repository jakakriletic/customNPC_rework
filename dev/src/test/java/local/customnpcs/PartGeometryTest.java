package local.customnpcs;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.model.*;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.rework.render.PartGeometry;
import org.junit.Test;
import static org.junit.Assert.*;

/** Contract tests of frozen vanilla-format geometry; GPU equivalence is a controller test. */
public class PartGeometryTest {
    private ModelScaleRenderer part() {
        ModelScaleRenderer p = new ModelScaleRenderer(new ModelBase() {}, 0, 0, EnumParts.BODY);
        p.addBox(-4, 0, -2, 8, 12, 4); return p;
    }

    @Test public void boxHasSixFacesAndSignedByteAxisNormals() {
        PartGeometry g = PartGeometry.capture(part(), 1f / 16);
        assertEquals(24, g.vertices); assertEquals(24 * 24, g.bytes.remaining());
        Set<String> normals = new HashSet<String>();
        for (int face = 0; face < 6; face++) {
            int at = face * 4 * 24;
            String normal = g.bytes.get(at + 20) + "," + g.bytes.get(at + 21) + "," + g.bytes.get(at + 22);
            normals.add(normal);
            for (int v = 1; v < 4; v++) { for (int i = 20; i < 23; i++) {
                assertEquals(g.bytes.get(at + i), g.bytes.get(at + v * 24 + i));
            } }
        }
        assertEquals(6, normals.size());
        for (String n : new String[] {"127,0,0", "-127,0,0", "0,127,0", "0,-127,0", "0,0,127", "0,0,-127"}) {
            assertTrue(n, normals.contains(n));
        }
    }

    @Test public void poseAndConfigAreNotBakedIntoGeometry() {
        ModelScaleRenderer p = part(); PartGeometry g = PartGeometry.capture(p, 1f / 16);
        p.rotateAngleX = 0.7f; p.rotationPointY = 16; p.config = new ModelPartConfig(); p.config.scaleX = 2;
        assertTrue(g.matches(p, 1f / 16));
        assertFalse(g.matches(p, 1f / 8));
        assertTrue(g.matches(p, 1f / 16));
    }

    @Test public void uvMutationIsDetectedWithoutChangingFrozenBytes() {
        ModelScaleRenderer p = part(); PartGeometry g = PartGeometry.capture(p, 1f / 16);
        ByteBuffer before = ByteBuffer.allocate(g.bytes.remaining()); before.put(g.bytes.duplicate()).flip();
        TexturedQuad[] q = ObfuscationReflectionHelper.getPrivateValue(ModelBox.class, p.cubeList.get(0), 1);
        PositionTextureVertex old = q[0].vertexPositions[0];
        q[0].vertexPositions[0] = old.setTexturePosition(old.texturePositionX + 0.125f, old.texturePositionY);
        assertFalse(g.matches(p, 1f / 16)); assertEquals(before, g.bytes);
        q[0].vertexPositions[0] = old;
        assertTrue(g.matches(p, 1f / 16));
    }

    @Test public void listAndMirrorChangesInvalidateSnapshot() {
        ModelScaleRenderer p = part(); PartGeometry g = PartGeometry.capture(p, 1f / 16);
        p.mirror = true; assertFalse(g.matches(p, 1f / 16)); p.mirror = false;
        p.addBox(0, 0, 0, 1, 1, 1); assertFalse(g.matches(p, 1f / 16));
        p.cubeList.remove(1); assertTrue(g.matches(p, 1f / 16));
        p.textureWidth = 128; assertFalse(g.matches(p, 1f / 16));
    }

    @Test public void rejectsOverriddenBoxesAndMalformedFaces() {
        ModelScaleRenderer p = part();
        p.cubeList.add(new ModelBox(p, 0, 0, 0, 0, 0, 1, 1, 1, 0) {});
        assertNull(PartGeometry.capture(p, 1f / 16)); p.cubeList.remove(1);
        TexturedQuad[] q = ObfuscationReflectionHelper.getPrivateValue(ModelBox.class, p.cubeList.get(0), 1);
        q[0].nVertices = 3; assertNull(PartGeometry.capture(p, 1f / 16));
    }

    @Test public void emptyPartIsRepresentableAndHasNoVertices() {
        ModelScaleRenderer p = part(); p.cubeList.clear();
        PartGeometry g = PartGeometry.capture(p, 1f / 16);
        assertEquals(0, g.vertices); assertEquals(0, g.bytes.remaining()); assertTrue(g.matches(p, 1f / 16));
    }
}
