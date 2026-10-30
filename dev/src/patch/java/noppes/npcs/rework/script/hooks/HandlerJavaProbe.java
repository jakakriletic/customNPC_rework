package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.check;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.WorldEvent;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.ForgeEvent;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;

/** Explicit P5 fixture entry point only (java-hooks-run.ps1 -Package p5). On one packaged Forge server
 * with RwJavaClasspath=auto: the player, forge, block, door and item handlers each get a JS tab and then
 * a Java tab through the same NBT their GUI save packet applies (setPlayerScripts, setForgeScripts,
 * tile setNBT, item setMCNbt); real EventHooks fire one event and both languages must leave the same
 * trace. The language list every script GUI receives contains Java. A Java tab whose hook does not
 * accept the event stops with hook.signature and runs nothing. Java types reach game classes, so the
 * auto classpath must resolve runtime Minecraft names.
 */
public final class HandlerJavaProbe {
    private HandlerJavaProbe() { }

    static final String KEY = "p5";
    static final BlockPos BLOCK = new BlockPos(5, 4, 5), DOOR = new BlockPos(7, 4, 5);

    static String js(String hook, String expr) {
        return "function " + hook + "(e){var w=Java.type('noppes.npcs.api.NpcAPI').Instance().getIWorld(0);"
                + "w.getTempdata().put('" + KEY + "', " + expr + ");}\n";
    }

    static String java(String cls, String hook, String event, String expr) {
        return "package p5;\nimport net.minecraft.world.WorldServer;\nimport noppes.npcs.api.NpcAPI;\n"
                + "public class " + cls + " {\n public void " + hook + "(" + event + " e) {\n"
                + "  WorldServer mc = NpcAPI.Instance().getIWorld(0).getMCWorld();\n"
                + "  NpcAPI.Instance().getIWorld(mc).getTempdata().put(\"" + KEY + "\", " + expr + ");\n }\n}\n";
    }

    public static boolean run(ICustomNpc control, String output, String mode) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> traces = new LinkedHashMap<>();
        proof.put("mode", mode); proof.put("checks", checks); proof.put("traces", traces); proof.put("success", false);
        WorldServer world = control.getWorld().getMCWorld();
        IWorld api = NpcAPI.Instance().getIWorld(world);
        try {
            FakePlayer player = FakePlayerFactory.getMinecraft(world);
            player.capabilities.isCreativeMode = true;
            check(checks, "classpath-auto", "auto".equals(CustomNpcs.RwJavaClasspath.trim()));
            boolean listed = false;
            NBTTagList languages = ScriptController.Instance.nbtLanguages();
            for (int i = 0; i < languages.tagCount(); i++) { listed |= ScriptHooks.JAVA.equals(languages.getCompoundTagAt(i).getString("Language")); }
            check(checks, "gui-languages-java", listed);

            // Player: global player scripts, per-player copy, timer.
            String[] player2 = new String[2];
            for (int l = 0; l < 2; l++) {
                api.getTempdata().remove(KEY);
                ScriptController.Instance.setPlayerScripts(data(l == 0 ? "ECMAScript" : ScriptHooks.JAVA, l == 0
                        ? js("timer", "'player:'+e.id+':'+e.player.getName()")
                        : java("PlayerTab", "timer", "noppes.npcs.api.event.PlayerEvent.TimerEvent", "\"player:\" + e.id + \":\" + e.player.getName()")));
                EventHooks.onPlayerTimer(PlayerData.get(player), 5);
                player2[l] = String.valueOf(api.getTempdata().get(KEY));
            }
            traces.put("playerJs", player2[0]); traces.put("playerJava", player2[1]);
            check(checks, "player-js", ("player:5:" + player.getName()).equals(player2[0]));
            check(checks, "player-java-equals-js", player2[0].equals(player2[1]));
            check(checks, "player-java-not-errored", !PlayerData.get(player).scriptData.getScripts().get(0).errored);

            // Forge: string hook named after the event class, raw Forge event, scheduled on this thread.
            String[] forge2 = new String[2];
            for (int l = 0; l < 2; l++) {
                api.getTempdata().remove(KEY);
                ScriptController.Instance.setForgeScripts(data(l == 0 ? "ECMAScript" : ScriptHooks.JAVA, l == 0
                        ? js("worldEventSave", "'forge:'+(e.getWorld()!=null)")
                        : java("ForgeTab", "worldEventSave", "net.minecraftforge.event.world.WorldEvent.Save", "\"forge:\" + (e.getWorld() instanceof WorldServer)")));
                WorldEvent.Save save = new WorldEvent.Save(world);
                EventHooks.onForgeEvent(new ForgeEvent.WorldEvent(save, api), save);
                forge2[l] = String.valueOf(api.getTempdata().get(KEY));
            }
            traces.put("forgeJs", forge2[0]); traces.put("forgeJava", forge2[1]);
            check(checks, "forge-js", "forge:true".equals(forge2[0]));
            check(checks, "forge-java-equals-js", forge2[0].equals(forge2[1]));
            check(checks, "forge-java-not-errored", !ScriptController.Instance.forgeScripts.getScripts().get(0).errored);

            // Block and door: tile setNBT like ScriptBlockDataSave/ScriptDoorDataSave, then a timer.
            world.setBlockState(BLOCK, CustomItems.scripted.getDefaultState());
            world.setBlockState(DOOR, CustomItems.scriptedDoor.getDefaultState());
            TileEntity blockTile = world.getTileEntity(BLOCK), doorTile = world.getTileEntity(DOOR);
            check(checks, "tiles-placed", blockTile instanceof TileScripted && doorTile instanceof TileScriptedDoor);
            TileScripted block = (TileScripted) blockTile;
            TileScriptedDoor door = (TileScriptedDoor) doorTile;
            String[] block2 = new String[2], door2 = new String[2];
            for (int l = 0; l < 2; l++) {
                String language = l == 0 ? "ECMAScript" : ScriptHooks.JAVA;
                api.getTempdata().remove(KEY);
                block.setNBT(data(language, l == 0 ? js("timer", "'block:'+e.id+':'+e.block.getName()")
                        : java("BlockTab", "timer", "noppes.npcs.api.event.BlockEvent.TimerEvent", "\"block:\" + e.id + \":\" + e.block.getName()")));
                block.lastInited = -1L;
                EventHooks.onScriptBlockTimer(block, 6);
                block2[l] = String.valueOf(api.getTempdata().get(KEY));
                api.getTempdata().remove(KEY);
                door.setNBT(data(language, l == 0 ? js("timer", "'door:'+e.id+':'+e.block.getName()")
                        : java("DoorTab", "timer", "noppes.npcs.api.event.BlockEvent.TimerEvent", "\"door:\" + e.id + \":\" + e.block.getName()")));
                door.lastInited = -1L;
                EventHooks.onScriptBlockTimer(door, 6);
                door2[l] = String.valueOf(api.getTempdata().get(KEY));
            }
            traces.put("blockJs", block2[0]); traces.put("blockJava", block2[1]); traces.put("doorJs", door2[0]); traces.put("doorJava", door2[1]);
            check(checks, "block-js", ("block:6:" + block.getBlock().getName()).equals(block2[0]));
            check(checks, "block-java-equals-js", block2[0].equals(block2[1]));
            check(checks, "door-js", ("door:6:" + door.getBlock().getName()).equals(door2[0]));
            check(checks, "door-java-equals-js", door2[0].equals(door2[1]));
            check(checks, "block-door-java-not-errored", !block.getScripts().get(0).errored && !door.getScripts().get(0).errored);

            // Item: wrapper setMCNbt like ScriptItemDataSave, then the held-item update hook.
            String[] item2 = new String[2];
            for (int l = 0; l < 2; l++) {
                ItemScriptedWrapper item = (ItemScriptedWrapper) NpcAPI.Instance().getIItemStack(new ItemStack(CustomItems.scripted_item));
                api.getTempdata().remove(KEY);
                NBTTagCompound nbt = item.getMCNbt();
                nbt.merge(data(l == 0 ? "ECMAScript" : ScriptHooks.JAVA, l == 0 ? js("tick", "'item:'+e.item.getName()+':'+e.player.getName()")
                        : java("ItemTab", "tick", "noppes.npcs.api.event.ItemEvent.UpdateEvent", "\"item:\" + e.item.getName() + \":\" + e.player.getName()")));
                item.setMCNbt(nbt);
                item.lastInited = -1L;
                EventHooks.onScriptItemUpdate(item, player);
                item2[l] = String.valueOf(api.getTempdata().get(KEY));
                if (l == 1) { check(checks, "item-java-not-errored", !item.getScripts().get(0).errored); }
            }
            traces.put("itemJs", item2[0]); traces.put("itemJava", item2[1]);
            check(checks, "item-js", item2[0].startsWith("item:") && item2[0].endsWith(":" + player.getName()));
            check(checks, "item-java-equals-js", item2[0].equals(item2[1]));

            // A hook whose parameter does not accept the dispatched event is an error, not a silent skip.
            api.getTempdata().remove(KEY);
            block.setNBT(data(ScriptHooks.JAVA, java("WrongTab", "timer", "noppes.npcs.api.event.BlockEvent.InitEvent", "\"ran\"")));
            block.lastInited = -1L;
            EventHooks.onScriptBlockTimer(block, 6);
            String console = String.join("\n", block.getScripts().get(0).console.values());
            traces.put("wrongConsole", console);
            check(checks, "wrong-event-type-error", block.getScripts().get(0).errored && console.contains("hook.signature"));
            check(checks, "wrong-event-type-not-run", !api.getTempdata().has(KEY));

            boolean allOk = true;
            for (Map<String, Object> c : checks) { allOk &= Boolean.TRUE.equals(c.get("ok")); }
            proof.put("success", allOk);
            return allOk;
        } catch (Throwable failure) {
            proof.put("error", failure.toString());
            failure.printStackTrace();
            return false;
        } finally {
            api.getTempdata().remove(KEY);
            world.setBlockState(BLOCK, Blocks.AIR.getDefaultState());
            world.setBlockState(DOOR, Blocks.AIR.getDefaultState());
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** The compound a script GUI save sends: one tab, language, enabled. */
    static NBTTagCompound data(String language, String code) {
        NBTTagCompound tab = new NBTTagCompound();
        tab.setString("Script", code);
        tab.setTag("Console", new NBTTagList());
        tab.setTag("ScriptList", new NBTTagList());
        NBTTagList list = new NBTTagList();
        list.appendTag(tab);
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Scripts", list);
        data.setString("ScriptLanguage", language);
        data.setBoolean("ScriptEnabled", true);
        return data;
    }

}
