/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.CapabilityInject
 *  net.minecraftforge.common.capabilities.ICapabilityProvider
 *  net.minecraftforge.event.AttachCapabilitiesEvent
 */
package noppes.npcs.controllers.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.controllers.data.PlayerBankData;
import noppes.npcs.controllers.data.PlayerDialogData;
import noppes.npcs.controllers.data.PlayerFactionData;
import noppes.npcs.controllers.data.PlayerItemGiverData;
import noppes.npcs.controllers.data.PlayerMailData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.PlayerScriptData;
import noppes.npcs.controllers.data.PlayerTransportData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.NBTJsonUtil;

public class PlayerData
implements ICapabilityProvider {
    @CapabilityInject(value=PlayerData.class)
    public static Capability<PlayerData> PLAYERDATA_CAPABILITY = null;
    public PlayerDialogData dialogData = new PlayerDialogData();
    public PlayerBankData bankData = new PlayerBankData();
    public PlayerQuestData questData = new PlayerQuestData();
    public PlayerTransportData transportData = new PlayerTransportData();
    public PlayerFactionData factionData = new PlayerFactionData();
    public PlayerItemGiverData itemgiverData = new PlayerItemGiverData();
    public PlayerMailData mailData = new PlayerMailData();
    public PlayerScriptData scriptData;
    public DataTimers timers = new DataTimers(this);
    public EntityNPCInterface editingNpc;
    public NBTTagCompound cloned;
    public EntityPlayer player;
    public String playername = "";
    public String uuid = "";
    private EntityNPCInterface activeCompanion = null;
    public int companionID = 0;
    public int playerLevel = 0;
    public boolean updateClient = false;
    public int dialogId = -1;
    private static final ResourceLocation key = new ResourceLocation("customnpcs", "playerdata");

    public void setNBT(NBTTagCompound data) {
        this.dialogData.loadNBTData(data);
        this.bankData.loadNBTData(data);
        this.questData.loadNBTData(data);
        this.transportData.loadNBTData(data);
        this.factionData.loadNBTData(data);
        this.itemgiverData.loadNBTData(data);
        this.mailData.loadNBTData(data);
        this.timers.readFromNBT(data);
        if (this.player != null) {
            this.playername = this.player.func_70005_c_();
            this.uuid = this.player.getPersistentID().toString();
        } else {
            this.playername = data.func_74779_i("PlayerName");
            this.uuid = data.func_74779_i("UUID");
        }
        this.companionID = data.func_74762_e("PlayerCompanionId");
        if (data.func_74764_b("PlayerCompanion") && !this.hasCompanion()) {
            EntityCustomNpc npc = new EntityCustomNpc(this.player.field_70170_p);
            npc.func_70037_a(data.func_74775_l("PlayerCompanion"));
            npc.func_70107_b(this.player.field_70165_t, this.player.field_70163_u, this.player.field_70161_v);
            if (npc.advanced.role == 6) {
                this.setCompanion(npc);
                ((RoleCompanion)npc.roleInterface).setSitting(false);
                this.player.field_70170_p.func_72838_d((Entity)npc);
            }
        }
    }

    public NBTTagCompound getSyncNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        this.dialogData.saveNBTData(compound);
        this.questData.saveNBTData(compound);
        this.factionData.saveNBTData(compound);
        return compound;
    }

    public NBTTagCompound getNBT() {
        NBTTagCompound nbt;
        if (this.player != null) {
            this.playername = this.player.func_70005_c_();
            this.uuid = this.player.getPersistentID().toString();
        }
        NBTTagCompound compound = new NBTTagCompound();
        this.dialogData.saveNBTData(compound);
        this.bankData.saveNBTData(compound);
        this.questData.saveNBTData(compound);
        this.transportData.saveNBTData(compound);
        this.factionData.saveNBTData(compound);
        this.itemgiverData.saveNBTData(compound);
        this.mailData.saveNBTData(compound);
        this.timers.writeToNBT(compound);
        compound.func_74778_a("PlayerName", this.playername);
        compound.func_74778_a("UUID", this.uuid);
        compound.func_74768_a("PlayerCompanionId", this.companionID);
        if (this.hasCompanion() && this.activeCompanion.func_184198_c(nbt = new NBTTagCompound())) {
            compound.func_74782_a("PlayerCompanion", (NBTBase)nbt);
        }
        return compound;
    }

    public boolean hasCompanion() {
        return this.activeCompanion != null && !this.activeCompanion.field_70128_L;
    }

    public void setCompanion(EntityNPCInterface npc) {
        if (npc != null && npc.advanced.role != 6) {
            return;
        }
        ++this.companionID;
        this.activeCompanion = npc;
        if (npc != null) {
            ((RoleCompanion)npc.roleInterface).companionID = this.companionID;
        }
        this.save(false);
    }

    public void updateCompanion(World world) {
        if (!this.hasCompanion() || world == this.activeCompanion.field_70170_p) {
            return;
        }
        RoleCompanion role = (RoleCompanion)this.activeCompanion.roleInterface;
        role.owner = this.player;
        if (!role.isFollowing()) {
            return;
        }
        NBTTagCompound nbt = new NBTTagCompound();
        this.activeCompanion.func_184198_c(nbt);
        this.activeCompanion.field_70128_L = true;
        EntityCustomNpc npc = new EntityCustomNpc(world);
        npc.func_70037_a(nbt);
        npc.func_70107_b(this.player.field_70165_t, this.player.field_70163_u, this.player.field_70161_v);
        this.setCompanion(npc);
        ((RoleCompanion)npc.roleInterface).setSitting(false);
        world.func_72838_d((Entity)npc);
    }

    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == PLAYERDATA_CAPABILITY;
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (this.hasCapability(capability, facing)) {
            return (T)this;
        }
        return null;
    }

    public static void register(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(key, (ICapabilityProvider)new PlayerData());
        }
    }

    public synchronized void save(boolean update) {
        NBTTagCompound compound = this.getNBT();
        String filename = this.uuid + ".json";
        CustomNPCsScheduler.runTack(() -> {
            try {
                File saveDir = CustomNpcs.getWorldSaveDirectory("playerdata");
                File file = new File(saveDir, filename + "_new");
                File file1 = new File(saveDir, filename);
                NBTJsonUtil.SaveFile(file, compound);
                if (file1.exists()) {
                    file1.delete();
                }
                file.renameTo(file1);
            }
            catch (Exception e) {
                LogWriter.except(e);
            }
        });
        if (update) {
            this.updateClient = true;
        }
    }

    public static NBTTagCompound loadPlayerDataOld(String player) {
        File file;
        File saveDir = CustomNpcs.getWorldSaveDirectory("playerdata");
        String filename = player;
        if (filename.isEmpty()) {
            filename = "noplayername";
        }
        filename = filename + ".dat";
        try {
            file = new File(saveDir, filename);
            if (file.exists()) {
                NBTTagCompound comp = CompressedStreamTools.func_74796_a((InputStream)new FileInputStream(file));
                file.delete();
                file = new File(saveDir, filename + "_old");
                if (file.exists()) {
                    file.delete();
                }
                return comp;
            }
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
        try {
            file = new File(saveDir, filename + "_old");
            if (file.exists()) {
                return CompressedStreamTools.func_74796_a((InputStream)new FileInputStream(file));
            }
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
        return new NBTTagCompound();
    }

    public static NBTTagCompound loadPlayerData(String player) {
        File saveDir = CustomNpcs.getWorldSaveDirectory("playerdata");
        String filename = player;
        if (filename.isEmpty()) {
            filename = "noplayername";
        }
        filename = filename + ".json";
        File file = null;
        try {
            file = new File(saveDir, filename);
            if (file.exists()) {
                return NBTJsonUtil.LoadFile(file);
            }
        }
        catch (Exception e) {
            LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
        }
        return new NBTTagCompound();
    }

    public static PlayerData get(EntityPlayer player) {
        if (player.field_70170_p.field_72995_K) {
            return CustomNpcs.proxy.getPlayerData(player);
        }
        PlayerData data = (PlayerData)player.getCapability(PLAYERDATA_CAPABILITY, null);
        if (data.player == null) {
            data.player = player;
            data.playerLevel = player.field_71068_ca;
            data.scriptData = new PlayerScriptData(player);
            NBTTagCompound compound = PlayerData.loadPlayerData(player.getPersistentID().toString());
            if (compound.func_82582_d()) {
                compound = PlayerData.loadPlayerDataOld(player.func_70005_c_());
            }
            data.setNBT(compound);
        }
        return data;
    }
}

