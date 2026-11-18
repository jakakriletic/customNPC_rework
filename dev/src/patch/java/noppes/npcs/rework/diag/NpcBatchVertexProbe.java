package noppes.npcs.rework.diag;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.FloatBuffer;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.rework.render.PartGeometry;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** Diagnostic transform feedback, never a replacement for the production image test.
 * Called on the actual native part stack. Four basis inputs expose the built-in MVP
 * independently of the geometry. Compare ftransform, built-in MVP, captured uniform
 * clip and split projection/model-view products before rasterization. The small probe
 * program is not the production shader: matching it alone is not package acceptance. */
final class NpcBatchVertexProbe implements AutoCloseable {
    private final int program;

    NpcBatchVertexProbe() {
        if (!GLContext.getCapabilities().OpenGL30) { throw new IllegalStateException("Vertex diagnosis needs GL 3.0 transform feedback"); }
        int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        int p = GL20.glCreateProgram();
        try {
            GL20.glShaderSource(shader, "#version 120\n"
                + "invariant gl_Position;\nuniform mat4 rwClip;\n"
                + "varying vec4 rwFixed, rwBuiltin, rwUniform, rwSplit;\n"
                + "void main() {\n"
                + "rwFixed = ftransform();\n"
                + "rwBuiltin = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
                + "rwUniform = rwClip * gl_Vertex;\n"
                + "rwSplit = gl_ProjectionMatrix * (gl_ModelViewMatrix * gl_Vertex);\n"
                + "gl_Position = rwFixed;\n}\n");
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("Vertex probe compile: " + GL20.glGetShaderInfoLog(shader, 4096));
            }
            GL20.glAttachShader(p, shader);
            GL30.glTransformFeedbackVaryings(p, new CharSequence[] {"rwFixed", "rwBuiltin", "rwUniform", "rwSplit"}, GL30.GL_INTERLEAVED_ATTRIBS);
            GL20.glLinkProgram(p);
            if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("Vertex probe link: " + GL20.glGetProgramInfoLog(p, 4096));
            }
            program = p;
        } catch (RuntimeException error) { GL20.glDeleteProgram(p); throw error; }
        finally { GL20.glDeleteShader(shader); }
    }

    JsonObject capture(ModelScaleRenderer part, float scale, float[] clip) {
        PartGeometry geometry = PartGeometry.capture(part, scale);
        if (geometry == null) { throw new IllegalStateException("Unsupported probe geometry"); }
        int count = geometry.vertices + 4;
        FloatBuffer positions = BufferUtils.createFloatBuffer(count * 4);
        for (int v = 0; v < geometry.vertices; v++) {
            positions.put(geometry.bytes.getFloat(v * 24)).put(geometry.bytes.getFloat(v * 24 + 4))
                .put(geometry.bytes.getFloat(v * 24 + 8)).put(1);
        }
        for (int i = 0; i < 4; i++) { for (int j = 0; j < 4; j++) { positions.put(i == j ? 1 : 0); } }
        positions.flip();
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), array = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int generic = GL11.glGetInteger(GL30.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING);
        int indexed = GL30.glGetInteger(GL30.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING, 0);
        boolean discard = GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD), begun = false;
        int buffer = GL15.glGenBuffers();
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL11.glVertexPointer(4, 0, positions); GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY); GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            for (int unit = 0; unit < 8; unit++) {
                GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + unit); GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            }
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, buffer);
            GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, (long) count * 16 * 4, GL15.GL_STREAM_READ);
            GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, buffer);
            GL20.glUseProgram(program);
            FloatBuffer uniform = BufferUtils.createFloatBuffer(16); uniform.put(clip).flip();
            GL20.glUniformMatrix4(GL20.glGetUniformLocation(program, "rwClip"), false, uniform);
            GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
            GL30.glBeginTransformFeedback(GL11.GL_POINTS); begun = true;
            GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
            GL30.glEndTransformFeedback(); begun = false;
            FloatBuffer values = BufferUtils.createFloatBuffer(count * 16);
            GL15.glGetBufferSubData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, values);
            float[] raw = new float[count * 16]; values.get(raw);
            JsonObject r = new JsonObject(); r.addProperty("vertices", geometry.vertices); r.addProperty("basisInputs", 4);
            r.addProperty("layout", "per input: fixed[4], builtinMvp[4], uniformClip[4], split[4]; geometry then four basis vectors");
            float[] input = new float[count * 4]; positions.duplicate().get(input);
            r.add("inputs", new Gson().toJsonTree(input)); r.add("outputs", new Gson().toJsonTree(raw));
            r.add("clip", new Gson().toJsonTree(clip));
            for (int variant = 1; variant < 4; variant++) {
                int different = 0, basisDifferent = 0; float max = 0;
                for (int v = 0; v < count; v++) { for (int k = 0; k < 4; k++) {
                    float a = raw[v * 16 + k], b = raw[v * 16 + variant * 4 + k];
                    if (!Float.isFinite(a) || !Float.isFinite(b)) { throw new IllegalStateException("Non-finite vertex feedback"); }
                    if (Float.floatToRawIntBits(a) != Float.floatToRawIntBits(b)) {
                        if (v < geometry.vertices) { different++; } else { basisDifferent++; }
                    }
                    max = Math.max(max, Math.abs(a - b));
                } }
                JsonObject diff = new JsonObject(); diff.addProperty("geometryComponentsDifferent", different);
                diff.addProperty("basisComponentsDifferent", basisDifferent); diff.addProperty("maxAbsolute", max);
                r.add(new String[] {"", "builtinVsFixed", "uniformVsFixed", "splitVsFixed"}[variant], diff);
            }
            r.addProperty("gl", GL11.glGetError());
            return r;
        } finally {
            if (begun) { GL30.glEndTransformFeedback(); }
            if (!discard) { GL11.glDisable(GL30.GL_RASTERIZER_DISCARD); }
            GL20.glUseProgram(oldProgram);
            GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, indexed);
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, generic);
            GL11.glPopClientAttrib(); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, array);
            GL15.glDeleteBuffers(buffer);
        }
    }

    @Override public void close() { GL20.glDeleteProgram(program); }
}
