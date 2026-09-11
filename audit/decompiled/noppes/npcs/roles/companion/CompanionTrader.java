/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles.companion;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.roles.companion.CompanionJobInterface;

public class CompanionTrader
extends CompanionJobInterface {
    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        return compound;
    }

    @Override
    public void setNBT(NBTTagCompound compound) {
    }

    public void interact(EntityPlayer player) {
        NoppesUtilServer.sendOpenGui(player, EnumGuiType.CompanionTrader, this.npc);
    }
}

