/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Multimap
 *  net.minecraft.block.Block
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.EnumCreatureAttribute
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.entity.ai.attributes.AttributeModifier
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemFood
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.item.ItemWritableBook
 *  net.minecraft.item.ItemWrittenBook
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTPrimitive
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.common.IPlantable
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.CapabilityInject
 *  net.minecraftforge.common.capabilities.ICapabilityProvider
 *  net.minecraftforge.common.capabilities.ICapabilitySerializable
 *  net.minecraftforge.event.AttachCapabilitiesEvent
 */
package noppes.npcs.api.wrapper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.ItemStackEmptyWrapper;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntityLiving;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemArmorWrapper;
import noppes.npcs.api.wrapper.ItemBlockWrapper;
import noppes.npcs.api.wrapper.ItemBookWrapper;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;

public class ItemStackWrapper
implements IItemStack,
ICapabilityProvider,
ICapabilitySerializable {
    private Map<String, Object> tempData = new HashMap<String, Object>();
    @CapabilityInject(value=ItemStackWrapper.class)
    public static Capability<ItemStackWrapper> ITEMSCRIPTEDDATA_CAPABILITY = null;
    private static final EntityEquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
    public ItemStack item;
    private NBTTagCompound storedData = new NBTTagCompound();
    public static ItemStackWrapper AIR = new ItemStackEmptyWrapper();
    private final IData tempdata = new IData(){

        @Override
        public void put(String key, Object value) {
            ItemStackWrapper.this.tempData.put(key, value);
        }

        @Override
        public Object get(String key) {
            return ItemStackWrapper.this.tempData.get(key);
        }

        @Override
        public void remove(String key) {
            ItemStackWrapper.this.tempData.remove(key);
        }

        @Override
        public boolean has(String key) {
            return ItemStackWrapper.this.tempData.containsKey(key);
        }

        @Override
        public void clear() {
            ItemStackWrapper.this.tempData.clear();
        }

        @Override
        public String[] getKeys() {
            return ItemStackWrapper.this.tempData.keySet().toArray(new String[ItemStackWrapper.this.tempData.size()]);
        }
    };
    private final IData storeddata = new IData(){

        @Override
        public void put(String key, Object value) {
            if (value instanceof Number) {
                ItemStackWrapper.this.storedData.setDouble(key, ((Number)value).doubleValue());
            } else if (value instanceof String) {
                ItemStackWrapper.this.storedData.setString(key, (String)value);
            }
        }

        @Override
        public Object get(String key) {
            if (!ItemStackWrapper.this.storedData.hasKey(key)) {
                return null;
            }
            NBTBase base = ItemStackWrapper.this.storedData.getTag(key);
            if (base instanceof NBTPrimitive) {
                return ((NBTPrimitive)base).getDouble();
            }
            return ((NBTTagString)base).getString();
        }

        @Override
        public void remove(String key) {
            ItemStackWrapper.this.storedData.removeTag(key);
        }

        @Override
        public boolean has(String key) {
            return ItemStackWrapper.this.storedData.hasKey(key);
        }

        @Override
        public void clear() {
            ItemStackWrapper.this.storedData = new NBTTagCompound();
        }

        @Override
        public String[] getKeys() {
            return ItemStackWrapper.this.storedData.getKeySet().toArray(new String[ItemStackWrapper.this.storedData.getKeySet().size()]);
        }
    };
    private static final ResourceLocation key = new ResourceLocation("customnpcs", "itemscripteddata");

    protected ItemStackWrapper(ItemStack item) {
        this.item = item;
    }

    @Override
    public IData getTempdata() {
        return this.tempdata;
    }

    @Override
    public IData getStoreddata() {
        return this.storeddata;
    }

    @Override
    public int getStackSize() {
        return this.item.getCount();
    }

    @Override
    public void setStackSize(int size) {
        if (size > this.getMaxStackSize()) {
            throw new CustomNPCsException("Can't set the stacksize bigger than MaxStacksize", new Object[0]);
        }
        this.item.setCount(size);
    }

    @Override
    public void setAttribute(String name, double value) {
        this.setAttribute(name, value, -1);
    }

    @Override
    public void setAttribute(String name, double value, int slot) {
        if (slot < -1 || slot > 5) {
            throw new CustomNPCsException("Slot has to be between -1 and 5, given was: " + slot, new Object[0]);
        }
        NBTTagCompound compound = this.item.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            this.item.setTagCompound(compound);
        }
        NBTTagList nbttaglist = compound.getTagList("AttributeModifiers", 10);
        NBTTagList newList = new NBTTagList();
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound c = nbttaglist.getCompoundTagAt(i);
            if (c.getString("AttributeName").equals(name)) continue;
            newList.appendTag((NBTBase)c);
        }
        if (value != 0.0) {
            NBTTagCompound nbttagcompound = SharedMonsterAttributes.writeAttributeModifierToNBT((AttributeModifier)new AttributeModifier(name, value, 0));
            nbttagcompound.setString("AttributeName", name);
            if (slot >= 0) {
                nbttagcompound.setString("Slot", EntityEquipmentSlot.values()[slot].getName());
            }
            newList.appendTag((NBTBase)nbttagcompound);
        }
        compound.setTag("AttributeModifiers", (NBTBase)newList);
    }

    @Override
    public double getAttribute(String name) {
        NBTTagCompound compound = this.item.getTagCompound();
        if (compound == null) {
            return 0.0;
        }
        Multimap map = this.item.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
        for (Map.Entry entry : map.entries()) {
            if (!((String)entry.getKey()).equals(name)) continue;
            AttributeModifier mod = (AttributeModifier)entry.getValue();
            return mod.getAmount();
        }
        return 0.0;
    }

    @Override
    public boolean hasAttribute(String name) {
        NBTTagCompound compound = this.item.getTagCompound();
        if (compound == null) {
            return false;
        }
        NBTTagList nbttaglist = compound.getTagList("AttributeModifiers", 10);
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound c = nbttaglist.getCompoundTagAt(i);
            if (!c.getString("AttributeName").equals(name)) continue;
            return true;
        }
        return false;
    }

    @Override
    public int getItemDamage() {
        return this.item.getItemDamage();
    }

    @Override
    public void setItemDamage(int value) {
        this.item.setItemDamage(value);
    }

    @Override
    public void addEnchantment(String id, int strenght) {
        Enchantment ench = Enchantment.getEnchantmentByLocation((String)id);
        if (ench == null) {
            throw new CustomNPCsException("Unknown enchant id:" + id, new Object[0]);
        }
        this.item.addEnchantment(ench, strenght);
    }

    @Override
    public boolean isEnchanted() {
        return this.item.isItemEnchanted();
    }

    @Override
    public boolean hasEnchant(String id) {
        Enchantment ench = Enchantment.getEnchantmentByLocation((String)id);
        if (ench == null) {
            throw new CustomNPCsException("Unknown enchant id:" + id, new Object[0]);
        }
        if (!this.isEnchanted()) {
            return false;
        }
        int enchId = Enchantment.getEnchantmentID((Enchantment)ench);
        NBTTagList list = this.item.getEnchantmentTagList();
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound compound = list.getCompoundTagAt(i);
            if (compound.getShort("id") != enchId) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeEnchant(String id) {
        Enchantment ench = Enchantment.getEnchantmentByLocation((String)id);
        if (ench == null) {
            throw new CustomNPCsException("Unknown enchant id:" + id, new Object[0]);
        }
        if (!this.isEnchanted()) {
            return false;
        }
        int enchId = Enchantment.getEnchantmentID((Enchantment)ench);
        NBTTagList list = this.item.getEnchantmentTagList();
        NBTTagList newList = new NBTTagList();
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound compound = list.getCompoundTagAt(i);
            if (compound.getShort("id") == enchId) continue;
            newList.appendTag((NBTBase)compound);
        }
        if (list.tagCount() == newList.tagCount()) {
            return false;
        }
        this.item.getTagCompound().setTag("ench", (NBTBase)newList);
        return true;
    }

    @Override
    public boolean isBlock() {
        Block block = Block.getBlockFromItem((Item)this.item.getItem());
        return block != null && block != Blocks.AIR;
    }

    @Override
    public boolean hasCustomName() {
        return this.item.hasDisplayName();
    }

    @Override
    public void setCustomName(String name) {
        this.item.setStackDisplayName(name);
    }

    @Override
    public String getDisplayName() {
        return this.item.getDisplayName();
    }

    @Override
    public String getItemName() {
        return this.item.getItem().getItemStackDisplayName(this.item);
    }

    @Override
    public String getName() {
        return Item.REGISTRY.getNameForObject((Object)this.item.getItem()) + "";
    }

    @Override
    public INbt getNbt() {
        NBTTagCompound compound = this.item.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            this.item.setTagCompound(compound);
        }
        return NpcAPI.Instance().getINbt(compound);
    }

    @Override
    public boolean hasNbt() {
        NBTTagCompound compound = this.item.getTagCompound();
        return compound != null && !compound.hasNoTags();
    }

    @Override
    public ItemStack getMCItemStack() {
        return this.item;
    }

    public static ItemStack MCItem(IItemStack item) {
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return item.getMCItemStack();
    }

    @Override
    public void damageItem(int damage, IEntityLiving living) {
        this.item.damageItem(damage, living == null ? null : (EntityLivingBase)living.getMCEntity());
    }

    @Override
    public boolean isBook() {
        return false;
    }

    @Override
    public int getFoodLevel() {
        if (this.item.getItem() instanceof ItemFood) {
            return ((ItemFood)this.item.getItem()).getHealAmount(this.item);
        }
        return 0;
    }

    @Override
    public IItemStack copy() {
        return ItemStackWrapper.createNew(this.item.copy());
    }

    @Override
    public int getMaxStackSize() {
        return this.item.getMaxStackSize();
    }

    @Override
    public int getMaxItemDamage() {
        return this.item.getMaxDamage();
    }

    @Override
    public INbt getItemNbt() {
        NBTTagCompound compound = new NBTTagCompound();
        this.item.writeToNBT(compound);
        return NpcAPI.Instance().getINbt(compound);
    }

    @Override
    public double getAttackDamage() {
        HashMultimap map = (HashMultimap)this.item.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
        Iterator iterator = map.entries().iterator();
        double damage = 0.0;
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (!entry.getKey().equals(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) continue;
            AttributeModifier mod = (AttributeModifier)entry.getValue();
            damage = mod.getAmount();
        }
        return damage += (double)EnchantmentHelper.getModifierForCreature((ItemStack)this.item, (EnumCreatureAttribute)EnumCreatureAttribute.UNDEFINED);
    }

    @Override
    public boolean isEmpty() {
        return this.item.isEmpty();
    }

    @Override
    public int getType() {
        if (this.item.getItem() instanceof IPlantable) {
            return 5;
        }
        if (this.item.getItem() instanceof ItemSword) {
            return 4;
        }
        return 0;
    }

    @Override
    public boolean isWearable() {
        for (EntityEquipmentSlot slot : VALID_EQUIPMENT_SLOTS) {
            if (!this.item.getItem().isValidArmor(this.item, slot, (Entity)EntityNPCInterface.CommandPlayer)) continue;
            return true;
        }
        return false;
    }

    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == ITEMSCRIPTEDDATA_CAPABILITY;
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (this.hasCapability(capability, facing)) {
            return (T)this;
        }
        return null;
    }

    public static void register(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStackWrapper wrapper = ItemStackWrapper.createNew((ItemStack)event.getObject());
        event.addCapability(key, (ICapabilityProvider)wrapper);
    }

    private static ItemStackWrapper createNew(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return AIR;
        }
        if (item.getItem() instanceof ItemScripted) {
            return new ItemScriptedWrapper(item);
        }
        if (item.getItem() == Items.WRITTEN_BOOK || item.getItem() == Items.WRITABLE_BOOK || item.getItem() instanceof ItemWritableBook || item.getItem() instanceof ItemWrittenBook) {
            return new ItemBookWrapper(item);
        }
        if (item.getItem() instanceof ItemArmor) {
            return new ItemArmorWrapper(item);
        }
        Block block = Block.getBlockFromItem((Item)item.getItem());
        if (block != Blocks.AIR) {
            return new ItemBlockWrapper(item);
        }
        return new ItemStackWrapper(item);
    }

    @Override
    public String[] getLore() {
        NBTTagCompound compound = this.item.getSubCompound("display");
        if (compound == null || compound.getTagId("Lore") != 9) {
            return new String[0];
        }
        NBTTagList nbttaglist = compound.getTagList("Lore", 8);
        if (nbttaglist.hasNoTags()) {
            return new String[0];
        }
        ArrayList<String> lore = new ArrayList<String>();
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            lore.add(nbttaglist.getStringTagAt(i));
        }
        return lore.toArray(new String[lore.size()]);
    }

    @Override
    public void setLore(String[] lore) {
        NBTTagCompound compound = this.item.getOrCreateSubCompound("display");
        if (lore == null || lore.length == 0) {
            compound.removeTag("Lore");
            return;
        }
        NBTTagList nbtlist = new NBTTagList();
        for (String s : lore) {
            nbtlist.appendTag((NBTBase)new NBTTagString(s));
        }
        compound.setTag("Lore", (NBTBase)nbtlist);
    }

    public NBTBase serializeNBT() {
        return this.getMCNbt();
    }

    public void deserializeNBT(NBTBase nbt) {
        this.setMCNbt((NBTTagCompound)nbt);
    }

    public NBTTagCompound getMCNbt() {
        NBTTagCompound compound = new NBTTagCompound();
        if (!this.storedData.hasNoTags()) {
            compound.setTag("StoredData", (NBTBase)this.storedData);
        }
        return compound;
    }

    public void setMCNbt(NBTTagCompound compound) {
        this.storedData = compound.getCompoundTag("StoredData");
    }

    @Override
    public void removeNbt() {
        this.item.setTagCompound(null);
    }

    @Override
    public boolean compare(IItemStack item, boolean ignoreNBT) {
        if (item == null) {
            item = AIR;
        }
        return NoppesUtilPlayer.compareItems(this.getMCItemStack(), item.getMCItemStack(), false, ignoreNBT);
    }
}

