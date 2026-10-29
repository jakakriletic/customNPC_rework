package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.*;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.IContext;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.PacketHandlerServer;
import noppes.npcs.Server;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;

/** M6.8 controller-only fixture. Calls the real packet entry and registered command manager
 * on the server thread. A real EntityPlayerMP has a synthetic, unconnected transport; no
 * client login, GUI, permission plugin integration or hostile-code isolation is claimed.
 * Temporarily replaces Forge's permission provider only in this disposable test server.
 */
public final class TrustProbe {
    private TrustProbe() { }

    private static final class Grants implements IPermissionHandler {
        final IPermissionHandler original;
        final UUID player;
        boolean script;
        int calls;
        Grants(IPermissionHandler original, UUID player) { this.original = original; this.player = player; }
        @Override public void registerNode(String node, DefaultPermissionLevel level, String description) {
            original.registerNode(node, level, description);
        }
        @Override public Collection<String> getRegisteredNodes() { return original.getRegisteredNodes(); }
        @Override public String getNodeDescription(String node) { return original.getNodeDescription(node); }
        @Override public boolean hasPermission(GameProfile profile, String node, IContext context) {
            if (player.equals(profile.getId()) && CustomNpcsPermissions.TOOL_SCRIPTER.name.equals(node)) {
                calls++; return script;
            }
            return original.hasPermission(profile, node, context);
        }
    }

    public static boolean run(ICustomNpc control, String output, String mode) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> traces = new LinkedHashMap<>();
        proof.put("mode", mode); proof.put("checks", checks); proof.put("traces", traces); proof.put("success", false);
        long started = System.nanoTime();
        boolean ops = CustomNpcs.OpsOnly, disabled = CustomNpcs.DisablePermissions;
        boolean enabled = CustomNpcs.EnableScripting;
        SecurityManager security = System.getSecurityManager();
        IPermissionHandler original = PermissionAPI.getPermissionHandler();
        Field provider = PermissionAPI.class.getDeclaredField("permissionHandler");
        provider.setAccessible(true);
        ScriptController sc = ScriptController.Instance;
        Map<String, String> includes = new LinkedHashMap<>(sc.scripts);
        long loaded = sc.lastLoaded;
        EntityPlayerMP player = null;
        MinecraftServer server = null;
        GameProfile profile = new GameProfile(UUID.fromString("06800000-0000-0000-0000-000000000001"), "M68_Author");
        boolean addedOp = false;
        try {
            WorldServer world = control.getWorld().getMCWorld();
            server = world.getMinecraftServer();
            check(checks, "server-thread", server.isCallingFromMinecraftThread());
            player = new EntityPlayerMP(server, world, profile, new PlayerInteractionManager(world));
            NetworkManager network = new NetworkManager(EnumPacketDirection.SERVERBOUND);
            player.connection = new NetHandlerPlayServer(server, network, player);
            network.setNetHandler(player.connection);
            check(checks, "starts-non-op", !NoppesUtilServer.isOp(player));
            Grants grants = new Grants(original, profile.getId());
            // Forge permits public registration only in PreInit. This fixture is invoked after
            // world start, so inject/restore just the provider field; never change Loader state.
            // All queries still go through the actual PermissionAPI and packet entry.
            provider.set(null, grants);
            CustomNpcs.DisablePermissions = false;
            CustomNpcs.EnableScripting = true;
            check(checks, "save-node-existing", EnumPacketServer.ScriptDataSave.permission == CustomNpcsPermissions.TOOL_SCRIPTER);
            check(checks, "default-grant-is-not-trust", CustomNpcsPermissions.TOOL_SCRIPTER.defaultValue);

            Path marker = Paths.get(output + ".capability.txt");
            check(checks, "marker-absent-before", !Files.exists(marker));
            String literal = new GsonBuilder().create().toJson(marker.toAbsolutePath().toString());
            String java = "import noppes.npcs.api.event.NpcEvent;\n"
                    + "public class Trusted { public void timer(NpcEvent.TimerEvent e) { try {\n"
                    + "String v=(String)Class.forName(\"java.lang.System\").getMethod(\"getProperty\",String.class).invoke(null,\"java.version\");\n"
                    + "java.nio.file.Files.write(java.nio.file.Paths.get(" + literal + "),v.getBytes(java.nio.charset.StandardCharsets.UTF_8));\n"
                    + "e.npc.getTempdata().put(\"t\",\"trusted\");\n"
                    + "} catch(Exception x) { throw new RuntimeException(x); } } }\n";
            String js = "function timer(e) { e.npc.getTempdata().put('t','trusted'); }";
            for (String language : new String[] { "ECMAScript", ScriptHooks.JAVA }) {
                String key = ScriptHooks.JAVA.equals(language) ? "java" : "js";
                EntityNPCInterface target = npc(world, "ECMAScript", "function timer(e) {} ");
                PlayerData.get(player).editingNpc = target;
                target.updateAI = false; target.script.lastInited = 123L;
                NBTTagCompound before = target.script.writeToNBT(new NBTTagCompound());
                EntityNPCInterface source = npc(world, language, key.equals("java") ? java : js);
                source.script.getScripts().get(0).scripts.add("m68-untrusted-include." + key);
                NBTTagCompound upload = source.script.writeToNBT(new NBTTagCompound());
                // The same upload carries language, enabled, inline source and ordered include names.

                CustomNpcs.OpsOnly = true; grants.script = true;
                int calls = grants.calls;
                int read = save(network, player, upload);
                check(checks, key + "-ops-only-denies", unchanged(target, before) && read == 0 && grants.calls == calls);

                CustomNpcs.OpsOnly = false; grants.script = false;
                player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(CustomItems.scripter));
                calls = grants.calls;
                read = save(network, player, upload);
                check(checks, key + "-permission-denies-upload", unchanged(target, before) && read == 4 && grants.calls > calls);

                grants.script = true;
                player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);
                read = save(network, player, upload);
                check(checks, key + "-tool-denies", unchanged(target, before) && read == 4);

                // Legitimate save has no fake include. The previous denial covered include names too.
                source.script.getScripts().get(0).scripts.clear();
                upload = source.script.writeToNBT(new NBTTagCompound());
                player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(CustomItems.scripter));
                read = save(network, player, upload);
                check(checks, key + "-trusted-save", upload.equals(target.script.writeToNBT(new NBTTagCompound()))
                        && target.updateAI && target.script.lastInited == -1L && read > 4);
                EventHooks.onNPCTimer(target, 7);
                traces.put(key, trace(target)); traces.put(key + "Console", console(target));
                check(checks, key + "-trusted-runs", "trusted".equals(trace(target)) && !target.script.getScripts().get(0).errored);

                // A denied save may not disable or replace an already running handler.
                before = target.script.writeToNBT(new NBTTagCompound());
                target.updateAI = false; target.script.lastInited = 123L;
                NBTTagCompound attack = npc(world, language, "broken source").script.writeToNBT(new NBTTagCompound());
                attack.setBoolean("ScriptEnabled", false);
                grants.script = false;
                save(network, player, attack);
                check(checks, key + "-denied-replacement-keeps-handler", unchanged(target, before));
            }
            check(checks, "java-bounded-file-reflection-allowed", Files.isRegularFile(marker)
                    && System.getProperty("java.version").equals(new String(Files.readAllBytes(marker), StandardCharsets.UTF_8)));

            // Defaults and DisablePermissions are compatibility modes, not deny policies.
            grants.script = false;
            check(checks, "explicit-deny-active", !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER));
            CustomNpcs.DisablePermissions = true;
            check(checks, "disabled-permissions-bypass", CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER));
            CustomNpcs.DisablePermissions = false;

            sc.scripts.put("m68-reload-sentinel", "unchanged");
            long beforeReload = sc.lastLoaded;
            int result = server.getCommandManager().executeCommand(player, "noppes script reload");
            traces.put("deniedReloadResult", result);
            check(checks, "non-op-reload-denied", result == 0 && sc.lastLoaded == beforeReload
                    && "unchanged".equals(sc.scripts.get("m68-reload-sentinel")));
            // An op is a trusted author under the recommended OpsOnly deployment policy.
            server.getPlayerList().addOp(profile); addedOp = true;
            check(checks, "op-granted", NoppesUtilServer.isOp(player) && player.canUseCommand(4, "commands.noppes.script.reload"));
            CustomNpcs.OpsOnly = true; grants.script = true;
            EntityNPCInterface target = npc(world, "ECMAScript", "");
            PlayerData.get(player).editingNpc = target;
            NBTTagCompound upload = npc(world, ScriptHooks.JAVA, java).script.writeToNBT(new NBTTagCompound());
            save(network, player, upload);
            check(checks, "op-trusted-java-save", upload.equals(target.script.writeToNBT(new NBTTagCompound())));
            // Force a distinct reload token without sleeping or changing the clock.
            sc.lastLoaded = -1;
            result = server.getCommandManager().executeCommand(player, "noppes script reload");
            traces.put("allowedReloadResult", result);
            check(checks, "op-reload-allowed", result == 1 && sc.lastLoaded > 0 && !sc.scripts.containsKey("m68-reload-sentinel"));
            EventHooks.onNPCTimer(target, 7);
            traces.put("javaAfterReload", trace(target));
            check(checks, "java-runs-after-authorized-reload", "trusted".equals(trace(target)) && !target.script.getScripts().get(0).errored);
            check(checks, "security-manager-unchanged", System.getSecurityManager() == security);
        } catch (Throwable failure) {
            proof.put("error", failure.toString()); failure.printStackTrace();
        } finally {
            if (addedOp) { server.getPlayerList().removeOp(profile); }
            if (player != null) { PlayerData.get(player).editingNpc = null; }
            provider.set(null, original);
            CustomNpcs.OpsOnly = ops; CustomNpcs.DisablePermissions = disabled; CustomNpcs.EnableScripting = enabled;
            sc.scripts.clear(); sc.scripts.putAll(includes); sc.lastLoaded = loaded;
            check(checks, "provider-config-restored", PermissionAPI.getPermissionHandler() == original
                    && CustomNpcs.OpsOnly == ops && CustomNpcs.DisablePermissions == disabled && CustomNpcs.EnableScripting == enabled);
            boolean ok = !proof.containsKey("error");
            for (Map<String, Object> check : checks) { ok &= Boolean.TRUE.equals(check.get("ok")); }
            proof.put("success", ok);
            proof.put("elapsedNanos", System.nanoTime() - started);
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
        return Boolean.TRUE.equals(proof.get("success"));
    }

    private static boolean unchanged(EntityNPCInterface npc, NBTTagCompound before) {
        return before.equals(npc.script.writeToNBT(new NBTTagCompound())) && !npc.updateAI && npc.script.lastInited == 123L;
    }

    /** Invoke exactly the public entry. main-thread scheduling is synchronous in MinecraftServer.
     * Record read progress before releasing fixture-owned buffers (OpsOnly returns before release).
     */
    private static int save(NetworkManager network, EntityPlayerMP player, NBTTagCompound data) throws Exception {
        ByteBuf body = Unpooled.buffer();
        body.writeInt(EnumPacketServer.ScriptDataSave.ordinal()); Server.writeNBT(body, data);
        // Keep one fixture reference: the packet handler releases its own reference on dispatch.
        body.retain();
        try {
            FMLProxyPacket packet = new FMLProxyPacket(new PacketBuffer(body), "CustomNPCs");
            new PacketHandlerServer().onServerPacket(new FMLNetworkEvent.ServerCustomPacketEvent(network, packet));
            return body.readerIndex();
        } finally { while (body.refCnt() > 0) { body.release(); } }
    }
}
