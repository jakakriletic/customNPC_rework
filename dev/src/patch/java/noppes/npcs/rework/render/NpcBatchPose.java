package noppes.npcs.rework.render;

import net.minecraft.client.model.ModelRenderer;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.client.model.ModelScaleRenderer;

/** M14.2: CPU copy of the matrix operations that ModelPlayerAlt.render and
 * ModelScaleRenderer.render/postRender issue for one NPC, relative to the model-view
 * matrix at mainModel.render. Same operation order and float arguments as the GL calls:
 * sneak translate, config translate, rotation point, rotate Z/Y/X in degrees, config scale.
 * Output is one column-major 4x4 per part (glUniformMatrix4 layout). A part the original
 * skips (hidden or not shown) becomes a zero-scale matrix: its quads collapse to a point
 * and produce no fragments. No GL calls.
 */
public final class NpcBatchPose {
    private static final float DEG = 180F / (float) Math.PI;
    // Render thread only; no per-NPC allocation.
    private static final float[] BASE = new float[16];

    private NpcBatchPose() { }

    public static void compute(ModelRenderer[] parts, boolean sneaking, float scale, float[] out) {
        compute(parts, sneaking, scale, null, out);
    }

    /** Compose each operation directly onto the incoming model-view, like the original
     * GL matrix stack. Multiplying an independently rounded local matrix afterwards
     * is not the same floating-point expression at shared edges or texel boundaries. */
    public static void compute(ModelRenderer[] parts, boolean sneaking, float scale, float[] modelView, float[] out) {
        float[] base = BASE;
        if (modelView == null) { identity(base, 0); }
        else { System.arraycopy(modelView, 0, base, 0, 16); }
        if (sneaking) { translate(base, 0, 0.0f, 0.2f, 0.0f); }
        for (int i = 0; i < parts.length; i++) {
            int at = i * 16;
            ModelRenderer p = parts[i];
            if (!p.showModel || p.isHidden) {
                for (int k = 0; k < 16; k++) { out[at + k] = 0.0f; }
                out[at + 15] = 1.0f;
                continue;
            }
            System.arraycopy(base, 0, out, at, 16);
            ModelPartConfig config = p instanceof ModelScaleRenderer ? ((ModelScaleRenderer) p).config : null;
            if (config != null) { translate(out, at, config.transX, config.transY, config.transZ); }
            if (p.rotateAngleX == 0.0F && p.rotateAngleY == 0.0F && p.rotateAngleZ == 0.0F) {
                if (p.rotationPointX != 0.0F || p.rotationPointY != 0.0F || p.rotationPointZ != 0.0F) {
                    translate(out, at, p.rotationPointX * scale, p.rotationPointY * scale, p.rotationPointZ * scale);
                }
            } else {
                translate(out, at, p.rotationPointX * scale, p.rotationPointY * scale, p.rotationPointZ * scale);
                if (p.rotateAngleZ != 0.0F) { rotateZ(out, at, p.rotateAngleZ * DEG); }
                if (p.rotateAngleY != 0.0F) { rotateY(out, at, p.rotateAngleY * DEG); }
                if (p.rotateAngleX != 0.0F) { rotateX(out, at, p.rotateAngleX * DEG); }
            }
            if (config != null) { scale(out, at, config.scaleX, config.scaleY, config.scaleZ); }
        }
    }

    /** Diagnostic projection times each complete model-view, rounded once per element.
     * Not a numerical equality guarantee for the driver's fixed-function product.
     * The production candidate obtains that product from the native matrix stack. */
    public static void project(float[] projection, float[] modelViews, float[] out) {
        for (int at = 0; at < modelViews.length; at += 16) {
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    double value = 0.0;
                    for (int k = 0; k < 4; k++) {
                        value += (double) projection[k * 4 + row] * modelViews[at + col * 4 + k];
                    }
                    out[at + col * 4 + row] = (float) value;
                }
            }
        }
    }

    static void identity(float[] m, int at) {
        for (int k = 0; k < 16; k++) { m[at + k] = (k % 5 == 0) ? 1.0f : 0.0f; }
    }

    /** m = m * T(x, y, z), as glTranslatef. */
    static void translate(float[] m, int at, float x, float y, float z) {
        for (int r = 0; r < 4; r++) {
            m[at + 12 + r] = m[at + r] * x + m[at + 4 + r] * y + m[at + 8 + r] * z + m[at + 12 + r];
        }
    }

    /** m = m * S(x, y, z), as glScalef. */
    static void scale(float[] m, int at, float x, float y, float z) {
        for (int r = 0; r < 4; r++) {
            m[at + r] *= x; m[at + 4 + r] *= y; m[at + 8 + r] *= z;
        }
    }

    /** m = m * R(degrees) about one axis, as glRotatef with a unit axis. */
    public static void rotateX(float[] m, int at, float degrees) { rotate(m, at, degrees, 4, 8); }
    static void rotateY(float[] m, int at, float degrees) { rotate(m, at, degrees, 8, 0); }
    static void rotateZ(float[] m, int at, float degrees) { rotate(m, at, degrees, 0, 4); }

    /** Columns a and b of the rotation plane: a' = c*a + s*b, b' = -s*a + c*b. */
    private static void rotate(float[] m, int at, float degrees, int a, int b) {
        double rad = (double) degrees * Math.PI / 180.0;
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        for (int r = 0; r < 4; r++) {
            float va = m[at + a + r], vb = m[at + b + r];
            m[at + a + r] = va * c + vb * s;
            m[at + b + r] = vb * c - va * s;
        }
    }
}
