package noppes.npcs.rework.script.hooks;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

/** Explicit M6.2 fixture entry point only (java-hooks-run.ps1). Drives the real EventHooks
 * with unspawned NPCs, so no natural tick adds events, and compares JS and Java traces.
 */
public final class HookTraceProbe {
    private HookTraceProbe() { }

    static final String JS_ONE = "var inits=0;\n"
            + "function t(e,s){var d=e.npc.getTempdata();d.put('t',(d.has('t')?d.get('t'):'')+s+';');}\n"
            + "function init(e){inits++;t(e,'1:init:'+inits);}\n"
            + "function tick(e){t(e,'1:tick');}\n"
            + "function interact(e){t(e,'1:interact:'+e.isCanceled());e.setCanceled(true);}\n"
            + "function timer(e){t(e,'1:timer:'+e.id);}\n";
    static final String JS_TWO =
            "function t(e,s){var d=e.npc.getTempdata();d.put('t',(d.has('t')?d.get('t'):'')+s+';');}\n"
            + "function init(e){t(e,'2:init');}\n"
            + "function tick(e){t(e,'2:tick');}\n"
            + "function interact(e){t(e,'2:interact:'+e.isCanceled());}\n"
            + "function timer(e){t(e,'2:timer:'+e.id);}\n";
    private static final String TRACE = " static void t(NpcEvent e, String s) { IData d = e.npc.getTempdata();"
            + " d.put(\"t\", (d.has(\"t\") ? d.get(\"t\") : \"\") + s + \";\"); }\n";
    static final String JAVA_ONE = "package m62;\nimport noppes.npcs.api.entity.data.IData;\nimport noppes.npcs.api.event.NpcEvent;\n"
            + "public class TabOne {\n static int inits;\n" + TRACE
            + " public void init(NpcEvent.InitEvent e) { inits++; t(e, \"1:init:\" + inits); }\n"
            + " public void tick(NpcEvent.UpdateEvent e) { t(e, \"1:tick\"); }\n"
            + " public void interact(NpcEvent.InteractEvent e) { t(e, \"1:interact:\" + e.isCanceled()); e.setCanceled(true); }\n"
            + " public void timer(NpcEvent.TimerEvent e) { t(e, \"1:timer:\" + e.id); }\n"
            + " public void helper(int unrelated) { }\n}\n";
    static final String JAVA_TWO = "package m62;\nimport noppes.npcs.api.entity.data.IData;\nimport noppes.npcs.api.event.NpcEvent;\n"
            + "public class TabTwo {\n" + TRACE
            + " public void init(NpcEvent e) { t(e, \"2:init\"); }\n"
            + " public void tick(NpcEvent.UpdateEvent e) { t(e, \"2:tick\"); }\n"
            + " public void interact(NpcEvent.InteractEvent e) { t(e, \"2:interact:\" + e.isCanceled()); }\n"
            + " public void timer(NpcEvent.TimerEvent e) { t(e, \"2:timer:\" + e.id); }\n}\n";
    static final String JAVA_SIGNATURE = "package m62;\npublic class Sig {\n public void tick(String e) { }\n}\n";
    static final String JAVA_SYNTAX = "package m62;\npublic class Syntax {\n public void tick(Object e) { int x = ; }\n}\n";
    static final String JAVA_THROW = "package m62;\nimport noppes.npcs.api.event.NpcEvent;\npublic class Throw {\n"
            + " public void init(NpcEvent.InitEvent e) { e.npc.getTempdata().put(\"t\", \"1:init:1;\"); }\n"
            + " public void tick(NpcEvent.UpdateEvent e) { throw new IllegalStateException(\"m62 fixture\"); }\n}\n";

    static final String GOLDEN = "1:init:1;2:init;bus:init;1:tick;2:tick;bus:tick;1:tick;2:tick;bus:tick;"
            + "1:interact:false;2:interact:true;bus:interact:true;ret:true;1:timer:7;2:timer:7;bus:timer;";
    static final String GOLDEN_RELOAD = "1:init:1;2:init;bus:init;1:tick;2:tick;bus:tick;";
    static final String BUS_ONLY = "bus:init;bus:tick;bus:tick;bus:interact:false;ret:false;bus:timer;";

    /** Records the API bus post that EventHooks makes after the scripts, for the probe NPCs only. */
    public static final class Bus {
        final Map<Object, Boolean> npcs = new IdentityHashMap<>();
        @SubscribeEvent(receiveCanceled = true)
        public void on(NpcEvent e) {
            if (!npcs.containsKey(e.npc.getMCEntity())) { return; }
            ScriptHooks.Hook hook = ScriptHooks.npcByEvent(e.getClass());
            String name = hook == null ? e.getClass().getSimpleName() : hook.name;
            mark(e.npc, "bus:" + name + (name.equals("interact") ? ":" + e.isCanceled() : ""));
        }
    }

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
            EntityNPCInterface java = npc(world, ScriptHooks.JAVA, JAVA_ONE, JAVA_TWO);
            EntityNPCInterface signature = npc(world, ScriptHooks.JAVA, JAVA_SIGNATURE);
            EntityNPCInterface syntax = npc(world, ScriptHooks.JAVA, JAVA_SYNTAX);
            EntityNPCInterface thrower = npc(world, ScriptHooks.JAVA, JAVA_THROW);
            EntityNPCInterface[] all = { js, java, signature, syntax, thrower };
            for (EntityNPCInterface npc : all) { bus.npcs.put(npc, Boolean.TRUE); }
            NpcAPI.Instance().events().register(bus);
            busRegistered = true;
            check(checks, "registry-16-npc-hooks", ScriptHooks.npc().size() == 16);
            for (EntityNPCInterface npc : all) { drive(npc, player); }
            String jsTrace = trace(js), javaTrace = trace(java);
            traces.put("js", jsTrace); traces.put("java", javaTrace);
            check(checks, "js-golden-trace", GOLDEN.equals(jsTrace));
            check(checks, "java-equals-js", javaTrace.equals(jsTrace));
            check(checks, "java-language-kept", ScriptHooks.JAVA.equals(java.script.getLanguage()) && "ECMAScript".equals(js.script.getLanguage()));
            check(checks, "java-tabs-not-errored", !java.script.getScripts().get(0).errored && !java.script.getScripts().get(1).errored);

            traces.put("signature", trace(signature)); traces.put("signatureConsole", console(signature));
            check(checks, "signature-error-not-missing", signature.script.getScripts().get(0).errored && console(signature).contains("hook.signature"));
            check(checks, "signature-bus-unchanged", BUS_ONLY.equals(trace(signature)));
            traces.put("syntax", trace(syntax)); traces.put("syntaxConsole", console(syntax));
            check(checks, "syntax-file-line", syntax.script.getScripts().get(0).errored && console(syntax).contains("Syntax.java:3:"));
            check(checks, "syntax-bus-unchanged", BUS_ONLY.equals(trace(syntax)));
            traces.put("throw", trace(thrower)); traces.put("throwConsole", console(thrower));
            check(checks, "runtime-file-line", thrower.script.getScripts().get(0).errored && console(thrower).contains("Throw.java:5"));
            check(checks, "errored-tab-stops", ("1:init:1;" + BUS_ONLY).equals(trace(thrower)));
            check(checks, "current-cleared", ScriptContainer.Current == null);

            // A script reload: both languages re-init once before the next event with fresh script state.
            for (EntityNPCInterface npc : new EntityNPCInterface[] { js, java }) { npc.wrappedNPC.getTempdata().remove("t"); }
            ScriptController.Instance.lastLoaded = ScriptController.Instance.lastLoaded + 1;
            EventHooks.onNPCTick(js);
            EventHooks.onNPCTick(java);
            traces.put("jsReload", trace(js)); traces.put("javaReload", trace(java));
            check(checks, "js-reload-golden", GOLDEN_RELOAD.equals(trace(js)));
            check(checks, "java-reload-equals-js", trace(java).equals(trace(js)));
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

    private static EntityNPCInterface npc(WorldServer world, String language, String... tabs) {
        EntityCustomNpc npc = new EntityCustomNpc(world);
        npc.setPosition(0.5, 4, 0.5);
        NBTTagList list = new NBTTagList();
        for (String code : tabs) {
            NBTTagCompound tab = new NBTTagCompound();
            tab.setString("Script", code);
            tab.setTag("Console", new NBTTagList());
            tab.setTag("ScriptList", new NBTTagList());
            list.appendTag(tab);
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Scripts", list);
        data.setString("ScriptLanguage", language);
        data.setBoolean("ScriptEnabled", true);
        npc.script.readFromNBT(data);
        return npc;
    }

    private static void drive(EntityNPCInterface npc, EntityPlayer player) {
        EventHooks.onNPCTick(npc);
        EventHooks.onNPCTick(npc);
        mark(npc.wrappedNPC, "ret:" + EventHooks.onNPCInteract(npc, player));
        EventHooks.onNPCTimer(npc, 7);
    }

    static void mark(ICustomNpc npc, String s) {
        IData d = npc.getTempdata();
        d.put("t", (d.has("t") ? d.get("t") : "") + s + ";");
    }

    private static String trace(EntityNPCInterface npc) {
        Object t = npc.wrappedNPC.getTempdata().get("t");
        return t == null ? "" : t.toString();
    }

    private static String console(EntityNPCInterface npc) {
        StringBuilder out = new StringBuilder();
        for (ScriptContainer tab : npc.script.getScripts()) {
            for (String line : tab.console.values()) { out.append(line).append('\n'); }
        }
        return out.toString();
    }

    private static void check(List<Map<String, Object>> checks, String key, boolean ok) {
        Map<String, Object> check = new LinkedHashMap<>(); check.put("key", key); check.put("ok", ok); checks.add(check);
    }
}
