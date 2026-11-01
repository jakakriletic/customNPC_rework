package noppes.npcs.rework.anim;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.world.World;
import noppes.npcs.LogWriter;

/** P7b (D-098, Q274, Q282): client side of {@link IAnimator#blendTo}. The coremod
 * ({@code rework.core.AnimatedModelPatch}, only with RwAnimHooks>=1 on this client) passes every pose
 * the optional mod's {@code AnimatedModel.buildPose} returns through {@link #pose}. A blend message
 * binds to the action token; on the first frame of that action the last drawn pose is copied and the
 * new pose is mixed in linearly over the blend ticks (action age + frame fraction). Props come from the
 * new pose unblended; death, another action, a new blend or an unload end the transition. Without a
 * previously drawn pose the new pose is used as is. Render thread only; no optional-mod compile types. */
public final class AnimBlend {
    /** A message whose action does not arrive within this many client world ticks is dropped. */
    static final int PENDING_TICKS = 40;

    /** Harness observation of one returned pose; built is a copy before blending (only with a probe). */
    public interface Probe {
        void sample(Entity npc, int token, int age, float partial, float weight, String state,
                Map<String, float[]> from, Map<String, float[]> built, Map<String, float[]> out);
    }

    static final class State {
        final int token, ticks;
        final long received;
        Map<String, float[]> from;
        /** A running blend replaced by this one, drawn on until this action arrives. */
        State prev;
        State(int token, int ticks, long received) { this.token = token; this.ticks = ticks; this.received = received; }
    }

    private static final Map<Entity, State> STATES = new WeakHashMap<>();
    private static final Map<Entity, Map<String, float[]>> LAST = new WeakHashMap<>();
    private static volatile Probe probe;
    private static boolean failed;
    static int accepted, rejected;

    private AnimBlend() { }

    private static final ClassValue<Binding> BINDINGS = new ClassValue<Binding>() {
        @Override protected Binding computeValue(Class<?> type) {
            for (Class<?> base = type; base != null; base = base.getSuperclass()) {
                if (!base.getName().equals(Animators.ANIMATED)) { continue; }
                try { return new Binding(base); }
                catch (ReflectiveOperationException | RuntimeException failure) {
                    LogWriter.error("RWANIM incompatible optional animation blend API", failure);
                    return null;
                }
            }
            return null;
        }
    };

    static final class Binding {
        final Method age;
        final DataParameter<?> action;
        Binding(Class<?> base) throws ReflectiveOperationException {
            age = base.getMethod("getActionAge");
            Field f = base.getDeclaredField("ACTION");
            f.setAccessible(true);
            action = (DataParameter<?>) f.get(null);
            if (age.getReturnType() != int.class) { throw new NoSuchMethodException("Unsupported getActionAge"); }
        }
        int token(Entity npc) { return ((Number) npc.getDataManager().get(action)).intValue(); }
        int age(Entity npc) throws ReflectiveOperationException { return ((Number) age.invoke(npc)).intValue(); }
    }

    /** Raw action token ({@code sequence << 8 | action}) of an animated NPC, or 0 when unsupported. */
    public static int token(Entity npc) {
        Binding b = BINDINGS.get(npc.getClass());
        return b == null ? 0 : b.token(npc);
    }

    public static void setProbe(Probe p) { probe = p; }

    /** Messages accepted / rejected by this client since start (harness data). */
    public static int[] counts() { synchronized (AnimBlend.class) { return new int[]{accepted, rejected}; } }

    /** Client main thread: a decoded blend message for this client's world. */
    static synchronized void accept(World world, BlendCodec.Message m) {
        Entity npc = world == null || world.provider.getDimension() != m.dimension ? null : world.getEntityByID(m.entityId);
        if (npc == null || BINDINGS.get(npc.getClass()) == null || !new UUID(m.most, m.least).equals(npc.getUniqueID())) {
            rejected++;
            return;
        }
        State old = STATES.get(npc), next = new State(m.token, m.ticks, world.getTotalWorldTime());
        // The message may come before the new action: keep drawing a running blend meanwhile.
        if (old != null && old.from != null) { next.prev = old; }
        else if (old != null) { next.prev = old.prev; }
        STATES.put(npc, next);
        accepted++;
    }

    /** Client world unload or disconnect: forget all transitions and drawn poses. */
    static synchronized void clear() { STATES.clear(); LAST.clear(); }

    /** Called by the patched {@code AnimatedModel.buildPose} right before it returns. Never throws. */
    public static Object pose(Object pose, Object npc, float partial) {
        if (failed || !(npc instanceof EntityLivingBase)) { return pose; }
        try { return apply(pose, (EntityLivingBase) npc, partial); }
        catch (Throwable failure) {
            failed = true;
            LogWriter.error("RWANIM pose blending stopped", failure instanceof Exception ? (Exception) failure : new RuntimeException(failure));
            return pose;
        }
    }

    @SuppressWarnings("unchecked")
    private static synchronized Object apply(Object pose, EntityLivingBase npc, float partial) throws ReflectiveOperationException {
        Binding b = BINDINGS.get(npc.getClass());
        if (b == null) { return pose; }
        Map<String, float[]> bones = (Map<String, float[]>) BONES.get(pose.getClass()).get(pose);
        State st = STATES.get(npc);
        Probe p = probe;
        Map<String, float[]> built = p == null ? null : PoseBlend.copy(bones);
        int token = b.token(npc), age = b.age(npc);
        float w = -1;
        String state = "none";
        Map<String, float[]> from = null;
        if (st != null) {
            if (npc.deathTime > 0) { STATES.remove(npc); state = "death"; }
            else if (token == st.token) {
                if (st.from == null) {
                    Map<String, float[]> last = LAST.get(npc);
                    if (last == null) { STATES.remove(npc); state = "no-previous"; st = null; }
                    else { st.from = PoseBlend.copy(last); }
                }
                if (st != null) {
                    w = PoseBlend.weight(age, partial, st.ticks);
                    from = st.from;
                    PoseBlend.blend(from, bones, w);
                    state = w >= 1 ? "done" : "blend";
                    if (w >= 1) { STATES.remove(npc); }
                }
            } else if (st.from != null) { STATES.remove(npc); state = "interrupted"; }
            else if (npc.world.getTotalWorldTime() - st.received > PENDING_TICKS) { STATES.remove(npc); state = "expired"; }
            else if (st.prev != null && token == st.prev.token) {
                w = PoseBlend.weight(age, partial, st.prev.ticks);
                from = st.prev.from;
                PoseBlend.blend(from, bones, w);
                state = "blend-prev";
                if (w >= 1) { st.prev = null; }
            } else { state = "pending"; }
        }
        LAST.put(npc, bones);
        if (p != null) { p.sample(npc, token, age, partial, w, state, from, built, bones); }
        return pose;
    }

    private static final ClassValue<Field> BONES = new ClassValue<Field>() {
        @Override protected Field computeValue(Class<?> type) {
            try {
                Field f = type.getField("bones");
                if (!Map.class.isAssignableFrom(f.getType())) { throw new IllegalStateException("Pose.bones is no map"); }
                return f;
            } catch (NoSuchFieldException failure) { throw new IllegalStateException("Pose without bones", failure); }
        }
    };
}
