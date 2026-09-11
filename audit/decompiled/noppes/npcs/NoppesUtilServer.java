/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.command.ICommandManager
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.projectile.EntityArrow
 *  net.minecraft.entity.projectile.EntityThrowable
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.rcon.RConConsoleSource
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.tileentity.MobSpawnerBaseLogic
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityMobSpawner
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.SoundEvent
 *  net.minecraft.util.WeightedSpawnerEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.world.World
 */
package noppes.npcs;

import io.netty.buffer.ByteBuf;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.LogWriter;
import noppes.npcs.NBTTags;
import noppes.npcs.Server;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumPlayerData;
import noppes.npcs.containers.ContainerManageBanks;
import noppes.npcs.containers.ContainerManageRecipes;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerBankData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerDialogData;
import noppes.npcs.controllers.data.PlayerFactionData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.PlayerTransportData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTransporter;
import noppes.npcs.util.CustomNPCsScheduler;

public class NoppesUtilServer {
    private static HashMap<UUID, Quest> editingQuests = new HashMap();
    private static HashMap<UUID, Quest> editingQuestsClient = new HashMap();

    public static void setEditingNpc(EntityPlayer player, EntityNPCInterface npc) {
        PlayerData data = PlayerData.get(player);
        data.editingNpc = npc;
        if (npc != null) {
            Server.sendDataChecked((EntityPlayerMP)player, EnumPacketClient.EDIT_NPC, npc.func_145782_y());
        }
    }

    public static EntityNPCInterface getEditingNpc(EntityPlayer player) {
        PlayerData data = PlayerData.get(player);
        return data.editingNpc;
    }

    public static void setEditingQuest(EntityPlayer player, Quest quest) {
        if (player.field_70170_p.field_72995_K) {
            editingQuestsClient.put(player.func_110124_au(), quest);
        } else {
            editingQuests.put(player.func_110124_au(), quest);
        }
    }

    public static Quest getEditingQuest(EntityPlayer player) {
        if (player.field_70170_p.field_72995_K) {
            return editingQuestsClient.get(player.func_110124_au());
        }
        return editingQuests.get(player.func_110124_au());
    }

    public static void sendRoleData(EntityPlayer player, EntityNPCInterface npc) {
        if (npc.advanced.role == 0) {
            return;
        }
        NBTTagCompound comp = new NBTTagCompound();
        npc.roleInterface.writeToNBT(comp);
        comp.func_74768_a("EntityId", npc.func_145782_y());
        comp.func_74768_a("Role", npc.advanced.role);
        Server.sendData((EntityPlayerMP)player, EnumPacketClient.ROLE, comp);
    }

    public static void sendFactionDataAll(EntityPlayerMP player) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (Faction faction : FactionController.instance.factions.values()) {
            map.put(faction.name, faction.id);
        }
        NoppesUtilServer.sendScrollData(player, map);
    }

    public static void sendBankDataAll(EntityPlayerMP player) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (Bank bank : BankController.getInstance().banks.values()) {
            map.put(bank.name, bank.id);
        }
        NoppesUtilServer.sendScrollData(player, map);
    }

    public static void openDialog(EntityPlayer player, EntityNPCInterface npc, Dialog dia) {
        Dialog dialog = dia.copy(player);
        PlayerData playerdata = PlayerData.get(player);
        if (EventHooks.onNPCDialog(npc, player, dialog)) {
            playerdata.dialogId = -1;
            return;
        }
        playerdata.dialogId = dialog.id;
        if (npc instanceof EntityDialogNpc || dia.id < 0) {
            dialog.hideNPC = true;
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.DIALOG_DUMMY, npc.func_70005_c_(), dialog.writeToNBT(new NBTTagCompound()));
        } else {
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.DIALOG, npc.func_145782_y(), dialog.id);
        }
        dia.factionOptions.addPoints(player);
        if (dialog.hasQuest()) {
            PlayerQuestController.addActiveQuest(dialog.getQuest(), player);
        }
        if (!dialog.command.isEmpty()) {
            NoppesUtilServer.runCommand(npc, npc.func_70005_c_(), dialog.command, player);
        }
        if (dialog.mail.isValid()) {
            PlayerDataController.instance.addPlayerMessage(player.func_184102_h(), player.func_70005_c_(), dialog.mail);
        }
        PlayerDialogData data = playerdata.dialogData;
        if (!data.dialogsRead.contains(dialog.id) && dialog.id >= 0) {
            data.dialogsRead.add(dialog.id);
            playerdata.updateClient = true;
        }
        NoppesUtilServer.setEditingNpc(player, npc);
    }

    public static String runCommand(ICommandSender executer, String name, String command, EntityPlayer player) {
        return NoppesUtilServer.runCommand(executer.func_130014_f_(), executer.func_180425_c(), name, command, player, executer);
    }

    public static String runCommand(final World world, final BlockPos pos, final String name, String command, EntityPlayer player, final ICommandSender executer) {
        if (!world.func_73046_m().func_82356_Z()) {
            LogWriter.warn("Cant run commands if CommandBlocks are disabled");
            return "Cant run commands if CommandBlocks are disabled";
        }
        if (player != null) {
            command = command.replace("@dp", player.func_70005_c_());
        }
        command = command.replace("@npc", name);
        final TextComponentString output = new TextComponentString("");
        RConConsoleSource icommandsender = new RConConsoleSource(world.func_73046_m()){

            public String func_70005_c_() {
                return "@CustomNPCs-" + name;
            }

            public ITextComponent func_145748_c_() {
                return new TextComponentString(this.func_70005_c_());
            }

            public void func_145747_a(ITextComponent component) {
                output.func_150257_a(component);
            }

            public boolean func_70003_b(int permLevel, String commandName) {
                if (CustomNpcs.NpcUseOpCommands) {
                    return true;
                }
                return permLevel <= 2;
            }

            public BlockPos func_180425_c() {
                return pos;
            }

            public Vec3d func_174791_d() {
                return new Vec3d((double)pos.func_177958_n() + 0.5, (double)pos.func_177956_o() + 0.5, (double)pos.func_177952_p() + 0.5);
            }

            public World func_130014_f_() {
                return world;
            }

            public Entity func_174793_f() {
                if (executer == null) {
                    return null;
                }
                return executer.func_174793_f();
            }

            public boolean func_174792_t_() {
                return this.func_184102_h().field_71305_c[0].func_82736_K().func_82766_b("commandBlockOutput");
            }
        };
        ICommandManager icommandmanager = world.func_73046_m().func_71187_D();
        icommandmanager.func_71556_a((ICommandSender)icommandsender, command);
        if (output.func_150260_c().isEmpty()) {
            return null;
        }
        return output.func_150260_c();
    }

    public static void consumeItemStack(int i, EntityPlayer player) {
        ItemStack item = player.field_71071_by.func_70448_g();
        if (player.field_71075_bZ.field_75098_d || item == null || item.func_190926_b()) {
            return;
        }
        item.func_190918_g(1);
        if (item.func_190916_E() <= 0) {
            player.func_184611_a(EnumHand.MAIN_HAND, null);
        }
    }

    public static DataOutputStream getDataOutputStream(ByteArrayOutputStream stream) throws IOException {
        return new DataOutputStream(new GZIPOutputStream(stream));
    }

    public static void sendOpenGui(EntityPlayer player, EnumGuiType gui, EntityNPCInterface npc) {
        NoppesUtilServer.sendOpenGui(player, gui, npc, 0, 0, 0);
    }

    public static void sendOpenGui(EntityPlayer player, EnumGuiType gui, EntityNPCInterface npc, int i, int j, int k) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        NoppesUtilServer.setEditingNpc(player, npc);
        NoppesUtilServer.sendExtraData(player, npc, gui, i, j, k);
        CustomNPCsScheduler.runTack(() -> {
            if (CustomNpcs.proxy.getServerGuiElement(gui.ordinal(), player, player.field_70170_p, i, j, k) != null) {
                player.openGui((Object)CustomNpcs.instance, gui.ordinal(), player.field_70170_p, i, j, k);
                return;
            }
            Server.sendDataChecked((EntityPlayerMP)player, EnumPacketClient.GUI, gui.ordinal(), i, j, k);
            ArrayList<String> list = NoppesUtilServer.getScrollData(player, gui, npc);
            if (list == null || list.isEmpty()) {
                return;
            }
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.SCROLL_LIST, list);
        }, 200);
    }

    private static void sendExtraData(EntityPlayer player, EntityNPCInterface npc, EnumGuiType gui, int i, int j, int k) {
        if (gui == EnumGuiType.PlayerFollower || gui == EnumGuiType.PlayerFollowerHire || gui == EnumGuiType.PlayerTrader || gui == EnumGuiType.PlayerTransporter) {
            NoppesUtilServer.sendRoleData(player, npc);
        }
    }

    private static ArrayList<String> getScrollData(EntityPlayer player, EnumGuiType gui, EntityNPCInterface npc) {
        if (gui == EnumGuiType.PlayerTransporter) {
            RoleTransporter role = (RoleTransporter)npc.roleInterface;
            ArrayList<String> list = new ArrayList<String>();
            TransportLocation location = role.getLocation();
            String name = role.getLocation().name;
            for (TransportLocation loc : location.category.getDefaultLocations()) {
                if (list.contains(loc.name)) continue;
                list.add(loc.name);
            }
            PlayerTransportData playerdata = PlayerData.get((EntityPlayer)player).transportData;
            for (int i : playerdata.transports) {
                TransportLocation loc = TransportController.getInstance().getTransport(i);
                if (loc == null || !location.category.locations.containsKey(loc.id) || list.contains(loc.name)) continue;
                list.add(loc.name);
            }
            list.remove(name);
            return list;
        }
        return null;
    }

    public static void spawnParticle(Entity entity, String particle, int dimension) {
        Server.sendAssociatedData(entity, EnumPacketClient.PARTICLE, entity.field_70165_t, entity.field_70163_u, entity.field_70161_v, Float.valueOf(entity.field_70131_O), Float.valueOf(entity.field_70130_N), particle);
    }

    public static void deleteNpc(EntityNPCInterface npc, EntityPlayer player) {
        Server.sendAssociatedData((Entity)npc, EnumPacketClient.DELETE_NPC, npc.func_145782_y());
    }

    public static void createMobSpawner(BlockPos pos, NBTTagCompound comp, EntityPlayer player) {
        ServerCloneController.Instance.cleanTags(comp);
        if (comp.func_74779_i("id").equalsIgnoreCase("entityhorse")) {
            player.func_145747_a((ITextComponent)new TextComponentTranslation("Currently you cant create horse spawner, its a minecraft bug", new Object[0]));
            return;
        }
        player.field_70170_p.func_175656_a(pos, Blocks.field_150474_ac.func_176223_P());
        TileEntityMobSpawner tile = (TileEntityMobSpawner)player.field_70170_p.func_175625_s(pos);
        MobSpawnerBaseLogic logic = tile.func_145881_a();
        if (!comp.func_150297_b("id", 8)) {
            comp.func_74778_a("id", "Pig");
        }
        comp.func_74783_a("StartPosNew", new int[]{pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p()});
        logic.func_184993_a(new WeightedSpawnerEntity(1, comp));
    }

    public static void sendPlayerData(EnumPlayerData type, EntityPlayerMP player, String name) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        if (type == EnumPlayerData.Players) {
            for (String username : PlayerDataController.instance.nameUUIDs.keySet()) {
                map.put(username, 0);
            }
            for (String username : player.func_184102_h().func_184103_al().func_72369_d()) {
                map.put(username, 0);
            }
        } else {
            PlayerData playerdata = PlayerDataController.instance.getDataFromUsername(player.func_184102_h(), name);
            if (type == EnumPlayerData.Dialog) {
                PlayerDialogData data = playerdata.dialogData;
                for (int questId : data.dialogsRead) {
                    Dialog dialog = DialogController.instance.dialogs.get(questId);
                    if (dialog == null) continue;
                    map.put(dialog.category.title + ": " + dialog.title, questId);
                }
            } else if (type == EnumPlayerData.Quest) {
                Quest quest;
                PlayerQuestData data = playerdata.questData;
                for (int questId : data.activeQuests.keySet()) {
                    quest = QuestController.instance.quests.get(questId);
                    if (quest == null) continue;
                    map.put(quest.category.title + ": " + quest.title + "(Active quest)", questId);
                }
                for (int questId : data.finishedQuests.keySet()) {
                    quest = QuestController.instance.quests.get(questId);
                    if (quest == null) continue;
                    map.put(quest.category.title + ": " + quest.title + "(Finished quest)", questId);
                }
            } else if (type == EnumPlayerData.Transport) {
                PlayerTransportData data = playerdata.transportData;
                for (int questId : data.transports) {
                    TransportLocation location = TransportController.getInstance().getTransport(questId);
                    if (location == null) continue;
                    map.put(location.category.title + ": " + location.name, questId);
                }
            } else if (type == EnumPlayerData.Bank) {
                PlayerBankData data = playerdata.bankData;
                for (int bankId : data.banks.keySet()) {
                    Bank bank = BankController.getInstance().banks.get(bankId);
                    if (bank == null) continue;
                    map.put(bank.name, bankId);
                }
            } else if (type == EnumPlayerData.Factions) {
                PlayerFactionData data = playerdata.factionData;
                for (int factionId : data.factionData.keySet()) {
                    Faction faction = FactionController.instance.factions.get(factionId);
                    if (faction == null) continue;
                    map.put(faction.name + "(" + data.getFactionPoints((EntityPlayer)player, factionId) + ")", factionId);
                }
            }
        }
        NoppesUtilServer.sendScrollData(player, map);
    }

    public static void removePlayerData(ByteBuf buffer, EntityPlayerMP player) throws IOException {
        Object data;
        int id = buffer.readInt();
        if (EnumPlayerData.values().length <= id) {
            return;
        }
        String name = Server.readString(buffer);
        if (name == null || name.isEmpty()) {
            return;
        }
        EnumPlayerData type = EnumPlayerData.values()[id];
        EntityPlayerMP pl = player.func_184102_h().func_184103_al().func_152612_a(name);
        PlayerData playerdata = null;
        playerdata = pl == null ? PlayerDataController.instance.getDataFromUsername(player.func_184102_h(), name) : PlayerData.get((EntityPlayer)pl);
        if (type == EnumPlayerData.Players) {
            File file = new File(CustomNpcs.getWorldSaveDirectory("playerdata"), playerdata.uuid + ".json");
            if (file.exists()) {
                file.delete();
            }
            if (pl != null) {
                playerdata.setNBT(new NBTTagCompound());
                NoppesUtilServer.sendPlayerData(type, player, name);
                playerdata.save(true);
                return;
            }
            PlayerDataController.instance.nameUUIDs.remove(name);
        }
        if (type == EnumPlayerData.Quest) {
            data = playerdata.questData;
            int questId = buffer.readInt();
            ((PlayerQuestData)data).activeQuests.remove(questId);
            ((PlayerQuestData)data).finishedQuests.remove(questId);
            playerdata.save(true);
        }
        if (type == EnumPlayerData.Dialog) {
            data = playerdata.dialogData;
            ((PlayerDialogData)data).dialogsRead.remove(buffer.readInt());
            playerdata.save(true);
        }
        if (type == EnumPlayerData.Transport) {
            data = playerdata.transportData;
            ((PlayerTransportData)data).transports.remove(buffer.readInt());
            playerdata.save(true);
        }
        if (type == EnumPlayerData.Bank) {
            data = playerdata.bankData;
            ((PlayerBankData)data).banks.remove(buffer.readInt());
            playerdata.save(true);
        }
        if (type == EnumPlayerData.Factions) {
            data = playerdata.factionData;
            ((PlayerFactionData)data).factionData.remove(buffer.readInt());
            playerdata.save(true);
        }
        if (pl != null) {
            SyncController.syncPlayer(pl);
        }
        NoppesUtilServer.sendPlayerData(type, player, name);
    }

    public static void sendRecipeData(EntityPlayerMP player, int size) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        if (size == 3) {
            for (RecipeCarpentry recipe : RecipeController.instance.globalRecipes.values()) {
                map.put(recipe.name, recipe.id);
            }
        } else {
            for (RecipeCarpentry recipe : RecipeController.instance.anvilRecipes.values()) {
                map.put(recipe.name, recipe.id);
            }
        }
        NoppesUtilServer.sendScrollData(player, map);
    }

    public static void sendScrollData(EntityPlayerMP player, Map<String, Integer> map) {
        HashMap<String, Integer> send = new HashMap<String, Integer>();
        for (String key : map.keySet()) {
            send.put(key, map.get(key));
            if (send.size() != 100) continue;
            Server.sendData(player, EnumPacketClient.SCROLL_DATA_PART, send);
            send = new HashMap();
        }
        Server.sendData(player, EnumPacketClient.SCROLL_DATA, send);
    }

    public static void sendTransportCategoryData(EntityPlayerMP player) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (TransportCategory category : TransportController.getInstance().categories.values()) {
            map.put(category.title, category.id);
        }
        NoppesUtilServer.sendScrollData(player, map);
    }

    public static void sendTransportData(EntityPlayerMP player, int categoryid) {
        TransportCategory category = TransportController.getInstance().categories.get(categoryid);
        if (category == null) {
            return;
        }
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (TransportLocation transport : category.locations.values()) {
            map.put(transport.name, transport.id);
        }
        NoppesUtilServer.sendScrollData(player, map);
    }

    public static void sendNpcDialogs(EntityPlayer player) {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        if (npc == null) {
            return;
        }
        for (int pos : npc.dialogs.keySet()) {
            DialogOption option = npc.dialogs.get(pos);
            if (option == null || !option.hasDialog()) continue;
            NBTTagCompound compound = option.writeNBT();
            compound.func_74768_a("Position", pos);
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.GUI_DATA, compound);
        }
    }

    public static DialogOption setNpcDialog(int slot, int dialogId, EntityPlayer player) {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        if (npc == null) {
            return null;
        }
        if (!npc.dialogs.containsKey(slot)) {
            npc.dialogs.put(slot, new DialogOption());
        }
        DialogOption option = npc.dialogs.get(slot);
        option.dialogId = dialogId;
        option.optionType = 1;
        if (option.hasDialog()) {
            option.title = option.getDialog().title;
        }
        return option;
    }

    public static TileEntity saveTileEntity(EntityPlayerMP player, NBTTagCompound compound) {
        int z;
        int y;
        int x = compound.func_74762_e("x");
        TileEntity tile = player.field_70170_p.func_175625_s(new BlockPos(x, y = compound.func_74762_e("y"), z = compound.func_74762_e("z")));
        if (tile != null) {
            tile.func_145839_a(compound);
        }
        return tile;
    }

    public static void setRecipeGui(EntityPlayerMP player, RecipeCarpentry recipe) {
        if (recipe == null) {
            return;
        }
        if (!(player.field_71070_bA instanceof ContainerManageRecipes)) {
            return;
        }
        ContainerManageRecipes container = (ContainerManageRecipes)player.field_71070_bA;
        container.setRecipe(recipe);
        Server.sendData(player, EnumPacketClient.GUI_DATA, recipe.writeNBT());
    }

    public static void sendBank(EntityPlayerMP player, Bank bank) {
        NBTTagCompound compound = new NBTTagCompound();
        bank.writeEntityToNBT(compound);
        Server.sendData(player, EnumPacketClient.GUI_DATA, compound);
        if (player.field_71070_bA instanceof ContainerManageBanks) {
            ((ContainerManageBanks)player.field_71070_bA).setBank(bank);
        }
        player.func_71110_a(player.field_71070_bA, player.field_71070_bA.func_75138_a());
    }

    public static void sendNearbyNpcs(EntityPlayerMP player) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (Entity entity : player.field_70170_p.field_72996_f) {
            if (!(entity instanceof EntityNPCInterface)) continue;
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            if (npc.field_70128_L) continue;
            float distance = player.func_70032_d((Entity)npc);
            DecimalFormat df = new DecimalFormat("#.#");
            String s = df.format(distance);
            if (distance < 10.0f) {
                s = "0" + s;
            }
            map.put(s + " : " + npc.display.getName(), npc.func_145782_y());
        }
        NoppesUtilServer.sendScrollData(player, map);
    }

    public static void sendGuiError(EntityPlayer player, int i) {
        Server.sendData((EntityPlayerMP)player, EnumPacketClient.GUI_ERROR, i, new NBTTagCompound());
    }

    public static void sendGuiClose(EntityPlayerMP player, int i, NBTTagCompound comp) {
        Server.sendData(player, EnumPacketClient.GUI_CLOSE, i, comp);
    }

    public static Entity spawnClone(NBTTagCompound compound, double x, double y, double z, World world) {
        ServerCloneController.Instance.cleanTags(compound);
        compound.func_74782_a("Pos", (NBTBase)NBTTags.nbtDoubleList(x, y, z));
        Entity entity = EntityList.func_75615_a((NBTTagCompound)compound, (World)world);
        if (entity == null) {
            return null;
        }
        if (entity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            npc.ais.setStartPos(new BlockPos((Entity)npc));
        }
        world.func_72838_d(entity);
        return entity;
    }

    public static boolean isOp(EntityPlayer player) {
        return player.func_184102_h().func_184103_al().func_152596_g(player.func_146103_bH());
    }

    public static void GivePlayerItem(Entity entity, EntityPlayer player, ItemStack item) {
        if (entity.field_70170_p.field_72995_K || item == null || item.func_190926_b()) {
            return;
        }
        item = item.func_77946_l();
        float f = 0.7f;
        double d = (double)(entity.field_70170_p.field_73012_v.nextFloat() * f) + (double)(1.0f - f);
        double d1 = (double)(entity.field_70170_p.field_73012_v.nextFloat() * f) + (double)(1.0f - f);
        double d2 = (double)(entity.field_70170_p.field_73012_v.nextFloat() * f) + (double)(1.0f - f);
        EntityItem entityitem = new EntityItem(entity.field_70170_p, entity.field_70165_t + d, entity.field_70163_u + d1, entity.field_70161_v + d2, item);
        entityitem.func_174867_a(2);
        entity.field_70170_p.func_72838_d((Entity)entityitem);
        int i = item.func_190916_E();
        if (player.field_71071_by.func_70441_a(item)) {
            entity.field_70170_p.func_184148_a(null, player.field_70165_t, player.field_70163_u, player.field_70161_v, SoundEvents.field_187638_cR, SoundCategory.PLAYERS, 0.2f, ((player.func_70681_au().nextFloat() - player.func_70681_au().nextFloat()) * 0.7f + 1.0f) * 2.0f);
            player.func_71001_a((Entity)entityitem, i);
            PlayerQuestData playerdata = PlayerData.get((EntityPlayer)player).questData;
            playerdata.checkQuestCompletion(player, 0);
            if (item.func_190916_E() <= 0) {
                entityitem.func_70106_y();
            }
        }
    }

    public static BlockPos GetClosePos(BlockPos origin, World world) {
        for (int x = -1; x < 2; ++x) {
            for (int z = -1; z < 2; ++z) {
                for (int y = 2; y >= -2; --y) {
                    BlockPos pos = origin.func_177982_a(x, y, z);
                    if (!world.isSideSolid(pos, EnumFacing.UP) || !world.func_175623_d(pos.func_177984_a()) || !world.func_175623_d(pos.func_177981_b(2))) continue;
                    return pos.func_177984_a();
                }
            }
        }
        return world.func_175672_r(origin);
    }

    public static void NotifyOPs(String message, Object ... obs) {
        TextComponentTranslation chatcomponenttranslation = new TextComponentTranslation(message, obs);
        chatcomponenttranslation.func_150256_b().func_150238_a(TextFormatting.GRAY);
        chatcomponenttranslation.func_150256_b().func_150217_b(Boolean.valueOf(true));
        for (EntityPlayer entityplayer : CustomNpcs.Server.func_184103_al().func_181057_v()) {
            if (!entityplayer.func_174792_t_() || !NoppesUtilServer.isOp(entityplayer)) continue;
            entityplayer.func_145747_a((ITextComponent)chatcomponenttranslation);
        }
        if (CustomNpcs.Server.field_71305_c[0].func_82736_K().func_82766_b("logAdminCommands")) {
            LogWriter.info(chatcomponenttranslation.func_150260_c());
        }
    }

    public static void playSound(EntityLivingBase entity, SoundEvent sound, float volume, float pitch) {
        entity.field_70170_p.func_184148_a(null, entity.field_70165_t, entity.field_70163_u, entity.field_70161_v, sound, SoundCategory.NEUTRAL, volume, pitch);
    }

    public static void playSound(World world, BlockPos pos, SoundEvent sound, SoundCategory cat, float volume, float pitch) {
        world.func_184133_a(null, pos, sound, cat, volume, pitch);
    }

    public static EntityPlayer getPlayer(MinecraftServer minecraftserver, UUID id) {
        List list = minecraftserver.func_184103_al().func_181057_v();
        for (EntityPlayer player : list) {
            if (!id.equals(player.func_110124_au())) continue;
            return player;
        }
        return null;
    }

    public static Entity GetDamageSourcee(DamageSource damagesource) {
        Entity entity = damagesource.func_76346_g();
        if (entity == null) {
            entity = damagesource.func_76364_f();
        }
        if (entity instanceof EntityArrow && ((EntityArrow)entity).field_70250_c instanceof EntityLivingBase) {
            entity = ((EntityArrow)entity).field_70250_c;
        } else if (entity instanceof EntityThrowable) {
            entity = ((EntityThrowable)entity).func_85052_h();
        }
        return entity;
    }

    public static boolean IsItemStackNull(ItemStack is) {
        return is == null || is.func_190926_b() || is == ItemStack.field_190927_a || is.func_77973_b() == null;
    }

    public static ItemStack ChangeItemStack(ItemStack is, Item item) {
        NBTTagCompound comp = is.func_77955_b(new NBTTagCompound());
        ResourceLocation resourcelocation = (ResourceLocation)Item.field_150901_e.func_177774_c((Object)item);
        comp.func_74778_a("id", resourcelocation == null ? "minecraft:air" : resourcelocation.toString());
        return new ItemStack(comp);
    }
}

