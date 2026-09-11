/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.block.Block
 *  net.minecraft.block.material.EnumPushReaction
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.EnumCreatureAttribute
 *  net.minecraft.entity.IRangedAttackMob
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.entity.ai.EntityAIHurtByTarget
 *  net.minecraft.entity.ai.EntityAIOpenDoor
 *  net.minecraft.entity.ai.EntityAIRestrictSun
 *  net.minecraft.entity.ai.EntityAITasks
 *  net.minecraft.entity.ai.EntityAITasks$EntityAITaskEntry
 *  net.minecraft.entity.ai.EntityMoveHelper
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.passive.EntityBat
 *  net.minecraft.entity.passive.IAnimals
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.MobEffects
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagInt
 *  net.minecraft.network.datasync.DataParameter
 *  net.minecraft.network.datasync.DataSerializer
 *  net.minecraft.network.datasync.DataSerializers
 *  net.minecraft.network.datasync.EntityDataManager
 *  net.minecraft.pathfinding.PathNavigateFlying
 *  net.minecraft.pathfinding.PathNavigateGround
 *  net.minecraft.pathfinding.PathNavigateSwimmer
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.SoundEvent
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.world.BossInfo$Color
 *  net.minecraft.world.BossInfo$Overlay
 *  net.minecraft.world.BossInfoServer
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.common.ForgeHooks
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.common.util.FakePlayer
 *  net.minecraftforge.event.ServerChatEvent
 *  net.minecraftforge.fml.common.eventhandler.Event
 *  net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData
 */
package noppes.npcs.entity;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIRestrictSun;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.IChatMessages;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcDamageSource;
import noppes.npcs.Server;
import noppes.npcs.VersionCompatibility;
import noppes.npcs.ai.CombatHandler;
import noppes.npcs.ai.EntityAIAmbushTarget;
import noppes.npcs.ai.EntityAIAnimation;
import noppes.npcs.ai.EntityAIAttackTarget;
import noppes.npcs.ai.EntityAIAvoidTarget;
import noppes.npcs.ai.EntityAIBustDoor;
import noppes.npcs.ai.EntityAIDodgeShoot;
import noppes.npcs.ai.EntityAIFindShade;
import noppes.npcs.ai.EntityAIFollow;
import noppes.npcs.ai.EntityAIJob;
import noppes.npcs.ai.EntityAILook;
import noppes.npcs.ai.EntityAIMoveIndoors;
import noppes.npcs.ai.EntityAIMovingPath;
import noppes.npcs.ai.EntityAIOrbitTarget;
import noppes.npcs.ai.EntityAIPanic;
import noppes.npcs.ai.EntityAIPounceTarget;
import noppes.npcs.ai.EntityAIRangedAttack;
import noppes.npcs.ai.EntityAIReturn;
import noppes.npcs.ai.EntityAIRole;
import noppes.npcs.ai.EntityAISprintToTarget;
import noppes.npcs.ai.EntityAIStalkTarget;
import noppes.npcs.ai.EntityAITransform;
import noppes.npcs.ai.EntityAIWander;
import noppes.npcs.ai.EntityAIWatchClosest;
import noppes.npcs.ai.EntityAIWaterNav;
import noppes.npcs.ai.EntityAIWorldLines;
import noppes.npcs.ai.EntityAIZigZagTarget;
import noppes.npcs.ai.FlyingMoveHelper;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.ai.target.EntityAIClearTarget;
import noppes.npcs.ai.target.EntityAIClosestTarget;
import noppes.npcs.ai.target.EntityAIOwnerHurtByTarget;
import noppes.npcs.ai.target.EntityAIOwnerHurtTarget;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.controllers.data.DataTransform;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataAbilities;
import noppes.npcs.entity.data.DataAdvanced;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataScript;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.items.ItemSoulstoneFilled;
import noppes.npcs.roles.JobBard;
import noppes.npcs.roles.JobFollower;
import noppes.npcs.roles.JobInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.roles.RoleInterface;
import noppes.npcs.util.GameProfileAlt;

public abstract class EntityNPCInterface
extends EntityCreature
implements IEntityAdditionalSpawnData,
ICommandSender,
IRangedAttackMob,
IAnimals {
    public static final DataParameter<Boolean> Attacking = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.BOOLEAN);
    protected static final DataParameter<Integer> Animation = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.VARINT);
    private static final DataParameter<String> RoleData = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.STRING);
    private static final DataParameter<String> JobData = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.STRING);
    private static final DataParameter<Integer> FactionData = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.VARINT);
    private static final DataParameter<Boolean> Walking = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> Interacting = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IsDead = EntityDataManager.createKey(EntityNPCInterface.class, (DataSerializer)DataSerializers.BOOLEAN);
    public static final GameProfileAlt CommandProfile = new GameProfileAlt();
    public static final GameProfileAlt ChatEventProfile = new GameProfileAlt();
    public static final GameProfileAlt GenericProfile = new GameProfileAlt();
    public static FakePlayer ChatEventPlayer;
    public static FakePlayer CommandPlayer;
    public static FakePlayer GenericPlayer;
    public ICustomNpc wrappedNPC;
    public DataAbilities abilities;
    public DataDisplay display;
    public DataStats stats;
    public DataAI ais;
    public DataAdvanced advanced;
    public DataInventory inventory;
    public DataScript script;
    public DataTransform transform;
    public DataTimers timers;
    public CombatHandler combatHandler = new CombatHandler(this);
    public String linkedName = "";
    public long linkedLast = 0L;
    public LinkedNpcController.LinkedData linkedData;
    public float baseHeight = 1.8f;
    public float scaleX;
    public float scaleY;
    public float scaleZ;
    private boolean wasKilled = false;
    public RoleInterface roleInterface;
    public JobInterface jobInterface;
    public HashMap<Integer, DialogOption> dialogs;
    public boolean hasDied = false;
    public long killedtime = 0L;
    public long totalTicksAlive = 0L;
    private int taskCount = 1;
    public int lastInteract = 0;
    public Faction faction;
    private EntityAIRangedAttack aiRange;
    private EntityAIBase aiAttackTarget;
    public EntityAILook lookAi;
    public EntityAIAnimation animateAi;
    public List<EntityLivingBase> interactingEntities = new ArrayList<EntityLivingBase>();
    public ResourceLocation textureLocation = null;
    public ResourceLocation textureGlowLocation = null;
    public ResourceLocation textureCloakLocation = null;
    public int currentAnimation = 0;
    public int animationStart = 0;
    public int npcVersion = VersionCompatibility.ModRev;
    public IChatMessages messages;
    public boolean updateClient = false;
    public boolean updateAI = false;
    public final BossInfoServer bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
    public double field_20066_r;
    public double field_20065_s;
    public double field_20064_t;
    public double field_20063_u;
    public double field_20062_v;
    public double field_20061_w;
    private double startYPos = -1.0;

    public EntityNPCInterface(World world) {
        super(world);
        if (!this.isRemote()) {
            this.wrappedNPC = new NPCWrapper<EntityNPCInterface>(this);
        }
        this.dialogs = new HashMap();
        if (!CustomNpcs.DefaultInteractLine.isEmpty()) {
            this.advanced.interactLines.lines.put(0, new Line(CustomNpcs.DefaultInteractLine));
        }
        this.experienceValue = 0;
        this.scaleZ = 0.9375f;
        this.scaleY = 0.9375f;
        this.scaleX = 0.9375f;
        this.faction = this.getFaction();
        this.setFaction(this.faction.id);
        this.setSize(1.0f, 1.0f);
        this.updateAI = true;
        this.bossInfo.setVisible(false);
    }

    public boolean canBreatheUnderwater() {
        return this.ais.movementType == 2;
    }

    public boolean isPushedByWater() {
        return this.ais.movementType != 2;
    }

    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.abilities = new DataAbilities(this);
        this.display = new DataDisplay(this);
        this.stats = new DataStats(this);
        this.ais = new DataAI(this);
        this.advanced = new DataAdvanced(this);
        this.inventory = new DataInventory(this);
        this.transform = new DataTransform(this);
        this.script = new DataScript(this);
        this.timers = new DataTimers((Object)this);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue((double)this.stats.maxHealth);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue((double)CustomNpcs.NpcNavRange);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue((double)this.getSpeed());
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue((double)this.stats.melee.getStrength());
        this.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue((double)(this.getSpeed() * 2.0f));
    }

    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(RoleData, (Object)String.valueOf(""));
        this.dataManager.register(JobData, (Object)String.valueOf(""));
        this.dataManager.register(FactionData, (Object)0);
        this.dataManager.register(Animation, (Object)0);
        this.dataManager.register(Walking, (Object)false);
        this.dataManager.register(Interacting, (Object)false);
        this.dataManager.register(IsDead, (Object)false);
        this.dataManager.register(Attacking, (Object)false);
    }

    public boolean isEntityAlive() {
        return super.isEntityAlive() && !this.isKilled();
    }

    public void onUpdate() {
        super.onUpdate();
        if (this.ticksExisted % 10 == 0) {
            this.startYPos = this.calculateStartYPos(this.ais.startPos()) + 1.0;
            if (this.startYPos < 0.0 && !this.isRemote()) {
                this.setDead();
            }
            EventHooks.onNPCTick(this);
        }
        this.timers.update();
        if (this.world.isRemote && this.wasKilled != this.isKilled()) {
            this.deathTime = 0;
            this.updateHitbox();
        }
        this.wasKilled = this.isKilled();
        if (this.currentAnimation == 14) {
            this.deathTime = 19;
        }
    }

    public boolean attackEntityAsMob(Entity par1Entity) {
        boolean var4;
        float f = this.stats.melee.getStrength();
        if (this.stats.melee.getDelay() < 10) {
            par1Entity.hurtResistantTime = 0;
        }
        if (par1Entity instanceof EntityLivingBase) {
            NpcEvent.MeleeAttackEvent event = new NpcEvent.MeleeAttackEvent(this.wrappedNPC, (EntityLivingBase)par1Entity, f);
            if (EventHooks.onNPCAttacksMelee(this, event)) {
                return false;
            }
            f = event.damage;
        }
        if (var4 = par1Entity.attackEntityFrom((DamageSource)new NpcDamageSource("mob", (Entity)this), f)) {
            if (this.getOwner() instanceof EntityPlayer) {
                EntityUtil.setRecentlyHit((EntityLivingBase)par1Entity);
            }
            if (this.stats.melee.getKnockback() > 0) {
                par1Entity.addVelocity((double)(-MathHelper.sin((float)(this.rotationYaw * (float)Math.PI / 180.0f)) * (float)this.stats.melee.getKnockback() * 0.5f), 0.1, (double)(MathHelper.cos((float)(this.rotationYaw * (float)Math.PI / 180.0f)) * (float)this.stats.melee.getKnockback() * 0.5f));
                this.motionX *= 0.6;
                this.motionZ *= 0.6;
            }
            if (this.advanced.role == 6) {
                ((RoleCompanion)this.roleInterface).attackedEntity(par1Entity);
            }
        }
        if (this.stats.melee.getEffectType() != 0) {
            if (this.stats.melee.getEffectType() != 1) {
                ((EntityLivingBase)par1Entity).addPotionEffect(new PotionEffect(PotionEffectType.getMCType(this.stats.melee.getEffectType()), this.stats.melee.getEffectTime() * 20, this.stats.melee.getEffectStrength()));
            } else {
                par1Entity.setFire(this.stats.melee.getEffectTime());
            }
        }
        return var4;
    }

    public void onLivingUpdate() {
        float f;
        if (CustomNpcs.FreezeNPCs) {
            return;
        }
        if (this.isAIDisabled()) {
            super.onLivingUpdate();
            return;
        }
        ++this.totalTicksAlive;
        this.updateArmSwingProgress();
        if (this.ticksExisted % 20 == 0) {
            this.faction = this.getFaction();
        }
        if (!this.world.isRemote) {
            if (!this.isKilled() && this.ticksExisted % 20 == 0) {
                this.advanced.scenes.update();
                if (this.getHealth() < this.getMaxHealth()) {
                    if (this.stats.healthRegen > 0 && !this.isAttacking()) {
                        this.heal(this.stats.healthRegen);
                    }
                    if (this.stats.combatRegen > 0 && this.isAttacking()) {
                        this.heal(this.stats.combatRegen);
                    }
                }
                if (this.faction.getsAttacked && !this.isAttacking()) {
                    List list = this.world.getEntitiesWithinAABB(EntityMob.class, this.getEntityBoundingBox().grow(16.0, 16.0, 16.0));
                    for (EntityMob mob : list) {
                        if (mob.getAttackTarget() != null || !this.canSee((Entity)mob)) continue;
                        mob.setAttackTarget((EntityLivingBase)this);
                    }
                }
                if (this.linkedData != null && this.linkedData.time > this.linkedLast) {
                    LinkedNpcController.Instance.loadNpcData(this);
                }
                if (this.updateClient) {
                    this.updateClient();
                }
                if (this.updateAI) {
                    this.updateTasks();
                    this.updateAI = false;
                }
            }
            if (this.getHealth() <= 0.0f && !this.isKilled()) {
                this.clearActivePotions();
                this.dataManager.set(IsDead, (Object)true);
                this.updateTasks();
                this.updateHitbox();
            }
            if (this.display.getBossbar() == 2) {
                this.bossInfo.setVisible(this.getAttackTarget() != null);
            }
            this.dataManager.set(Walking, (Object)(!this.getNavigator().noPath() ? 1 : 0));
            this.dataManager.set(Interacting, (Object)this.isInteracting());
            this.combatHandler.update();
            this.onCollide();
        }
        if (this.wasKilled != this.isKilled() && this.wasKilled) {
            this.reset();
        }
        if (this.world.isDaytime() && !this.world.isRemote && this.stats.burnInSun && (f = this.getBrightness()) > 0.5f && this.rand.nextFloat() * 30.0f < (f - 0.4f) * 2.0f && this.world.canBlockSeeSky(new BlockPos((Entity)this))) {
            this.setFire(8);
        }
        super.onLivingUpdate();
        if (this.world.isRemote) {
            if (this.roleInterface != null) {
                this.roleInterface.clientUpdate();
            }
            if (this.textureCloakLocation != null) {
                this.cloakUpdate();
            }
            if (this.currentAnimation != (Integer)this.dataManager.get(Animation)) {
                this.currentAnimation = (Integer)this.dataManager.get(Animation);
                this.animationStart = this.ticksExisted;
                this.updateHitbox();
            }
            if (this.advanced.job == 1) {
                ((JobBard)this.jobInterface).onLivingUpdate();
            }
        }
        if (this.display.getBossbar() > 0) {
            this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        }
    }

    public void updateClient() {
        NBTTagCompound compound = this.writeSpawnData();
        compound.setInteger("EntityId", this.getEntityId());
        Server.sendAssociatedData((Entity)this, EnumPacketClient.UPDATE_NPC, compound);
        this.updateClient = false;
    }

    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        super.damageEntity(damageSrc, damageAmount);
        this.combatHandler.damage(damageSrc, damageAmount);
    }

    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (this.world.isRemote) {
            return !this.isAttacking();
        }
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (stack != null) {
            Item item = stack.getItem();
            if (item == CustomItems.cloner || item == CustomItems.wand || item == CustomItems.mount || item == CustomItems.scripter) {
                this.setAttackTarget(null);
                this.setRevengeTarget(null);
                return true;
            }
            if (item == CustomItems.moving) {
                this.setAttackTarget(null);
                stack.setTagInfo("NPCID", (NBTBase)new NBTTagInt(this.getEntityId()));
                player.sendMessage((ITextComponent)new TextComponentTranslation("Registered " + this.getName() + " to your NPC Pather", new Object[0]));
                return true;
            }
        }
        if (EventHooks.onNPCInteract(this, player)) {
            return false;
        }
        if (this.getFaction().isAggressiveToPlayer(player)) {
            return !this.isAttacking();
        }
        this.addInteract((EntityLivingBase)player);
        Dialog dialog = this.getDialog(player);
        QuestData data = PlayerData.get((EntityPlayer)player).questData.getQuestCompletion(player, this);
        if (data != null) {
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.QUEST_COMPLETION, data.quest.id);
        } else if (dialog != null) {
            NoppesUtilServer.openDialog(player, this, dialog);
        } else if (this.roleInterface != null) {
            this.roleInterface.interact(player);
        } else {
            this.say(player, this.advanced.getInteractLine());
        }
        return true;
    }

    public void addInteract(EntityLivingBase entity) {
        if (!this.ais.stopAndInteract || this.isAttacking() || !entity.isEntityAlive() || this.isAIDisabled()) {
            return;
        }
        if (this.ticksExisted - this.lastInteract < 180) {
            this.interactingEntities.clear();
        }
        this.getNavigator().clearPath();
        this.lastInteract = this.ticksExisted;
        if (!this.interactingEntities.contains(entity)) {
            this.interactingEntities.add(entity);
        }
    }

    public boolean isInteracting() {
        if (this.ticksExisted - this.lastInteract < 40 || this.isRemote() && ((Boolean)this.dataManager.get(Interacting)).booleanValue()) {
            return true;
        }
        return this.ais.stopAndInteract && !this.interactingEntities.isEmpty() && this.ticksExisted - this.lastInteract < 180;
    }

    private Dialog getDialog(EntityPlayer player) {
        for (DialogOption option : this.dialogs.values()) {
            if (option == null || !option.hasDialog()) continue;
            Dialog dialog = option.getDialog();
            if (!dialog.availability.isAvailable(player)) continue;
            return dialog;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean attackEntityFrom(DamageSource damagesource, float i) {
        if (this.world.isRemote || CustomNpcs.FreezeNPCs || damagesource.damageType.equals("inWall")) {
            return false;
        }
        if (damagesource.damageType.equals("outOfWorld") && this.isKilled()) {
            this.reset();
        }
        i = this.stats.resistances.applyResistance(damagesource, i);
        if ((float)this.hurtResistantTime > (float)this.maxHurtResistantTime / 2.0f && i <= this.lastDamage) {
            return false;
        }
        Entity entity = NoppesUtilServer.GetDamageSourcee(damagesource);
        EntityLivingBase attackingEntity = null;
        if (entity instanceof EntityLivingBase) {
            attackingEntity = (EntityLivingBase)entity;
        }
        if (attackingEntity != null && attackingEntity == this.getOwner()) {
            return false;
        }
        if (attackingEntity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface)attackingEntity;
            if (npc.faction.id == this.faction.id) {
                return false;
            }
            if (npc.getOwner() instanceof EntityPlayer) {
                this.recentlyHit = 100;
            }
        } else if (attackingEntity instanceof EntityPlayer && this.faction.isFriendlyToPlayer((EntityPlayer)attackingEntity)) {
            ForgeHooks.onLivingAttack((EntityLivingBase)this, (DamageSource)damagesource, (float)i);
            return false;
        }
        NpcEvent.DamagedEvent event = new NpcEvent.DamagedEvent(this.wrappedNPC, entity, i, damagesource);
        if (EventHooks.onNPCDamaged(this, event)) {
            ForgeHooks.onLivingAttack((EntityLivingBase)this, (DamageSource)damagesource, (float)i);
            return false;
        }
        i = event.damage;
        if (this.isKilled()) {
            return false;
        }
        if (attackingEntity == null) {
            return super.attackEntityFrom(damagesource, i);
        }
        try {
            if (this.isAttacking()) {
                if (this.getAttackTarget() != null && attackingEntity != null && this.getDistanceSq((Entity)this.getAttackTarget()) > this.getDistanceSq((Entity)attackingEntity)) {
                    this.setAttackTarget(attackingEntity);
                }
                boolean bl = super.attackEntityFrom(damagesource, i);
                return bl;
            }
            if (i > 0.0f) {
                List inRange = this.world.getEntitiesWithinAABB(EntityNPCInterface.class, this.getEntityBoundingBox().grow(32.0, 16.0, 32.0));
                for (EntityNPCInterface npc : inRange) {
                    if (npc.isKilled() || !npc.advanced.defendFaction || npc.faction.id != this.faction.id || !npc.canSee((Entity)this) && !npc.ais.directLOS && !npc.canSee((Entity)attackingEntity)) continue;
                    npc.onAttack(attackingEntity);
                }
                this.setAttackTarget(attackingEntity);
            }
            boolean bl = super.attackEntityFrom(damagesource, i);
            return bl;
        }
        finally {
            if (event.clearTarget) {
                this.setAttackTarget(null);
                this.setRevengeTarget(null);
            }
        }
    }

    public void onAttack(EntityLivingBase entity) {
        if (entity == null || entity == this || this.isAttacking() || this.ais.onAttack == 3 || entity == this.getOwner()) {
            return;
        }
        super.setAttackTarget(entity);
    }

    public void setAttackTarget(EntityLivingBase entity) {
        Line line;
        if (entity instanceof EntityPlayer && ((EntityPlayer)entity).capabilities.disableDamage || entity != null && entity == this.getOwner() || this.getAttackTarget() == entity) {
            return;
        }
        if (entity != null) {
            Object event = new NpcEvent.TargetEvent(this.wrappedNPC, (EntityLivingBase)entity);
            if (EventHooks.onNPCTarget(this, event)) {
                return;
            }
            entity = ((NpcEvent.TargetEvent)((Object)event)).entity == null ? null : ((NpcEvent.TargetEvent)((Object)event)).entity.getMCEntity();
        } else {
            for (EntityAITasks.EntityAITaskEntry en : this.targetTasks.taskEntries) {
                if (!en.using) continue;
                en.using = false;
                en.action.resetTask();
            }
            if (EventHooks.onNPCTargetLost(this, this.getAttackTarget())) {
                return;
            }
        }
        if (entity != null && entity != this && this.ais.onAttack != 3 && !this.isAttacking() && !this.isRemote() && (line = this.advanced.getAttackLine()) != null) {
            this.saySurrounding(line.formatTarget((EntityLivingBase)entity));
        }
        super.setAttackTarget(entity);
    }

    public void attackEntityWithRangedAttack(EntityLivingBase entity, float f) {
        ItemStack proj = ItemStackWrapper.MCItem(this.inventory.getProjectile());
        if (proj == null) {
            this.updateAI = true;
            return;
        }
        NpcEvent.RangedLaunchedEvent event = new NpcEvent.RangedLaunchedEvent(this.wrappedNPC, entity, this.stats.ranged.getStrength());
        for (int i = 0; i < this.stats.ranged.getShotCount(); ++i) {
            EntityProjectile projectile = this.shoot(entity, this.stats.ranged.getAccuracy(), proj, f == 1.0f);
            projectile.damage = event.damage;
            projectile.callback = (projectile1, pos, entity1) -> {
                Entity e;
                if (proj.getItem() == CustomItems.soulstoneFull && (e = ItemSoulstoneFilled.Spawn(null, proj, this.world, pos)) instanceof EntityLivingBase && entity1 instanceof EntityLivingBase) {
                    if (e instanceof EntityLiving) {
                        ((EntityLiving)e).setAttackTarget((EntityLivingBase)entity1);
                    } else {
                        ((EntityLivingBase)e).setRevengeTarget((EntityLivingBase)entity1);
                    }
                }
                projectile1.playSound(this.stats.ranged.getSoundEvent(entity1 != null ? 1 : 2), 1.0f, 1.2f / (this.getRNG().nextFloat() * 0.2f + 0.9f));
                return false;
            };
            this.playSound(this.stats.ranged.getSoundEvent(0), 2.0f, 1.0f);
            event.projectiles.add((IProjectile)NpcAPI.Instance().getIEntity((Entity)projectile));
        }
        EventHooks.onNPCRangedLaunched(this, event);
    }

    public EntityProjectile shoot(EntityLivingBase entity, int accuracy, ItemStack proj, boolean indirect) {
        return this.shoot(entity.posX, entity.getEntityBoundingBox().minY + (double)(entity.height / 2.0f), entity.posZ, accuracy, proj, indirect);
    }

    public EntityProjectile shoot(double x, double y, double z, int accuracy, ItemStack proj, boolean indirect) {
        EntityProjectile projectile = new EntityProjectile(this.world, (EntityLivingBase)this, proj.copy(), true);
        double varX = x - this.posX;
        double varY = y - (this.posY + (double)this.getEyeHeight());
        double varZ = z - this.posZ;
        float varF = projectile.hasGravity() ? MathHelper.sqrt((double)(varX * varX + varZ * varZ)) : 0.0f;
        float angle = projectile.getAngleForXYZ(varX, varY, varZ, varF, indirect);
        float acc = 20.0f - (float)MathHelper.floor((float)((float)accuracy / 5.0f));
        projectile.shoot(varX, varY, varZ, angle, acc);
        this.world.spawnEntity((Entity)projectile);
        return projectile;
    }

    private void clearTasks(EntityAITasks tasks) {
        Iterator iterator = tasks.taskEntries.iterator();
        ArrayList list = new ArrayList(tasks.taskEntries);
        for (EntityAITasks.EntityAITaskEntry entityaitaskentry : list) {
            tasks.removeTask(entityaitaskentry.action);
        }
        tasks.taskEntries.clear();
    }

    private void updateTasks() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.clearTasks(this.tasks);
        this.clearTasks(this.targetTasks);
        if (this.isKilled()) {
            return;
        }
        NPCAttackSelector attackEntitySelector = new NPCAttackSelector(this);
        this.targetTasks.addTask(0, (EntityAIBase)new EntityAIClearTarget(this));
        this.targetTasks.addTask(1, (EntityAIBase)new EntityAIHurtByTarget((EntityCreature)this, false, new Class[0]));
        this.targetTasks.addTask(2, (EntityAIBase)new EntityAIClosestTarget(this, EntityLivingBase.class, 4, this.ais.directLOS, false, attackEntitySelector));
        this.targetTasks.addTask(3, (EntityAIBase)new EntityAIOwnerHurtByTarget(this));
        this.targetTasks.addTask(4, (EntityAIBase)new EntityAIOwnerHurtTarget(this));
        this.world.pathListener.onEntityRemoved((Entity)this);
        if (this.ais.movementType == 1) {
            this.moveHelper = new FlyingMoveHelper(this);
            this.navigator = new PathNavigateFlying((EntityLiving)this, this.world);
        } else if (this.ais.movementType == 2) {
            this.moveHelper = new FlyingMoveHelper(this);
            this.navigator = new PathNavigateSwimmer((EntityLiving)this, this.world);
        } else {
            this.moveHelper = new EntityMoveHelper((EntityLiving)this);
            this.navigator = new PathNavigateGround((EntityLiving)this, this.world);
            this.tasks.addTask(0, (EntityAIBase)new EntityAIWaterNav(this));
        }
        this.world.pathListener.onEntityAdded((Entity)this);
        this.taskCount = 1;
        this.addRegularEntries();
        this.doorInteractType();
        this.seekShelter();
        this.setResponse();
        this.setMoveType();
    }

    private void setResponse() {
        this.aiRange = null;
        this.aiAttackTarget = null;
        if (this.ais.canSprint) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAISprintToTarget(this));
        }
        if (this.ais.onAttack == 1) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIPanic(this, 1.2f));
        } else if (this.ais.onAttack == 2) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIAvoidTarget(this));
        } else if (this.ais.onAttack == 0) {
            if (this.ais.canLeap) {
                this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIPounceTarget(this));
            }
            if (this.inventory.getProjectile() == null) {
                switch (this.ais.tacticalVariant) {
                    case 1: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIZigZagTarget(this, 1.3));
                        break;
                    }
                    case 2: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIOrbitTarget(this, 1.3, true));
                        break;
                    }
                    case 3: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIAvoidTarget(this));
                        break;
                    }
                    case 4: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIAmbushTarget(this, 1.2));
                        break;
                    }
                    case 5: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIStalkTarget(this));
                        break;
                    }
                }
            } else {
                switch (this.ais.tacticalVariant) {
                    case 1: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIDodgeShoot(this));
                        break;
                    }
                    case 2: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIOrbitTarget(this, 1.3, false));
                        break;
                    }
                    case 3: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIAvoidTarget(this));
                        break;
                    }
                    case 4: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIAmbushTarget(this, 1.3));
                        break;
                    }
                    case 5: {
                        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIStalkTarget(this));
                        break;
                    }
                }
            }
            this.aiAttackTarget = new EntityAIAttackTarget(this);
            this.tasks.addTask(this.taskCount, this.aiAttackTarget);
            ((EntityAIAttackTarget)this.aiAttackTarget).navOverride(this.ais.tacticalVariant == 6);
            if (this.inventory.getProjectile() != null) {
                this.aiRange = new EntityAIRangedAttack(this);
                this.tasks.addTask(this.taskCount++, (EntityAIBase)this.aiRange);
                this.aiRange.navOverride(this.ais.tacticalVariant == 6);
            }
        } else if (this.ais.onAttack == 3) {
            // empty if block
        }
    }

    public boolean canFly() {
        return false;
    }

    public void setMoveType() {
        if (this.ais.getMovingType() == 1) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIWander(this));
        }
        if (this.ais.getMovingType() == 2) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIMovingPath(this));
        }
    }

    public void doorInteractType() {
        if (this.canFly()) {
            return;
        }
        Object aiDoor = null;
        if (this.ais.doorInteract == 1) {
            aiDoor = new EntityAIOpenDoor((EntityLiving)this, true);
            this.tasks.addTask(this.taskCount++, (EntityAIBase)aiDoor);
        } else if (this.ais.doorInteract == 0) {
            aiDoor = new EntityAIBustDoor((EntityLiving)this);
            this.tasks.addTask(this.taskCount++, (EntityAIBase)aiDoor);
        }
        if (this.getNavigator() instanceof PathNavigateGround) {
            ((PathNavigateGround)this.getNavigator()).setBreakDoors(aiDoor != null);
        }
    }

    public void seekShelter() {
        if (this.ais.findShelter == 0) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIMoveIndoors(this));
        } else if (this.ais.findShelter == 1) {
            if (!this.canFly()) {
                this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIRestrictSun((EntityCreature)this));
            }
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIFindShade(this));
        }
    }

    public void addRegularEntries() {
        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIReturn(this));
        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIFollow(this));
        if (this.ais.getStandingType() != 1 && this.ais.getStandingType() != 3) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIWatchClosest(this, EntityLivingBase.class, 5.0f));
        }
        this.lookAi = new EntityAILook(this);
        this.tasks.addTask(this.taskCount++, (EntityAIBase)this.lookAi);
        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIWorldLines(this));
        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIJob(this));
        this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAIRole(this));
        this.animateAi = new EntityAIAnimation(this);
        this.tasks.addTask(this.taskCount++, (EntityAIBase)this.animateAi);
        if (this.transform.isValid()) {
            this.tasks.addTask(this.taskCount++, (EntityAIBase)new EntityAITransform(this));
        }
    }

    public float getSpeed() {
        return (float)this.ais.getWalkingSpeed() / 20.0f;
    }

    public float getBlockPathWeight(BlockPos pos) {
        if (this.ais.movementType == 2) {
            return this.world.getBlockState(pos).getMaterial() == Material.WATER ? 10.0f : 0.0f;
        }
        float weight = this.world.getLightBrightness(pos) - 0.5f;
        if (this.world.getBlockState(pos).isOpaqueCube()) {
            weight += 10.0f;
        }
        return weight;
    }

    protected int decreaseAirSupply(int par1) {
        if (!this.stats.canDrown) {
            return par1;
        }
        return super.decreaseAirSupply(par1);
    }

    public EnumCreatureAttribute getCreatureAttribute() {
        return this.stats == null ? null : this.stats.creatureType;
    }

    public int getTalkInterval() {
        return 160;
    }

    public void playLivingSound() {
        if (!this.isEntityAlive()) {
            return;
        }
        this.advanced.playSound(this.getAttackTarget() != null ? 1 : 0, this.getSoundVolume(), this.getSoundPitch());
    }

    protected void playHurtSound(DamageSource source) {
        this.advanced.playSound(2, this.getSoundVolume(), this.getSoundPitch());
    }

    public SoundEvent getDeathSound() {
        return null;
    }

    protected float getSoundPitch() {
        if (this.advanced.disablePitch) {
            return 1.0f;
        }
        return super.getSoundPitch();
    }

    protected void playStepSound(BlockPos pos, Block block) {
        if (this.advanced.getSound(4) != null) {
            this.advanced.playSound(4, 0.15f, 1.0f);
        } else {
            super.playStepSound(pos, block);
        }
    }

    public EntityPlayerMP getFakeChatPlayer() {
        if (this.world.isRemote) {
            return null;
        }
        EntityUtil.Copy((EntityLivingBase)this, (EntityLivingBase)ChatEventPlayer);
        EntityNPCInterface.ChatEventProfile.npc = this;
        ChatEventPlayer.refreshDisplayName();
        ChatEventPlayer.setWorld(this.world);
        ChatEventPlayer.setPosition(this.posX, this.posY, this.posZ);
        return ChatEventPlayer;
    }

    public void saySurrounding(Line line) {
        if (line == null || line.text == null) {
            return;
        }
        if (!line.hideText && !line.text.isEmpty()) {
            ServerChatEvent event = new ServerChatEvent(this.getFakeChatPlayer(), line.text, (ITextComponent)new TextComponentTranslation(line.text.replace("%", "%%"), new Object[0]));
            if (MinecraftForge.EVENT_BUS.post((Event)event) || event.getComponent() == null) {
                return;
            }
            line.text = event.getComponent().getUnformattedText().replace("%%", "%");
        }
        List inRange = this.world.getEntitiesWithinAABB(EntityPlayer.class, this.getEntityBoundingBox().grow(20.0, 20.0, 20.0));
        for (EntityPlayer player : inRange) {
            this.say(player, line);
        }
    }

    public void say(EntityPlayer player, Line line) {
        if (line == null || !this.canSee((Entity)player)) {
            return;
        }
        if (!line.sound.isEmpty()) {
            BlockPos pos = this.getPosition();
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.PLAY_SOUND, line.sound, pos.getX(), pos.getY(), pos.getZ(), Float.valueOf(this.getSoundVolume()), Float.valueOf(this.getSoundPitch()));
        }
        if (line.text != null && !line.text.isEmpty()) {
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.CHATBUBBLE, this.getEntityId(), line.text, !line.hideText);
        }
    }

    public boolean getAlwaysRenderNameTagForRender() {
        return true;
    }

    public void addVelocity(double d, double d1, double d2) {
        if (this.isWalking() && !this.isKilled()) {
            super.addVelocity(d, d1, d2);
        }
    }

    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.npcVersion = compound.getInteger("ModRev");
        VersionCompatibility.CheckNpcCompatibility(this, compound);
        this.display.readToNBT(compound);
        this.stats.readToNBT(compound);
        this.ais.readToNBT(compound);
        this.script.readFromNBT(compound);
        this.timers.readFromNBT(compound);
        this.advanced.readToNBT(compound);
        if (this.advanced.role != 0 && this.roleInterface != null) {
            this.roleInterface.readFromNBT(compound);
        }
        if (this.advanced.job != 0 && this.jobInterface != null) {
            this.jobInterface.readFromNBT(compound);
        }
        this.inventory.readEntityFromNBT(compound);
        this.transform.readToNBT(compound);
        this.killedtime = compound.getLong("KilledTime");
        this.totalTicksAlive = compound.getLong("TotalTicksAlive");
        this.linkedName = compound.getString("LinkedNpcName");
        if (!this.isRemote()) {
            LinkedNpcController.Instance.loadNpcData(this);
        }
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue((double)CustomNpcs.NpcNavRange);
        this.updateAI = true;
    }

    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        this.display.writeToNBT(compound);
        this.stats.writeToNBT(compound);
        this.ais.writeToNBT(compound);
        this.script.writeToNBT(compound);
        this.timers.writeToNBT(compound);
        this.advanced.writeToNBT(compound);
        if (this.advanced.role != 0 && this.roleInterface != null) {
            this.roleInterface.writeToNBT(compound);
        }
        if (this.advanced.job != 0 && this.jobInterface != null) {
            this.jobInterface.writeToNBT(compound);
        }
        this.inventory.writeEntityToNBT(compound);
        this.transform.writeToNBT(compound);
        compound.setLong("KilledTime", this.killedtime);
        compound.setLong("TotalTicksAlive", this.totalTicksAlive);
        compound.setInteger("ModRev", this.npcVersion);
        compound.setString("LinkedNpcName", this.linkedName);
    }

    public void updateHitbox() {
        if (this.currentAnimation == 2 || this.currentAnimation == 7 || this.deathTime > 0) {
            this.width = 0.8f;
            this.height = 0.4f;
        } else if (this.isRiding()) {
            this.width = 0.6f;
            this.height = this.baseHeight * 0.77f;
        } else {
            this.width = 0.6f;
            this.height = this.baseHeight;
        }
        this.width = this.width / 5.0f * (float)this.display.getSize();
        this.height = this.height / 5.0f * (float)this.display.getSize();
        if (!this.display.getHasHitbox() || this.isKilled() && this.stats.hideKilledBody) {
            this.width = 1.0E-5f;
        }
        if ((double)(this.width / 2.0f) > World.MAX_ENTITY_RADIUS) {
            World.MAX_ENTITY_RADIUS = this.width / 2.0f;
        }
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    public void onDeathUpdate() {
        if (this.stats.spawnCycle == 3 || this.stats.spawnCycle == 4) {
            super.onDeathUpdate();
            return;
        }
        ++this.deathTime;
        if (this.world.isRemote) {
            return;
        }
        if (!this.hasDied) {
            this.setDead();
        }
        if (this.killedtime < System.currentTimeMillis() && (this.stats.spawnCycle == 0 || this.world.isDaytime() && this.stats.spawnCycle == 1 || !this.world.isDaytime() && this.stats.spawnCycle == 2)) {
            this.reset();
        }
    }

    public void reset() {
        this.hasDied = false;
        this.isDead = false;
        this.wasKilled = false;
        this.setSprinting(false);
        this.setHealth(this.getMaxHealth());
        this.dataManager.set(Animation, (Object)0);
        this.dataManager.set(Walking, (Object)false);
        this.dataManager.set(IsDead, (Object)false);
        this.dataManager.set(Interacting, (Object)false);
        this.interactingEntities.clear();
        this.combatHandler.reset();
        this.setAttackTarget(null);
        this.setRevengeTarget(null);
        this.deathTime = 0;
        if (this.ais.returnToStart && !this.hasOwner() && !this.isRemote() && !this.isRiding()) {
            this.setLocationAndAngles(this.getStartXPos(), this.getStartYPos(), this.getStartZPos(), this.rotationYaw, this.rotationPitch);
        }
        this.killedtime = 0L;
        this.extinguish();
        this.clearActivePotions();
        this.travel(0.0f, 0.0f, 0.0f);
        this.distanceWalkedModified = 0.0f;
        this.getNavigator().clearPath();
        this.currentAnimation = 0;
        this.updateHitbox();
        this.updateAI = true;
        this.ais.movingPos = 0;
        if (this.getOwner() != null) {
            this.getOwner().setLastAttackedEntity(null);
        }
        this.bossInfo.setVisible(this.display.getBossbar() == 1);
        if (this.jobInterface != null) {
            this.jobInterface.reset();
        }
        EventHooks.onNPCInit(this);
    }

    public void onCollide() {
        if (!this.isEntityAlive() || this.ticksExisted % 4 != 0 || this.world.isRemote) {
            return;
        }
        AxisAlignedBB axisalignedbb = null;
        axisalignedbb = this.getRidingEntity() != null && this.getRidingEntity().isEntityAlive() ? this.getEntityBoundingBox().union(this.getRidingEntity().getEntityBoundingBox()).grow(1.0, 0.0, 1.0) : this.getEntityBoundingBox().grow(1.0, 0.5, 1.0);
        List list = this.world.getEntitiesWithinAABB(EntityLivingBase.class, axisalignedbb);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity)list.get(i);
            if (entity == this || !entity.isEntityAlive()) continue;
            EventHooks.onNPCCollide(this, entity);
        }
    }

    public void setPortal(BlockPos pos) {
    }

    public void cloakUpdate() {
        this.field_20066_r = this.field_20063_u;
        this.field_20065_s = this.field_20062_v;
        this.field_20064_t = this.field_20061_w;
        double d = this.posX - this.field_20063_u;
        double d1 = this.posY - this.field_20062_v;
        double d2 = this.posZ - this.field_20061_w;
        double d3 = 10.0;
        if (d > d3) {
            this.field_20066_r = this.field_20063_u = this.posX;
        }
        if (d2 > d3) {
            this.field_20064_t = this.field_20061_w = this.posZ;
        }
        if (d1 > d3) {
            this.field_20065_s = this.field_20062_v = this.posY;
        }
        if (d < -d3) {
            this.field_20066_r = this.field_20063_u = this.posX;
        }
        if (d2 < -d3) {
            this.field_20064_t = this.field_20061_w = this.posZ;
        }
        if (d1 < -d3) {
            this.field_20065_s = this.field_20062_v = this.posY;
        }
        this.field_20063_u += d * 0.25;
        this.field_20061_w += d2 * 0.25;
        this.field_20062_v += d1 * 0.25;
    }

    protected boolean canDespawn() {
        return this.stats.spawnCycle == 4;
    }

    public ItemStack getHeldItemMainhand() {
        IItemStack item = null;
        item = this.isAttacking() ? this.inventory.getRightHand() : (this.advanced.role == 6 ? ((RoleCompanion)this.roleInterface).getHeldItem() : (this.jobInterface != null && this.jobInterface.overrideMainHand ? this.jobInterface.getMainhand() : this.inventory.getRightHand()));
        return ItemStackWrapper.MCItem(item);
    }

    public ItemStack getHeldItemOffhand() {
        IItemStack item = null;
        item = this.isAttacking() ? this.inventory.getLeftHand() : (this.jobInterface != null && this.jobInterface.overrideOffHand ? this.jobInterface.getOffhand() : this.inventory.getLeftHand());
        return ItemStackWrapper.MCItem(item);
    }

    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            return this.getHeldItemMainhand();
        }
        if (slot == EntityEquipmentSlot.OFFHAND) {
            return this.getHeldItemOffhand();
        }
        return ItemStackWrapper.MCItem(this.inventory.getArmor(3 - slot.getIndex()));
    }

    public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack item) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            this.inventory.weapons.put(0, NpcAPI.Instance().getIItemStack(item));
        } else if (slot == EntityEquipmentSlot.OFFHAND) {
            this.inventory.weapons.put(2, NpcAPI.Instance().getIItemStack(item));
        } else {
            this.inventory.armor.put(3 - slot.getIndex(), NpcAPI.Instance().getIItemStack(item));
        }
    }

    public Iterable<ItemStack> getArmorInventoryList() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        for (int i = 0; i < 4; ++i) {
            list.add(ItemStackWrapper.MCItem(this.inventory.armor.get(3 - i)));
        }
        return list;
    }

    public Iterable<ItemStack> getHeldEquipment() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        list.add(ItemStackWrapper.MCItem(this.inventory.weapons.get(0)));
        list.add(ItemStackWrapper.MCItem(this.inventory.weapons.get(2)));
        return list;
    }

    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
    }

    public void onDeath(DamageSource damagesource) {
        this.setSprinting(false);
        this.getNavigator().clearPath();
        this.extinguish();
        this.clearActivePotions();
        if (!this.isRemote()) {
            this.advanced.playSound(3, this.getSoundVolume(), this.getSoundPitch());
            Entity attackingEntity = NoppesUtilServer.GetDamageSourcee(damagesource);
            EventHooks.onNPCDied(this, attackingEntity, damagesource);
            this.bossInfo.setVisible(false);
            this.inventory.dropStuff(attackingEntity, damagesource);
            Line line = this.advanced.getKilledLine();
            if (line != null) {
                this.saySurrounding(line.formatTarget(attackingEntity instanceof EntityLivingBase ? (EntityLivingBase)attackingEntity : null));
            }
        }
        super.onDeath(damagesource);
    }

    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    public void setDead() {
        this.hasDied = true;
        this.removePassengers();
        this.dismountRidingEntity();
        if (this.world.isRemote || this.stats.spawnCycle == 3 || this.stats.spawnCycle == 4) {
            this.delete();
        } else {
            this.setHealth(-1.0f);
            this.setSprinting(false);
            this.getNavigator().clearPath();
            this.setCurrentAnimation(2);
            this.updateHitbox();
            if (this.killedtime <= 0L) {
                this.killedtime = (long)(this.stats.respawnTime * 1000) + System.currentTimeMillis();
            }
            if (this.advanced.role != 0 && this.roleInterface != null) {
                this.roleInterface.killed();
            }
            if (this.advanced.job != 0 && this.jobInterface != null) {
                this.jobInterface.killed();
            }
        }
    }

    public void delete() {
        if (this.advanced.role != 0 && this.roleInterface != null) {
            this.roleInterface.delete();
        }
        if (this.advanced.job != 0 && this.jobInterface != null) {
            this.jobInterface.delete();
        }
        super.setDead();
    }

    public float getStartXPos() {
        return (float)this.ais.startPos().getX() + this.ais.bodyOffsetX / 10.0f;
    }

    public float getStartZPos() {
        return (float)this.ais.startPos().getZ() + this.ais.bodyOffsetZ / 10.0f;
    }

    public boolean isVeryNearAssignedPlace() {
        double xx = this.posX - (double)this.getStartXPos();
        double zz = this.posZ - (double)this.getStartZPos();
        if (xx < -0.2 || xx > 0.2) {
            return false;
        }
        return !(zz < -0.2) && !(zz > 0.2);
    }

    public double getStartYPos() {
        if (this.startYPos < 0.0) {
            return this.calculateStartYPos(this.ais.startPos());
        }
        return this.startYPos;
    }

    private double calculateStartYPos(BlockPos pos) {
        BlockPos startPos = this.ais.startPos();
        while (pos.getY() > 0) {
            IBlockState state = this.world.getBlockState(pos);
            AxisAlignedBB bb = state.getBoundingBox((IBlockAccess)this.world, pos).offset(pos);
            if (bb != null) {
                if (this.ais.movementType == 2 && startPos.getY() <= pos.getY() && state.getMaterial() == Material.WATER) {
                    pos = pos.down();
                    continue;
                }
                return bb.maxY;
            }
            pos = pos.down();
        }
        return 0.0;
    }

    private BlockPos calculateTopPos(BlockPos pos) {
        BlockPos check = pos;
        while (check.getY() > 0) {
            IBlockState state = this.world.getBlockState(pos);
            AxisAlignedBB bb = state.getBoundingBox((IBlockAccess)this.world, pos).offset(pos);
            if (bb != null) {
                return check;
            }
            check = check.down();
        }
        return pos;
    }

    public boolean isInRange(Entity entity, double range) {
        return this.isInRange(entity.posX, entity.posY, entity.posZ, range);
    }

    public boolean isInRange(double posX, double posY, double posZ, double range) {
        double y = Math.abs(this.posY - posY);
        if (posY >= 0.0 && y > range) {
            return false;
        }
        double x = Math.abs(this.posX - posX);
        double z = Math.abs(this.posZ - posZ);
        return x <= range && z <= range;
    }

    public void givePlayerItem(EntityPlayer player, ItemStack item) {
        if (this.world.isRemote) {
            return;
        }
        item = item.copy();
        float f = 0.7f;
        double d = (double)(this.world.rand.nextFloat() * f) + (double)(1.0f - f);
        double d1 = (double)(this.world.rand.nextFloat() * f) + (double)(1.0f - f);
        double d2 = (double)(this.world.rand.nextFloat() * f) + (double)(1.0f - f);
        EntityItem entityitem = new EntityItem(this.world, this.posX + d, this.posY + d1, this.posZ + d2, item);
        entityitem.setPickupDelay(2);
        this.world.spawnEntity((Entity)entityitem);
        int i = item.getCount();
        if (player.inventory.addItemStackToInventory(item)) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            player.onItemPickup((Entity)entityitem, i);
            if (item.getCount() <= 0) {
                entityitem.setDead();
            }
        }
    }

    public boolean isPlayerSleeping() {
        return this.currentAnimation == 2 && !this.isAttacking();
    }

    public boolean isWalking() {
        return this.ais.getMovingType() != 0 || this.isAttacking() || this.isFollower() || (Boolean)this.dataManager.get(Walking) != false;
    }

    public boolean isSneaking() {
        return this.currentAnimation == 4;
    }

    public void knockBack(Entity par1Entity, float strength, double ratioX, double ratioZ) {
        super.knockBack(par1Entity, strength * (2.0f - this.stats.resistances.knockback), ratioX, ratioZ);
    }

    public Faction getFaction() {
        Faction fac = FactionController.instance.getFaction((Integer)this.dataManager.get(FactionData));
        if (fac == null) {
            return FactionController.instance.getFaction(FactionController.instance.getFirstFactionId());
        }
        return fac;
    }

    public boolean isRemote() {
        return this.world == null || this.world.isRemote;
    }

    public void setFaction(int id) {
        if (id < 0 || this.isRemote()) {
            return;
        }
        this.dataManager.set(FactionData, (Object)id);
    }

    public boolean isPotionApplicable(PotionEffect effect) {
        if (this.stats.potionImmune) {
            return false;
        }
        if (this.getCreatureAttribute() == EnumCreatureAttribute.ARTHROPOD && effect.getPotion() == MobEffects.POISON) {
            return false;
        }
        return super.isPotionApplicable(effect);
    }

    public boolean isAttacking() {
        return (Boolean)this.dataManager.get(Attacking);
    }

    public boolean isKilled() {
        return this.isDead || (Boolean)this.dataManager.get(IsDead) != false;
    }

    public void writeSpawnData(ByteBuf buffer) {
        try {
            Server.writeNBT(buffer, this.writeSpawnData());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NBTTagCompound writeSpawnData() {
        NBTTagCompound bard;
        NBTTagCompound compound = new NBTTagCompound();
        this.display.writeToNBT(compound);
        compound.setInteger("MaxHealth", this.stats.maxHealth);
        compound.setTag("Armor", (NBTBase)NBTTags.nbtIItemStackMap(this.inventory.armor));
        compound.setTag("Weapons", (NBTBase)NBTTags.nbtIItemStackMap(this.inventory.weapons));
        compound.setInteger("Speed", this.ais.getWalkingSpeed());
        compound.setBoolean("DeadBody", this.stats.hideKilledBody);
        compound.setInteger("StandingState", this.ais.getStandingType());
        compound.setInteger("MovingState", this.ais.getMovingType());
        compound.setInteger("Orientation", this.ais.orientation);
        compound.setFloat("PositionXOffset", this.ais.bodyOffsetX);
        compound.setFloat("PositionYOffset", this.ais.bodyOffsetY);
        compound.setFloat("PositionZOffset", this.ais.bodyOffsetZ);
        compound.setInteger("Role", this.advanced.role);
        compound.setInteger("Job", this.advanced.job);
        if (this.advanced.job == 1) {
            bard = new NBTTagCompound();
            this.jobInterface.writeToNBT(bard);
            compound.setTag("Bard", (NBTBase)bard);
        }
        if (this.advanced.job == 9) {
            bard = new NBTTagCompound();
            this.jobInterface.writeToNBT(bard);
            compound.setTag("Puppet", (NBTBase)bard);
        }
        if (this.advanced.role == 6) {
            bard = new NBTTagCompound();
            this.roleInterface.writeToNBT(bard);
            compound.setTag("Companion", (NBTBase)bard);
        }
        if (this instanceof EntityCustomNpc) {
            compound.setTag("ModelData", (NBTBase)((EntityCustomNpc)this).modelData.writeToNBT());
        }
        return compound;
    }

    public void readSpawnData(ByteBuf buf) {
        try {
            this.readSpawnData(Server.readNBT(buf));
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public void readSpawnData(NBTTagCompound compound) {
        NBTTagCompound puppet;
        this.stats.setMaxHealth(compound.getInteger("MaxHealth"));
        this.ais.setWalkingSpeed(compound.getInteger("Speed"));
        this.stats.hideKilledBody = compound.getBoolean("DeadBody");
        this.ais.setStandingType(compound.getInteger("StandingState"));
        this.ais.setMovingType(compound.getInteger("MovingState"));
        this.ais.orientation = compound.getInteger("Orientation");
        this.ais.bodyOffsetX = compound.getFloat("PositionXOffset");
        this.ais.bodyOffsetY = compound.getFloat("PositionYOffset");
        this.ais.bodyOffsetZ = compound.getFloat("PositionZOffset");
        this.inventory.armor = NBTTags.getIItemStackMap(compound.getTagList("Armor", 10));
        this.inventory.weapons = NBTTags.getIItemStackMap(compound.getTagList("Weapons", 10));
        this.advanced.setRole(compound.getInteger("Role"));
        this.advanced.setJob(compound.getInteger("Job"));
        if (this.advanced.job == 1) {
            NBTTagCompound bard = compound.getCompoundTag("Bard");
            this.jobInterface.readFromNBT(bard);
        }
        if (this.advanced.job == 9) {
            puppet = compound.getCompoundTag("Puppet");
            this.jobInterface.readFromNBT(puppet);
        }
        if (this.advanced.role == 6) {
            puppet = compound.getCompoundTag("Companion");
            this.roleInterface.readFromNBT(puppet);
        }
        if (this instanceof EntityCustomNpc) {
            ((EntityCustomNpc)this).modelData.readFromNBT(compound.getCompoundTag("ModelData"));
        }
        this.display.readToNBT(compound);
    }

    public Entity getCommandSenderEntity() {
        if (this.world.isRemote) {
            return this;
        }
        EntityUtil.Copy((EntityLivingBase)this, (EntityLivingBase)CommandPlayer);
        CommandPlayer.setWorld(this.world);
        CommandPlayer.setPosition(this.posX, this.posY, this.posZ);
        return CommandPlayer;
    }

    public String getName() {
        return this.display.getName();
    }

    public BlockPos getPosition() {
        return new BlockPos(this.posX, this.posY, this.posZ);
    }

    public Vec3d getPositionVector() {
        return new Vec3d(this.posX, this.posY, this.posZ);
    }

    public boolean canAttackClass(Class par1Class) {
        return EntityBat.class != par1Class;
    }

    public void setImmuneToFire(boolean immuneToFire) {
        this.isImmuneToFire = immuneToFire;
        this.stats.immuneToFire = immuneToFire;
    }

    public void fall(float distance, float modifier) {
        if (!this.stats.noFallDamage) {
            super.fall(distance, modifier);
        }
    }

    public void setInWeb() {
        if (!this.stats.ignoreCobweb) {
            super.setInWeb();
        }
    }

    public boolean canBeCollidedWith() {
        return !this.isKilled() && this.display.getHasHitbox();
    }

    public boolean canBePushed() {
        return super.canBePushed() && this.display.getHasHitbox();
    }

    public EnumPushReaction getPushReaction() {
        return this.display.getHasHitbox() ? super.getPushReaction() : EnumPushReaction.IGNORE;
    }

    public EntityAIRangedAttack getRangedTask() {
        return this.aiRange;
    }

    public String getRoleData() {
        return (String)this.dataManager.get(RoleData);
    }

    public void setRoleData(String s) {
        this.dataManager.set(RoleData, (Object)s);
    }

    public String getJobData() {
        return (String)this.dataManager.get(RoleData);
    }

    public void setJobData(String s) {
        this.dataManager.set(RoleData, (Object)s);
    }

    public World getEntityWorld() {
        return this.world;
    }

    public boolean isInvisibleToPlayer(EntityPlayer player) {
        return this.display.getVisible() == 1 && (player.getHeldItemMainhand().isEmpty() || player.getHeldItemMainhand().getItem() != CustomItems.wand);
    }

    public boolean isInvisible() {
        return this.display.getVisible() != 0;
    }

    public void sendMessage(ITextComponent var1) {
    }

    public void setCurrentAnimation(int animation) {
        this.currentAnimation = animation;
        this.dataManager.set(Animation, (Object)animation);
    }

    public boolean canSee(Entity entity) {
        return this.getEntitySenses().canSee(entity);
    }

    public boolean isFollower() {
        if (this.advanced.scenes.getOwner() != null) {
            return true;
        }
        return this.roleInterface != null && this.roleInterface.isFollowing() || this.jobInterface != null && this.jobInterface.isFollowing();
    }

    public EntityLivingBase getOwner() {
        if (this.advanced.scenes.getOwner() != null) {
            return this.advanced.scenes.getOwner();
        }
        if (this.advanced.role == 2 && this.roleInterface instanceof RoleFollower) {
            return ((RoleFollower)this.roleInterface).owner;
        }
        if (this.advanced.role == 6 && this.roleInterface instanceof RoleCompanion) {
            return ((RoleCompanion)this.roleInterface).owner;
        }
        if (this.advanced.job == 5 && this.jobInterface instanceof JobFollower) {
            return ((JobFollower)this.jobInterface).following;
        }
        return null;
    }

    public boolean hasOwner() {
        if (this.advanced.scenes.getOwner() != null) {
            return true;
        }
        return this.advanced.role == 2 && ((RoleFollower)this.roleInterface).hasOwner() || this.advanced.role == 6 && ((RoleCompanion)this.roleInterface).hasOwner() || this.advanced.job == 5 && ((JobFollower)this.jobInterface).hasOwner();
    }

    public int followRange() {
        if (this.advanced.scenes.getOwner() != null) {
            return 4;
        }
        if (this.advanced.role == 2 && this.roleInterface.isFollowing()) {
            return 6;
        }
        if (this.advanced.role == 6 && this.roleInterface.isFollowing()) {
            return 4;
        }
        if (this.advanced.job == 5 && this.jobInterface.isFollowing()) {
            return 4;
        }
        return 15;
    }

    public void setHomePosAndDistance(BlockPos pos, int range) {
        super.setHomePosAndDistance(pos, range);
        this.ais.setStartPos(pos);
    }

    protected float applyArmorCalculations(DamageSource source, float damage) {
        if (this.advanced.role == 6) {
            damage = ((RoleCompanion)this.roleInterface).applyArmorCalculations(source, damage);
        }
        return damage;
    }

    public boolean isOnSameTeam(Entity entity) {
        if (!this.isRemote()) {
            if (entity instanceof EntityPlayer && this.getFaction().isFriendlyToPlayer((EntityPlayer)entity)) {
                return true;
            }
            if (entity == this.getOwner()) {
                return true;
            }
            if (entity instanceof EntityNPCInterface && ((EntityNPCInterface)entity).faction.id == this.faction.id) {
                return true;
            }
        }
        return super.isOnSameTeam(entity);
    }

    public void setDataWatcher(EntityDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void travel(float f1, float f2, float f3) {
        double d0 = this.posX;
        double d1 = this.posY;
        double d2 = this.posZ;
        super.travel(f1, f2, f3);
        if (this.advanced.role == 6 && !this.isRemote()) {
            ((RoleCompanion)this.roleInterface).addMovementStat(this.posX - d0, this.posY - d1, this.posZ - d2);
        }
    }

    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    public boolean getLeashed() {
        return false;
    }

    public boolean nearPosition(BlockPos pos) {
        BlockPos npcpos = this.getPosition();
        float x = npcpos.getX() - pos.getX();
        float z = npcpos.getZ() - pos.getZ();
        float y = npcpos.getY() - pos.getY();
        float height = MathHelper.ceil((float)(this.height + 1.0f)) * MathHelper.ceil((float)(this.height + 1.0f));
        return (double)(x * x + z * z) < 2.5 && (double)(y * y) < (double)height + 2.5;
    }

    public void tpTo(EntityLivingBase owner) {
        if (owner == null) {
            return;
        }
        EnumFacing facing = owner.getHorizontalFacing().getOpposite();
        BlockPos pos = new BlockPos(owner.posX, owner.getEntityBoundingBox().minY, owner.posZ);
        pos = pos.add(facing.getFrontOffsetX(), 0, facing.getFrontOffsetZ());
        pos = this.calculateTopPos(pos);
        block0: for (int i = -1; i < 2; ++i) {
            for (int j = 0; j < 3; ++j) {
                BlockPos check = facing.getFrontOffsetX() == 0 ? pos.add(i, 0, j * facing.getFrontOffsetZ()) : pos.add(j * facing.getFrontOffsetX(), 0, i);
                if (this.world.getBlockState(check = this.calculateTopPos(check)).isFullBlock() || this.world.getBlockState(check.up()).isFullBlock()) continue;
                this.setLocationAndAngles((float)check.getX() + 0.5f, check.getY(), (float)check.getZ() + 0.5f, this.rotationYaw, this.rotationPitch);
                this.getNavigator().clearPath();
                continue block0;
            }
        }
    }

    public void setSwingingArms(boolean swingingArms) {
    }

    public boolean getCanSpawnHere() {
        return this.getBlockPathWeight(new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ)) >= 0.0f && this.world.getBlockState(new BlockPos((Entity)this).down()).canEntitySpawn((Entity)this);
    }

    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }
}

