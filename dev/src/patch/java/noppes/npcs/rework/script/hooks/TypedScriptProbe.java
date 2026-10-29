package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.*;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;

/** Explicit M6.3 fixture entry point only (java-hooks-run.ps1 -Package m63). Same real EventHooks
 * drive as the M6.2 probe; the Java tabs extend the typed base NpcScript and use the existing API.
 */
public final class TypedScriptProbe {
    private TypedScriptProbe() { }

    private static final String HEAD = "package m63;\nimport noppes.npcs.api.entity.data.IData;\nimport noppes.npcs.api.event.NpcEvent;\n"
            + "import noppes.npcs.rework.script.NpcScript;\n";
    private static final String TRACE = " static void t(NpcEvent e, String s) { IData d = e.npc.getTempdata();"
            + " d.put(\"t\", (d.has(\"t\") ? d.get(\"t\") : \"\") + s + \";\"); }\n";
    static final String TYPED_ONE = HEAD + "public class TypedOne extends NpcScript {\n int inits;\n" + TRACE
            + " @Override public void init(NpcEvent.InitEvent e) { inits++; t(e, \"1:init:\" + inits); }\n"
            + " @Override public void tick(NpcEvent.UpdateEvent e) { t(e, \"1:tick\"); }\n"
            + " @Override public void interact(NpcEvent.InteractEvent e) { t(e, \"1:interact:\" + e.isCanceled()); e.setCanceled(true); }\n"
            + " @Override public void timer(NpcEvent.TimerEvent e) { t(e, \"1:timer:\" + e.id);"
            + " e.npc.getDisplay().setName(\"m63:\" + e.id); log(\"m63 timer \" + e.id); }\n}\n";
    static final String TYPED_TWO = HEAD + "public class TypedTwo extends NpcScript {\n" + TRACE
            + " @Override public void init(NpcEvent.InitEvent e) { t(e, \"2:init\"); }\n"
            + " @Override public void tick(NpcEvent.UpdateEvent e) { t(e, \"2:tick\"); }\n"
            + " @Override public void interact(NpcEvent.InteractEvent e) { t(e, \"2:interact:\" + e.isCanceled()); }\n"
            + " @Override public void timer(NpcEvent.TimerEvent e) { t(e, \"2:timer:\" + e.id); }\n}\n";
    /** Overrides only timer: the no-op defaults must not mark, cancel or break the bus. */
    static final String TYPED_TIMER_ONLY = HEAD + "public class TimerOnly extends NpcScript {\n" + TRACE
            + " @Override public void timer(NpcEvent.TimerEvent e) { t(e, \"x:timer:\" + e.id); }\n}\n";
    static final String WRONG_OVERRIDE = "package m63;\nimport noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class Wrong extends NpcScript {\n @Override public void tick(NpcEvent.TimerEvent e) { }\n}\n";
    static final String WRONG_OVERLOAD = "package m63;\nimport noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class Overload extends NpcScript {\n public void tick(NpcEvent.TimerEvent e) { }\n}\n";

    static final String TIMER_ONLY = "bus:init;bus:tick;bus:tick;bus:interact:false;ret:false;x:timer:7;bus:timer;";

    public static boolean run(ICustomNpc control, String output, String mode) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> traces = new LinkedHashMap<>();
        proof.put("mode", mode); proof.put("checks", checks); proof.put("traces", traces); proof.put("success", false);
        Bus bus = new Bus();
        boolean busRegistered = false;
        try {
            WorldServer world = control.getWorld().getMCWorld();
            FakePlayer player = FakePlayerFactory.getMinecraft(world);
            // Creative skips the faction lookup; the probe NPCs are neither attacking nor killed.
            player.capabilities.isCreativeMode = true;
            EntityNPCInterface js = npc(world, "ECMAScript", JS_ONE, JS_TWO);
            EntityNPCInterface typed = npc(world, ScriptHooks.JAVA, TYPED_ONE, TYPED_TWO);
            EntityNPCInterface plain = npc(world, ScriptHooks.JAVA, JAVA_ONE, JAVA_TWO);
            EntityNPCInterface timerOnly = npc(world, ScriptHooks.JAVA, TYPED_TIMER_ONLY);
            EntityNPCInterface override = npc(world, ScriptHooks.JAVA, WRONG_OVERRIDE);
            EntityNPCInterface overload = npc(world, ScriptHooks.JAVA, WRONG_OVERLOAD);
            EntityNPCInterface[] all = { js, typed, plain, timerOnly, override, overload };
            for (EntityNPCInterface npc : all) { bus.npcs.put(npc, Boolean.TRUE); }
            NpcAPI.Instance().events().register(bus);
            busRegistered = true;
            for (EntityNPCInterface npc : all) { drive(npc, player); }
            String jsTrace = trace(js), typedTrace = trace(typed);
            traces.put("js", jsTrace); traces.put("typed", typedTrace); traces.put("typedConsole", console(typed));
            check(checks, "js-golden-trace", GOLDEN.equals(jsTrace));
            check(checks, "typed-equals-js", typedTrace.equals(jsTrace));
            check(checks, "typed-tabs-not-errored", !typed.script.getScripts().get(0).errored && !typed.script.getScripts().get(1).errored);
            traces.put("typedName", typed.wrappedNPC.getDisplay().getName());
            check(checks, "typed-api-call", "m63:7".equals(typed.wrappedNPC.getDisplay().getName()));
            check(checks, "typed-log-console", console(typed).contains("m63 timer 7"));
            traces.put("plain", trace(plain));
            check(checks, "plain-class-unchanged", GOLDEN.equals(trace(plain)));
            traces.put("timerOnly", trace(timerOnly));
            check(checks, "defaults-not-called", TIMER_ONLY.equals(trace(timerOnly)) && !timerOnly.script.getScripts().get(0).errored);

            traces.put("override", trace(override)); traces.put("overrideConsole", console(override));
            check(checks, "override-wrong-type-compile-error", override.script.getScripts().get(0).errored
                    && console(override).contains("Wrong.java:5:") && console(override).contains("compiler.err.method.does.not.override.superclass"));
            check(checks, "override-bus-unchanged", BUS_ONLY.equals(trace(override)));
            traces.put("overload", trace(overload)); traces.put("overloadConsole", console(overload));
            check(checks, "overload-signature-error", overload.script.getScripts().get(0).errored && console(overload).contains("hook.signature"));
            check(checks, "overload-bus-unchanged", BUS_ONLY.equals(trace(overload)));
            check(checks, "current-cleared", ScriptContainer.Current == null);

            // A script reload: both languages re-init once before the next event with fresh script state.
            for (EntityNPCInterface npc : new EntityNPCInterface[] { js, typed }) { npc.wrappedNPC.getTempdata().remove("t"); }
            ScriptController.Instance.lastLoaded = ScriptController.Instance.lastLoaded + 1;
            EventHooks.onNPCTick(js);
            EventHooks.onNPCTick(typed);
            traces.put("jsReload", trace(js)); traces.put("typedReload", trace(typed));
            check(checks, "js-reload-golden", GOLDEN_RELOAD.equals(trace(js)));
            check(checks, "typed-reload-equals-js", trace(typed).equals(trace(js)));
            boolean allOk = true;
            for (Map<String, Object> c : checks) { allOk &= Boolean.TRUE.equals(c.get("ok")); }
            proof.put("success", allOk);
            return allOk;
        } catch (Throwable failure) {
            proof.put("error", failure.toString());
            failure.printStackTrace();
            return false;
        } finally {
            if (busRegistered) { NpcAPI.Instance().events().unregister(bus); }
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
    }
}
