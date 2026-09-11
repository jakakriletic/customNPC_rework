/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.HashMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.BankData;

public class PlayerBankData {
    public HashMap<Integer, BankData> banks = new HashMap();

    public void loadNBTData(NBTTagCompound compound) {
        HashMap<Integer, BankData> banks = new HashMap<Integer, BankData>();
        NBTTagList list = compound.func_150295_c("BankData", 10);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = list.func_150305_b(i);
            BankData data = new BankData();
            data.readNBT(nbttagcompound);
            banks.put(data.bankId, data);
        }
        this.banks = banks;
    }

    public void saveNBTData(NBTTagCompound playerData) {
        NBTTagList list = new NBTTagList();
        for (BankData data : this.banks.values()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            data.writeNBT(nbttagcompound);
            list.func_74742_a((NBTBase)nbttagcompound);
        }
        playerData.func_74782_a("BankData", (NBTBase)list);
    }

    public BankData getBank(int bankId) {
        return this.banks.get(bankId);
    }

    public BankData getBankOrDefault(int bankId) {
        BankData data = this.banks.get(bankId);
        if (data != null) {
            return data;
        }
        Bank bank = BankController.getInstance().getBank(bankId);
        return this.banks.get(bank.id);
    }

    public boolean hasBank(int bank) {
        return this.banks.containsKey(bank);
    }

    public void loadNew(int bank) {
        BankData data = new BankData();
        data.bankId = bank;
        this.banks.put(bank, data);
    }
}

