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
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.rework.data.WorldSaveSession;

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
            this.playername = this.player.getName();
            this.uuid = this.player.getPersistentID().toString();
        } else {
            this.playername = data.getString("PlayerName");
            this.uuid = data.getString("UUID");
        }
        this.companionID = data.getInteger("PlayerCompanionId");
        if (data.hasKey("PlayerCompanion") && !this.hasCompanion()) {
            EntityCustomNpc npc = new EntityCustomNpc(this.player.world);
            npc.readEntityFromNBT(data.getCompoundTag("PlayerCompanion"));
            npc.setPosition(this.player.posX, this.player.posY, this.player.posZ);
            if (npc.advanced.role == 6) {
                this.setCompanion(npc);
                ((RoleCompanion)npc.roleInterface).setSitting(false);
                this.player.world.spawnEntity((Entity)npc);
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
            this.playername = this.player.getName();
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
        compound.setString("PlayerName", this.playername);
        compound.setString("UUID", this.uuid);
        compound.setInteger("PlayerCompanionId", this.companionID);
        if (this.hasCompanion() && this.activeCompanion.writeToNBTAtomically(nbt = new NBTTagCompound())) {
            compound.setTag("PlayerCompanion", (NBTBase)nbt);
        }
        return compound;
    }

    public boolean hasCompanion() {
        return this.activeCompanion != null && !this.activeCompanion.isDead;
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
        if (!this.hasCompanion() || world == this.activeCompanion.world) {
            return;
        }
        RoleCompanion role = (RoleCompanion)this.activeCompanion.roleInterface;
        role.owner = this.player;
        if (!role.isFollowing()) {
            return;
        }
        NBTTagCompound nbt = new NBTTagCompound();
        this.activeCompanion.writeToNBTAtomically(nbt);
        this.activeCompanion.isDead = true;
        EntityCustomNpc npc = new EntityCustomNpc(world);
        npc.readEntityFromNBT(nbt);
        npc.setPosition(this.player.posX, this.player.posY, this.player.posZ);
        this.setCompanion(npc);
        ((RoleCompanion)npc.roleInterface).setSitting(false);
        world.spawnEntity((Entity)npc);
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
        WorldSaveSession session = WorldSaveSession.current();
        if (session != null) {
            if (!session.savePlayerData(filename, compound)) {
                LogWriter.error("Player data save rejected because the world is stopping: " + filename);
            }
        } else {
            try {
                File saveDir = CustomNpcs.getWorldSaveDirectory("playerdata");
                File file = new File(saveDir, filename);
                NBTJsonUtil.SaveFile(file, compound);
            }
            catch (Exception e) {
                LogWriter.except(e);
            }
        }
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
                NBTTagCompound comp = CompressedStreamTools.readCompressed((InputStream)new FileInputStream(file));
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
                return CompressedStreamTools.readCompressed((InputStream)new FileInputStream(file));
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
        if (player.world.isRemote) {
            return CustomNpcs.proxy.getPlayerData(player);
        }
        PlayerData data = (PlayerData)player.getCapability(PLAYERDATA_CAPABILITY, null);
        if (data.player == null) {
            data.player = player;
            data.playerLevel = player.experienceLevel;
            data.scriptData = new PlayerScriptData(player);
            NBTTagCompound compound = PlayerData.loadPlayerData(player.getPersistentID().toString());
            if (compound.hasNoTags()) {
                compound = PlayerData.loadPlayerDataOld(player.getName());
            }
            data.setNBT(compound);
        }
        return data;
    }
}

