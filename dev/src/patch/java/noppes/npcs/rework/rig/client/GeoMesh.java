package noppes.npcs.rework.rig.client;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import noppes.npcs.rework.rig.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * A Bedrock .geo.json model (bones, boxes with per-face UVs) drawn with a Pose.
 * The maths matches tools/anim/rig.py and render.py exactly ("Gecko space":
 * Bedrock x mirrored, the model faces -Z, its right hand is at +X), so the
 * preview pictures show what the game draws.  Units are model pixels.
 */
@SideOnly(Side.CLIENT)
public final class GeoMesh {
    // Corner order per face: top-left, top-right, bottom-right, bottom-left of the UV rectangle;
    // each corner picks x, y, z from (lo = 0, hi = 1).  Same table as render.py FACES.
    private static final String[] FACE_NAMES = {"north", "south", "east", "west", "up", "down"};
    private static final int[][][] FACE_CORNERS = {
            {{1, 1, 0}, {0, 1, 0}, {0, 0, 0}, {1, 0, 0}},
            {{0, 1, 1}, {1, 1, 1}, {1, 0, 1}, {0, 0, 1}},
            {{1, 1, 1}, {1, 1, 0}, {1, 0, 0}, {1, 0, 1}},
            {{0, 1, 0}, {0, 1, 1}, {0, 0, 1}, {0, 0, 0}},
            {{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}},
            {{0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {0, 0, 0}},
    };

    private final List<Bone> roots = new ArrayList<>();
    private final Map<String, Bone> bones = new HashMap<>();

    public GeoMesh(ResourceLocation location) {
        this(read(location));
    }

    /** Parses an already loaded "minecraft:geometry" entry (also used by tests). */
    public GeoMesh(JsonObject geo) {
        JsonObject desc = geo.getAsJsonObject("description");
        float texW = desc.get("texture_width").getAsFloat();
        float texH = desc.get("texture_height").getAsFloat();
        List<Bone> order = new ArrayList<>();
        for (JsonElement el : geo.getAsJsonArray("bones")) {
            Bone bone = new Bone(el.getAsJsonObject(), texW, texH);
            bones.put(bone.name, bone);
            order.add(bone);
        }
        for (Bone bone : order) {
            Bone parent = bone.parent != null ? bones.get(bone.parent) : null;
            if (parent != null) parent.children.add(bone);
            else roots.add(bone);
        }
    }

    private static JsonObject read(ResourceLocation location) {
        try (InputStreamReader reader = new InputStreamReader(
                Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream(), StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot load model " + location, e);
        }
    }

    public static JsonObject parse(java.io.Reader reader) {
        return new JsonParser().parse(reader).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    public java.util.Set<String> boneNames() {
        return java.util.Collections.unmodifiableSet(bones.keySet());
    }

    /** Number of drawn quads (all bones). */
    public int quadCount() {
        int n = 0;
        for (Bone b : bones.values()) n += b.quads.length / 32;
        return n;
    }

    public boolean hasBone(String name) {
        return bones.containsKey(name);
    }

    /** Draws the model in the current GL frame (model pixels, Y up, facing -Z). */
    public void render(Pose pose) {
        Tessellator tessellator = Tessellator.getInstance();
        for (Bone root : roots) root.render(pose, tessellator);
    }

    private static float[] vec(JsonObject o, String key, float x, float y, float z) {
        if (!o.has(key)) return new float[]{x, y, z};
        JsonArray a = o.getAsJsonArray(key);   // uv and uv_size have 2 values, the rest 3
        float[] out = {x, y, z};
        for (int i = 0; i < Math.min(3, a.size()); i++) out[i] = a.get(i).getAsFloat();
        return out;
    }

    private static final class Bone {
        final String name;
        final String parent;
        final float[] pivot;     // Gecko space
        final float[] bindRot;   // Gecko space, degrees
        final List<Bone> children = new ArrayList<>();
        final float[] quads;     // per vertex: x y z u v nx ny nz
        private int displayList = -1;   // compiled on first draw, like vanilla ModelRenderer

        Bone(JsonObject json, float texW, float texH) {
            name = json.get("name").getAsString();
            parent = json.has("parent") ? json.get("parent").getAsString() : null;
            float[] p = vec(json, "pivot", 0, 0, 0);
            pivot = new float[]{-p[0], p[1], p[2]};
            float[] r = vec(json, "rotation", 0, 0, 0);
            bindRot = new float[]{-r[0], -r[1], r[2]};
            List<Float> data = new ArrayList<>();
            if (json.has("cubes")) {
                for (JsonElement c : json.getAsJsonArray("cubes")) addCube(c.getAsJsonObject(), texW, texH, data);
            }
            quads = new float[data.size()];
            for (int i = 0; i < quads.length; i++) quads[i] = data.get(i);
        }

        private static void addCube(JsonObject cube, float texW, float texH, List<Float> out) {
            float[] o = vec(cube, "origin", 0, 0, 0);
            float[] s = vec(cube, "size", 0, 0, 0);
            float inf = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0;
            float[] lo = {-(o[0] + s[0]) - inf, o[1] - inf, o[2] - inf};
            float[] hi = {-o[0] + inf, o[1] + s[1] + inf, o[2] + s[2] + inf};
            float[][] local = cubeMatrix(cube);
            JsonObject uv = cube.getAsJsonObject("uv");
            for (int f = 0; f < 6; f++) {
                if (uv == null || !uv.has(FACE_NAMES[f])) continue;
                JsonObject face = uv.getAsJsonObject(FACE_NAMES[f]);
                float[] uv0 = vec(face, "uv", 0, 0, 0);
                float[] size = vec(face, "uv_size", 0, 0, 0);
                float[][] pts = new float[4][];
                for (int k = 0; k < 4; k++) {
                    int[] c = FACE_CORNERS[f][k];
                    pts[k] = transform(local, c[0] == 1 ? hi[0] : lo[0], c[1] == 1 ? hi[1] : lo[1], c[2] == 1 ? hi[2] : lo[2]);
                }
                // Outward normal (the mirrored x flips the corner winding, hence the minus).
                float[] a = sub(pts[1], pts[0]), b = sub(pts[3], pts[0]);
                float nx = -(a[1] * b[2] - a[2] * b[1]);
                float ny = -(a[2] * b[0] - a[0] * b[2]);
                float nz = -(a[0] * b[1] - a[1] * b[0]);
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len < 1e-6F) continue;
                float[][] uvs = {{uv0[0], uv0[1]}, {uv0[0] + size[0], uv0[1]},
                        {uv0[0] + size[0], uv0[1] + size[1]}, {uv0[0], uv0[1] + size[1]}};
                for (int k = 0; k < 4; k++) {
                    out.add(pts[k][0]); out.add(pts[k][1]); out.add(pts[k][2]);
                    out.add(uvs[k][0] / texW); out.add(uvs[k][1] / texH);
                    out.add(nx / len); out.add(ny / len); out.add(nz / len);
                }
            }
        }

        /** 3x4 matrix of an optionally rotated cube, in Gecko space. */
        private static float[][] cubeMatrix(JsonObject cube) {
            float[][] m = {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}};
            if (!cube.has("rotation")) return m;
            float[] r = vec(cube, "rotation", 0, 0, 0);
            float[] p = vec(cube, "pivot", 0, 0, 0);
            float[] piv = {-p[0], p[1], p[2]};
            double rx = Math.toRadians(-r[0]), ry = Math.toRadians(-r[1]), rz = Math.toRadians(r[2]);
            // R = Rz * Ry * Rx
            double cx = Math.cos(rx), sx = Math.sin(rx), cy = Math.cos(ry), sy = Math.sin(ry), cz = Math.cos(rz), sz = Math.sin(rz);
            double[][] rot = {
                    {cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx},
                    {sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx},
                    {-sy, cy * sx, cy * cx}};
            for (int i = 0; i < 3; i++) {
                double t = piv[i];
                for (int j = 0; j < 3; j++) {
                    m[i][j] = (float) rot[i][j];
                    t -= rot[i][j] * piv[j];
                }
                m[i][3] = (float) t;
            }
            return m;
        }

        private void draw(Tessellator tessellator) {
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
            for (int i = 0; i < quads.length; i += 8) {
                buffer.pos(quads[i], quads[i + 1], quads[i + 2]).tex(quads[i + 3], quads[i + 4])
                        .normal(quads[i + 5], quads[i + 6], quads[i + 7]).endVertex();
            }
            tessellator.draw();
        }

        private static float[] transform(float[][] m, float x, float y, float z) {
            return new float[]{
                    m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3],
                    m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3],
                    m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3]};
        }

        private static float[] sub(float[] a, float[] b) {
            return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
        }

        void render(Pose pose, Tessellator tessellator) {
            if (Pose.isProp(name) && (!pose.has(name) || pose.get(name)[Pose.SCALE] < 0.01F)) return;
            float[] ch = pose.get(name);
            GlStateManager.pushMatrix();
            // Same order as rig.py: T(position) T(pivot) Rz Ry Rx S T(-pivot).
            GlStateManager.translate(-ch[Pose.POS], ch[Pose.POS + 1], ch[Pose.POS + 2]);
            GlStateManager.translate(pivot[0], pivot[1], pivot[2]);
            float rz = bindRot[2] + ch[Pose.ROT + 2];
            float ry = bindRot[1] - ch[Pose.ROT + 1];
            float rx = bindRot[0] - ch[Pose.ROT];
            if (rz != 0) GlStateManager.rotate(rz, 0, 0, 1);
            if (ry != 0) GlStateManager.rotate(ry, 0, 1, 0);
            if (rx != 0) GlStateManager.rotate(rx, 1, 0, 0);
            if (ch[Pose.SCALE] != 1 || ch[Pose.SCALE + 1] != 1 || ch[Pose.SCALE + 2] != 1) {
                GlStateManager.scale(ch[Pose.SCALE], ch[Pose.SCALE + 1], ch[Pose.SCALE + 2]);
            }
            GlStateManager.translate(-pivot[0], -pivot[1], -pivot[2]);
            if (quads.length > 0) {
                if (displayList < 0) {
                    displayList = GLAllocation.generateDisplayLists(1);
                    GlStateManager.glNewList(displayList, GL11.GL_COMPILE);
                    draw(tessellator);
                    GlStateManager.glEndList();
                }
                GlStateManager.callList(displayList);
            }
            for (Bone child : children) child.render(pose, tessellator);
            GlStateManager.popMatrix();
        }
    }
}
