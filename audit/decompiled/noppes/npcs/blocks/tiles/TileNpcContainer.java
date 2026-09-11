/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ItemStackHelper
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.NonNullList
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks.tiles;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.tiles.TileColorable;

public abstract class TileNpcContainer
extends TileColorable
implements IInventory {
    public final NonNullList<ItemStack> inventoryContents = NonNullList.func_191197_a((int)this.func_70302_i_(), (Object)ItemStack.field_190927_a);
    public String customName = "";
    public int playerUsing = 0;

    @Override
    public void func_145839_a(NBTTagCompound compound) {
        super.func_145839_a(compound);
        NBTTagList nbttaglist = compound.func_150295_c("Items", 10);
        if (compound.func_150297_b("CustomName", 8)) {
            this.customName = compound.func_74779_i("CustomName");
        }
        this.inventoryContents.clear();
        for (int i = 0; i < nbttaglist.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound1 = nbttaglist.func_150305_b(i);
            int j = nbttagcompound1.func_74771_c("Slot") & 0xFF;
            if (j < 0 || j >= this.inventoryContents.size()) continue;
            this.inventoryContents.set(j, (Object)new ItemStack(nbttagcompound1));
        }
    }

    @Override
    public NBTTagCompound func_189515_b(NBTTagCompound compound) {
        NBTTagList nbttaglist = new NBTTagList();
        for (int i = 0; i < this.inventoryContents.size(); ++i) {
            if (((ItemStack)this.inventoryContents.get(i)).func_190926_b()) continue;
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.func_74774_a("Slot", (byte)i);
            ((ItemStack)this.inventoryContents.get(i)).func_77955_b(tagCompound);
            nbttaglist.func_74742_a((NBTBase)tagCompound);
        }
        compound.func_74782_a("Items", (NBTBase)nbttaglist);
        if (this.func_145818_k_()) {
            compound.func_74778_a("CustomName", this.customName);
        }
        return super.func_189515_b(compound);
    }

    public boolean func_145842_c(int id, int type) {
        if (id == 1) {
            this.playerUsing = type;
            return true;
        }
        return super.func_145842_c(id, type);
    }

    public int func_70302_i_() {
        return 54;
    }

    public ItemStack func_70301_a(int index) {
        return (ItemStack)this.inventoryContents.get(index);
    }

    public ItemStack func_70298_a(int index, int count) {
        ItemStack itemstack = ItemStackHelper.func_188382_a(this.inventoryContents, (int)index, (int)count);
        if (!itemstack.func_190926_b()) {
            this.func_70296_d();
        }
        return itemstack;
    }

    public ItemStack func_70304_b(int index) {
        return (ItemStack)this.inventoryContents.set(index, (Object)ItemStack.field_190927_a);
    }

    public void func_70299_a(int index, ItemStack stack) {
        this.inventoryContents.set(index, (Object)stack);
        if (stack.func_190916_E() > this.func_70297_j_()) {
            stack.func_190920_e(this.func_70297_j_());
        }
        this.func_70296_d();
    }

    public ITextComponent func_145748_c_() {
        return new TextComponentString(this.func_145818_k_() ? this.customName : this.func_70005_c_());
    }

    public abstract String func_70005_c_();

    public boolean func_145818_k_() {
        return !this.customName.isEmpty();
    }

    public int func_70297_j_() {
        return 64;
    }

    public boolean func_70300_a(EntityPlayer player) {
        return (player.field_70128_L || this.field_145850_b.func_175625_s(this.field_174879_c) == this) && player.func_70092_e((double)this.field_174879_c.func_177958_n() + 0.5, (double)this.field_174879_c.func_177956_o() + 0.5, (double)this.field_174879_c.func_177952_p() + 0.5) <= 64.0;
    }

    public void func_174889_b(EntityPlayer player) {
        ++this.playerUsing;
    }

    public void func_174886_c(EntityPlayer player) {
        --this.playerUsing;
    }

    public int func_174887_a_(int id) {
        return 0;
    }

    public void func_174885_b(int id, int value) {
    }

    public int func_174890_g() {
        return 0;
    }

    public void func_174888_l() {
    }

    public boolean func_94041_b(int var1, ItemStack var2) {
        return true;
    }

    public void dropItems(World world, BlockPos pos) {
        for (int i1 = 0; i1 < this.func_70302_i_(); ++i1) {
            ItemStack itemstack = this.func_70301_a(i1);
            if (NoppesUtilServer.IsItemStackNull(itemstack)) continue;
            float f = world.field_73012_v.nextFloat() * 0.8f + 0.1f;
            float f1 = world.field_73012_v.nextFloat() * 0.8f + 0.1f;
            float f2 = world.field_73012_v.nextFloat() * 0.8f + 0.1f;
            while (itemstack.func_190916_E() > 0) {
                int j1 = world.field_73012_v.nextInt(21) + 10;
                if (j1 > itemstack.func_190916_E()) {
                    j1 = itemstack.func_190916_E();
                }
                itemstack.func_190920_e(itemstack.func_190916_E() - j1);
                EntityItem entityitem = new EntityItem(world, (double)((float)pos.func_177958_n() + f), (double)((float)pos.func_177956_o() + f1), (double)((float)pos.func_177952_p() + f2), new ItemStack(itemstack.func_77973_b(), j1, itemstack.func_77952_i()));
                float f3 = 0.05f;
                entityitem.field_70159_w = (float)world.field_73012_v.nextGaussian() * f3;
                entityitem.field_70181_x = (float)world.field_73012_v.nextGaussian() * f3 + 0.2f;
                entityitem.field_70179_y = (float)world.field_73012_v.nextGaussian() * f3;
                if (itemstack.func_77942_o()) {
                    entityitem.func_92059_d().func_77982_d(itemstack.func_77978_p().func_74737_b());
                }
                world.func_72838_d((Entity)entityitem);
            }
        }
    }

    public boolean func_191420_l() {
        for (int slot = 0; slot < this.func_70302_i_(); ++slot) {
            ItemStack item = this.func_70301_a(slot);
            if (NoppesUtilServer.IsItemStackNull(item) || item.func_190926_b()) continue;
            return false;
        }
        return true;
    }
}

