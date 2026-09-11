/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.item.EntityXPOrb
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraftforge.common.ForgeHooks
 */
package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.INPCInventory;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class DataInventory
implements IInventory,
INPCInventory {
    public Map<Integer, IItemStack> drops = new HashMap<Integer, IItemStack>();
    public Map<Integer, Integer> dropchance = new HashMap<Integer, Integer>();
    public Map<Integer, IItemStack> weapons = new HashMap<Integer, IItemStack>();
    public Map<Integer, IItemStack> armor = new HashMap<Integer, IItemStack>();
    private int minExp = 0;
    private int maxExp = 0;
    public int lootMode = 0;
    private EntityNPCInterface npc;

    public DataInventory(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public NBTTagCompound writeEntityToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.func_74768_a("MinExp", this.minExp);
        nbttagcompound.func_74768_a("MaxExp", this.maxExp);
        nbttagcompound.func_74782_a("NpcInv", (NBTBase)NBTTags.nbtIItemStackMap(this.drops));
        nbttagcompound.func_74782_a("Armor", (NBTBase)NBTTags.nbtIItemStackMap(this.armor));
        nbttagcompound.func_74782_a("Weapons", (NBTBase)NBTTags.nbtIItemStackMap(this.weapons));
        nbttagcompound.func_74782_a("DropChance", (NBTBase)NBTTags.nbtIntegerIntegerMap(this.dropchance));
        nbttagcompound.func_74768_a("LootMode", this.lootMode);
        return nbttagcompound;
    }

    public void readEntityFromNBT(NBTTagCompound nbttagcompound) {
        this.minExp = nbttagcompound.func_74762_e("MinExp");
        this.maxExp = nbttagcompound.func_74762_e("MaxExp");
        this.drops = NBTTags.getIItemStackMap(nbttagcompound.func_150295_c("NpcInv", 10));
        this.armor = NBTTags.getIItemStackMap(nbttagcompound.func_150295_c("Armor", 10));
        this.weapons = NBTTags.getIItemStackMap(nbttagcompound.func_150295_c("Weapons", 10));
        this.dropchance = NBTTags.getIntegerIntegerMap(nbttagcompound.func_150295_c("DropChance", 10));
        this.lootMode = nbttagcompound.func_74762_e("LootMode");
    }

    @Override
    public IItemStack getArmor(int slot) {
        return this.armor.get(slot);
    }

    @Override
    public void setArmor(int slot, IItemStack item) {
        this.armor.put(slot, item);
        this.npc.updateClient = true;
    }

    @Override
    public IItemStack getRightHand() {
        return this.weapons.get(0);
    }

    @Override
    public void setRightHand(IItemStack item) {
        this.weapons.put(0, item);
        this.npc.updateClient = true;
    }

    @Override
    public IItemStack getProjectile() {
        return this.weapons.get(1);
    }

    @Override
    public void setProjectile(IItemStack item) {
        this.weapons.put(1, item);
        this.npc.updateAI = true;
    }

    @Override
    public IItemStack getLeftHand() {
        return this.weapons.get(2);
    }

    @Override
    public void setLeftHand(IItemStack item) {
        this.weapons.put(2, item);
        this.npc.updateClient = true;
    }

    @Override
    public IItemStack getDropItem(int slot) {
        if (slot < 0 || slot > 8) {
            throw new CustomNPCsException("Bad slot number: " + slot, new Object[0]);
        }
        IItemStack item = this.npc.inventory.drops.get(slot);
        if (item == null) {
            return null;
        }
        return NpcAPI.Instance().getIItemStack(item.getMCItemStack());
    }

    @Override
    public void setDropItem(int slot, IItemStack item, int chance) {
        if (slot < 0 || slot > 8) {
            throw new CustomNPCsException("Bad slot number: " + slot, new Object[0]);
        }
        chance = ValueUtil.CorrectInt(chance, 1, 100);
        if (item == null || item.isEmpty()) {
            this.dropchance.remove(slot);
            this.drops.remove(slot);
        } else {
            this.dropchance.put(slot, chance);
            this.drops.put(slot, item);
        }
    }

    public void dropStuff(Entity entity, DamageSource damagesource) {
        int n;
        ArrayList<EntityItem> list = new ArrayList<EntityItem>();
        for (int i : this.drops.keySet()) {
            EntityItem e;
            int chance;
            IItemStack iItemStack = this.drops.get(i);
            if (iItemStack == null) continue;
            int dchance = 100;
            if (this.dropchance.containsKey(i)) {
                dchance = this.dropchance.get(i);
            }
            if ((chance = this.npc.field_70170_p.field_73012_v.nextInt(100) + dchance) < 100 || (e = this.getEntityItem(iItemStack.getMCItemStack().func_77946_l())) == null) continue;
            list.add(e);
        }
        int enchant = 0;
        if (damagesource.func_76346_g() instanceof EntityPlayer) {
            enchant = EnchantmentHelper.func_185283_h((EntityLivingBase)((EntityLivingBase)damagesource.func_76346_g()));
        }
        if (!ForgeHooks.onLivingDrops((EntityLivingBase)this.npc, (DamageSource)damagesource, list, (int)enchant, (boolean)true)) {
            for (EntityItem entityItem : list) {
                if (this.lootMode == 1 && entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer)entity;
                    entityItem.func_174867_a(2);
                    this.npc.field_70170_p.func_72838_d((Entity)entityItem);
                    ItemStack stack = entityItem.func_92059_d();
                    int i = stack.func_190916_E();
                    if (!player.field_71071_by.func_70441_a(stack)) continue;
                    entity.field_70170_p.func_184148_a(null, player.field_70165_t, player.field_70163_u, player.field_70161_v, SoundEvents.field_187638_cR, SoundCategory.PLAYERS, 0.2f, ((player.func_70681_au().nextFloat() - player.func_70681_au().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                    player.func_71001_a((Entity)entityItem, i);
                    if (stack.func_190916_E() > 0) continue;
                    entityItem.func_70106_y();
                    continue;
                }
                this.npc.field_70170_p.func_72838_d((Entity)entityItem);
            }
        }
        for (int exp = this.getExpRNG(); exp > 0; exp -= n) {
            n = EntityXPOrb.func_70527_a((int)exp);
            if (this.lootMode == 1 && entity instanceof EntityPlayer) {
                this.npc.field_70170_p.func_72838_d((Entity)new EntityXPOrb(entity.field_70170_p, entity.field_70165_t, entity.field_70163_u, entity.field_70161_v, n));
                continue;
            }
            this.npc.field_70170_p.func_72838_d((Entity)new EntityXPOrb(this.npc.field_70170_p, this.npc.field_70165_t, this.npc.field_70163_u, this.npc.field_70161_v, n));
        }
    }

    public EntityItem getEntityItem(ItemStack itemstack) {
        if (itemstack == null || itemstack.func_190926_b()) {
            return null;
        }
        EntityItem entityitem = new EntityItem(this.npc.field_70170_p, this.npc.field_70165_t, this.npc.field_70163_u - (double)0.3f + (double)this.npc.func_70047_e(), this.npc.field_70161_v, itemstack);
        entityitem.func_174867_a(40);
        float f2 = this.npc.func_70681_au().nextFloat() * 0.5f;
        float f4 = this.npc.func_70681_au().nextFloat() * 3.141593f * 2.0f;
        entityitem.field_70159_w = -MathHelper.func_76126_a((float)f4) * f2;
        entityitem.field_70179_y = MathHelper.func_76134_b((float)f4) * f2;
        entityitem.field_70181_x = 0.2f;
        return entityitem;
    }

    public int func_70302_i_() {
        return 15;
    }

    public ItemStack func_70301_a(int i) {
        if (i < 4) {
            return ItemStackWrapper.MCItem(this.getArmor(i));
        }
        if (i < 7) {
            return ItemStackWrapper.MCItem(this.weapons.get(i - 4));
        }
        return ItemStackWrapper.MCItem(this.drops.get(i - 7));
    }

    public ItemStack func_70298_a(int par1, int par2) {
        Map<Integer, IItemStack> var3;
        int i = 0;
        if (par1 >= 7) {
            var3 = this.drops;
            par1 -= 7;
        } else if (par1 >= 4) {
            var3 = this.weapons;
            par1 -= 4;
            i = 1;
        } else {
            var3 = this.armor;
            i = 2;
        }
        ItemStack var4 = null;
        if (var3.get(par1) != null) {
            if (var3.get(par1).getMCItemStack().func_190916_E() <= par2) {
                var4 = var3.get(par1).getMCItemStack();
                var3.put(par1, null);
            } else {
                var4 = var3.get(par1).getMCItemStack().func_77979_a(par2);
                if (var3.get(par1).getMCItemStack().func_190916_E() == 0) {
                    var3.put(par1, null);
                }
            }
        }
        if (i == 1) {
            this.weapons = var3;
        }
        if (i == 2) {
            this.armor = var3;
        }
        if (var4 == null) {
            return ItemStack.field_190927_a;
        }
        return var4;
    }

    public ItemStack func_70304_b(int par1) {
        Map<Integer, IItemStack> var2;
        int i = 0;
        if (par1 >= 7) {
            var2 = this.drops;
            par1 -= 7;
        } else if (par1 >= 4) {
            var2 = this.weapons;
            par1 -= 4;
            i = 1;
        } else {
            var2 = this.armor;
            i = 2;
        }
        if (var2.get(par1) != null) {
            ItemStack var3 = var2.get(par1).getMCItemStack();
            var2.put(par1, null);
            if (i == 1) {
                this.weapons = var2;
            }
            if (i == 2) {
                this.armor = var2;
            }
            return var3;
        }
        return ItemStack.field_190927_a;
    }

    public void func_70299_a(int par1, ItemStack par2ItemStack) {
        Map<Integer, IItemStack> var3;
        int i = 0;
        if (par1 >= 7) {
            var3 = this.drops;
            par1 -= 7;
        } else if (par1 >= 4) {
            var3 = this.weapons;
            par1 -= 4;
            i = 1;
        } else {
            var3 = this.armor;
            i = 2;
        }
        var3.put(par1, NpcAPI.Instance().getIItemStack(par2ItemStack));
        if (i == 1) {
            this.weapons = var3;
        }
        if (i == 2) {
            this.armor = var3;
        }
    }

    public int func_70297_j_() {
        return 64;
    }

    public boolean func_70300_a(EntityPlayer var1) {
        return true;
    }

    public boolean func_94041_b(int i, ItemStack itemstack) {
        return true;
    }

    public String func_70005_c_() {
        return "NPC Inventory";
    }

    public void func_70296_d() {
    }

    public boolean func_145818_k_() {
        return true;
    }

    public ITextComponent func_145748_c_() {
        return null;
    }

    public void func_174889_b(EntityPlayer player) {
    }

    public void func_174886_c(EntityPlayer player) {
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

    @Override
    public int getExpMin() {
        return this.npc.inventory.minExp;
    }

    @Override
    public int getExpMax() {
        return this.npc.inventory.maxExp;
    }

    @Override
    public int getExpRNG() {
        int exp = this.minExp;
        if (this.maxExp - this.minExp > 0) {
            exp += this.npc.field_70170_p.field_73012_v.nextInt(this.maxExp - this.minExp);
        }
        return exp;
    }

    @Override
    public void setExp(int min, int max) {
        this.npc.inventory.minExp = min = Math.min(min, max);
        this.npc.inventory.maxExp = max;
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

