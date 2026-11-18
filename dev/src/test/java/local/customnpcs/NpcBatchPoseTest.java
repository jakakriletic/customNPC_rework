package local.customnpcs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.rework.render.NpcBatchPose;
import org.junit.Test;
import static org.junit.Assert.*;

/** M14.2: CPU part matrices against an independent double-precision composition of the
 * GL calls of ModelPlayerAlt.render + ModelScaleRenderer.postRender. GPU equality is a
 * controller test. */
public class NpcBatchPoseTest {
    private static final float SCALE = 0.0625f;

    private static ModelScaleRenderer part() {
        ModelScaleRenderer p = new ModelScaleRenderer(new ModelBase() {}, 0, 0, EnumParts.BODY);
        p.addBox(-4, 0, -2, 8, 12, 4);
        p.config = new ModelPartConfig();
        return p;
    }

    private static double[] identity() {
        return new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    /** m * other, both column-major. */
    private static double[] mul(double[] m, double[] o) {
        double[] r = new double[16];
        for (int c = 0; c < 4; c++) {
            for (int row = 0; row < 4; row++) {
                double s = 0;
                for (int k = 0; k < 4; k++) { s += m[k * 4 + row] * o[c * 4 + k]; }
                r[c * 4 + row] = s;
            }
        }
        return r;
    }

    private static double[] t(double x, double y, double z) {
        double[] m = identity(); m[12] = x; m[13] = y; m[14] = z; return m;
    }

    private static double[] s(double x, double y, double z) {
        double[] m = identity(); m[0] = x; m[5] = y; m[10] = z; return m;
    }

    /** glRotatef(degrees, axis) for a unit axis: 0 = X, 1 = Y, 2 = Z. */
    private static double[] r(double degrees, int axis) {
        double a = Math.toRadians(degrees), c = Math.cos(a), sn = Math.sin(a);
        double[] m = identity();
        int i = (axis + 1) % 3, j = (axis + 2) % 3;
        m[i * 4 + i] = c; m[i * 4 + j] = sn; m[j * 4 + i] = -sn; m[j * 4 + j] = c;
        return m;
    }

    private static void assertClose(double[] expected, float[] actual, int at) {
        for (int k = 0; k < 16; k++) {
            assertEquals("element " + k, expected[k], actual[at + k], 1e-5);
        }
    }

    @Test public void rotatedPartFollowsGlOperationOrder() {
        ModelScaleRenderer p = part();
        p.config.transX = 0.1f; p.config.transY = -0.2f; p.config.transZ = 0.05f;
        p.config.scaleX = 1.2f; p.config.scaleY = 0.8f; p.config.scaleZ = 1.1f;
        p.rotationPointX = -5; p.rotationPointY = 2; p.rotationPointZ = 1;
        p.rotateAngleX = 0.3f; p.rotateAngleY = -0.7f; p.rotateAngleZ = 0.2f;
        float[] out = new float[16];
        NpcBatchPose.compute(new ModelRenderer[] {p}, true, SCALE, out);
        float deg = 180F / (float) Math.PI;
        double[] e = t(0, 0.2f, 0);
        e = mul(e, t(0.1f, -0.2f, 0.05f));
        e = mul(e, t(-5 * SCALE, 2 * SCALE, 1 * SCALE));
        e = mul(e, r(0.2f * deg, 2));
        e = mul(e, r(-0.7f * deg, 1));
        e = mul(e, r(0.3f * deg, 0));
        e = mul(e, s(1.2f, 0.8f, 1.1f));
        assertClose(e, out, 0);
    }

    @Test public void unrotatedPartOnlyTranslatesAndScales() {
        ModelScaleRenderer p = part();
        p.rotationPointY = 12; p.config.scaleY = 2;
        float[] out = new float[16];
        NpcBatchPose.compute(new ModelRenderer[] {p}, false, SCALE, out);
        assertClose(mul(t(0, 12 * SCALE, 0), s(1, 2, 1)), out, 0);
    }

    @Test public void hiddenOrNotShownPartCollapsesToAPoint() {
        ModelScaleRenderer a = part(), b = part(), c = part();
        b.isHidden = true; c.showModel = false;
        a.rotateAngleX = 0.5f; b.rotateAngleX = 0.5f; c.rotateAngleX = 0.5f;
        float[] out = new float[48];
        NpcBatchPose.compute(new ModelRenderer[] {a, b, c}, false, SCALE, out);
        for (int part = 1; part < 3; part++) {
            for (int k = 0; k < 15; k++) { assertEquals(0.0f, out[part * 16 + k], 0.0f); }
            assertEquals(1.0f, out[part * 16 + 15], 0.0f);
        }
        assertNotEquals(0.0f, out[0], 0.0f);
    }

    @Test public void sneakingShiftsEveryPartAndPartsAreIndependent() {
        ModelScaleRenderer a = part(), b = part();
        a.rotateAngleZ = 1.0f; b.rotationPointX = 4;
        float[] still = new float[32], sneak = new float[32];
        NpcBatchPose.compute(new ModelRenderer[] {a, b}, false, SCALE, still);
        NpcBatchPose.compute(new ModelRenderer[] {a, b}, true, SCALE, sneak);
        for (int part = 0; part < 2; part++) {
            assertEquals(still[part * 16 + 13] + 0.2f, sneak[part * 16 + 13], 1e-6);
            assertEquals(still[part * 16 + 12], sneak[part * 16 + 12], 0.0f);
        }
        assertClose(t(4 * SCALE, 0, 0), still, 16);
    }

    @Test public void partWithoutConfigUsesVanillaPostRenderOnly() {
        ModelScaleRenderer p = part();
        p.config = null; p.rotationPointZ = 3; p.rotateAngleY = 0.4f;
        float[] out = new float[16];
        NpcBatchPose.compute(new ModelRenderer[] {p}, false, SCALE, out);
        assertClose(mul(t(0, 0, 3 * SCALE), r(0.4f * (180F / (float) Math.PI), 1)), out, 0);
    }
}
