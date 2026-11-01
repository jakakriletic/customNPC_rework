package noppes.npcs.rework.anim;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.anim.ActionJournal.Step;
import noppes.npcs.rework.core.AnimatedNpcPatch;

/** M7.5 (D-095, Q194, Q203-Q205), P7a (D-098, Q277-Q279): RwAnimHooks=1 dispatches animationStart,
 * animationEvent and animationEnd for the actions of every EntityAnimatedNpc of customnpcentities
 * 0.7.0 from an exact journal the instrumented entity writes (AnimJournal) and drains at world tick
 * END; 2 adds the idle, walk and death layers (Locomotion). Without the instrumented class (pattern
 * mismatch) the hooks stay off with an error; there is no inexact polling fallback. Registered only
 * with the switch on; with 0 no listener, no journal and no dispatch exist. No compile dependency on
 * the optional mod. */
public final class AnimationHooks {
    enum Record { REPLACE, START, STOP, NATURAL, EVENT }

    private static AnimationHooks installed;
    private final boolean layers;
    private final List<Entry> tracked = new ArrayList<>();
    private final Map<EntityNPCInterface, Entry> index = new WeakHashMap<>();

    private AnimationHooks(boolean layers) { this.layers = layers; }

    /** @param mode RwAnimHooks: 1 actions, 2 actions and idle/walk/death layers */
    public static synchronized void install(int mode) {
        if (installed != null || mode < 1) { return; }
        installed = new AnimationHooks(mode >= 2);
        AnimJournal.attach(installed);
        MinecraftForge.EVENT_BUS.register(installed);
    }

    private static final ClassValue<Binding> BINDINGS = new ClassValue<Binding>() {
        @Override protected Binding computeValue(Class<?> type) {
            for (Class<?> base = type; base != null; base = base.getSuperclass()) {
                if (!base.getName().equals(Animators.ANIMATED)) { continue; }
                if (!AnimatedNpcPatch.patched()) {
                    LogWriter.error("RWANIM incompatible optional animation hook API: exact journal not installed (rwcore.animpatch="
                            + System.getProperty(AnimatedNpcPatch.PROP_PATCH) + "); animation hooks are off");
                    return null;
                }
                try { return new Binding(base); }
                catch (ReflectiveOperationException | RuntimeException failure) {
                    LogWriter.error("RWANIM incompatible optional animation hook API", failure);
                    return null;
                }
            }
            return null;
        }
    };

    private static final class Binding {
        final Method action, age, clipFor, library, clipOrNull, walk, idle, death;
        Binding(Class<?> base) throws ReflectiveOperationException {
            action = base.getMethod("getAction"); age = base.getMethod("getActionAge");
            clipFor = base.getMethod("clipForAction", int.class); library = base.getMethod("animations");
            clipOrNull = library.getReturnType().getMethod("clipOrNull", String.class);
            walk = base.getMethod("walkClip"); idle = base.getMethod("idleClip"); death = base.getMethod("deathClip");
            if (action.getReturnType() != int.class || age.getReturnType() != int.class || clipFor.getReturnType() != String.class) {
                throw new NoSuchMethodException("Unsupported animation action API");
            }
        }
        int action(Object npc) throws ReflectiveOperationException { return ((Number) action.invoke(npc)).intValue(); }
        int age(Object npc) throws ReflectiveOperationException { return ((Number) age.invoke(npc)).intValue(); }
        String clip(Object npc, int action) throws ReflectiveOperationException { return (String) clipFor.invoke(npc, action); }
        Object clipOrNull(Object npc, Method name) throws ReflectiveOperationException {
            return clipOrNull.invoke(library.invoke(npc), (String) name.invoke(npc));
        }
    }

    /** Reflective view of one AnimClip for the layer clocks; members resolved once per clip class. */
    private static final class ClipView implements Locomotion.Clip {
        private static final ClassValue<Method[]> MEMBERS = new ClassValue<Method[]>() {
            @Override protected Method[] computeValue(Class<?> type) {
                try { return new Method[]{type.getMethod("lengthTicks"), type.getMethod("eventsBetween", float.class, float.class, List.class)}; }
                catch (NoSuchMethodException failure) { throw new IllegalStateException("RWANIM unsupported clip API", failure); }
            }
        };
        final Object raw;
        final String name;
        final float length, stride;
        final boolean loop;
        final int lengthTicks;
        final Method between;
        final List<Object> scratch = new ArrayList<>();
        ClipView(Object raw) throws ReflectiveOperationException {
            this.raw = raw;
            Method[] members = MEMBERS.get(raw.getClass());
            Class<?> c = raw.getClass();
            name = (String) c.getField("name").get(raw);
            length = c.getField("length").getFloat(raw);
            stride = c.getField("stride").getFloat(raw);
            loop = c.getField("loop").getBoolean(raw);
            lengthTicks = ((Number) members[0].invoke(raw)).intValue();
            between = members[1];
        }
        public String name() { return name; }
        public float length() { return length; }
        public int lengthTicks() { return lengthTicks; }
        public float stride() { return stride; }
        public boolean loop() { return loop; }
        public void eventsBetween(float from, float to, List<HookTracker.Fired> out) {
            scratch.clear();
            try {
                between.invoke(raw, from, to, scratch);
                for (Object e : scratch) { out.add(fired(e)); }
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("RWANIM clip events failed", failure); }
        }
    }

    static HookTracker.Fired fired(Object e) throws ReflectiveOperationException {
        Class<?> c = e.getClass();
        return new HookTracker.Fired((String) c.getField("type").get(e), c.getField("time").getFloat(e), e.toString());
    }

    private static final class Entry implements Locomotion.Clips {
        final WeakReference<EntityNPCInterface> npc;
        final Binding binding;
        final ActionJournal journal = new ActionJournal();
        final Locomotion locomotion = new Locomotion();
        ClipView walk, idle, death;
        Entry(EntityNPCInterface npc, Binding binding) { this.npc = new WeakReference<>(npc); this.binding = binding; }
        ClipView view(ClipView old, Method name) throws ReflectiveOperationException {
            Object raw = binding.clipOrNull(npc.get(), name);
            return raw == null ? null : old != null && old.raw == raw ? old : new ClipView(raw);
        }
        void refresh() throws ReflectiveOperationException {
            walk = view(walk, binding.walk); idle = view(idle, binding.idle); death = view(death, binding.death);
        }
        public Locomotion.Clip walk() { return walk; }
        public Locomotion.Clip idle() { return idle; }
        public Locomotion.Clip death() { return death; }
    }

    private Entry entry(EntityNPCInterface npc) {
        Entry entry = index.get(npc);
        if (entry != null) { return entry; }
        Binding binding = BINDINGS.get(npc.getClass());
        if (binding == null) { return null; }
        entry = new Entry(npc, binding);
        index.put(npc, entry); tracked.add(entry);
        return entry;
    }

    /** Called from AnimJournal inside the entities mod; records only, never throws. */
    void record(Object raw, Record kind, int action, Object event) {
        if (!(raw instanceof EntityNPCInterface) || ((Entity) raw).world == null || ((Entity) raw).world.isRemote) { return; }
        EntityNPCInterface npc = (EntityNPCInterface) raw;
        try {
            Entry entry = entry(npc);
            if (entry == null) { return; }
            Binding b = entry.binding;
            switch (kind) {
                case REPLACE: case STOP: {
                    int current = b.action(npc);
                    if (current != 0) { entry.journal.end(b.clip(npc, current), b.age(npc)); }
                    break;
                }
                case START: {
                    int current = b.action(npc);
                    if (current != 0) { entry.journal.start(b.clip(npc, current)); }
                    break;
                }
                case NATURAL: entry.journal.natural(); break;
                default: {
                    HookTracker.Fired f = fired(event);
                    entry.journal.event(b.clip(npc, action), b.age(npc), f.type, f.time, f.data);
                }
            }
        } catch (RuntimeException | ReflectiveOperationException failure) {
            forget(index.get(npc), npc);
            LogWriter.error("RWANIM hooks stopped for " + npc.getName(), failure);
        }
    }

    @SubscribeEvent public void join(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityNPCInterface)) { return; }
        entry((EntityNPCInterface) event.getEntity());
    }

    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) { return; }
        // Scripts may spawn or remove entities while hooks run; iterate a snapshot.
        for (Entry entry : tracked.toArray(new Entry[0])) {
            EntityNPCInterface npc = entry.npc.get();
            if (npc == null || npc.isDead) { forget(entry, npc); continue; }
            if (npc.world != event.world) { continue; }
            try { observe(entry, npc); }
            catch (RuntimeException | ReflectiveOperationException failure) {
                forget(entry, npc);
                LogWriter.error("RWANIM hooks stopped for " + npc.getName(), failure);
            }
        }
    }

    @SubscribeEvent public void unload(WorldEvent.Unload event) {
        for (Entry entry : tracked.toArray(new Entry[0])) {
            EntityNPCInterface npc = entry.npc.get();
            if (npc == null || npc.world == event.getWorld()) { forget(entry, npc); }
        }
    }

    private void forget(Entry entry, EntityNPCInterface npc) {
        if (entry != null) { tracked.remove(entry); }
        if (npc != null) { index.remove(npc); }
    }

    private void observe(Entry entry, EntityNPCInterface npc) throws ReflectiveOperationException {
        List<Step> steps = entry.journal.drain();
        int dropped = entry.journal.takeDropped();
        if (dropped > 0) { LogWriter.error("RWANIM journal overflow for " + npc.getName() + ": " + dropped + " steps dropped (limit " + ActionJournal.LIMIT + ")"); }
        if (layers) {
            entry.refresh();
            List<Step> all = new ArrayList<>(steps);
            entry.locomotion.observe(entry, entry.binding.action(npc) != 0, npc.deathTime, npc.limbSwing, npc.limbSwingAmount, npc.ticksExisted, all);
            steps = all;
        }
        if (steps.isEmpty() || !npc.script.isEnabled()) { return; }
        // Hooks that start or stop clips write the journal again; those steps go to the next drain.
        for (Step step : steps) {
            switch (step.kind) {
                case START: npc.script.runScript("animationStart", new AnimationEvent.Started(npc.wrappedNPC, step.clip, step.source)); break;
                case END: npc.script.runScript("animationEnd", new AnimationEvent.Ended(npc.wrappedNPC, step.clip, step.tick, step.finished, step.source)); break;
                default: npc.script.runScript("animationEvent", new AnimationEvent.ClipEvent(npc.wrappedNPC, step.clip, step.tick,
                        step.type, step.time, step.data, step.source));
            }
        }
    }
}
