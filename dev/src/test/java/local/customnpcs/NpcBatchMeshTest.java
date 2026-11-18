package local.customnpcs;

import java.nio.ByteBuffer;
import net.minecraft.client.model.ModelBase;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.rework.render.NpcBatchMesh;
import noppes.npcs.rework.render.PartGeometry;
import org.junit.Test;
import static org.junit.Assert.*;

/** M14.2: the combined mesh keeps the exact frozen bytes of every part and tags each vertex
 * with its part index; the last normal/UV per part are those the original lists leave. */
public class NpcBatchMeshTest {
    private static PartGeometry geometry(int boxes) {
        ModelScaleRenderer p = new ModelScaleRenderer(new ModelBase() {}, 8, 16, EnumParts.ARM_LEFT);
        for (int i = 0; i < boxes; i++) { p.addBox(-1 + i, -2, -3, 4, 12, 4); }
        return PartGeometry.capture(p, 1f / 16);
    }

    @Test public void copiesEveryPartInOrderWithItsIndex() {
        PartGeometry[] parts = {geometry(1), geometry(0), geometry(2)};
        NpcBatchMesh mesh = NpcBatchMesh.build(parts);
        assertEquals(24 + 0 + 48, mesh.vertices);
        assertEquals(mesh.vertices * NpcBatchMesh.STRIDE, mesh.bytes.remaining());
        assertArrayEquals(new int[] {24, 0, 48}, mesh.partVertices);
        int v = 0;
        for (int part = 0; part < parts.length; part++) {
            ByteBuffer in = parts[part].bytes.duplicate().order(parts[part].bytes.order());
            for (int i = 0; i < parts[part].vertices; i++, v++) {
                int o = v * NpcBatchMesh.STRIDE, src = i * 24;
                for (int k = 0; k < 3; k++) { assertEquals(in.getFloat(src + 4 * k), mesh.bytes.getFloat(o + 4 * k), 0.0f); }
                assertEquals(part, mesh.bytes.getFloat(o + 12), 0.0f);
                assertEquals(in.getFloat(src + 12), mesh.bytes.getFloat(o + 16), 0.0f);
                assertEquals(in.getFloat(src + 16), mesh.bytes.getFloat(o + 20), 0.0f);
                for (int k = 0; k < 3; k++) { assertEquals(in.get(src + 20 + k), mesh.bytes.get(o + 24 + k)); }
                assertEquals(0, mesh.bytes.get(o + 27));
            }
        }
    }

    @Test public void lastNormalAndUvAreThoseOfTheLastVertexOfEachPart() {
        PartGeometry g = geometry(1);
        NpcBatchMesh mesh = NpcBatchMesh.build(new PartGeometry[] {g});
        ByteBuffer in = g.bytes.duplicate().order(g.bytes.order());
        int last = (g.vertices - 1) * 24;
        assertEquals(in.get(last + 20), mesh.lastNormal[0]);
        assertEquals(in.get(last + 21), mesh.lastNormal[1]);
        assertEquals(in.get(last + 22), mesh.lastNormal[2]);
        assertEquals(in.getFloat(last + 12), mesh.lastUv[0], 0.0f);
        assertEquals(in.getFloat(last + 16), mesh.lastUv[1], 0.0f);
    }
}
