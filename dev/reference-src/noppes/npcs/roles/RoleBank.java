/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.BankData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleInterface;

public class RoleBank
extends RoleInterface {
    public int bankId = -1;

    public RoleBank(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.setInteger("RoleBankID", this.bankId);
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        this.bankId = nbttagcompound.getInteger("RoleBankID");
    }

    @Override
    public void interact(EntityPlayer player) {
        BankData data = PlayerDataController.instance.getBankData(player, this.bankId).getBankOrDefault(this.bankId);
        data.openBankGui(player, this.npc, this.bankId, 0);
        this.npc.say(player, this.npc.advanced.getInteractLine());
    }

    public Bank getBank() {
        Bank bank = BankController.getInstance().banks.get(this.bankId);
        if (bank != null) {
            return bank;
        }
        return BankController.getInstance().banks.values().iterator().next();
    }
}

