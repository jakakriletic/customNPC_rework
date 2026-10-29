package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.*;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.wrapper.ContainerCustomChestWrapper;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.containers.ContainerCustomChest;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;

/** Explicit M6.6 fixture only. Real script dispatch and original binary chest/projectile wrappers.
 * FakePlayer replaces only GUI transport: no client or rendered chest is claimed. Never spawned.
 * Before a fix, success means that the loss of ownership was reproduced in all eight cases.
 */
public final class ContextProbe {
    private ContextProbe() { }
    private static EntityNPCInterface outer, inner;
    private static WorldServer world;
    private static PlayerWrapper<ChestPlayer> player;
    private static List<Map<String, Object>> observations;
    private static boolean throwing;
    private static boolean repeatAndThrow;

    public static final class ChestPlayer extends FakePlayer {
        public ChestPlayer(WorldServer world) {
            super(world, new GameProfile(UUID.fromString("06600000-0000-0000-0000-000000000001"), "M66_Chest"));
        }
        @Override public void openGui(Object mod, int id, World world, int rows, int y, int z) {
            this.openContainer = new ContainerCustomChest(this, rows);
        }
    }

    private static String script(String language, boolean outside) {
        String call = outside ? "outside" : "inside";
        if (ScriptHooks.JAVA.equals(language)) {
            return "package m66; import noppes.npcs.api.event.NpcEvent; public class "
                    + (outside ? "Outer" : "Inner") + " { public void tick(NpcEvent.UpdateEvent e) { "
                    + ContextProbe.class.getName() + "." + call + "(); } }";
        }
        return "function tick(e) { Java.type('" + ContextProbe.class.getName() + "')." + call + "(); }";
    }

    public static void outside() {
        observe("before", outer.script.getScripts().get(0));
        EventHooks.onNPCTick(inner);
        observe("after", outer.script.getScripts().get(0));
        if (repeatAndThrow) {
            // A stopped inner tab returns at its guard on the second call.
            EventHooks.onNPCTick(inner);
            observe("after-second", outer.script.getScripts().get(0));
            throw new IllegalStateException("m66 outer fixture");
        }
    }

    public static void inside() {
        observe("inner", inner.script.getScripts().get(0));
        if (throwing) { throw new IllegalStateException("m66 nested fixture"); }
    }

    private static void observe(String phase, ScriptContainer expected) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("phase", phase);
        row.put("owner", ScriptContainer.Current == expected ? "expected" : ScriptContainer.Current == null ? "null" : "other");
        // showChestGui itself captures Current before the close/open calls and assigns the real wrapper.
        ContainerCustomChestWrapper chest = (ContainerCustomChestWrapper) player.showChestGui(1);
        row.put("chest", chest.script == expected ? "expected" : chest.script == null ? "null" : "other");
        EntityProjectile projectile = new EntityProjectile(world);
        String error = "";
        try {
            ((noppes.npcs.api.entity.IProjectile) NpcAPI.Instance().getIEntity(projectile)).enableEvents();
            // Calling twice must not register two callbacks.
            ((noppes.npcs.api.entity.IProjectile) NpcAPI.Instance().getIEntity(projectile)).enableEvents();
        } catch (RuntimeException e) { error = e.getMessage(); }
        row.put("projectile", projectile.scripts.size() == 1 && projectile.scripts.get(0) == expected ? "expected"
                : projectile.scripts.isEmpty() ? "empty" : "other");
        row.put("projectileError", error);
        observations.add(row);
    }

    public static boolean run(ICustomNpc control, String output, String mode, boolean fixed) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        List<Map<String, Object>> cases = new ArrayList<>();
        List<Map<String, Object>> extraCases = new ArrayList<>();
        proof.put("mode", mode); proof.put("expectedFixed", fixed); proof.put("checks", checks);
        proof.put("cases", cases); proof.put("success", false);
        proof.put("extraCases", extraCases);
        ScriptContainer prior = ScriptContainer.Current;
        try {
            world = control.getWorld().getMCWorld();
            player = new PlayerWrapper<ChestPlayer>(new ChestPlayer(world)) {
                // No network connection: suppress only close GUI transport, not showChestGui ownership.
                @Override public void closeGui() { }
            };
            repeatAndThrow = false;
            for (String a : new String[] { "ECMAScript", ScriptHooks.JAVA }) {
                for (String b : new String[] { "ECMAScript", ScriptHooks.JAVA }) {
                    for (boolean fail : new boolean[] { false, true }) {
                        throwing = fail;
                        observations = new ArrayList<>();
                        outer = npc(world, a, script(a, true)); inner = npc(world, b, script(b, false));
                        ScriptContainer.Current = null;
                        EventHooks.onNPCTick(outer);
                        String id = a + "->" + b + (fail ? ":throw" : ":return");
                        Map<String, Object> c = new LinkedHashMap<>();
                        c.put("id", id); c.put("observations", observations);
                        c.put("outerConsole", console(outer)); c.put("innerConsole", console(inner)); cases.add(c);
                        check(checks, id + ":three-phases", observations.size() == 3);
                        if (observations.size() == 3) {
                            check(checks, id + ":before-owner", owns(observations.get(0)));
                            check(checks, id + ":inner-owner", owns(observations.get(1)));
                            Map<String, Object> after = observations.get(2);
                            check(checks, id + ":after-owner", fixed ? owns(after)
                                    : "null".equals(after.get("owner")) && "null".equals(after.get("chest"))
                                    && "empty".equals(after.get("projectile"))
                                    && String.valueOf(after.get("projectileError")).contains("Can only be called during scripts"));
                        }
                        check(checks, id + ":error-contained", !outer.script.getScripts().get(0).errored
                                && inner.script.getScripts().get(0).errored == fail
                                && (!fail || console(inner).contains("m66 nested fixture")));
                        check(checks, id + ":top-level-cleared", ScriptContainer.Current == null);
                    }
                }
            }
            if (fixed) {
                repeatAndThrow = true;
                for (String a : new String[] { "ECMAScript", ScriptHooks.JAVA }) {
                    for (String b : new String[] { "ECMAScript", ScriptHooks.JAVA }) {
                        for (boolean fail : new boolean[] { false, true }) {
                            throwing = fail;
                            observations = new ArrayList<>();
                            outer = npc(world, a, script(a, true)); inner = npc(world, b, script(b, false));
                            // The fixture itself is running inside the controller's JS tab.
                            ScriptContainer.Current = prior;
                            EventHooks.onNPCTick(outer);
                            String id = a + "->" + b + (fail ? ":stopped-inner" : ":repeat") + ":outer-throw";
                            Map<String, Object> c = new LinkedHashMap<>();
                            c.put("id", id); c.put("observations", observations);
                            c.put("outerConsole", console(outer)); c.put("innerConsole", console(inner)); extraCases.add(c);
                            check(checks, id + ":phases", observations.size() == (fail ? 4 : 5));
                            boolean owners = true;
                            for (Map<String, Object> row : observations) { owners &= owns(row); }
                            check(checks, id + ":all-owners", owners);
                            check(checks, id + ":inner-error", inner.script.getScripts().get(0).errored == fail);
                            check(checks, id + ":outer-error", outer.script.getScripts().get(0).errored
                                    && console(outer).contains("m66 outer fixture"));
                            check(checks, id + ":caller-restored", prior != null && ScriptContainer.Current == prior);
                        }
                    }
                }
            }
            boolean ok = checks.size() == (fixed ? 88 : 48);
            for (Map<String, Object> c : checks) { ok &= Boolean.TRUE.equals(c.get("ok")); }
            proof.put("success", ok);
            return ok;
        } catch (Throwable e) {
            proof.put("error", e.toString()); e.printStackTrace(); return false;
        } finally {
            ScriptContainer.Current = prior;
            outer = null; inner = null; world = null; player = null; observations = null;
            repeatAndThrow = false;
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean owns(Map<String, Object> row) {
        return "expected".equals(row.get("owner")) && "expected".equals(row.get("chest"))
                && "expected".equals(row.get("projectile")) && "".equals(row.get("projectileError"));
    }
}
