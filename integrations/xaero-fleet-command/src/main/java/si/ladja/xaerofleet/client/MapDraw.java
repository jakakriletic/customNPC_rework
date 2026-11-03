package si.ladja.xaerofleet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * The few drawing primitives the overlay needs, in GUI coordinates with
 * sub-pixel positions: filled rectangles, lines, rings and the captain icons.
 *
 * <p>Every call leaves the state as {@code Gui.drawRect} does - texturing on,
 * blending off, colour white - because Xaero draws its own tooltips and menus
 * after other overlays have run and expects to find it that way.</p>
 */
final class MapDraw {

    private static final Map<Integer, ResourceLocation> ICONS = new HashMap<>();

    private MapDraw() {}

    /** The captain's face with this hat colour, uploaded once per colour. */
    static ResourceLocation icon(int hat) {
        ResourceLocation location = ICONS.get(hat);
        if (location != null) return location;
        DynamicTexture texture = new DynamicTexture(CaptainIcon.SIZE, CaptainIcon.SIZE);
        int[] data = texture.getTextureData();
        int[] pixels = CaptainIcon.pixels(hat);
        System.arraycopy(pixels, 0, data, 0, pixels.length);
        texture.updateDynamicTexture();
        location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
                "xaerofleet_captain_" + Integer.toHexString(hat), texture);
        ICONS.put(hat, location);
        return location;
    }

    /** Draws a captain icon centred on a point, {@code size} GUI pixels across. */
    static void icon(int hat, double cx, double cy, double size, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(icon(hat));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1F, 1F, 1F, alpha);
        double h = size / 2.0D;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(cx - h, cy + h, 0).tex(0, 1).endVertex();
        buffer.pos(cx + h, cy + h, 0).tex(1, 1).endVertex();
        buffer.pos(cx + h, cy - h, 0).tex(1, 0).endVertex();
        buffer.pos(cx - h, cy - h, 0).tex(0, 0).endVertex();
        tessellator.draw();
        done();
    }

    static void rect(double x0, double y0, double x1, double y1, int argb) {
        begin(argb);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(Math.min(x0, x1), Math.max(y0, y1), 0).endVertex();
        buffer.pos(Math.max(x0, x1), Math.max(y0, y1), 0).endVertex();
        buffer.pos(Math.max(x0, x1), Math.min(y0, y1), 0).endVertex();
        buffer.pos(Math.min(x0, x1), Math.min(y0, y1), 0).endVertex();
        tessellator.draw();
        done();
    }

    /** An outlined rectangle, the outline {@code width} GUI pixels thick. */
    static void frame(double x0, double y0, double x1, double y1, double width, int argb) {
        double left = Math.min(x0, x1);
        double right = Math.max(x0, x1);
        double top = Math.min(y0, y1);
        double bottom = Math.max(y0, y1);
        rect(left, top, right, top + width, argb);
        rect(left, bottom - width, right, bottom, argb);
        rect(left, top + width, left + width, bottom - width, argb);
        rect(right - width, top + width, right, bottom - width, argb);
    }

    /** A straight line, optionally dashed, {@code width} in real pixels. */
    static void line(double x0, double y0, double x1, double y1, float width,
                     int argb, double dash, double phase) {
        begin(argb);
        GL11.glLineWidth(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        double length = Math.hypot(x1 - x0, y1 - y0);
        if (dash <= 0 || length < 1e-3) {
            buffer.pos(x0, y0, 0).endVertex();
            buffer.pos(x1, y1, 0).endVertex();
        } else {
            double dx = (x1 - x0) / length;
            double dy = (y1 - y0) / length;
            double start = -((phase % (2 * dash)) + 2 * dash) % (2 * dash);
            for (double s = start; s < length; s += 2 * dash) {
                double a = Math.max(0, s);
                double b = Math.min(length, s + dash);
                if (b <= a) continue;
                buffer.pos(x0 + dx * a, y0 + dy * a, 0).endVertex();
                buffer.pos(x0 + dx * b, y0 + dy * b, 0).endVertex();
            }
        }
        tessellator.draw();
        GL11.glLineWidth(1F);
        done();
    }

    /** A circle outline around a point. */
    static void ring(double cx, double cy, double radius, float width, int argb) {
        begin(argb);
        GL11.glLineWidth(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        int segments = Math.max(12, Math.min(64, (int) (radius * 1.5)));
        for (int i = 0; i < segments; i++) {
            double angle = i * Math.PI * 2 / segments;
            buffer.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0)
                    .endVertex();
        }
        tessellator.draw();
        GL11.glLineWidth(1F);
        done();
    }

    /**
     * Four corner brackets around a point - the selection mark.
     *
     * <p>Brackets rather than a box so the face inside stays visible.</p>
     */
    static void brackets(double cx, double cy, double half, double arm, double width,
                         int argb) {
        double l = cx - half;
        double r = cx + half;
        double t = cy - half;
        double b = cy + half;
        rect(l, t, l + arm, t + width, argb);
        rect(l, t, l + width, t + arm, argb);
        rect(r - arm, t, r, t + width, argb);
        rect(r - width, t, r, t + arm, argb);
        rect(l, b - width, l + arm, b, argb);
        rect(l, b - arm, l + width, b, argb);
        rect(r - arm, b - width, r, b, argb);
        rect(r - width, b - arm, r, b, argb);
    }

    /** A small filled triangle pointing from a centre out along an angle. */
    static void pointer(double cx, double cy, double angle, double distance, double size,
                        int argb) {
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        double tipX = cx + dx * (distance + size);
        double tipY = cy + dy * (distance + size);
        double baseX = cx + dx * distance;
        double baseY = cy + dy * distance;
        double px = -dy * size * 0.7;
        double py = dx * size * 0.7;
        begin(argb);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        buffer.pos(tipX, tipY, 0).endVertex();
        buffer.pos(baseX - px, baseY - py, 0).endVertex();
        buffer.pos(baseX + px, baseY + py, 0).endVertex();
        tessellator.draw();
        done();
    }

    /** A filled dot. */
    static void dot(double cx, double cy, double radius, int argb) {
        begin(argb);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        buffer.pos(cx, cy, 0).endVertex();
        int segments = 12;
        for (int i = segments; i >= 0; i--) {
            double angle = i * Math.PI * 2 / segments;
            buffer.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0)
                    .endVertex();
        }
        tessellator.draw();
        done();
    }

    static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
        return a << 24 | (argb & 0xFFFFFF);
    }

    private static void begin(int argb) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F,
                (argb & 0xFF) / 255F, ((argb >>> 24) & 0xFF) / 255F);
    }

    private static void done() {
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
