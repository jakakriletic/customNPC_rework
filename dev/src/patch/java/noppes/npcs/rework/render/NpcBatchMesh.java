package noppes.npcs.rework.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** M14.2: the frozen vanilla bytes of all main-model parts in render order, in one buffer.
 * Per vertex: x, y, z and the part index as w (4 floats), u, v (2 floats), the same signed
 * byte normal and one pad byte; 28 bytes. Positions, UV and normals are the exact values of
 * {@link PartGeometry}. The last normal and UV of every part are kept, because the original
 * display lists leave them as the current GL values. No GL calls.
 */
public final class NpcBatchMesh {
    public static final int STRIDE = 28;
    public final ByteBuffer bytes;
    public final int vertices;
    /** Vertices of each part; 0 for an empty part. */
    public final int[] partVertices;
    /** Signed byte normal (3 per part) and UV (2 per part) of the last vertex of each part. */
    public final byte[] lastNormal;
    public final float[] lastUv;

    private NpcBatchMesh(ByteBuffer bytes, int vertices, int[] partVertices, byte[] lastNormal, float[] lastUv) {
        this.bytes = bytes; this.vertices = vertices; this.partVertices = partVertices;
        this.lastNormal = lastNormal; this.lastUv = lastUv;
    }

    public static NpcBatchMesh build(PartGeometry[] parts) {
        int total = 0;
        for (PartGeometry g : parts) { total += g.vertices; }
        ByteBuffer out = ByteBuffer.allocateDirect(total * STRIDE).order(ByteOrder.nativeOrder());
        int[] counts = new int[parts.length];
        byte[] normals = new byte[parts.length * 3];
        float[] uvs = new float[parts.length * 2];
        for (int i = 0; i < parts.length; i++) {
            ByteBuffer in = parts[i].bytes.duplicate().order(parts[i].bytes.order());
            counts[i] = parts[i].vertices;
            for (int v = 0; v < parts[i].vertices; v++) {
                int at = v * 24;
                out.putFloat(in.getFloat(at)).putFloat(in.getFloat(at + 4)).putFloat(in.getFloat(at + 8)).putFloat(i);
                out.putFloat(in.getFloat(at + 12)).putFloat(in.getFloat(at + 16));
                out.put(in.get(at + 20)).put(in.get(at + 21)).put(in.get(at + 22)).put((byte) 0);
                if (v == parts[i].vertices - 1) {
                    normals[i * 3] = in.get(at + 20); normals[i * 3 + 1] = in.get(at + 21); normals[i * 3 + 2] = in.get(at + 22);
                    uvs[i * 2] = in.getFloat(at + 12); uvs[i * 2 + 1] = in.getFloat(at + 16);
                }
            }
        }
        out.flip();
        return new NpcBatchMesh(out, total, counts, normals, uvs);
    }
}
