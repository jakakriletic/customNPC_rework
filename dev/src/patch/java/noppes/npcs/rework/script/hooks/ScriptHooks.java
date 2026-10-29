package noppes.npcs.rework.script.hooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import noppes.npcs.api.event.DialogEvent;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.rework.anim.AnimationEvent;
import org.apache.commons.lang3.StringUtils;

/** M6.2 (D-090, Q128): deterministic list of script hooks and their event types.
 * Metadata only; EventHooks, the API event bus and the JS engines stay as they are.
 * Source: every {@code npc.script.runScript} call in EventHooks (16 NPC hooks).
 */
public final class ScriptHooks {
    /** Value of the existing NBT key ScriptLanguage that selects the Java branch of the NPC handler. */
    public static final String JAVA = "Java";
    /** Handlers whose Java branch is outside v1 (Q92); their JS hooks stay the original. */
    public static final List<String> V1_EXCLUDED = Collections.unmodifiableList(Arrays.asList(
            "player", "block", "door", "item", "projectile", "forge"));

    public static final class Hook {
        /** Null for a named hook outside EnumScriptType (M7.5 animation hooks). */
        public final EnumScriptType type;
        public final String name;
        /** Static type EventHooks passes; a hook parameter must accept it. */
        public final Class<?> event;
        Hook(EnumScriptType type, Class<?> event) { this.type = type; this.name = type.function; this.event = event; }
        Hook(String name, Class<?> event) { this.type = null; this.name = name; this.event = event; }
        @Override public String toString() { return name + "(" + event.getName() + ")"; }
    }

    private static final List<Hook> NPC;
    /** M7.5 (Q194): dispatched by rework.anim.AnimationHooks only with RwAnimHooks=1; not EventHooks. */
    private static final List<Hook> ANIMATION = Collections.unmodifiableList(Arrays.asList(
            new Hook("animationStart", AnimationEvent.Started.class),
            new Hook("animationEnd", AnimationEvent.Ended.class),
            new Hook("animationEvent", AnimationEvent.ClipEvent.class)));
    private static final List<Hook> JAVA_HOOKS;
    static {
        Map<EnumScriptType, Class<?>> npc = new EnumMap<>(EnumScriptType.class);
        npc.put(EnumScriptType.INIT, NpcEvent.InitEvent.class);
        npc.put(EnumScriptType.TICK, NpcEvent.UpdateEvent.class);
        npc.put(EnumScriptType.INTERACT, NpcEvent.InteractEvent.class);
        npc.put(EnumScriptType.DIALOG, DialogEvent.OpenEvent.class);
        npc.put(EnumScriptType.DAMAGED, NpcEvent.DamagedEvent.class);
        npc.put(EnumScriptType.DIED, NpcEvent.DiedEvent.class);
        npc.put(EnumScriptType.ATTACK_MELEE, NpcEvent.MeleeAttackEvent.class);
        npc.put(EnumScriptType.TARGET, NpcEvent.TargetEvent.class);
        npc.put(EnumScriptType.COLLIDE, NpcEvent.CollideEvent.class);
        npc.put(EnumScriptType.KILL, NpcEvent.KilledEntityEvent.class);
        npc.put(EnumScriptType.DIALOG_OPTION, DialogEvent.OptionEvent.class);
        npc.put(EnumScriptType.TARGET_LOST, NpcEvent.TargetLostEvent.class);
        npc.put(EnumScriptType.ROLE, RoleEvent.class);
        npc.put(EnumScriptType.RANGED_LAUNCHED, NpcEvent.RangedLaunchedEvent.class);
        npc.put(EnumScriptType.TIMER, NpcEvent.TimerEvent.class);
        npc.put(EnumScriptType.DIALOG_CLOSE, DialogEvent.CloseEvent.class);
        List<Hook> list = new ArrayList<>();
        // EnumMap iterates in enum ordinal order, so the list never depends on put order.
        for (Map.Entry<EnumScriptType, Class<?>> entry : npc.entrySet()) { list.add(new Hook(entry.getKey(), entry.getValue())); }
        NPC = Collections.unmodifiableList(list);
        List<Hook> java = new ArrayList<>(NPC);
        java.addAll(ANIMATION);
        JAVA_HOOKS = Collections.unmodifiableList(java);
    }

    private ScriptHooks() { }

    public static List<Hook> npc() { return NPC; }

    public static Hook npc(String name) {
        for (Hook hook : NPC) { if (hook.name.equals(name)) { return hook; } }
        return null;
    }

    public static List<Hook> animation() { return ANIMATION; }

    /** Every hook an NPC Java tab can bind: the 16 EventHooks hooks, then the animation hooks. */
    public static List<Hook> java() { return JAVA_HOOKS; }

    public static Hook java(String name) {
        for (Hook hook : JAVA_HOOKS) { if (hook.name.equals(name)) { return hook; } }
        return null;
    }

    /** NPC hook whose static event type is exactly {@code eventClass}, or null. */
    public static Hook npcByEvent(Class<?> eventClass) {
        for (Hook hook : NPC) { if (hook.event == eventClass) { return hook; } }
        return null;
    }

    /** String hooks outside the enum: EventHooks.onForgeEvent names the forge handler
     * function after the event class. Same rule, so registry and runtime cannot disagree. */
    public static String forgeHookName(Class<?> eventClass) {
        String name = eventClass.getName();
        int i = name.lastIndexOf(".");
        return StringUtils.uncapitalize(name.substring(i + 1).replace("$", ""));
    }
}
