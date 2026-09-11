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
        nbttagcompound.setInteger("MinExp", this.minExp);
        nbttagcompound.setInteger("MaxExp", this.maxExp);
        nbttagcompound.setTag("NpcInv", (NBTBase)NBTTags.nbtIItemStackMap(this.drops));
        nbttagcompound.setTag("Armor", (NBTBase)NBTTags.nbtIItemStackMap(this.armor));
        nbttagcompound.setTag("Weapons", (NBTBase)NBTTags.nbtIItemStackMap(this.weapons));
        nbttagcompound.setTag("DropChance", (NBTBase)NBTTags.nbtIntegerIntegerMap(this.dropchance));
        nbttagcompound.setInteger("LootMode", this.lootMode);
        return nbttagcompound;
    }

    public void readEntityFromNBT(NBTTagCompound nbttagcompound) {
        this.minExp = nbttagcompound.getInteger("MinExp");
        this.maxExp = nbttagcompound.getInteger("MaxExp");
        this.drops = NBTTags.getIItemStackMap(nbttagcompound.getTagList("NpcInv", 10));
        this.armor = NBTTags.getIItemStackMap(nbttagcompound.getTagList("Armor", 10));
        this.weapons = NBTTags.getIItemStackMap(nbttagcompound.getTagList("Weapons", 10));
        this.dropchance = NBTTags.getIntegerIntegerMap(nbttagcompound.getTagList("DropChance", 10));
        this.lootMode = nbttagcompound.getInteger("LootMode");
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
            if ((chance = this.npc.world.rand.nextInt(100) + dchance) < 100 || (e = this.getEntityItem(iItemStack.getMCItemStack().copy())) == null) continue;
            list.add(e);
        }
        int enchant = 0;
        if (damagesource.getTrueSource() instanceof EntityPlayer) {
            enchant = EnchantmentHelper.getLootingModifier((EntityLivingBase)((EntityLivingBase)damagesource.getTrueSource()));
        }
        if (!ForgeHooks.onLivingDrops((EntityLivingBase)this.npc, (DamageSource)damagesource, list, (int)enchant, (boolean)true)) {
            for (EntityItem entityItem : list) {
                if (this.lootMode == 1 && entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer)entity;
                    entityItem.setPickupDelay(2);
                    this.npc.world.spawnEntity((Entity)entityItem);
                    ItemStack stack = entityItem.getItem();
                    int i = stack.getCount();
                    if (!player.inventory.addItemStackToInventory(stack)) continue;
                    entity.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                    player.onItemPickup((Entity)entityItem, i);
                    if (stack.getCount() > 0) continue;
                    entityItem.setDead();
                    continue;
                }
                this.npc.world.spawnEntity((Entity)entityItem);
            }
        }
        for (int exp = this.getExpRNG(); exp > 0; exp -= n) {
            n = EntityXPOrb.getXPSplit((int)exp);
            if (this.lootMode == 1 && entity instanceof EntityPlayer) {
                this.npc.world.spawnEntity((Entity)new EntityXPOrb(entity.world, entity.posX, entity.posY, entity.posZ, n));
                continue;
            }
            this.npc.world.spawnEntity((Entity)new EntityXPOrb(this.npc.world, this.npc.posX, this.npc.posY, this.npc.posZ, n));
        }
    }

    public EntityItem getEntityItem(ItemStack itemstack) {
        if (itemstack == null || itemstack.isEmpty()) {
            return null;
        }
        EntityItem entityitem = new EntityItem(this.npc.world, this.npc.posX, this.npc.posY - (double)0.3f + (double)this.npc.getEyeHeight(), this.npc.posZ, itemstack);
        entityitem.setPickupDelay(40);
        float f2 = this.npc.getRNG().nextFloat() * 0.5f;
        float f4 = this.npc.getRNG().nextFloat() * 3.141593f * 2.0f;
        entityitem.motionX = -MathHelper.sin((float)f4) * f2;
        entityitem.motionZ = MathHelper.cos((float)f4) * f2;
        entityitem.motionY = 0.2f;
        return entityitem;
    }

    public int getSizeInventory() {
        return 15;
    }

    public ItemStack getStackInSlot(int i) {
        if (i < 4) {
            return ItemStackWrapper.MCItem(this.getArmor(i));
        }
        if (i < 7) {
            return ItemStackWrapper.MCItem(this.weapons.get(i - 4));
        }
        return ItemStackWrapper.MCItem(this.drops.get(i - 7));
    }

    public ItemStack decrStackSize(int par1, int par2) {
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
            if (var3.get(par1).getMCItemStack().getCount() <= par2) {
                var4 = var3.get(par1).getMCItemStack();
                var3.put(par1, null);
            } else {
                var4 = var3.get(par1).getMCItemStack().splitStack(par2);
                if (var3.get(par1).getMCItemStack().getCount() == 0) {
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
            return ItemStack.EMPTY;
        }
        return var4;
    }

    public ItemStack removeStackFromSlot(int par1) {
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
        return ItemStack.EMPTY;
    }

    public void setInventorySlotContents(int par1, ItemStack par2ItemStack) {
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

    public int getInventoryStackLimit() {
        return 64;
    }

    public boolean isUsableByPlayer(EntityPlayer var1) {
        return true;
    }

    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return true;
    }

    public String getName() {
        return "NPC Inventory";
    }

    public void markDirty() {
    }

    public boolean hasCustomName() {
        return true;
    }

    public ITextComponent getDisplayName() {
        return null;
    }

    public void openInventory(EntityPlayer player) {
    }

    public void closeInventory(EntityPlayer player) {
    }

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {
    }

    public int getFieldCount() {
        return 0;
    }

    public void clear() {
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
            exp += this.npc.world.rand.nextInt(this.maxExp - this.minExp);
        }
        return exp;
    }

    @Override
    public void setExp(int min, int max) {
        this.npc.inventory.minExp = min = Math.min(min, max);
        this.npc.inventory.maxExp = max;
    }

    public boolean isEmpty() {
        for (int slot = 0; slot < this.getSizeInventory(); ++slot) {
            ItemStack item = this.getStackInSlot(slot);
            if (NoppesUtilServer.IsItemStackNull(item) || item.isEmpty()) continue;
            return false;
        }
        return true;
    }
}

