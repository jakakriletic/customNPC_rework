/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleInterface;

public class RolePostman
extends RoleInterface {
    public NpcMiscInventory inventory = new NpcMiscInventory(1);
    private List<EntityPlayer> recentlyChecked = new ArrayList<EntityPlayer>();
    private List<EntityPlayer> toCheck;

    public RolePostman(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public boolean aiShouldExecute() {
        if (this.npc.ticksExisted % 20 != 0) {
            return false;
        }
        this.toCheck = this.npc.world.getEntitiesWithinAABB(EntityPlayer.class, this.npc.getEntityBoundingBox().grow(10.0, 10.0, 10.0));
        this.toCheck.removeAll(this.recentlyChecked);
        List listMax = this.npc.world.getEntitiesWithinAABB(EntityPlayer.class, this.npc.getEntityBoundingBox().grow(20.0, 20.0, 20.0));
        this.recentlyChecked.retainAll(listMax);
        this.recentlyChecked.addAll(this.toCheck);
        for (EntityPlayer player : this.toCheck) {
            if (!PlayerData.get((EntityPlayer)player).mailData.hasMail()) continue;
            player.sendMessage((ITextComponent)new TextComponentTranslation("You've got mail", new Object[0]));
        }
        return false;
    }

    @Override
    public boolean aiContinueExecute() {
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.setTag("PostInv", (NBTBase)this.inventory.getToNBT());
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        this.inventory.setFromNBT(nbttagcompound.getCompoundTag("PostInv"));
    }

    @Override
    public void interact(EntityPlayer player) {
        player.openGui((Object)CustomNpcs.instance, EnumGuiType.PlayerMailman.ordinal(), player.world, 1, 1, 0);
    }
}

