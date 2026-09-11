/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  net.minecraft.block.Block
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.entity.ai.attributes.AttributeModifier
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemArmor$ArmorMaterial
 *  net.minecraft.item.ItemBow
 *  net.minecraft.item.ItemFood
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.EnumParticleTypes
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 *  net.minecraftforge.fml.common.ObfuscationReflectionHelper
 */
package noppes.npcs.roles;

import com.google.common.collect.HashMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumCompanionJobs;
import noppes.npcs.constants.EnumCompanionStage;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleInterface;
import noppes.npcs.roles.companion.CompanionFarmer;
import noppes.npcs.roles.companion.CompanionFoodStats;
import noppes.npcs.roles.companion.CompanionGuard;
import noppes.npcs.roles.companion.CompanionJobInterface;
import noppes.npcs.roles.companion.CompanionTrader;

public class RoleCompanion
extends RoleInterface {
    public NpcMiscInventory inventory;
    public String uuid = "";
    public String ownerName = "";
    public Map<EnumCompanionTalent, Integer> talents = new TreeMap<EnumCompanionTalent, Integer>();
    public boolean canAge = true;
    public long ticksActive = 0L;
    public EnumCompanionStage stage = EnumCompanionStage.FULLGROWN;
    public EntityPlayer owner = null;
    public int companionID;
    public EnumCompanionJobs job = EnumCompanionJobs.NONE;
    public CompanionJobInterface jobInterface = null;
    public boolean hasInv = true;
    public boolean defendOwner = true;
    public CompanionFoodStats foodstats = new CompanionFoodStats();
    private int eatingTicks = 20;
    private IItemStack eating = null;
    private int eatingDelay = 0;
    public int currentExp = 0;

    public RoleCompanion(EntityNPCInterface npc) {
        super(npc);
        this.inventory = new NpcMiscInventory(12);
    }

    @Override
    public boolean aiShouldExecute() {
        EntityPlayer prev = this.owner;
        this.owner = this.getOwner();
        if (this.jobInterface != null && this.jobInterface.isSelfSufficient()) {
            return true;
        }
        if (this.owner == null && !this.uuid.isEmpty()) {
            this.npc.isDead = true;
        } else if (prev != this.owner && this.owner != null) {
            this.ownerName = this.owner.getDisplayNameString();
            PlayerData data = PlayerData.get(this.owner);
            if (data.companionID != this.companionID) {
                this.npc.isDead = true;
            }
        }
        return this.owner != null;
    }

    @Override
    public void aiUpdateTask() {
        if (!(this.owner == null || this.jobInterface != null && this.jobInterface.isSelfSufficient())) {
            this.foodstats.onUpdate(this.npc);
        }
        if (this.foodstats.getFoodLevel() >= 18) {
            this.npc.stats.healthRegen = 0;
            this.npc.stats.combatRegen = 0;
        }
        if (this.foodstats.needFood() && this.isSitting()) {
            if (this.eatingDelay > 0) {
                --this.eatingDelay;
                return;
            }
            IItemStack prev = this.eating;
            this.eating = this.getFood();
            if (prev != null && this.eating == null) {
                this.npc.setRoleData("");
            }
            if (prev == null && this.eating != null) {
                this.npc.setRoleData("eating");
                this.eatingTicks = 20;
            }
            if (this.isEating()) {
                this.doEating();
            }
        } else if (this.eating != null && !this.isSitting()) {
            this.eating = null;
            this.eatingDelay = 20;
            this.npc.setRoleData("");
        }
        ++this.ticksActive;
        if (this.canAge && this.stage != EnumCompanionStage.FULLGROWN) {
            if (this.stage == EnumCompanionStage.BABY && this.ticksActive > (long)EnumCompanionStage.CHILD.matureAge) {
                this.matureTo(EnumCompanionStage.CHILD);
            } else if (this.stage == EnumCompanionStage.CHILD && this.ticksActive > (long)EnumCompanionStage.TEEN.matureAge) {
                this.matureTo(EnumCompanionStage.TEEN);
            } else if (this.stage == EnumCompanionStage.TEEN && this.ticksActive > (long)EnumCompanionStage.ADULT.matureAge) {
                this.matureTo(EnumCompanionStage.ADULT);
            } else if (this.stage == EnumCompanionStage.ADULT && this.ticksActive > (long)EnumCompanionStage.FULLGROWN.matureAge) {
                this.matureTo(EnumCompanionStage.FULLGROWN);
            }
        }
    }

    @Override
    public void clientUpdate() {
        if (this.npc.getRoleData().equals("eating")) {
            this.eating = this.getFood();
            if (this.isEating()) {
                this.doEating();
            }
        } else if (this.eating != null) {
            this.eating = null;
        }
    }

    private void doEating() {
        if (this.eating == null) {
            return;
        }
        ItemStack eating = this.eating.getMCItemStack();
        if (this.npc.world.isRemote) {
            Random rand = this.npc.getRNG();
            for (int j = 0; j < 2; ++j) {
                Vec3d vec3 = new Vec3d(((double)rand.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
                vec3.rotateYaw(-this.npc.rotationPitch * (float)Math.PI / 180.0f);
                vec3.rotatePitch(-this.npc.renderYawOffset * (float)Math.PI / 180.0f);
                Vec3d vec31 = new Vec3d(((double)rand.nextFloat() - 0.5) * 0.3, (double)(-rand.nextFloat()) * 0.6 - 0.3, (double)(this.npc.width / 2.0f) + 0.1);
                vec31.rotateYaw(-this.npc.rotationPitch * (float)Math.PI / 180.0f);
                vec31.rotatePitch(-this.npc.renderYawOffset * (float)Math.PI / 180.0f);
                vec31 = vec31.addVector(this.npc.posX, this.npc.posY + (double)this.npc.height + 0.1, this.npc.posZ);
                String s = "iconcrack_" + Item.getIdFromItem((Item)eating.getItem());
                if (eating.getHasSubtypes()) {
                    this.npc.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05, vec3.z, new int[]{Item.getIdFromItem((Item)eating.getItem()), eating.getMetadata()});
                    continue;
                }
                this.npc.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05, vec3.z, new int[]{Item.getIdFromItem((Item)eating.getItem())});
            }
        } else {
            --this.eatingTicks;
            if (this.eatingTicks <= 0) {
                if (this.inventory.decrStackSize(eating, 1)) {
                    ItemFood food = (ItemFood)eating.getItem();
                    this.foodstats.onFoodEaten(food, eating);
                    this.npc.playSound(SoundEvents.ENTITY_PLAYER_BURP, 0.5f, this.npc.getRNG().nextFloat() * 0.1f + 0.9f);
                }
                this.eatingDelay = 20;
                this.npc.setRoleData("");
                eating = null;
            } else if (this.eatingTicks > 3 && this.eatingTicks % 2 == 0) {
                Random rand = this.npc.getRNG();
                this.npc.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.5f + 0.5f * (float)rand.nextInt(2), (rand.nextFloat() - rand.nextFloat()) * 0.2f + 1.0f);
            }
        }
    }

    public void matureTo(EnumCompanionStage stage) {
        this.stage = stage;
        EntityCustomNpc npc = (EntityCustomNpc)this.npc;
        npc.ais.animationType = stage.animation;
        if (stage == EnumCompanionStage.BABY) {
            npc.modelData.getPartConfig(EnumParts.ARM_LEFT).setScale(0.5f, 0.5f, 0.5f);
            npc.modelData.getPartConfig(EnumParts.LEG_LEFT).setScale(0.5f, 0.5f, 0.5f);
            npc.modelData.getPartConfig(EnumParts.BODY).setScale(0.5f, 0.5f, 0.5f);
            npc.modelData.getPartConfig(EnumParts.HEAD).setScale(0.7f, 0.7f, 0.7f);
            npc.ais.onAttack = 1;
            npc.ais.setWalkingSpeed(3);
            if (!this.talents.containsKey((Object)EnumCompanionTalent.INVENTORY)) {
                this.talents.put(EnumCompanionTalent.INVENTORY, 0);
            }
        }
        if (stage == EnumCompanionStage.CHILD) {
            npc.modelData.getPartConfig(EnumParts.ARM_LEFT).setScale(0.6f, 0.6f, 0.6f);
            npc.modelData.getPartConfig(EnumParts.LEG_LEFT).setScale(0.6f, 0.6f, 0.6f);
            npc.modelData.getPartConfig(EnumParts.BODY).setScale(0.6f, 0.6f, 0.6f);
            npc.modelData.getPartConfig(EnumParts.HEAD).setScale(0.8f, 0.8f, 0.8f);
            npc.ais.onAttack = 0;
            npc.ais.setWalkingSpeed(4);
            if (!this.talents.containsKey((Object)EnumCompanionTalent.SWORD)) {
                this.talents.put(EnumCompanionTalent.SWORD, 0);
            }
        }
        if (stage == EnumCompanionStage.TEEN) {
            npc.modelData.getPartConfig(EnumParts.ARM_LEFT).setScale(0.8f, 0.8f, 0.8f);
            npc.modelData.getPartConfig(EnumParts.LEG_LEFT).setScale(0.8f, 0.8f, 0.8f);
            npc.modelData.getPartConfig(EnumParts.BODY).setScale(0.8f, 0.8f, 0.8f);
            npc.modelData.getPartConfig(EnumParts.HEAD).setScale(0.9f, 0.9f, 0.9f);
            npc.ais.onAttack = 0;
            npc.ais.setWalkingSpeed(5);
            if (!this.talents.containsKey((Object)EnumCompanionTalent.ARMOR)) {
                this.talents.put(EnumCompanionTalent.ARMOR, 0);
            }
        }
        if (stage == EnumCompanionStage.ADULT || stage == EnumCompanionStage.FULLGROWN) {
            npc.modelData.getPartConfig(EnumParts.ARM_LEFT).setScale(1.0f, 1.0f, 1.0f);
            npc.modelData.getPartConfig(EnumParts.LEG_LEFT).setScale(1.0f, 1.0f, 1.0f);
            npc.modelData.getPartConfig(EnumParts.BODY).setScale(1.0f, 1.0f, 1.0f);
            npc.modelData.getPartConfig(EnumParts.HEAD).setScale(1.0f, 1.0f, 1.0f);
            npc.ais.onAttack = 0;
            npc.ais.setWalkingSpeed(5);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("CompanionInventory", (NBTBase)this.inventory.getToNBT());
        compound.setString("CompanionOwner", this.uuid);
        compound.setString("CompanionOwnerName", this.ownerName);
        compound.setInteger("CompanionID", this.companionID);
        compound.setInteger("CompanionStage", this.stage.ordinal());
        compound.setInteger("CompanionExp", this.currentExp);
        compound.setBoolean("CompanionCanAge", this.canAge);
        compound.setLong("CompanionAge", this.ticksActive);
        compound.setBoolean("CompanionHasInv", this.hasInv);
        compound.setBoolean("CompanionDefendOwner", this.defendOwner);
        this.foodstats.writeNBT(compound);
        compound.setInteger("CompanionJob", this.job.ordinal());
        if (this.jobInterface != null) {
            compound.setTag("CompanionJobData", (NBTBase)this.jobInterface.getNBT());
        }
        NBTTagList list = new NBTTagList();
        for (EnumCompanionTalent talent : this.talents.keySet()) {
            NBTTagCompound c = new NBTTagCompound();
            c.setInteger("Talent", talent.ordinal());
            c.setInteger("Exp", this.talents.get((Object)talent).intValue());
            list.appendTag((NBTBase)c);
        }
        compound.setTag("CompanionTalents", (NBTBase)list);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.inventory.setFromNBT(compound.getCompoundTag("CompanionInventory"));
        this.uuid = compound.getString("CompanionOwner");
        this.ownerName = compound.getString("CompanionOwnerName");
        this.companionID = compound.getInteger("CompanionID");
        this.stage = EnumCompanionStage.values()[compound.getInteger("CompanionStage")];
        this.currentExp = compound.getInteger("CompanionExp");
        this.canAge = compound.getBoolean("CompanionCanAge");
        this.ticksActive = compound.getLong("CompanionAge");
        this.hasInv = compound.getBoolean("CompanionHasInv");
        this.defendOwner = compound.getBoolean("CompanionDefendOwner");
        this.foodstats.readNBT(compound);
        NBTTagList list = compound.getTagList("CompanionTalents", 10);
        TreeMap<EnumCompanionTalent, Integer> talents = new TreeMap<EnumCompanionTalent, Integer>();
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            EnumCompanionTalent talent = EnumCompanionTalent.values()[c.getInteger("Talent")];
            talents.put(talent, c.getInteger("Exp"));
        }
        this.talents = talents;
        this.setJob(compound.getInteger("CompanionJob"));
        if (this.jobInterface != null) {
            this.jobInterface.setNBT(compound.getCompoundTag("CompanionJobData"));
        }
        this.setStats();
    }

    private void setJob(int i) {
        this.job = EnumCompanionJobs.values()[i];
        this.jobInterface = this.job == EnumCompanionJobs.SHOP ? new CompanionTrader() : (this.job == EnumCompanionJobs.FARMER ? new CompanionFarmer() : (this.job == EnumCompanionJobs.GUARD ? new CompanionGuard() : null));
        if (this.jobInterface != null) {
            this.jobInterface.npc = this.npc;
        }
    }

    @Override
    public void interact(EntityPlayer player) {
        this.interact(player, false);
    }

    public void interact(EntityPlayer player, boolean openGui) {
        if (player != null && this.job == EnumCompanionJobs.SHOP) {
            ((CompanionTrader)this.jobInterface).interact(player);
        }
        if (player != this.owner || !this.npc.isEntityAlive() || this.npc.isAttacking()) {
            return;
        }
        if (player.isSneaking() || openGui) {
            this.openGui(player);
        } else {
            this.setSitting(!this.isSitting());
        }
    }

    public int getTotalLevel() {
        int level = 0;
        for (EnumCompanionTalent talent : this.talents.keySet()) {
            level += this.getTalentLevel(talent);
        }
        return level;
    }

    public int getMaxExp() {
        return 500 + this.getTotalLevel() * 200;
    }

    public void addExp(int exp) {
        if (this.canAddExp(exp)) {
            this.currentExp += exp;
        }
    }

    public boolean canAddExp(int exp) {
        int newExp = this.currentExp + exp;
        return newExp >= 0 && newExp < this.getMaxExp();
    }

    public void gainExp(int chance) {
        if (this.npc.getRNG().nextInt(chance) == 0) {
            this.addExp(1);
        }
    }

    private void openGui(EntityPlayer player) {
        NoppesUtilServer.sendOpenGui(player, EnumGuiType.Companion, this.npc);
    }

    public EntityPlayer getOwner() {
        if (this.uuid == null || this.uuid.isEmpty()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(this.uuid);
            if (id != null) {
                return NoppesUtilServer.getPlayer(this.npc.getServer(), id);
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
        return null;
    }

    public void setOwner(EntityPlayer player) {
        this.uuid = player.getUniqueID().toString();
    }

    public boolean hasTalent(EnumCompanionTalent talent) {
        return this.getTalentLevel(talent) > 0;
    }

    public int getTalentLevel(EnumCompanionTalent talent) {
        if (!this.talents.containsKey((Object)talent)) {
            return 0;
        }
        int exp = this.talents.get((Object)talent);
        if (exp >= 5000) {
            return 5;
        }
        if (exp >= 3000) {
            return 4;
        }
        if (exp >= 1700) {
            return 3;
        }
        if (exp >= 1000) {
            return 2;
        }
        if (exp >= 400) {
            return 1;
        }
        return 0;
    }

    public Integer getNextLevel(EnumCompanionTalent talent) {
        if (!this.talents.containsKey((Object)talent)) {
            return 0;
        }
        int exp = this.talents.get((Object)talent);
        if (exp < 400) {
            return 400;
        }
        if (exp < 1000) {
            return 700;
        }
        if (exp < 1700) {
            return 1700;
        }
        if (exp < 3000) {
            return 3000;
        }
        return 5000;
    }

    public void levelSword() {
        if (!this.talents.containsKey((Object)EnumCompanionTalent.SWORD)) {
            return;
        }
    }

    public void levelTalent(EnumCompanionTalent talent, int exp) {
        if (!this.talents.containsKey((Object)EnumCompanionTalent.SWORD)) {
            return;
        }
        this.talents.put(talent, exp + this.talents.get((Object)talent));
    }

    public int getExp(EnumCompanionTalent talent) {
        if (this.talents.containsKey((Object)talent)) {
            return this.talents.get((Object)talent);
        }
        return -1;
    }

    public void setExp(EnumCompanionTalent talent, int exp) {
        this.talents.put(talent, exp);
    }

    private boolean isWeapon(ItemStack item) {
        if (item == null || item.getItem() == null) {
            return false;
        }
        return item.getItem() instanceof ItemSword || item.getItem() instanceof ItemBow || item.getItem() == Item.getItemFromBlock((Block)Blocks.COBBLESTONE);
    }

    public boolean canWearWeapon(IItemStack stack) {
        if (stack == null || stack.getMCItemStack().getItem() == null) {
            return false;
        }
        Item item = stack.getMCItemStack().getItem();
        if (item instanceof ItemSword) {
            return this.canWearSword(stack);
        }
        if (item instanceof ItemBow) {
            return this.getTalentLevel(EnumCompanionTalent.RANGED) > 2;
        }
        if (item == Item.getItemFromBlock((Block)Blocks.COBBLESTONE)) {
            return this.getTalentLevel(EnumCompanionTalent.RANGED) > 1;
        }
        return false;
    }

    public boolean canWearArmor(ItemStack item) {
        int level = this.getTalentLevel(EnumCompanionTalent.ARMOR);
        if (item == null || !(item.getItem() instanceof ItemArmor) || level <= 0) {
            return false;
        }
        if (level >= 5) {
            return true;
        }
        ItemArmor armor = (ItemArmor)item.getItem();
        int reduction = (Integer)ObfuscationReflectionHelper.getPrivateValue(ItemArmor.ArmorMaterial.class, (Object)armor.getArmorMaterial(), (int)6);
        if (reduction <= 5 && level >= 1) {
            return true;
        }
        if (reduction <= 7 && level >= 2) {
            return true;
        }
        if (reduction <= 15 && level >= 3) {
            return true;
        }
        return reduction <= 33 && level >= 4;
    }

    public boolean canWearSword(IItemStack item) {
        int level = this.getTalentLevel(EnumCompanionTalent.SWORD);
        if (item == null || !(item.getMCItemStack().getItem() instanceof ItemSword) || level <= 0) {
            return false;
        }
        if (level >= 5) {
            return true;
        }
        return this.getSwordDamage(item) - (double)level < 4.0;
    }

    private double getSwordDamage(IItemStack item) {
        if (item == null || !(item.getMCItemStack().getItem() instanceof ItemSword)) {
            return 0.0;
        }
        HashMultimap map = (HashMultimap)item.getMCItemStack().getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
        for (Map.Entry entry : map.entries()) {
            if (!entry.getKey().equals(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) continue;
            AttributeModifier mod = (AttributeModifier)entry.getValue();
            return mod.getAmount();
        }
        return 0.0;
    }

    public void setStats() {
        IItemStack weapon = this.npc.inventory.getRightHand();
        this.npc.stats.melee.setStrength((int)(1.0 + this.getSwordDamage(weapon)));
        this.npc.stats.healthRegen = 0;
        this.npc.stats.combatRegen = 0;
        int ranged = this.getTalentLevel(EnumCompanionTalent.RANGED);
        if (ranged > 0 && weapon != null) {
            Item item = weapon.getMCItemStack().getItem();
            if (ranged > 0 && item == Item.getItemFromBlock((Block)Blocks.COBBLESTONE)) {
                this.npc.inventory.setProjectile(weapon);
            }
            if (ranged > 0 && item instanceof ItemBow) {
                this.npc.inventory.setProjectile(NpcAPI.Instance().getIItemStack(new ItemStack(Items.ARROW)));
            }
        }
        this.inventory.setSize(2 + this.getTalentLevel(EnumCompanionTalent.INVENTORY) * 2);
    }

    public void setSelfsuficient(boolean bo) {
        if (this.owner == null || this.jobInterface != null && bo == this.jobInterface.isSelfSufficient()) {
            return;
        }
        PlayerData data = PlayerData.get(this.owner);
        if (!bo && data.hasCompanion()) {
            return;
        }
        data.setCompanion(bo ? null : this.npc);
        if (this.job == EnumCompanionJobs.GUARD) {
            ((CompanionGuard)this.jobInterface).isStanding = bo;
        } else if (this.job == EnumCompanionJobs.FARMER) {
            ((CompanionFarmer)this.jobInterface).isStanding = bo;
        }
    }

    public void setSitting(boolean sit) {
        if (sit) {
            this.npc.ais.animationType = 1;
            this.npc.ais.onAttack = 3;
            this.npc.ais.setStartPos(new BlockPos((Entity)this.npc));
            this.npc.getNavigator().clearPath();
            this.npc.setPositionAndUpdate(this.npc.getStartXPos(), this.npc.posY, this.npc.getStartZPos());
        } else {
            this.npc.ais.animationType = this.stage.animation;
            this.npc.ais.onAttack = 0;
        }
        this.npc.updateAI = true;
    }

    public boolean isSitting() {
        return this.npc.ais.animationType == 1;
    }

    public float applyArmorCalculations(DamageSource source, float damage) {
        if (!this.hasInv || this.getTalentLevel(EnumCompanionTalent.ARMOR) <= 0) {
            return damage;
        }
        if (!source.isUnblockable()) {
            this.damageArmor(damage);
            int i = 25 - this.getTotalArmorValue();
            float f1 = damage * (float)i;
            damage = f1 / 25.0f;
        }
        return damage;
    }

    private void damageArmor(float damage) {
        if ((damage /= 4.0f) < 1.0f) {
            damage = 1.0f;
        }
        boolean hasArmor = false;
        Iterator<Map.Entry<Integer, IItemStack>> ita = this.npc.inventory.armor.entrySet().iterator();
        while (ita.hasNext()) {
            Map.Entry<Integer, IItemStack> entry = ita.next();
            IItemStack item = entry.getValue();
            if (item == null || !(item.getMCItemStack().getItem() instanceof ItemArmor)) continue;
            hasArmor = true;
            item.getMCItemStack().damageItem((int)damage, (EntityLivingBase)this.npc);
            if (item.getStackSize() > 0) continue;
            ita.remove();
        }
        this.gainExp(hasArmor ? 4 : 8);
    }

    public int getTotalArmorValue() {
        int armorValue = 0;
        for (IItemStack armor : this.npc.inventory.armor.values()) {
            if (armor == null || !(armor.getMCItemStack().getItem() instanceof ItemArmor)) continue;
            armorValue += ((ItemArmor)armor.getMCItemStack().getItem()).damageReduceAmount;
        }
        return armorValue;
    }

    @Override
    public boolean isFollowing() {
        if (this.jobInterface != null && this.jobInterface.isSelfSufficient()) {
            return false;
        }
        return this.owner != null && !this.isSitting();
    }

    @Override
    public boolean defendOwner() {
        return this.defendOwner && this.owner != null && this.stage != EnumCompanionStage.BABY && (this.jobInterface == null || !this.jobInterface.isSelfSufficient());
    }

    public boolean hasOwner() {
        return !this.uuid.isEmpty();
    }

    public void addMovementStat(double x, double y, double z) {
        int i = Math.round(MathHelper.sqrt((double)(x * x + y * y + z * z)) * 100.0f);
        if (this.npc.isAttacking()) {
            this.foodstats.addExhaustion(0.04f * (float)i * 0.01f);
        } else {
            this.foodstats.addExhaustion(0.02f * (float)i * 0.01f);
        }
    }

    private IItemStack getFood() {
        ArrayList<ItemStack> food = new ArrayList<ItemStack>((Collection<ItemStack>)this.inventory.items);
        Iterator ite = food.iterator();
        int i = -1;
        while (ite.hasNext()) {
            ItemStack is = (ItemStack)ite.next();
            if (is.isEmpty() || !(is.getItem() instanceof ItemFood)) {
                ite.remove();
                continue;
            }
            int amount = ((ItemFood)is.getItem()).getDamage(is);
            if (i != -1 && amount >= i) continue;
            i = amount;
        }
        for (ItemStack is : food) {
            if (((ItemFood)is.getItem()).getDamage(is) != i) continue;
            return NpcAPI.Instance().getIItemStack(is);
        }
        return null;
    }

    public IItemStack getHeldItem() {
        if (this.eating != null) {
            return this.eating;
        }
        return this.npc.inventory.getRightHand();
    }

    public boolean isEating() {
        return this.eating != null;
    }

    public boolean hasInv() {
        if (!this.hasInv) {
            return false;
        }
        return this.hasTalent(EnumCompanionTalent.INVENTORY) || this.hasTalent(EnumCompanionTalent.ARMOR) || this.hasTalent(EnumCompanionTalent.SWORD);
    }

    public void attackedEntity(Entity entity) {
        IItemStack weapon = this.npc.inventory.getRightHand();
        this.gainExp(weapon == null ? 8 : 4);
        if (weapon == null) {
            return;
        }
        weapon.getMCItemStack().damageItem(1, (EntityLivingBase)this.npc);
        if (weapon.getMCItemStack().getCount() <= 0) {
            this.npc.inventory.setRightHand(null);
        }
    }

    public void addTalentExp(EnumCompanionTalent talent, int exp) {
        if (this.talents.containsKey((Object)talent)) {
            exp += this.talents.get((Object)talent).intValue();
        }
        this.talents.put(talent, exp);
    }
}

