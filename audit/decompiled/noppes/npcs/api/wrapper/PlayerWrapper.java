/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.stats.StatBase
 *  net.minecraft.stats.StatList
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldSettings
 */
package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.Server;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.IPos;
import noppes.npcs.api.ITimers;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IPixelmonPlayerData;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ContainerCustomChestWrapper;
import noppes.npcs.api.wrapper.ContainerWrapper;
import noppes.npcs.api.wrapper.EntityLivingBaseWrapper;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerDialogData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.util.ValueUtil;

public class PlayerWrapper<T extends EntityPlayerMP>
extends EntityLivingBaseWrapper<T>
implements IPlayer {
    private IContainer inventory;
    private Object pixelmonPartyStorage;
    private Object pixelmonPCStorage;
    private PlayerData data;

    public PlayerWrapper(T player) {
        super(player);
    }

    @Override
    public String getName() {
        return ((EntityPlayerMP)this.entity).func_70005_c_();
    }

    @Override
    public String getDisplayName() {
        return ((EntityPlayerMP)this.entity).getDisplayNameString();
    }

    @Override
    public int getHunger() {
        return ((EntityPlayerMP)this.entity).func_71024_bL().func_75116_a();
    }

    @Override
    public void setHunger(int level) {
        ((EntityPlayerMP)this.entity).func_71024_bL().func_75114_a(level);
    }

    @Override
    public boolean hasFinishedQuest(int id) {
        PlayerQuestData data = this.getData().questData;
        return data.finishedQuests.containsKey(id);
    }

    @Override
    public boolean hasActiveQuest(int id) {
        PlayerQuestData data = this.getData().questData;
        return data.activeQuests.containsKey(id);
    }

    @Override
    public IQuest[] getActiveQuests() {
        PlayerQuestData data = this.getData().questData;
        ArrayList<IQuest> quests = new ArrayList<IQuest>();
        for (int id : data.activeQuests.keySet()) {
            IQuest quest = QuestController.instance.quests.get(id);
            if (quest == null) continue;
            quests.add(quest);
        }
        return quests.toArray(new IQuest[quests.size()]);
    }

    @Override
    public IQuest[] getFinishedQuests() {
        PlayerQuestData data = this.getData().questData;
        ArrayList<IQuest> quests = new ArrayList<IQuest>();
        for (int id : data.finishedQuests.keySet()) {
            IQuest quest = QuestController.instance.quests.get(id);
            if (quest == null) continue;
            quests.add(quest);
        }
        return quests.toArray(new IQuest[quests.size()]);
    }

    @Override
    public void startQuest(int id) {
        Quest quest = QuestController.instance.quests.get(id);
        if (quest == null) {
            return;
        }
        QuestData questdata = new QuestData(quest);
        PlayerData data = this.getData();
        data.questData.activeQuests.put(id, questdata);
        Server.sendData((EntityPlayerMP)this.entity, EnumPacketClient.MESSAGE, "quest.newquest", quest.title, 2);
        Server.sendData((EntityPlayerMP)this.entity, EnumPacketClient.CHAT, "quest.newquest", ": ", quest.title);
        data.updateClient = true;
    }

    @Override
    public void sendNotification(String title, String msg, int type) {
        if (type < 0 || type > 3) {
            throw new CustomNPCsException("Wrong type value given " + type, new Object[0]);
        }
        Server.sendData((EntityPlayerMP)this.entity, EnumPacketClient.MESSAGE, title, msg, type);
    }

    @Override
    public void finishQuest(int id) {
        Quest quest = QuestController.instance.quests.get(id);
        if (quest == null) {
            return;
        }
        PlayerData data = this.getData();
        data.questData.finishedQuests.put(id, System.currentTimeMillis());
        data.updateClient = true;
    }

    @Override
    public void stopQuest(int id) {
        Quest quest = QuestController.instance.quests.get(id);
        if (quest == null) {
            return;
        }
        PlayerData data = this.getData();
        data.questData.activeQuests.remove(id);
        data.updateClient = true;
    }

    @Override
    public void removeQuest(int id) {
        Quest quest = QuestController.instance.quests.get(id);
        if (quest == null) {
            return;
        }
        PlayerData data = this.getData();
        data.questData.activeQuests.remove(id);
        data.questData.finishedQuests.remove(id);
        data.updateClient = true;
    }

    @Override
    public boolean hasReadDialog(int id) {
        PlayerDialogData data = this.getData().dialogData;
        return data.dialogsRead.contains(id);
    }

    @Override
    public void showDialog(int id, String name) {
        Dialog dialog = DialogController.instance.dialogs.get(id);
        if (dialog == null) {
            throw new CustomNPCsException("Unknown Dialog id: " + id, new Object[0]);
        }
        if (!dialog.availability.isAvailable((EntityPlayer)this.entity)) {
            return;
        }
        EntityDialogNpc npc = new EntityDialogNpc((World)this.getWorld().getMCWorld());
        npc.display.setName(name);
        EntityUtil.Copy((EntityLivingBase)this.entity, (EntityLivingBase)npc);
        DialogOption option = new DialogOption();
        option.dialogId = id;
        option.title = dialog.title;
        npc.dialogs.put(0, option);
        NoppesUtilServer.openDialog((EntityPlayer)this.entity, npc, dialog);
    }

    @Override
    public void addFactionPoints(int faction, int points) {
        PlayerData data = this.getData();
        data.factionData.increasePoints((EntityPlayer)this.entity, faction, points);
        data.updateClient = true;
    }

    @Override
    public int getFactionPoints(int faction) {
        return this.getData().factionData.getFactionPoints((EntityPlayer)this.entity, faction);
    }

    @Override
    public float getRotation() {
        return ((EntityPlayerMP)this.entity).field_70177_z;
    }

    @Override
    public void setRotation(float rotation) {
        ((EntityPlayerMP)this.entity).field_70177_z = rotation;
    }

    @Override
    public void message(String message) {
        ((EntityPlayerMP)this.entity).func_145747_a((ITextComponent)new TextComponentTranslation(NoppesStringUtils.formatText(message, this.entity), new Object[0]));
    }

    @Override
    public int getGamemode() {
        return ((EntityPlayerMP)this.entity).field_71134_c.func_73081_b().func_77148_a();
    }

    @Override
    public void setGamemode(int type) {
        ((EntityPlayerMP)this.entity).func_71033_a(WorldSettings.func_77161_a((int)type));
    }

    @Override
    public int inventoryItemCount(IItemStack item) {
        int count = 0;
        for (int i = 0; i < ((EntityPlayerMP)this.entity).field_71071_by.func_70302_i_(); ++i) {
            ItemStack is = ((EntityPlayerMP)this.entity).field_71071_by.func_70301_a(i);
            if (is == null || !this.isItemEqual(item.getMCItemStack(), is)) continue;
            count += is.func_190916_E();
        }
        return count;
    }

    private boolean isItemEqual(ItemStack stack, ItemStack other) {
        if (other.func_190926_b()) {
            return false;
        }
        if (stack.func_77973_b() != other.func_77973_b()) {
            return false;
        }
        if (stack.func_77952_i() < 0) {
            return true;
        }
        return stack.func_77952_i() == other.func_77952_i();
    }

    @Override
    public int inventoryItemCount(String id, int damage) {
        Item item = (Item)Item.field_150901_e.func_82594_a((Object)new ResourceLocation(id));
        if (item == null) {
            throw new CustomNPCsException("Unknown item id: " + id, new Object[0]);
        }
        return this.inventoryItemCount(NpcAPI.Instance().getIItemStack(new ItemStack(item, 1, damage)));
    }

    @Override
    public IContainer getInventory() {
        if (this.inventory == null) {
            this.inventory = new ContainerWrapper((IInventory)((EntityPlayerMP)this.entity).field_71071_by);
        }
        return this.inventory;
    }

    @Override
    public boolean removeItem(IItemStack item, int amount) {
        int count = this.inventoryItemCount(item);
        if (amount > count) {
            return false;
        }
        if (count == amount) {
            this.removeAllItems(item);
        } else {
            for (int i = 0; i < ((EntityPlayerMP)this.entity).field_71071_by.func_70302_i_(); ++i) {
                ItemStack is = ((EntityPlayerMP)this.entity).field_71071_by.func_70301_a(i);
                if (is == null || !this.isItemEqual(item.getMCItemStack(), is)) continue;
                if (amount >= is.func_190916_E()) {
                    ((EntityPlayerMP)this.entity).field_71071_by.func_70299_a(i, ItemStack.field_190927_a);
                    amount -= is.func_190916_E();
                    continue;
                }
                is.func_77979_a(amount);
                break;
            }
        }
        this.updatePlayerInventory();
        return true;
    }

    @Override
    public boolean removeItem(String id, int damage, int amount) {
        Item item = (Item)Item.field_150901_e.func_82594_a((Object)new ResourceLocation(id));
        if (item == null) {
            throw new CustomNPCsException("Unknown item id: " + id, new Object[0]);
        }
        return this.removeItem(NpcAPI.Instance().getIItemStack(new ItemStack(item, 1, damage)), amount);
    }

    @Override
    public boolean giveItem(IItemStack item) {
        ItemStack mcItem = item.getMCItemStack();
        if (mcItem.func_190926_b()) {
            return false;
        }
        boolean bo = ((EntityPlayerMP)this.entity).field_71071_by.func_70441_a(mcItem.func_77946_l());
        if (bo) {
            NoppesUtilServer.playSound((EntityLivingBase)this.entity, SoundEvents.field_187638_cR, 0.2f, ((((EntityPlayerMP)this.entity).func_70681_au().nextFloat() - ((EntityPlayerMP)this.entity).func_70681_au().nextFloat()) * 0.7f + 1.0f) * 2.0f);
            this.updatePlayerInventory();
        }
        return bo;
    }

    @Override
    public boolean giveItem(String id, int damage, int amount) {
        Item item = (Item)Item.field_150901_e.func_82594_a((Object)new ResourceLocation(id));
        if (item == null) {
            return false;
        }
        ItemStack mcStack = new ItemStack(item);
        IItemStack itemStack = NpcAPI.Instance().getIItemStack(mcStack);
        itemStack.setStackSize(amount);
        itemStack.setItemDamage(damage);
        return this.giveItem(itemStack);
    }

    @Override
    public void updatePlayerInventory() {
        ((EntityPlayerMP)this.entity).field_71069_bz.func_75142_b();
        PlayerQuestData playerdata = this.getData().questData;
        playerdata.checkQuestCompletion((EntityPlayer)this.entity, 0);
    }

    @Override
    public IBlock getSpawnPoint() {
        BlockPos pos = ((EntityPlayerMP)this.entity).func_180470_cg();
        if (pos == null) {
            return this.getWorld().getSpawnPoint();
        }
        return NpcAPI.Instance().getIBlock(((EntityPlayerMP)this.entity).field_70170_p, pos);
    }

    @Override
    public void setSpawnPoint(IBlock block) {
        ((EntityPlayerMP)this.entity).func_180473_a(new BlockPos(block.getX(), block.getY(), block.getZ()), true);
    }

    @Override
    public void setSpawnpoint(int x, int y, int z) {
        x = ValueUtil.CorrectInt(x, -30000000, 30000000);
        z = ValueUtil.CorrectInt(z, -30000000, 30000000);
        y = ValueUtil.CorrectInt(y, 0, 256);
        ((EntityPlayerMP)this.entity).func_180473_a(new BlockPos(x, y, z), true);
    }

    @Override
    public void resetSpawnpoint() {
        ((EntityPlayerMP)this.entity).func_180473_a(null, false);
    }

    @Override
    public void removeAllItems(IItemStack item) {
        for (int i = 0; i < ((EntityPlayerMP)this.entity).field_71071_by.func_70302_i_(); ++i) {
            ItemStack is = ((EntityPlayerMP)this.entity).field_71071_by.func_70301_a(i);
            if (is == null || !is.func_77969_a(item.getMCItemStack())) continue;
            ((EntityPlayerMP)this.entity).field_71071_by.func_70299_a(i, ItemStack.field_190927_a);
        }
    }

    @Override
    public boolean hasAchievement(String achievement) {
        StatBase statbase = StatList.func_151177_a((String)achievement);
        return false;
    }

    @Override
    public int getExpLevel() {
        return ((EntityPlayerMP)this.entity).field_71068_ca;
    }

    @Override
    public void setExpLevel(int level) {
        ((EntityPlayerMP)this.entity).field_71068_ca = level;
        ((EntityPlayerMP)this.entity).func_82242_a(0);
    }

    @Override
    public void setPosition(double x, double y, double z) {
        NoppesUtilPlayer.teleportPlayer((EntityPlayerMP)this.entity, x, y, z, ((EntityPlayerMP)this.entity).field_71093_bK);
    }

    @Override
    public void setPos(IPos pos) {
        NoppesUtilPlayer.teleportPlayer((EntityPlayerMP)this.entity, pos.getX(), pos.getY(), pos.getZ(), ((EntityPlayerMP)this.entity).field_71093_bK);
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public boolean typeOf(int type) {
        return type == 1 ? true : super.typeOf(type);
    }

    @Override
    public boolean hasPermission(String permission) {
        return CustomNpcsPermissions.hasPermissionString((EntityPlayer)this.entity, permission);
    }

    @Override
    public IPixelmonPlayerData getPixelmonData() {
        if (!PixelmonHelper.Enabled) {
            throw new CustomNPCsException("Pixelmon isnt installed", new Object[0]);
        }
        return new IPixelmonPlayerData(){

            @Override
            public Object getParty() {
                if (PlayerWrapper.this.pixelmonPartyStorage == null) {
                    PlayerWrapper.this.pixelmonPartyStorage = PixelmonHelper.getParty((EntityPlayerMP)PlayerWrapper.this.entity);
                }
                return PlayerWrapper.this.pixelmonPartyStorage;
            }

            @Override
            public Object getPC() {
                if (PlayerWrapper.this.pixelmonPCStorage == null) {
                    PlayerWrapper.this.pixelmonPCStorage = PixelmonHelper.getPc((EntityPlayerMP)PlayerWrapper.this.entity);
                }
                return PlayerWrapper.this.pixelmonPCStorage;
            }
        };
    }

    private PlayerData getData() {
        if (this.data == null) {
            this.data = PlayerData.get((EntityPlayer)this.entity);
        }
        return this.data;
    }

    @Override
    public ITimers getTimers() {
        return this.getData().timers;
    }

    @Override
    public void removeDialog(int id) {
        PlayerData data = this.getData();
        data.dialogData.dialogsRead.remove(id);
        data.updateClient = true;
    }

    @Override
    public void addDialog(int id) {
        PlayerData data = this.getData();
        data.dialogData.dialogsRead.add(id);
        data.updateClient = true;
    }

    @Override
    public void closeGui() {
        ((EntityPlayerMP)this.entity).func_71128_l();
        Server.sendData((EntityPlayerMP)this.entity, EnumPacketClient.GUI_CLOSE, -1, new NBTTagCompound());
    }

    @Override
    public int factionStatus(int factionId) {
        Faction faction = FactionController.instance.getFaction(factionId);
        if (faction == null) {
            throw new CustomNPCsException("Unknown faction: " + factionId, new Object[0]);
        }
        return faction.playerStatus(this);
    }

    @Override
    public void kick(String message) {
        ((EntityPlayerMP)this.entity).field_71135_a.func_194028_b((ITextComponent)new TextComponentTranslation(message, new Object[0]));
    }

    @Override
    public boolean canQuestBeAccepted(int questId) {
        return PlayerQuestController.canQuestBeAccepted((EntityPlayer)this.entity, questId);
    }

    @Override
    public void clearData() {
        PlayerData data = this.getData();
        data.setNBT(new NBTTagCompound());
        data.save(true);
    }

    @Override
    public IContainer showChestGui(int rows) {
        ScriptContainer current = ScriptContainer.Current;
        this.closeGui();
        ((EntityPlayerMP)this.entity).openGui((Object)CustomNpcs.instance, EnumGuiType.CustomChest.ordinal(), ((EntityPlayerMP)this.entity).field_70170_p, rows, 0, 0);
        ContainerCustomChestWrapper container = (ContainerCustomChestWrapper)NpcAPI.Instance().getIContainer(((EntityPlayerMP)this.entity).field_71070_bA);
        container.script = current;
        return container;
    }

    @Override
    public IContainer getOpenContainer() {
        return NpcAPI.Instance().getIContainer(((EntityPlayerMP)this.entity).field_71070_bA);
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        BlockPos pos = ((EntityPlayerMP)this.entity).func_180425_c();
        Server.sendData((EntityPlayerMP)this.entity, EnumPacketClient.PLAY_SOUND, sound, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), Float.valueOf(volume), Float.valueOf(pitch));
    }

    @Override
    public void sendMail(IPlayerMail mail) {
        PlayerData data = this.getData();
        data.mailData.playermail.add(((PlayerMail)mail).copy());
        data.save(false);
    }
}

