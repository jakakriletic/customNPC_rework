/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.roles;

import java.util.HashMap;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.role.IRoleFollower;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleInterface;

public class RoleFollower
extends RoleInterface
implements IRoleFollower {
    private String ownerUUID;
    public boolean isFollowing = true;
    public HashMap<Integer, Integer> rates;
    public NpcMiscInventory inventory;
    public String dialogHire = I18n.translateToLocal((String)"follower.hireText") + " {days} " + I18n.translateToLocal((String)"follower.days");
    public String dialogFarewell = I18n.translateToLocal((String)"follower.farewellText") + " {player}";
    public int daysHired;
    public long hiredTime;
    public boolean disableGui = false;
    public boolean infiniteDays = false;
    public boolean refuseSoulStone = false;
    public EntityPlayer owner = null;

    public RoleFollower(EntityNPCInterface npc) {
        super(npc);
        this.inventory = new NpcMiscInventory(3);
        this.rates = new HashMap();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.setInteger("MercenaryDaysHired", this.daysHired);
        nbttagcompound.setLong("MercenaryHiredTime", this.hiredTime);
        nbttagcompound.setString("MercenaryDialogHired", this.dialogHire);
        nbttagcompound.setString("MercenaryDialogFarewell", this.dialogFarewell);
        if (this.hasOwner()) {
            nbttagcompound.setString("MercenaryOwner", this.ownerUUID);
        }
        nbttagcompound.setTag("MercenaryDayRates", (NBTBase)NBTTags.nbtIntegerIntegerMap(this.rates));
        nbttagcompound.setTag("MercenaryInv", (NBTBase)this.inventory.getToNBT());
        nbttagcompound.setBoolean("MercenaryIsFollowing", this.isFollowing);
        nbttagcompound.setBoolean("MercenaryDisableGui", this.disableGui);
        nbttagcompound.setBoolean("MercenaryInfiniteDays", this.infiniteDays);
        nbttagcompound.setBoolean("MercenaryRefuseSoulstone", this.refuseSoulStone);
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        this.ownerUUID = nbttagcompound.getString("MercenaryOwner");
        this.daysHired = nbttagcompound.getInteger("MercenaryDaysHired");
        this.hiredTime = nbttagcompound.getLong("MercenaryHiredTime");
        this.dialogHire = nbttagcompound.getString("MercenaryDialogHired");
        this.dialogFarewell = nbttagcompound.getString("MercenaryDialogFarewell");
        this.rates = NBTTags.getIntegerIntegerMap(nbttagcompound.getTagList("MercenaryDayRates", 10));
        this.inventory.setFromNBT(nbttagcompound.getCompoundTag("MercenaryInv"));
        this.isFollowing = nbttagcompound.getBoolean("MercenaryIsFollowing");
        this.disableGui = nbttagcompound.getBoolean("MercenaryDisableGui");
        this.infiniteDays = nbttagcompound.getBoolean("MercenaryInfiniteDays");
        this.refuseSoulStone = nbttagcompound.getBoolean("MercenaryRefuseSoulstone");
    }

    @Override
    public boolean aiShouldExecute() {
        this.owner = this.getOwner();
        if (!this.infiniteDays && this.owner != null && this.getDays() <= 0) {
            RoleEvent.FollowerFinishedEvent event = new RoleEvent.FollowerFinishedEvent(this.owner, this.npc.wrappedNPC);
            EventHooks.onNPCRole(this.npc, event);
            this.owner.sendMessage((ITextComponent)new TextComponentTranslation(NoppesStringUtils.formatText(this.dialogFarewell, new Object[]{this.owner, this.npc}), new Object[0]));
            this.killed();
        }
        return false;
    }

    public EntityPlayer getOwner() {
        if (this.ownerUUID == null || this.ownerUUID.isEmpty()) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(this.ownerUUID);
            if (uuid != null) {
                return this.npc.world.getPlayerEntityByUUID(uuid);
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
        return this.npc.world.getPlayerEntityByName(this.ownerUUID);
    }

    public boolean hasOwner() {
        if (!this.infiniteDays && this.daysHired <= 0) {
            return false;
        }
        return this.ownerUUID != null && !this.ownerUUID.isEmpty();
    }

    @Override
    public void killed() {
        this.ownerUUID = null;
        this.daysHired = 0;
        this.hiredTime = 0L;
        this.isFollowing = true;
    }

    @Override
    public void reset() {
        this.killed();
    }

    @Override
    public void interact(EntityPlayer player) {
        if (this.ownerUUID == null || this.ownerUUID.isEmpty()) {
            this.npc.say(player, this.npc.advanced.getInteractLine());
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.PlayerFollowerHire, this.npc);
        } else if (player == this.owner && !this.disableGui) {
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.PlayerFollower, this.npc);
        }
    }

    @Override
    public boolean defendOwner() {
        return this.isFollowing() && this.npc.advanced.job == 3;
    }

    @Override
    public void delete() {
    }

    @Override
    public boolean isFollowing() {
        return this.owner != null && this.isFollowing && this.getDays() > 0;
    }

    public void setOwner(EntityPlayer player) {
        UUID id = player.getUniqueID();
        if (this.ownerUUID == null || id == null || !this.ownerUUID.equals(id.toString())) {
            this.killed();
        }
        this.ownerUUID = id.toString();
    }

    @Override
    public int getDays() {
        if (this.infiniteDays) {
            return 100;
        }
        if (this.daysHired <= 0) {
            return 0;
        }
        int days = (int)((this.npc.world.getTotalWorldTime() - this.hiredTime) / 24000L);
        return this.daysHired - days;
    }

    @Override
    public void addDays(int days) {
        this.daysHired = days + this.getDays();
        this.hiredTime = this.npc.world.getTotalWorldTime();
    }

    @Override
    public boolean getInfinite() {
        return this.infiniteDays;
    }

    @Override
    public void setInfinite(boolean infinite) {
        this.infiniteDays = infinite;
    }

    @Override
    public boolean getGuiDisabled() {
        return this.disableGui;
    }

    @Override
    public void setGuiDisabled(boolean disabled) {
        this.disableGui = disabled;
    }

    @Override
    public boolean getRefuseSoulstone() {
        return this.refuseSoulStone;
    }

    @Override
    public void setRefuseSoulstone(boolean refuse) {
        this.refuseSoulStone = refuse;
    }

    @Override
    public IPlayer getFollowing() {
        EntityPlayer owner = this.getOwner();
        if (owner != null) {
            return (IPlayer)NpcAPI.Instance().getIEntity((Entity)owner);
        }
        return null;
    }

    @Override
    public void setFollowing(IPlayer player) {
        if (player == null) {
            this.setOwner(null);
        } else {
            this.setOwner((EntityPlayer)player.getMCEntity());
        }
    }
}

