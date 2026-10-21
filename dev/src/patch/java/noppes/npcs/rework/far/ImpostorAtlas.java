package noppes.npcs.rework.far;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * M5-V.4: atlas impostorjev (raziskava M5-V.0 §5.4, D-046) - ena kvadratna RGBA tekstura z
 * {@link ImpostorFrames#LEVELS} stopnjami mipmap, en trak {@link ImpostorFrames#ANGLES} okvirjev na videz,
 * dodelitev trakov z LRU ({@link ImpostorSlots}). Filtriranje {@code GL_NEAREST} pri povecavi (pikselni
 * videz kot entitete vanille) in {@code GL_NEAREST_MIPMAP_LINEAR} pri pomanjsavi ("slabsa tekstura z
 * razdaljo"). Pri 2048 je to 168 videzov in ~21 MB VRAM (z mipmapi).
 */
@SideOnly(Side.CLIENT)
public final class ImpostorAtlas {
    /** Pecen videz: trak, velikost okvirja v tekslih in pravokotnik ploskve v blokih glede na noge. */
    public static final class Entry {
        public final long hash;
        public final int slot;
        public final int tw;
        public final int th;
        /** Sirina ploskve (bloki), sredina na osi entitete. */
        public final float width;
        /** Spodnji rob ploskve glede na noge (bloki). */
        public final float bottom;
        public final float height;

        Entry(long hash, int slot, int tw, int th, float width, float bottom, float height) {
            this.hash = hash;
            this.slot = slot;
            this.tw = tw;
            this.th = th;
            this.width = width;
            this.bottom = bottom;
            this.height = height;
        }
    }

    public final int size;
    final ImpostorSlots slots;
    private final Map<Long, Entry> entries = new HashMap<Long, Entry>();
    private int texture = -1;

    ImpostorAtlas(int size) {
        this.size = size;
        this.slots = new ImpostorSlots(ImpostorFrames.slots(size));
    }

    /** Veljavna stranica atlasa iz stikala {@code RwImpostorAtlas} (1024 / 2048 / 4096, sicer 2048). */
    public static int sanitize(int size) {
        return size == 1024 || size == 4096 ? size : 2048;
    }

    public int textureId() {
        if (this.texture < 0) {
            this.texture = GlStateManager.generateTexture();
            GlStateManager.bindTexture(this.texture);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, ImpostorFrames.LEVELS - 1);
            GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0f);
            GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, ImpostorFrames.LEVELS - 1);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            // Brez zacetne vsebine: vsak trak se ob peki zapise cel (z robovi) na vseh stopnjah, ostanka
            // atlasa izven trakov nihce ne vzorci.
            for (int l = 0; l < ImpostorFrames.LEVELS; ++l) {
                GlStateManager.glTexImage2D(GL11.GL_TEXTURE_2D, l, GL11.GL_RGBA, this.size >> l, this.size >> l, 0,
                        GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
            }
        }
        return this.texture;
    }

    public int count() {
        return this.entries.size();
    }

    public boolean contains(long hash) {
        return this.entries.containsKey(hash);
    }

    /** Pecen videz ali {@code null}; steje kot uporaba (LRU). */
    public Entry get(long hash) {
        Entry e = this.entries.get(hash);
        if (e != null) {
            this.slots.touch(hash);
        }
        return e;
    }

    /**
     * Zapise vse okvirje in stopnje videza ({@code cells[kot][stopnja]}, ARGB, celica
     * {@link ImpostorFrames#CELL_W} x {@link ImpostorFrames#CELL_H} na stopnji 0) v trak; ce je atlas poln,
     * izrine najdlje neuporabljen videz.
     */
    Entry store(long hash, int[][][] cells, int tw, int th, float width, float bottom, float height) {
        int slot = this.slots.allocate(hash);
        if (this.slots.lastEvicted != 0L) {
            this.entries.remove(this.slots.lastEvicted);
        }
        GlStateManager.bindTexture(textureId());
        GlStateManager.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        int sx = ImpostorFrames.slotX(slot, this.size);
        int sy = ImpostorFrames.slotY(slot, this.size);
        IntBuffer buf = BufferUtils.createIntBuffer(ImpostorFrames.CELL_W * ImpostorFrames.CELL_H);
        for (int k = 0; k < ImpostorFrames.ANGLES; ++k) {
            for (int l = 0; l < ImpostorFrames.LEVELS; ++l) {
                buf.clear();
                buf.put(cells[k][l]);
                buf.flip();
                GlStateManager.glTexSubImage2D(GL11.GL_TEXTURE_2D, l, (sx + k * ImpostorFrames.CELL_W) >> l, sy >> l,
                        ImpostorFrames.CELL_W >> l, ImpostorFrames.CELL_H >> l, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
            }
        }
        Entry e = new Entry(hash, slot, tw, th, width, bottom, height);
        this.entries.put(hash, e);
        return e;
    }

    /** UV okvirja {@code k}: {@code {u0, v0, u1, v1}}; {@code v0} je zgornji rob slike. */
    public float[] uv(Entry e, int k) {
        int x = ImpostorFrames.slotX(e.slot, this.size) + k * ImpostorFrames.CELL_W + ImpostorFrames.frameX(e.tw);
        int y = ImpostorFrames.slotY(e.slot, this.size) + ImpostorFrames.frameY(e.th);
        float s = this.size;
        return new float[] {x / s, y / s, (x + e.tw) / s, (y + e.th) / s};
    }

    void delete() {
        if (this.texture >= 0) {
            GlStateManager.deleteTexture(this.texture);
            this.texture = -1;
        }
        this.entries.clear();
        this.slots.clear();
    }
}
