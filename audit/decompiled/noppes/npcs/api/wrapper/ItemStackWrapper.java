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
                ItemStackWrapper.this.storedData.func_74780_a(key, ((Number)value).doubleValue());
            } else if (value instanceof String) {
                ItemStackWrapper.this.storedData.func_74778_a(key, (String)value);
            }
        }

        @Override
        public Object get(String key) {
            if (!ItemStackWrapper.this.storedData.func_74764_b(key)) {
                return null;
            }
            NBTBase base = ItemStackWrapper.this.storedData.func_74781_a(key);
            if (base instanceof NBTPrimitive) {
                return ((NBTPrimitive)base).func_150286_g();
            }
            return ((NBTTagString)base).func_150285_a_();
        }

        @Override
        public void remove(String key) {
            ItemStackWrapper.this.storedData.func_82580_o(key);
        }

        @Override
        public boolean has(String key) {
            return ItemStackWrapper.this.storedData.func_74764_b(key);
        }

        @Override
        public void clear() {
            ItemStackWrapper.this.storedData = new NBTTagCompound();
        }

        @Override
        public String[] getKeys() {
            return ItemStackWrapper.this.storedData.func_150296_c().toArray(new String[ItemStackWrapper.this.storedData.func_150296_c().size()]);
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
        return this.item.func_190916_E();
    }

    @Override
    public void setStackSize(int size) {
        if (size > this.getMaxStackSize()) {
            throw new CustomNPCsException("Can't set the stacksize bigger than MaxStacksize", new Object[0]);
        }
        this.item.func_190920_e(size);
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
        NBTTagCompound compound = this.item.func_77978_p();
        if (compound == null) {
            compound = new NBTTagCompound();
            this.item.func_77982_d(compound);
        }
        NBTTagList nbttaglist = compound.func_150295_c("AttributeModifiers", 10);
        NBTTagList newList = new NBTTagList();
        for (int i = 0; i < nbttaglist.func_74745_c(); ++i) {
            NBTTagCompound c = nbttaglist.func_150305_b(i);
            if (c.func_74779_i("AttributeName").equals(name)) continue;
            newList.func_74742_a((NBTBase)c);
        }
        if (value != 0.0) {
            NBTTagCompound nbttagcompound = SharedMonsterAttributes.func_111262_a((AttributeModifier)new AttributeModifier(name, value, 0));
            nbttagcompound.func_74778_a("AttributeName", name);
            if (slot >= 0) {
                nbttagcompound.func_74778_a("Slot", EntityEquipmentSlot.values()[slot].func_188450_d());
            }
            newList.func_74742_a((NBTBase)nbttagcompound);
        }
        compound.func_74782_a("AttributeModifiers", (NBTBase)newList);
    }

    @Override
    public double getAttribute(String name) {
        NBTTagCompound compound = this.item.func_77978_p();
        if (compound == null) {
            return 0.0;
        }
        Multimap map = this.item.func_111283_C(EntityEquipmentSlot.MAINHAND);
        for (Map.Entry entry : map.entries()) {
            if (!((String)entry.getKey()).equals(name)) continue;
            AttributeModifier mod = (AttributeModifier)entry.getValue();
            return mod.func_111164_d();
        }
        return 0.0;
    }

    @Override
    public boolean hasAttribute(String name) {
        NBTTagCompound compound = this.item.func_77978_p();
        if (compound == null) {
            return false;
        }
        NBTTagList nbttaglist = compound.func_150295_c("AttributeModifiers", 10);
        for (int i = 0; i < nbttaglist.func_74745_c(); ++i) {
            NBTTagCompound c = nbttaglist.func_150305_b(i);
            if (!c.func_74779_i("AttributeName").equals(name)) continue;
            return true;
        }
        return false;
    }

    @Override
    public int getItemDamage() {
        return this.item.func_77952_i();
    }

    @Override
    public void setItemDamage(int value) {
        this.item.func_77964_b(value);
    }

    @Override
    public void addEnchantment(String id, int strenght) {
        Enchantment ench = Enchantment.func_180305_b((String)id);
        if (ench == null) {
            throw new CustomNPCsException("Unknown enchant id:" + id, new Object[0]);
        }
        this.item.func_77966_a(ench, strenght);
    }

    @Override
    public boolean isEnchanted() {
        return this.item.func_77948_v();
    }

    @Override
    public boolean hasEnchant(String id) {
        Enchantment ench = Enchantment.func_180305_b((String)id);
        if (ench == null) {
            throw new CustomNPCsException("Unknown enchant id:" + id, new Object[0]);
        }
        if (!this.isEnchanted()) {
            return false;
        }
        int enchId = Enchantment.func_185258_b((Enchantment)ench);
        NBTTagList list = this.item.func_77986_q();
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound compound = list.func_150305_b(i);
            if (compound.func_74765_d("id") != enchId) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeEnchant(String id) {
        Enchantment ench = Enchantment.func_180305_b((String)id);
        if (ench == null) {
            throw new CustomNPCsException("Unknown enchant id:" + id, new Object[0]);
        }
        if (!this.isEnchanted()) {
            return false;
        }
        int enchId = Enchantment.func_185258_b((Enchantment)ench);
        NBTTagList list = this.item.func_77986_q();
        NBTTagList newList = new NBTTagList();
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound compound = list.func_150305_b(i);
            if (compound.func_74765_d("id") == enchId) continue;
            newList.func_74742_a((NBTBase)compound);
        }
        if (list.func_74745_c() == newList.func_74745_c()) {
            return false;
        }
        this.item.func_77978_p().func_74782_a("ench", (NBTBase)newList);
        return true;
    }

    @Override
    public boolean isBlock() {
        Block block = Block.func_149634_a((Item)this.item.func_77973_b());
        return block != null && block != Blocks.field_150350_a;
    }

    @Override
    public boolean hasCustomName() {
        return this.item.func_82837_s();
    }

    @Override
    public void setCustomName(String name) {
        this.item.func_151001_c(name);
    }

    @Override
    public String getDisplayName() {
        return this.item.func_82833_r();
    }

    @Override
    public String getItemName() {
        return this.item.func_77973_b().func_77653_i(this.item);
    }

    @Override
    public String getName() {
        return Item.field_150901_e.func_177774_c((Object)this.item.func_77973_b()) + "";
    }

    @Override
    public INbt getNbt() {
        NBTTagCompound compound = this.item.func_77978_p();
        if (compound == null) {
            compound = new NBTTagCompound();
            this.item.func_77982_d(compound);
        }
        return NpcAPI.Instance().getINbt(compound);
    }

    @Override
    public boolean hasNbt() {
        NBTTagCompound compound = this.item.func_77978_p();
        return compound != null && !compound.func_82582_d();
    }

    @Override
    public ItemStack getMCItemStack() {
        return this.item;
    }

    public static ItemStack MCItem(IItemStack item) {
        if (item == null) {
            return ItemStack.field_190927_a;
        }
        return item.getMCItemStack();
    }

    @Override
    public void damageItem(int damage, IEntityLiving living) {
        this.item.func_77972_a(damage, living == null ? null : (EntityLivingBase)living.getMCEntity());
    }

    @Override
    public boolean isBook() {
        return false;
    }

    @Override
    public int getFoodLevel() {
        if (this.item.func_77973_b() instanceof ItemFood) {
            return ((ItemFood)this.item.func_77973_b()).func_150905_g(this.item);
        }
        return 0;
    }

    @Override
    public IItemStack copy() {
        return ItemStackWrapper.createNew(this.item.func_77946_l());
    }

    @Override
    public int getMaxStackSize() {
        return this.item.func_77976_d();
    }

    @Override
    public int getMaxItemDamage() {
        return this.item.func_77958_k();
    }

    @Override
    public INbt getItemNbt() {
        NBTTagCompound compound = new NBTTagCompound();
        this.item.func_77955_b(compound);
        return NpcAPI.Instance().getINbt(compound);
    }

    @Override
    public double getAttackDamage() {
        HashMultimap map = (HashMultimap)this.item.func_111283_C(EntityEquipmentSlot.MAINHAND);
        Iterator iterator = map.entries().iterator();
        double damage = 0.0;
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (!entry.getKey().equals(SharedMonsterAttributes.field_111264_e.func_111108_a())) continue;
            AttributeModifier mod = (AttributeModifier)entry.getValue();
            damage = mod.func_111164_d();
        }
        return damage += (double)EnchantmentHelper.func_152377_a((ItemStack)this.item, (EnumCreatureAttribute)EnumCreatureAttribute.UNDEFINED);
    }

    @Override
    public boolean isEmpty() {
        return this.item.func_190926_b();
    }

    @Override
    public int getType() {
        if (this.item.func_77973_b() instanceof IPlantable) {
            return 5;
        }
        if (this.item.func_77973_b() instanceof ItemSword) {
            return 4;
        }
        return 0;
    }

    @Override
    public boolean isWearable() {
        for (EntityEquipmentSlot slot : VALID_EQUIPMENT_SLOTS) {
            if (!this.item.func_77973_b().isValidArmor(this.item, slot, (Entity)EntityNPCInterface.CommandPlayer)) continue;
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
        if (item == null || item.func_190926_b()) {
            return AIR;
        }
        if (item.func_77973_b() instanceof ItemScripted) {
            return new ItemScriptedWrapper(item);
        }
        if (item.func_77973_b() == Items.field_151164_bB || item.func_77973_b() == Items.field_151099_bA || item.func_77973_b() instanceof ItemWritableBook || item.func_77973_b() instanceof ItemWrittenBook) {
            return new ItemBookWrapper(item);
        }
        if (item.func_77973_b() instanceof ItemArmor) {
            return new ItemArmorWrapper(item);
        }
        Block block = Block.func_149634_a((Item)item.func_77973_b());
        if (block != Blocks.field_150350_a) {
            return new ItemBlockWrapper(item);
        }
        return new ItemStackWrapper(item);
    }

    @Override
    public String[] getLore() {
        NBTTagCompound compound = this.item.func_179543_a("display");
        if (compound == null || compound.func_150299_b("Lore") != 9) {
            return new String[0];
        }
        NBTTagList nbttaglist = compound.func_150295_c("Lore", 8);
        if (nbttaglist.func_82582_d()) {
            return new String[0];
        }
        ArrayList<String> lore = new ArrayList<String>();
        for (int i = 0; i < nbttaglist.func_74745_c(); ++i) {
            lore.add(nbttaglist.func_150307_f(i));
        }
        return lore.toArray(new String[lore.size()]);
    }

    @Override
    public void setLore(String[] lore) {
        NBTTagCompound compound = this.item.func_190925_c("display");
        if (lore == null || lore.length == 0) {
            compound.func_82580_o("Lore");
            return;
        }
        NBTTagList nbtlist = new NBTTagList();
        for (String s : lore) {
            nbtlist.func_74742_a((NBTBase)new NBTTagString(s));
        }
        compound.func_74782_a("Lore", (NBTBase)nbtlist);
    }

    public NBTBase serializeNBT() {
        return this.getMCNbt();
    }

    public void deserializeNBT(NBTBase nbt) {
        this.setMCNbt((NBTTagCompound)nbt);
    }

    public NBTTagCompound getMCNbt() {
        NBTTagCompound compound = new NBTTagCompound();
        if (!this.storedData.func_82582_d()) {
            compound.func_74782_a("StoredData", (NBTBase)this.storedData);
        }
        return compound;
    }

    public void setMCNbt(NBTTagCompound compound) {
        this.storedData = compound.func_74775_l("StoredData");
    }

    @Override
    public void removeNbt() {
        this.item.func_77982_d(null);
    }

    @Override
    public boolean compare(IItemStack item, boolean ignoreNBT) {
        if (item == null) {
            item = AIR;
        }
        return NoppesUtilPlayer.compareItems(this.getMCItemStack(), item.getMCItemStack(), false, ignoreNBT);
    }
}

