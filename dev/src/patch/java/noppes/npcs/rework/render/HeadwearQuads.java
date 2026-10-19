package noppes.npcs.rework.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Stirikotniki ekstrudiranega obraza pokrivala ({@code Model2DRenderer.renderItemIn2D}), M5-K K14b.
 *
 * <p>Original nariše za obraz {@code w} × {@code h} tekslov {@code 2 + 2w + 2h} stirikotnikov v tem
 * vrstnem redu: spredaj, zadaj, {@code w} trakov z normalo -x, {@code w} z normalo +x, {@code h} z
 * normalo +y, {@code h} z normalo -y. Trak vzorci en stolpec ali eno vrstico tekslov (koordinata u
 * oziroma v je sredina teksla). {@link #emit} zapise iste oglisca z isto aritmetiko float kot
 * original, le stirikotnike z {@code keep[q] == false} izpusti; {@link #uvBounds} vrne obseg uv, ki
 * ga stirikotnik vzorci. Brez GL, testljivo.
 */
public final class HeadwearQuads {
    private HeadwearQuads() {
    }

    public static int count(int w, int h) {
        return 2 + 2 * w + 2 * h;
    }

    /** Za vsak stirikotnik {umin, umax, vmin, vmax}, iste vrednosti kot v {@link #emit}. */
    public static float[][] uvBounds(float u1, float v1, float u2, float v2, int w, int h) {
        float[][] r = new float[count(w, h)][];
        float uLo = Math.min(u1, u2), uHi = Math.max(u1, u2), vLo = Math.min(v1, v2), vHi = Math.max(v1, v2);
        int q = 0;
        r[q++] = new float[] {uLo, uHi, vLo, vHi};
        r[q++] = new float[] {uLo, uHi, vLo, vHi};
        float f5 = 0.5f * (u1 - u2) / (float)w;
        float f6 = 0.5f * (v2 - v1) / (float)h;
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < w; ++k) {
                float f7 = (float)k / (float)w;
                float f8 = u1 + (u2 - u1) * f7 - f5;
                r[q++] = new float[] {f8, f8, vLo, vHi};
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < h; ++k) {
                float f7 = (float)k / (float)h;
                float f8 = v2 + (v1 - v2) * f7 - f6;
                r[q++] = new float[] {uLo, uHi, f8, f8};
            }
        }
        return r;
    }

    /**
     * Kot {@code renderItemIn2D(b, u1, v1, u2, v2, w, h, t)} brez {@code Tessellator.draw()}: zacne
     * {@code b} in zapise oglisca stirikotnikov s {@code keep[q]}. Telo je prepisano iz originala
     * (imena parametrov p1..p7), le vsak stirikotnik je pod pogojem.
     */
    public static void emit(BufferBuilder worldrenderer, float p_78439_1_, float p_78439_2_, float p_78439_3_, float p_78439_4_, int p_78439_5_, int p_78439_6_, float p_78439_7_, boolean[] keep) {
        float f9;
        float f8;
        float f7;
        int k;
        int q = 0;
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
        if (keep[q++]) {
            worldrenderer.pos(0.0, 0.0, 0.0).tex((double)p_78439_1_, (double)p_78439_4_).normal(0.0f, 0.0f, 1.0f).endVertex();
            worldrenderer.pos(1.0, 0.0, 0.0).tex((double)p_78439_3_, (double)p_78439_4_).normal(0.0f, 0.0f, 1.0f).endVertex();
            worldrenderer.pos(1.0, 1.0, 0.0).tex((double)p_78439_3_, (double)p_78439_2_).normal(0.0f, 0.0f, 1.0f).endVertex();
            worldrenderer.pos(0.0, 1.0, 0.0).tex((double)p_78439_1_, (double)p_78439_2_).normal(0.0f, 0.0f, 1.0f).endVertex();
        }
        if (keep[q++]) {
            worldrenderer.pos(0.0, 1.0, (double)(0.0f - p_78439_7_)).tex((double)p_78439_1_, (double)p_78439_2_).normal(0.0f, 0.0f, -1.0f).endVertex();
            worldrenderer.pos(1.0, 1.0, (double)(0.0f - p_78439_7_)).tex((double)p_78439_3_, (double)p_78439_2_).normal(0.0f, 0.0f, -1.0f).endVertex();
            worldrenderer.pos(1.0, 0.0, (double)(0.0f - p_78439_7_)).tex((double)p_78439_3_, (double)p_78439_4_).normal(0.0f, 0.0f, -1.0f).endVertex();
            worldrenderer.pos(0.0, 0.0, (double)(0.0f - p_78439_7_)).tex((double)p_78439_1_, (double)p_78439_4_).normal(0.0f, 0.0f, -1.0f).endVertex();
        }
        float f5 = 0.5f * (p_78439_1_ - p_78439_3_) / (float)p_78439_5_;
        float f6 = 0.5f * (p_78439_4_ - p_78439_2_) / (float)p_78439_6_;
        for (k = 0; k < p_78439_5_; ++k) {
            f7 = (float)k / (float)p_78439_5_;
            f8 = p_78439_1_ + (p_78439_3_ - p_78439_1_) * f7 - f5;
            if (keep[q++]) {
                worldrenderer.pos((double)f7, 0.0, (double)(0.0f - p_78439_7_)).tex((double)f8, (double)p_78439_4_).normal(-1.0f, 0.0f, 0.0f).endVertex();
                worldrenderer.pos((double)f7, 0.0, 0.0).tex((double)f8, (double)p_78439_4_).normal(-1.0f, 0.0f, 0.0f).endVertex();
                worldrenderer.pos((double)f7, 1.0, 0.0).tex((double)f8, (double)p_78439_2_).normal(-1.0f, 0.0f, 0.0f).endVertex();
                worldrenderer.pos((double)f7, 1.0, (double)(0.0f - p_78439_7_)).tex((double)f8, (double)p_78439_2_).normal(-1.0f, 0.0f, 0.0f).endVertex();
            }
        }
        for (k = 0; k < p_78439_5_; ++k) {
            f7 = (float)k / (float)p_78439_5_;
            f8 = p_78439_1_ + (p_78439_3_ - p_78439_1_) * f7 - f5;
            f9 = f7 + 1.0f / (float)p_78439_5_;
            if (keep[q++]) {
                worldrenderer.pos((double)f9, 1.0, (double)(0.0f - p_78439_7_)).tex((double)f8, (double)p_78439_2_).normal(1.0f, 0.0f, 0.0f).endVertex();
                worldrenderer.pos((double)f9, 1.0, 0.0).tex((double)f8, (double)p_78439_2_).normal(1.0f, 0.0f, 0.0f).endVertex();
                worldrenderer.pos((double)f9, 0.0, 0.0).tex((double)f8, (double)p_78439_4_).normal(1.0f, 0.0f, 0.0f).endVertex();
                worldrenderer.pos((double)f9, 0.0, (double)(0.0f - p_78439_7_)).tex((double)f8, (double)p_78439_4_).normal(1.0f, 0.0f, 0.0f).endVertex();
            }
        }
        for (k = 0; k < p_78439_6_; ++k) {
            f7 = (float)k / (float)p_78439_6_;
            f8 = p_78439_4_ + (p_78439_2_ - p_78439_4_) * f7 - f6;
            f9 = f7 + 1.0f / (float)p_78439_6_;
            if (keep[q++]) {
                worldrenderer.pos(0.0, (double)f9, 0.0).tex((double)p_78439_1_, (double)f8).normal(0.0f, 1.0f, 0.0f).endVertex();
                worldrenderer.pos(1.0, (double)f9, 0.0).tex((double)p_78439_3_, (double)f8).normal(0.0f, 1.0f, 0.0f).endVertex();
                worldrenderer.pos(1.0, (double)f9, (double)(0.0f - p_78439_7_)).tex((double)p_78439_3_, (double)f8).normal(0.0f, 1.0f, 0.0f).endVertex();
                worldrenderer.pos(0.0, (double)f9, (double)(0.0f - p_78439_7_)).tex((double)p_78439_1_, (double)f8).normal(0.0f, 1.0f, 0.0f).endVertex();
            }
        }
        for (k = 0; k < p_78439_6_; ++k) {
            f7 = (float)k / (float)p_78439_6_;
            f8 = p_78439_4_ + (p_78439_2_ - p_78439_4_) * f7 - f6;
            if (keep[q++]) {
                worldrenderer.pos(1.0, (double)f7, 0.0).tex((double)p_78439_3_, (double)f8).normal(0.0f, -1.0f, 0.0f).endVertex();
                worldrenderer.pos(0.0, (double)f7, 0.0).tex((double)p_78439_1_, (double)f8).normal(0.0f, -1.0f, 0.0f).endVertex();
                worldrenderer.pos(0.0, (double)f7, (double)(0.0f - p_78439_7_)).tex((double)p_78439_1_, (double)f8).normal(0.0f, -1.0f, 0.0f).endVertex();
                worldrenderer.pos(1.0, (double)f7, (double)(0.0f - p_78439_7_)).tex((double)p_78439_3_, (double)f8).normal(0.0f, -1.0f, 0.0f).endVertex();
            }
        }
    }
}
