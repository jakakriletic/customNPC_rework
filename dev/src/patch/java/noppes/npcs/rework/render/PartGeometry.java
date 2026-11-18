package noppes.npcs.rework.render;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.client.model.ModelScaleRenderer;
import org.lwjgl.BufferUtils;

/** M14.2: frozen geometry of one CNPC model part, recovered from the rejected M14.1b candidate (5d598d5). No GL calls.
 * Exact vanilla POSITION_TEX_NORMAL bytes (including signed byte normals), no float-normal replacement.
 * Public mutable geometry is checked without hashing or per-frame re-encoding/allocation.
 */
public final class PartGeometry {
    // Same fields as ObfuscationReflectionHelper index 1/2, resolved once: that helper copies
    // getDeclaredFields() on every call and dominated the first cost pair (Q482).
    private static final Field QUADS = field(ModelBox.class, 1), INVERT = field(TexturedQuad.class, 2);
    public final ByteBuffer bytes;
    public final int vertices;
    private long[] key = new long[256];
    private int size, cursor;
    private boolean checking, equal;

    private PartGeometry(ByteBuffer bytes, int vertices) { this.bytes = bytes; this.vertices = vertices; }

    public static PartGeometry capture(ModelScaleRenderer part, float scale) {
        BufferBuilder buffer = new BufferBuilder(1024);
        buffer.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
        int count = 0;
        for (ModelBox box : part.cubeList) {
            if (box == null || box.getClass() != ModelBox.class) { return null; }
            for (TexturedQuad quad : quads(box)) {
                if (!supported(quad)) { return null; }
                PositionTextureVertex[] v = quad.vertexPositions;
                // Same operation order and conversions as TexturedQuad.draw.
                Vec3d a = v[1].vector3D.subtractReverse(v[0].vector3D);
                Vec3d b = v[1].vector3D.subtractReverse(v[2].vector3D);
                Vec3d normal = b.crossProduct(a).normalize();
                float x = (float) normal.x, y = (float) normal.y, z = (float) normal.z;
                if (inverted(quad)) { x = -x; y = -y; z = -z; }
                for (PositionTextureVertex p : v) {
                    buffer.pos(p.vector3D.x * (double) scale, p.vector3D.y * (double) scale,
                            p.vector3D.z * (double) scale).tex((double) p.texturePositionX,
                            (double) p.texturePositionY).normal(x, y, z).endVertex();
                }
                count += 4;
                if (count > 65536) { return null; }
            }
        }
        buffer.finishDrawing();
        ByteBuffer bytes = BufferUtils.createByteBuffer(count * 24);
        bytes.put(buffer.getByteBuffer()).flip();
        PartGeometry result = new PartGeometry(bytes, count);
        if (!result.visit(part, scale)) { return null; }
        result.key = Arrays.copyOf(result.key, result.size);
        return result;
    }

    public boolean matches(ModelScaleRenderer part, float scale) {
        checking = true; equal = true; cursor = 0;
        return visit(part, scale) && equal && cursor == size;
    }

    private boolean visit(ModelScaleRenderer part, float scale) {
        word(Float.floatToRawIntBits(scale));
        word(Float.floatToRawIntBits(part.textureWidth)); word(Float.floatToRawIntBits(part.textureHeight));
        word(part.mirror ? 1 : 0); word(part.cubeList.size());
        for (ModelBox box : part.cubeList) {
            if (box == null || box.getClass() != ModelBox.class) { return false; }
            TexturedQuad[] faces = quads(box); word(faces.length);
            for (TexturedQuad q : faces) {
                if (!supported(q)) { return false; }
                word(q.nVertices); word(inverted(q) ? 1 : 0);
                for (PositionTextureVertex v : q.vertexPositions) {
                    word(Double.doubleToRawLongBits(v.vector3D.x)); word(Double.doubleToRawLongBits(v.vector3D.y));
                    word(Double.doubleToRawLongBits(v.vector3D.z));
                    word(Float.floatToRawIntBits(v.texturePositionX)); word(Float.floatToRawIntBits(v.texturePositionY));
                }
            }
        }
        return true;
    }

    private void word(long value) {
        if (checking) { if (cursor >= size || key[cursor] != value) { equal = false; } cursor++; }
        else { if (size == key.length) { key = Arrays.copyOf(key, key.length * 2); } key[size++] = value; }
    }

    private static boolean supported(TexturedQuad quad) {
        if (quad == null || quad.getClass() != TexturedQuad.class || quad.nVertices != 4
                || quad.vertexPositions == null || quad.vertexPositions.length != 4) { return false; }
        for (PositionTextureVertex v : quad.vertexPositions) {
            if (v == null || v.vector3D == null || !Double.isFinite(v.vector3D.x) || !Double.isFinite(v.vector3D.y)
                    || !Double.isFinite(v.vector3D.z) || !Float.isFinite(v.texturePositionX)
                    || !Float.isFinite(v.texturePositionY)) { return false; }
        }
        return true;
    }

    private static Field field(Class<?> owner, int index) {
        Field f = owner.getDeclaredFields()[index]; f.setAccessible(true); return f;
    }
    private static TexturedQuad[] quads(ModelBox box) {
        try { return (TexturedQuad[]) QUADS.get(box); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    private static boolean inverted(TexturedQuad quad) {
        try { return INVERT.getBoolean(quad); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
}
