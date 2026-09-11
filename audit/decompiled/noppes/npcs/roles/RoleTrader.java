/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.role.IRoleTrader;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleInterface;
import noppes.npcs.util.NBTJsonUtil;

public class RoleTrader
extends RoleInterface
implements IRoleTrader {
    public String marketName = "";
    public NpcMiscInventory inventoryCurrency = new NpcMiscInventory(36);
    public NpcMiscInventory inventorySold = new NpcMiscInventory(18);
    public boolean ignoreDamage = false;
    public boolean ignoreNBT = false;
    public boolean toSave = false;

    public RoleTrader(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.func_74778_a("TraderMarket", this.marketName);
        this.writeNBT(nbttagcompound);
        if (this.toSave && !this.npc.isRemote()) {
            RoleTrader.save(this, this.marketName);
        }
        this.toSave = false;
        return nbttagcompound;
    }

    public NBTTagCompound writeNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.func_74782_a("TraderCurrency", (NBTBase)this.inventoryCurrency.getToNBT());
        nbttagcompound.func_74782_a("TraderSold", (NBTBase)this.inventorySold.getToNBT());
        nbttagcompound.func_74757_a("TraderIgnoreDamage", this.ignoreDamage);
        nbttagcompound.func_74757_a("TraderIgnoreNBT", this.ignoreNBT);
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        this.marketName = nbttagcompound.func_74779_i("TraderMarket");
        this.readNBT(nbttagcompound);
    }

    public void readNBT(NBTTagCompound nbttagcompound) {
        this.inventoryCurrency.setFromNBT(nbttagcompound.func_74775_l("TraderCurrency"));
        this.inventorySold.setFromNBT(nbttagcompound.func_74775_l("TraderSold"));
        this.ignoreDamage = nbttagcompound.func_74767_n("TraderIgnoreDamage");
        this.ignoreNBT = nbttagcompound.func_74767_n("TraderIgnoreNBT");
    }

    @Override
    public void interact(EntityPlayer player) {
        this.npc.say(player, this.npc.advanced.getInteractLine());
        try {
            RoleTrader.load(this, this.marketName);
        }
        catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        NoppesUtilServer.sendOpenGui(player, EnumGuiType.PlayerTrader, this.npc);
    }

    public boolean hasCurrency(ItemStack itemstack) {
        if (itemstack == null) {
            return false;
        }
        for (ItemStack item : this.inventoryCurrency.items) {
            if (item.func_190926_b() || !NoppesUtilPlayer.compareItems(item, itemstack, this.ignoreDamage, this.ignoreNBT)) continue;
            return true;
        }
        return false;
    }

    @Override
    public IItemStack getSold(int slot) {
        return NpcAPI.Instance().getIItemStack(this.inventorySold.func_70301_a(slot));
    }

    @Override
    public IItemStack getCurrency1(int slot) {
        return NpcAPI.Instance().getIItemStack(this.inventoryCurrency.func_70301_a(slot));
    }

    @Override
    public IItemStack getCurrency2(int slot) {
        return NpcAPI.Instance().getIItemStack(this.inventoryCurrency.func_70301_a(slot + 18));
    }

    @Override
    public void set(int slot, IItemStack currency, IItemStack currency2, IItemStack sold) {
        if (sold == null) {
            throw new CustomNPCsException("Sold item was null", new Object[0]);
        }
        if (slot >= 18 || slot < 0) {
            throw new CustomNPCsException("Invalid slot: " + slot, new Object[0]);
        }
        if (currency == null) {
            currency = currency2;
            currency2 = null;
        }
        if (currency != null) {
            this.inventoryCurrency.items.set(slot, (Object)currency.getMCItemStack());
        } else {
            this.inventoryCurrency.items.set(slot, (Object)ItemStack.field_190927_a);
        }
        if (currency2 != null) {
            this.inventoryCurrency.items.set(slot + 18, (Object)currency2.getMCItemStack());
        } else {
            this.inventoryCurrency.items.set(slot + 18, (Object)ItemStack.field_190927_a);
        }
        this.inventorySold.items.set(slot, (Object)sold.getMCItemStack());
    }

    @Override
    public void remove(int slot) {
        if (slot >= 18 || slot < 0) {
            throw new CustomNPCsException("Invalid slot: " + slot, new Object[0]);
        }
        this.inventoryCurrency.items.set(slot, (Object)ItemStack.field_190927_a);
        this.inventoryCurrency.items.set(slot + 18, (Object)ItemStack.field_190927_a);
        this.inventorySold.items.set(slot, (Object)ItemStack.field_190927_a);
    }

    @Override
    public void setMarket(String name) {
        this.marketName = name;
        RoleTrader.load(this, name);
    }

    @Override
    public String getMarket() {
        return this.marketName;
    }

    public static void save(RoleTrader r, String name) {
        if (name.isEmpty()) {
            return;
        }
        File file = RoleTrader.getFile(name + "_new");
        File file1 = RoleTrader.getFile(name);
        try {
            NBTJsonUtil.SaveFile(file, r.writeNBT(new NBTTagCompound()));
            if (file1.exists()) {
                file1.delete();
            }
            file.renameTo(file1);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void load(RoleTrader role, String name) {
        if (role.npc.field_70170_p.field_72995_K) {
            return;
        }
        File file = RoleTrader.getFile(name);
        if (!file.exists()) {
            return;
        }
        try {
            role.readNBT(NBTJsonUtil.LoadFile(file));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static File getFile(String name) {
        File dir = new File(CustomNpcs.getWorldSaveDirectory(), "markets");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return new File(dir, name.toLowerCase() + ".json");
    }

    public static void setMarket(EntityNPCInterface npc, String marketName) {
        if (marketName.isEmpty()) {
            return;
        }
        if (!RoleTrader.getFile(marketName).exists()) {
            RoleTrader.save((RoleTrader)npc.roleInterface, marketName);
        }
        RoleTrader.load((RoleTrader)npc.roleInterface, marketName);
    }
}

