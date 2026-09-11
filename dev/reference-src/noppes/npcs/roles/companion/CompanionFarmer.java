/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles.companion;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.roles.companion.CompanionJobInterface;

public class CompanionFarmer
extends CompanionJobInterface {
    public boolean isStanding = false;

    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setBoolean("CompanionFarmerStanding", this.isStanding);
        return compound;
    }

    @Override
    public void setNBT(NBTTagCompound compound) {
        this.isStanding = compound.getBoolean("CompanionFarmerStanding");
    }

    @Override
    public boolean isSelfSufficient() {
        return this.isStanding;
    }

    @Override
    public void onUpdate() {
    }
}

