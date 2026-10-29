package noppes.npcs.rework.anim;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.datasync.DataParameter;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/** M7.5 (D-095, Q194, Q203-Q205): RwAnimHooks=1 observes the server action of every EntityAnimatedNpc
 * of customnpcentities 0.7.0 once per world tick END and dispatches animationStart, animationEvent and
 * animationEnd to the NPC's script handler (JS and Java). Registered only with the switch on; with 0
 * no listener, no tracking and no new dispatch exist. No compile dependency on the optional mod. */
public final class AnimationHooks {
    private static AnimationHooks installed;
    private final List<Entry> tracked = new ArrayList<>();
    private final Map<EntityNPCInterface, Entry> index = new WeakHashMap<>();

    private AnimationHooks() { }

    public static synchronized void install() {
        if (installed != null) { return; }
        installed = new AnimationHooks();
        MinecraftForge.EVENT_BUS.register(installed);
    }

    private static final ClassValue<Binding> BINDINGS = new ClassValue<Binding>() {
        @Override protected Binding computeValue(Class<?> type) {
            for (Class<?> base = type; base != null; base = base.getSuperclass()) {
                if (!base.getName().equals(Animators.ANIMATED)) { continue; }
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
        final DataParameter<Integer> action;
        final Method age, clip;
        Binding(Class<?> base) throws ReflectiveOperationException {
            Field field = base.getDeclaredField("ACTION");
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != DataParameter.class) { throw new NoSuchFieldException("Unsupported ACTION parameter"); }
            field.setAccessible(true);
            @SuppressWarnings("unchecked") DataParameter<Integer> param = (DataParameter<Integer>) field.get(null);
            action = param;
            age = base.getMethod("getActionAge"); clip = base.getMethod("getActionClip");
            if (age.getReturnType() != int.class) { throw new NoSuchMethodException("Unsupported getActionAge"); }
        }
    }

    /** Reflective view of one AnimClip; members are resolved once per clip class. */
    private static final class ClipView implements HookTracker.Clip {
        private static final ClassValue<Method[]> MEMBERS = new ClassValue<Method[]>() {
            @Override protected Method[] computeValue(Class<?> type) {
                try { return new Method[]{type.getMethod("lengthTicks"), type.getMethod("eventsBetween", float.class, float.class, List.class)}; }
                catch (NoSuchMethodException failure) { throw new IllegalStateException("RWANIM unsupported clip API", failure); }
            }
        };
        final Object raw;
        final String name;
        final float length;
        final int lengthTicks;
        final Method between;
        final List<Object> scratch = new ArrayList<>();
        ClipView(Object raw) throws ReflectiveOperationException {
            this.raw = raw;
            Method[] members = MEMBERS.get(raw.getClass());
            name = (String) raw.getClass().getField("name").get(raw);
            length = raw.getClass().getField("length").getFloat(raw);
            lengthTicks = ((Number) members[0].invoke(raw)).intValue();
            between = members[1];
        }
        public String name() { return name; }
        public int lengthTicks() { return lengthTicks; }
        public void fired(int age, List<HookTracker.Fired> out) {
            // Same window as EntityAnimatedNpc.tickAnimation for the step from age-1 to age.
            float from = age - 1 == 0 ? -1 : (age - 1) / 20F;
            float to = Math.min(age / 20F, length);
            scratch.clear();
            try {
                between.invoke(raw, from, to, scratch);
                for (Object e : scratch) {
                    Class<?> c = e.getClass();
                    out.add(new HookTracker.Fired((String) c.getField("type").get(e), c.getField("time").getFloat(e), e.toString()));
                }
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("RWANIM clip events failed", failure); }
        }
    }

    private static final class Entry {
        final WeakReference<EntityNPCInterface> npc;
        final Binding binding;
        final HookTracker tracker = new HookTracker();
        ClipView view;
        Entry(EntityNPCInterface npc, Binding binding) { this.npc = new WeakReference<>(npc); this.binding = binding; }
    }

    @SubscribeEvent public void join(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityNPCInterface)) { return; }
        EntityNPCInterface npc = (EntityNPCInterface) event.getEntity();
        Binding binding = BINDINGS.get(npc.getClass());
        if (binding == null || index.containsKey(npc)) { return; }
        Entry entry = new Entry(npc, binding);
        index.put(npc, entry); tracked.add(entry);
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
        tracked.remove(entry);
        if (npc != null) { index.remove(npc); }
    }

    private static void observe(Entry entry, EntityNPCInterface npc) throws ReflectiveOperationException {
        int raw = npc.getDataManager().get(entry.binding.action);
        Object clip = (raw & 0xFF) == 0 ? null : entry.binding.clip.invoke(npc);
        if (clip == null) { entry.view = null; }
        else if (entry.view == null || entry.view.raw != clip) { entry.view = new ClipView(clip); }
        int age = ((Number) entry.binding.age.invoke(npc)).intValue();
        List<HookTracker.Step> steps = entry.tracker.observe(raw, entry.view, age);
        if (steps.isEmpty() || !npc.script.isEnabled()) { return; }
        for (HookTracker.Step step : steps) {
            switch (step.kind) {
                case START: npc.script.runScript("animationStart", new AnimationEvent.Started(npc.wrappedNPC, step.clip)); break;
                case END: npc.script.runScript("animationEnd", new AnimationEvent.Ended(npc.wrappedNPC, step.clip, step.tick, step.finished)); break;
                default: npc.script.runScript("animationEvent", new AnimationEvent.ClipEvent(npc.wrappedNPC, step.clip, step.tick,
                        step.event.type, step.event.time, step.event.data));
            }
        }
    }
}
