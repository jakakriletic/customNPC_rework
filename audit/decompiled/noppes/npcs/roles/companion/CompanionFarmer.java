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
        compound.func_74757_a("CompanionFarmerStanding", this.isStanding);
        return compound;
    }

    @Override
    public void setNBT(NBTTagCompound compound) {
        this.isStanding = compound.func_74767_n("CompanionFarmerStanding");
    }

    @Override
    public boolean isSelfSufficient() {
        return this.isStanding;
    }

    @Override
    public void onUpdate() {
    }
}

