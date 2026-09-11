/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.monster.EntityCreeper
 *  net.minecraft.entity.monster.IMob
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles.companion;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.companion.CompanionJobInterface;

public class CompanionGuard
extends CompanionJobInterface {
    public boolean isStanding = false;

    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74757_a("CompanionGuardStanding", this.isStanding);
        return compound;
    }

    @Override
    public void setNBT(NBTTagCompound compound) {
        this.isStanding = compound.func_74767_n("CompanionGuardStanding");
    }

    public boolean isEntityApplicable(Entity entity) {
        if (entity instanceof EntityPlayer || entity instanceof EntityNPCInterface) {
            return false;
        }
        if (entity instanceof EntityCreeper) {
            return false;
        }
        return entity instanceof IMob;
    }

    @Override
    public boolean isSelfSufficient() {
        return this.isStanding;
    }
}

