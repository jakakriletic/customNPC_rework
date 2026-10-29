package noppes.npcs.rework.anim;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.LogWriter;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

/** Opt-in script entry point; no compile dependency on the optional entities mod.
 * Merely loading this class or calling of() never registers a tick listener or changes an entity.
 * Existing NPC classes, scripting dispatch, packets and NBT are untouched. */
public final class Animators {
    private Animators() { }
    private static final Map<EntityNPCInterface, Handle> HANDLES = new WeakHashMap<>();
    private static final Set<Handle> PENDING = new LinkedHashSet<>();
    private static QueueTicks listener;
    private static final IAnimator ABSENT = new IAnimator() {
        public boolean isAvailable() { return false; }
        public boolean play(String clip) { return false; }
        public boolean queue(String clip) { return false; }
        public boolean stop() { return false; }
        public String getClip() { return ""; }
        public int getTick() { return 0; }
        public String[] getClips() { return new String[0]; }
    };
    private static final ClassValue<Binding> BINDINGS = new ClassValue<Binding>() {
        @Override protected Binding computeValue(Class<?> type) {
            for (Class<?> base = type; base != null; base = base.getSuperclass()) {
                if (!base.getName().equals("dev.jakak.customnpcentities.entity.EntityAnimatedNpc")) { continue; }
                try { return new Binding(base); }
                catch (ReflectiveOperationException | SecurityException failure) {
                    LogWriter.error("RWANIM incompatible optional animation API", failure);
                    return null;
                }
            }
            return null;
        }
    };

    /** Returns a non-null animator. Ordinary NPCs, null and a missing/incompatible optional mod
     * return an unavailable, inert handle. All handles for the same entity share one queue. */
    public static synchronized IAnimator of(ICustomNpc<?> npc) {
        Object raw = npc == null ? null : npc.getMCEntity();
        if (!(raw instanceof EntityNPCInterface)) { return ABSENT; }
        EntityNPCInterface entity = (EntityNPCInterface) raw;
        Binding binding = BINDINGS.get(entity.getClass());
        if (binding == null) { return ABSENT; }
        Handle handle = HANDLES.get(entity);
        if (handle == null) { handle = new Handle(entity, binding); HANDLES.put(entity, handle); }
        return handle;
    }

    private static synchronized void update(Handle handle) {
        if (handle.queue.hasPending()) { PENDING.add(handle); }
        else { PENDING.remove(handle); }
        if (!PENDING.isEmpty() && listener == null) {
            listener = new QueueTicks(); MinecraftForge.EVENT_BUS.register(listener);
        } else if (PENDING.isEmpty() && listener != null) {
            MinecraftForge.EVENT_BUS.unregister(listener); listener = null;
        }
    }

    private static final class Binding {
        final Method play, action, age, clip, library, names, stop;
        Binding(Class<?> base) throws ReflectiveOperationException {
            play = base.getMethod("playClip", String.class);
            action = base.getMethod("getAction"); age = base.getMethod("getActionAge");
            clip = base.getMethod("clipForAction", int.class); library = base.getMethod("animations");
            names = library.getReturnType().getMethod("sortedClipNames");
            stop = base.getDeclaredMethod("stopAction"); stop.setAccessible(true);
            if (play.getReturnType() != boolean.class || action.getReturnType() != int.class
                    || age.getReturnType() != int.class || clip.getReturnType() != String.class
                    || stop.getReturnType() != void.class || !List.class.isAssignableFrom(names.getReturnType())) {
                throw new NoSuchMethodException("Unsupported animation API return types");
            }
            for (Method method : new Method[]{play, action, age, clip, library, names, stop}) {
                if (Modifier.isStatic(method.getModifiers())) { throw new NoSuchMethodException("Expected instance method " + method); }
            }
        }
        Object call(Method method, Object target, Object... args) {
            try { return method.invoke(target, args); }
            catch (IllegalAccessException | InvocationTargetException failure) {
                Throwable cause = failure instanceof InvocationTargetException ? failure.getCause() : failure;
                throw new IllegalStateException("RWANIM " + method.getName() + " failed", cause);
            }
        }
    }

    private static final class Handle implements IAnimator {
        // Values in HANDLES/PENDING must never retain the weak map's entity key or its world.
        final WeakReference<EntityNPCInterface> target;
        final Binding binding;
        final ClipQueue queue;
        Handle(EntityNPCInterface target, Binding binding) {
            this.target = new WeakReference<>(target); this.binding = binding;
            queue = new ClipQueue(new ClipQueue.Driver() {
                public boolean writable() { return Handle.this.writable(); }
                public boolean playing() { return Handle.this.playing(); }
                public String[] clips() { return getClips(); }
                public boolean play(String name) { return start(name); }
                public void stop() { binding.call(binding.stop, live()); }
            });
        }
        EntityNPCInterface live() {
            EntityNPCInterface npc = target.get();
            return npc == null || npc.isDead || !npc.isEntityAlive() ? null : npc;
        }
        public boolean isAvailable() { return live() != null; }
        public boolean writable() {
            EntityNPCInterface npc = live();
            return npc != null && npc.world instanceof WorldServer && !npc.world.isRemote
                    && ((WorldServer) npc.world).isCallingFromMinecraftThread();
        }
        public boolean playing() {
            EntityNPCInterface npc = live();
            return npc != null && ((Number) binding.call(binding.action, npc)).intValue() != 0;
        }
        // Driver and public API use different play/stop entry points to keep queue policy centralized.
        boolean start(String name) { return Boolean.TRUE.equals(binding.call(binding.play, live(), name)); }
        public boolean play(String name) { boolean ok = queue.play(name); update(this); return ok; }
        public boolean queue(String name) { boolean ok = queue.queue(name); update(this); return ok; }
        public boolean stop() { boolean ok = queue.stop(); update(this); return ok; }
        public String getClip() {
            EntityNPCInterface npc = live();
            if (npc == null) { return ""; }
            int action = ((Number) binding.call(binding.action, npc)).intValue();
            return action == 0 ? "" : (String) binding.call(binding.clip, npc, action);
        }
        public int getTick() {
            EntityNPCInterface npc = live();
            return npc == null || !playing() ? 0 : ((Number) binding.call(binding.age, npc)).intValue();
        }
        public String[] getClips() {
            EntityNPCInterface npc = live();
            if (npc == null) { return new String[0]; }
            Object lib = binding.call(binding.library, npc);
            List<?> names = (List<?>) binding.call(binding.names, lib);
            return names.toArray(new String[0]);
        }
    }

    /** Registered only while explicit queue() calls have pending work. */
    public static final class QueueTicks {
        private QueueTicks() { }
        @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world.isRemote) { return; }
            synchronized (Animators.class) {
                for (Handle h : new ArrayList<>(PENDING)) {
                    EntityNPCInterface npc = h.live();
                    if (npc == null) { h.queue.clear(); }
                    else if (npc.world != event.world) { continue; }
                    else if (event.world.getEntityByID(npc.getEntityId()) != npc) { h.queue.clear(); }
                    else {
                        try { h.queue.tick(); }
                        catch (RuntimeException failure) { h.queue.clear(); LogWriter.error("RWANIM queue cancelled", failure); }
                    }
                    update(h);
                }
            }
        }
        @SubscribeEvent public void unload(WorldEvent.Unload event) {
            synchronized (Animators.class) {
                for (Handle h : new ArrayList<>(PENDING)) {
                    EntityNPCInterface npc = h.target.get();
                    if (npc == null || npc.world == event.getWorld()) { h.queue.clear(); update(h); }
                }
            }
        }
    }
}
