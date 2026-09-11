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
    public static final DataParameter<Boolean> Attacking = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187198_h);
    protected static final DataParameter<Integer> Animation = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187192_b);
    private static final DataParameter<String> RoleData = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187194_d);
    private static final DataParameter<String> JobData = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187194_d);
    private static final DataParameter<Integer> FactionData = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187192_b);
    private static final DataParameter<Boolean> Walking = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<Boolean> Interacting = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<Boolean> IsDead = EntityDataManager.func_187226_a(EntityNPCInterface.class, (DataSerializer)DataSerializers.field_187198_h);
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
    public final BossInfoServer bossInfo = new BossInfoServer(this.func_145748_c_(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
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
        this.field_70728_aV = 0;
        this.scaleZ = 0.9375f;
        this.scaleY = 0.9375f;
        this.scaleX = 0.9375f;
        this.faction = this.getFaction();
        this.setFaction(this.faction.id);
        this.func_70105_a(1.0f, 1.0f);
        this.updateAI = true;
        this.bossInfo.func_186758_d(false);
    }

    public boolean func_70648_aU() {
        return this.ais.movementType == 2;
    }

    public boolean func_96092_aw() {
        return this.ais.movementType != 2;
    }

    protected void func_110147_ax() {
        super.func_110147_ax();
        this.abilities = new DataAbilities(this);
        this.display = new DataDisplay(this);
        this.stats = new DataStats(this);
        this.ais = new DataAI(this);
        this.advanced = new DataAdvanced(this);
        this.inventory = new DataInventory(this);
        this.transform = new DataTransform(this);
        this.script = new DataScript(this);
        this.timers = new DataTimers((Object)this);
        this.func_110140_aT().func_111150_b(SharedMonsterAttributes.field_111264_e);
        this.func_110140_aT().func_111150_b(SharedMonsterAttributes.field_193334_e);
        this.func_110148_a(SharedMonsterAttributes.field_111267_a).func_111128_a((double)this.stats.maxHealth);
        this.func_110148_a(SharedMonsterAttributes.field_111265_b).func_111128_a((double)CustomNpcs.NpcNavRange);
        this.func_110148_a(SharedMonsterAttributes.field_111263_d).func_111128_a((double)this.getSpeed());
        this.func_110148_a(SharedMonsterAttributes.field_111264_e).func_111128_a((double)this.stats.melee.getStrength());
        this.func_110148_a(SharedMonsterAttributes.field_193334_e).func_111128_a((double)(this.getSpeed() * 2.0f));
    }

    protected void func_70088_a() {
        super.func_70088_a();
        this.field_70180_af.func_187214_a(RoleData, (Object)String.valueOf(""));
        this.field_70180_af.func_187214_a(JobData, (Object)String.valueOf(""));
        this.field_70180_af.func_187214_a(FactionData, (Object)0);
        this.field_70180_af.func_187214_a(Animation, (Object)0);
        this.field_70180_af.func_187214_a(Walking, (Object)false);
        this.field_70180_af.func_187214_a(Interacting, (Object)false);
        this.field_70180_af.func_187214_a(IsDead, (Object)false);
        this.field_70180_af.func_187214_a(Attacking, (Object)false);
    }

    public boolean func_70089_S() {
        return super.func_70089_S() && !this.isKilled();
    }

    public void func_70071_h_() {
        super.func_70071_h_();
        if (this.field_70173_aa % 10 == 0) {
            this.startYPos = this.calculateStartYPos(this.ais.startPos()) + 1.0;
            if (this.startYPos < 0.0 && !this.isRemote()) {
                this.func_70106_y();
            }
            EventHooks.onNPCTick(this);
        }
        this.timers.update();
        if (this.field_70170_p.field_72995_K && this.wasKilled != this.isKilled()) {
            this.field_70725_aQ = 0;
            this.updateHitbox();
        }
        this.wasKilled = this.isKilled();
        if (this.currentAnimation == 14) {
            this.field_70725_aQ = 19;
        }
    }

    public boolean func_70652_k(Entity par1Entity) {
        boolean var4;
        float f = this.stats.melee.getStrength();
        if (this.stats.melee.getDelay() < 10) {
            par1Entity.field_70172_ad = 0;
        }
        if (par1Entity instanceof EntityLivingBase) {
            NpcEvent.MeleeAttackEvent event = new NpcEvent.MeleeAttackEvent(this.wrappedNPC, (EntityLivingBase)par1Entity, f);
            if (EventHooks.onNPCAttacksMelee(this, event)) {
                return false;
            }
            f = event.damage;
        }
        if (var4 = par1Entity.func_70097_a((DamageSource)new NpcDamageSource("mob", (Entity)this), f)) {
            if (this.getOwner() instanceof EntityPlayer) {
                EntityUtil.setRecentlyHit((EntityLivingBase)par1Entity);
            }
            if (this.stats.melee.getKnockback() > 0) {
                par1Entity.func_70024_g((double)(-MathHelper.func_76126_a((float)(this.field_70177_z * (float)Math.PI / 180.0f)) * (float)this.stats.melee.getKnockback() * 0.5f), 0.1, (double)(MathHelper.func_76134_b((float)(this.field_70177_z * (float)Math.PI / 180.0f)) * (float)this.stats.melee.getKnockback() * 0.5f));
                this.field_70159_w *= 0.6;
                this.field_70179_y *= 0.6;
            }
            if (this.advanced.role == 6) {
                ((RoleCompanion)this.roleInterface).attackedEntity(par1Entity);
            }
        }
        if (this.stats.melee.getEffectType() != 0) {
            if (this.stats.melee.getEffectType() != 1) {
                ((EntityLivingBase)par1Entity).func_70690_d(new PotionEffect(PotionEffectType.getMCType(this.stats.melee.getEffectType()), this.stats.melee.getEffectTime() * 20, this.stats.melee.getEffectStrength()));
            } else {
                par1Entity.func_70015_d(this.stats.melee.getEffectTime());
            }
        }
        return var4;
    }

    public void func_70636_d() {
        float f;
        if (CustomNpcs.FreezeNPCs) {
            return;
        }
        if (this.func_175446_cd()) {
            super.func_70636_d();
            return;
        }
        ++this.totalTicksAlive;
        this.func_82168_bl();
        if (this.field_70173_aa % 20 == 0) {
            this.faction = this.getFaction();
        }
        if (!this.field_70170_p.field_72995_K) {
            if (!this.isKilled() && this.field_70173_aa % 20 == 0) {
                this.advanced.scenes.update();
                if (this.func_110143_aJ() < this.func_110138_aP()) {
                    if (this.stats.healthRegen > 0 && !this.isAttacking()) {
                        this.func_70691_i(this.stats.healthRegen);
                    }
                    if (this.stats.combatRegen > 0 && this.isAttacking()) {
                        this.func_70691_i(this.stats.combatRegen);
                    }
                }
                if (this.faction.getsAttacked && !this.isAttacking()) {
                    List list = this.field_70170_p.func_72872_a(EntityMob.class, this.func_174813_aQ().func_72314_b(16.0, 16.0, 16.0));
                    for (EntityMob mob : list) {
                        if (mob.func_70638_az() != null || !this.canSee((Entity)mob)) continue;
                        mob.func_70624_b((EntityLivingBase)this);
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
            if (this.func_110143_aJ() <= 0.0f && !this.isKilled()) {
                this.func_70674_bp();
                this.field_70180_af.func_187227_b(IsDead, (Object)true);
                this.updateTasks();
                this.updateHitbox();
            }
            if (this.display.getBossbar() == 2) {
                this.bossInfo.func_186758_d(this.func_70638_az() != null);
            }
            this.field_70180_af.func_187227_b(Walking, (Object)(!this.func_70661_as().func_75500_f() ? 1 : 0));
            this.field_70180_af.func_187227_b(Interacting, (Object)this.isInteracting());
            this.combatHandler.update();
            this.onCollide();
        }
        if (this.wasKilled != this.isKilled() && this.wasKilled) {
            this.reset();
        }
        if (this.field_70170_p.func_72935_r() && !this.field_70170_p.field_72995_K && this.stats.burnInSun && (f = this.func_70013_c()) > 0.5f && this.field_70146_Z.nextFloat() * 30.0f < (f - 0.4f) * 2.0f && this.field_70170_p.func_175710_j(new BlockPos((Entity)this))) {
            this.func_70015_d(8);
        }
        super.func_70636_d();
        if (this.field_70170_p.field_72995_K) {
            if (this.roleInterface != null) {
                this.roleInterface.clientUpdate();
            }
            if (this.textureCloakLocation != null) {
                this.cloakUpdate();
            }
            if (this.currentAnimation != (Integer)this.field_70180_af.func_187225_a(Animation)) {
                this.currentAnimation = (Integer)this.field_70180_af.func_187225_a(Animation);
                this.animationStart = this.field_70173_aa;
                this.updateHitbox();
            }
            if (this.advanced.job == 1) {
                ((JobBard)this.jobInterface).onLivingUpdate();
            }
        }
        if (this.display.getBossbar() > 0) {
            this.bossInfo.func_186735_a(this.func_110143_aJ() / this.func_110138_aP());
        }
    }

    public void updateClient() {
        NBTTagCompound compound = this.writeSpawnData();
        compound.func_74768_a("EntityId", this.func_145782_y());
        Server.sendAssociatedData((Entity)this, EnumPacketClient.UPDATE_NPC, compound);
        this.updateClient = false;
    }

    protected void func_70665_d(DamageSource damageSrc, float damageAmount) {
        super.func_70665_d(damageSrc, damageAmount);
        this.combatHandler.damage(damageSrc, damageAmount);
    }

    public boolean func_184645_a(EntityPlayer player, EnumHand hand) {
        if (this.field_70170_p.field_72995_K) {
            return !this.isAttacking();
        }
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        ItemStack stack = player.func_184586_b(hand);
        if (stack != null) {
            Item item = stack.func_77973_b();
            if (item == CustomItems.cloner || item == CustomItems.wand || item == CustomItems.mount || item == CustomItems.scripter) {
                this.func_70624_b(null);
                this.func_70604_c(null);
                return true;
            }
            if (item == CustomItems.moving) {
                this.func_70624_b(null);
                stack.func_77983_a("NPCID", (NBTBase)new NBTTagInt(this.func_145782_y()));
                player.func_145747_a((ITextComponent)new TextComponentTranslation("Registered " + this.func_70005_c_() + " to your NPC Pather", new Object[0]));
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
        if (!this.ais.stopAndInteract || this.isAttacking() || !entity.func_70089_S() || this.func_175446_cd()) {
            return;
        }
        if (this.field_70173_aa - this.lastInteract < 180) {
            this.interactingEntities.clear();
        }
        this.func_70661_as().func_75499_g();
        this.lastInteract = this.field_70173_aa;
        if (!this.interactingEntities.contains(entity)) {
            this.interactingEntities.add(entity);
        }
    }

    public boolean isInteracting() {
        if (this.field_70173_aa - this.lastInteract < 40 || this.isRemote() && ((Boolean)this.field_70180_af.func_187225_a(Interacting)).booleanValue()) {
            return true;
        }
        return this.ais.stopAndInteract && !this.interactingEntities.isEmpty() && this.field_70173_aa - this.lastInteract < 180;
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
    public boolean func_70097_a(DamageSource damagesource, float i) {
        if (this.field_70170_p.field_72995_K || CustomNpcs.FreezeNPCs || damagesource.field_76373_n.equals("inWall")) {
            return false;
        }
        if (damagesource.field_76373_n.equals("outOfWorld") && this.isKilled()) {
            this.reset();
        }
        i = this.stats.resistances.applyResistance(damagesource, i);
        if ((float)this.field_70172_ad > (float)this.field_70771_an / 2.0f && i <= this.field_110153_bc) {
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
                this.field_70718_bc = 100;
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
            return super.func_70097_a(damagesource, i);
        }
        try {
            if (this.isAttacking()) {
                if (this.func_70638_az() != null && attackingEntity != null && this.func_70068_e((Entity)this.func_70638_az()) > this.func_70068_e((Entity)attackingEntity)) {
                    this.func_70624_b(attackingEntity);
                }
                boolean bl = super.func_70097_a(damagesource, i);
                return bl;
            }
            if (i > 0.0f) {
                List inRange = this.field_70170_p.func_72872_a(EntityNPCInterface.class, this.func_174813_aQ().func_72314_b(32.0, 16.0, 32.0));
                for (EntityNPCInterface npc : inRange) {
                    if (npc.isKilled() || !npc.advanced.defendFaction || npc.faction.id != this.faction.id || !npc.canSee((Entity)this) && !npc.ais.directLOS && !npc.canSee((Entity)attackingEntity)) continue;
                    npc.onAttack(attackingEntity);
                }
                this.func_70624_b(attackingEntity);
            }
            boolean bl = super.func_70097_a(damagesource, i);
            return bl;
        }
        finally {
            if (event.clearTarget) {
                this.func_70624_b(null);
                this.func_70604_c(null);
            }
        }
    }

    public void onAttack(EntityLivingBase entity) {
        if (entity == null || entity == this || this.isAttacking() || this.ais.onAttack == 3 || entity == this.getOwner()) {
            return;
        }
        super.func_70624_b(entity);
    }

    public void func_70624_b(EntityLivingBase entity) {
        Line line;
        if (entity instanceof EntityPlayer && ((EntityPlayer)entity).field_71075_bZ.field_75102_a || entity != null && entity == this.getOwner() || this.func_70638_az() == entity) {
            return;
        }
        if (entity != null) {
            Object event = new NpcEvent.TargetEvent(this.wrappedNPC, (EntityLivingBase)entity);
            if (EventHooks.onNPCTarget(this, event)) {
                return;
            }
            entity = ((NpcEvent.TargetEvent)((Object)event)).entity == null ? null : ((NpcEvent.TargetEvent)((Object)event)).entity.getMCEntity();
        } else {
            for (EntityAITasks.EntityAITaskEntry en : this.field_70715_bh.field_75782_a) {
                if (!en.field_188524_c) continue;
                en.field_188524_c = false;
                en.field_75733_a.func_75251_c();
            }
            if (EventHooks.onNPCTargetLost(this, this.func_70638_az())) {
                return;
            }
        }
        if (entity != null && entity != this && this.ais.onAttack != 3 && !this.isAttacking() && !this.isRemote() && (line = this.advanced.getAttackLine()) != null) {
            this.saySurrounding(line.formatTarget((EntityLivingBase)entity));
        }
        super.func_70624_b(entity);
    }

    public void func_82196_d(EntityLivingBase entity, float f) {
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
                if (proj.func_77973_b() == CustomItems.soulstoneFull && (e = ItemSoulstoneFilled.Spawn(null, proj, this.field_70170_p, pos)) instanceof EntityLivingBase && entity1 instanceof EntityLivingBase) {
                    if (e instanceof EntityLiving) {
                        ((EntityLiving)e).func_70624_b((EntityLivingBase)entity1);
                    } else {
                        ((EntityLivingBase)e).func_70604_c((EntityLivingBase)entity1);
                    }
                }
                projectile1.func_184185_a(this.stats.ranged.getSoundEvent(entity1 != null ? 1 : 2), 1.0f, 1.2f / (this.func_70681_au().nextFloat() * 0.2f + 0.9f));
                return false;
            };
            this.func_184185_a(this.stats.ranged.getSoundEvent(0), 2.0f, 1.0f);
            event.projectiles.add((IProjectile)NpcAPI.Instance().getIEntity((Entity)projectile));
        }
        EventHooks.onNPCRangedLaunched(this, event);
    }

    public EntityProjectile shoot(EntityLivingBase entity, int accuracy, ItemStack proj, boolean indirect) {
        return this.shoot(entity.field_70165_t, entity.func_174813_aQ().field_72338_b + (double)(entity.field_70131_O / 2.0f), entity.field_70161_v, accuracy, proj, indirect);
    }

    public EntityProjectile shoot(double x, double y, double z, int accuracy, ItemStack proj, boolean indirect) {
        EntityProjectile projectile = new EntityProjectile(this.field_70170_p, (EntityLivingBase)this, proj.func_77946_l(), true);
        double varX = x - this.field_70165_t;
        double varY = y - (this.field_70163_u + (double)this.func_70047_e());
        double varZ = z - this.field_70161_v;
        float varF = projectile.hasGravity() ? MathHelper.func_76133_a((double)(varX * varX + varZ * varZ)) : 0.0f;
        float angle = projectile.getAngleForXYZ(varX, varY, varZ, varF, indirect);
        float acc = 20.0f - (float)MathHelper.func_76141_d((float)((float)accuracy / 5.0f));
        projectile.func_70186_c(varX, varY, varZ, angle, acc);
        this.field_70170_p.func_72838_d((Entity)projectile);
        return projectile;
    }

    private void clearTasks(EntityAITasks tasks) {
        Iterator iterator = tasks.field_75782_a.iterator();
        ArrayList list = new ArrayList(tasks.field_75782_a);
        for (EntityAITasks.EntityAITaskEntry entityaitaskentry : list) {
            tasks.func_85156_a(entityaitaskentry.field_75733_a);
        }
        tasks.field_75782_a.clear();
    }

    private void updateTasks() {
        if (this.field_70170_p == null || this.field_70170_p.field_72995_K) {
            return;
        }
        this.clearTasks(this.field_70714_bg);
        this.clearTasks(this.field_70715_bh);
        if (this.isKilled()) {
            return;
        }
        NPCAttackSelector attackEntitySelector = new NPCAttackSelector(this);
        this.field_70715_bh.func_75776_a(0, (EntityAIBase)new EntityAIClearTarget(this));
        this.field_70715_bh.func_75776_a(1, (EntityAIBase)new EntityAIHurtByTarget((EntityCreature)this, false, new Class[0]));
        this.field_70715_bh.func_75776_a(2, (EntityAIBase)new EntityAIClosestTarget(this, EntityLivingBase.class, 4, this.ais.directLOS, false, attackEntitySelector));
        this.field_70715_bh.func_75776_a(3, (EntityAIBase)new EntityAIOwnerHurtByTarget(this));
        this.field_70715_bh.func_75776_a(4, (EntityAIBase)new EntityAIOwnerHurtTarget(this));
        this.field_70170_p.field_184152_t.func_72709_b((Entity)this);
        if (this.ais.movementType == 1) {
            this.field_70765_h = new FlyingMoveHelper(this);
            this.field_70699_by = new PathNavigateFlying((EntityLiving)this, this.field_70170_p);
        } else if (this.ais.movementType == 2) {
            this.field_70765_h = new FlyingMoveHelper(this);
            this.field_70699_by = new PathNavigateSwimmer((EntityLiving)this, this.field_70170_p);
        } else {
            this.field_70765_h = new EntityMoveHelper((EntityLiving)this);
            this.field_70699_by = new PathNavigateGround((EntityLiving)this, this.field_70170_p);
            this.field_70714_bg.func_75776_a(0, (EntityAIBase)new EntityAIWaterNav(this));
        }
        this.field_70170_p.field_184152_t.func_72703_a((Entity)this);
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
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAISprintToTarget(this));
        }
        if (this.ais.onAttack == 1) {
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIPanic(this, 1.2f));
        } else if (this.ais.onAttack == 2) {
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIAvoidTarget(this));
        } else if (this.ais.onAttack == 0) {
            if (this.ais.canLeap) {
                this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIPounceTarget(this));
            }
            if (this.inventory.getProjectile() == null) {
                switch (this.ais.tacticalVariant) {
                    case 1: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIZigZagTarget(this, 1.3));
                        break;
                    }
                    case 2: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIOrbitTarget(this, 1.3, true));
                        break;
                    }
                    case 3: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIAvoidTarget(this));
                        break;
                    }
                    case 4: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIAmbushTarget(this, 1.2));
                        break;
                    }
                    case 5: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIStalkTarget(this));
                        break;
                    }
                }
            } else {
                switch (this.ais.tacticalVariant) {
                    case 1: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIDodgeShoot(this));
                        break;
                    }
                    case 2: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIOrbitTarget(this, 1.3, false));
                        break;
                    }
                    case 3: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIAvoidTarget(this));
                        break;
                    }
                    case 4: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIAmbushTarget(this, 1.3));
                        break;
                    }
                    case 5: {
                        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIStalkTarget(this));
                        break;
                    }
                }
            }
            this.aiAttackTarget = new EntityAIAttackTarget(this);
            this.field_70714_bg.func_75776_a(this.taskCount, this.aiAttackTarget);
            ((EntityAIAttackTarget)this.aiAttackTarget).navOverride(this.ais.tacticalVariant == 6);
            if (this.inventory.getProjectile() != null) {
                this.aiRange = new EntityAIRangedAttack(this);
                this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)this.aiRange);
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
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIWander(this));
        }
        if (this.ais.getMovingType() == 2) {
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIMovingPath(this));
        }
    }

    public void doorInteractType() {
        if (this.canFly()) {
            return;
        }
        Object aiDoor = null;
        if (this.ais.doorInteract == 1) {
            aiDoor = new EntityAIOpenDoor((EntityLiving)this, true);
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)aiDoor);
        } else if (this.ais.doorInteract == 0) {
            aiDoor = new EntityAIBustDoor((EntityLiving)this);
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)aiDoor);
        }
        if (this.func_70661_as() instanceof PathNavigateGround) {
            ((PathNavigateGround)this.func_70661_as()).func_179688_b(aiDoor != null);
        }
    }

    public void seekShelter() {
        if (this.ais.findShelter == 0) {
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIMoveIndoors(this));
        } else if (this.ais.findShelter == 1) {
            if (!this.canFly()) {
                this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIRestrictSun((EntityCreature)this));
            }
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIFindShade(this));
        }
    }

    public void addRegularEntries() {
        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIReturn(this));
        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIFollow(this));
        if (this.ais.getStandingType() != 1 && this.ais.getStandingType() != 3) {
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIWatchClosest(this, EntityLivingBase.class, 5.0f));
        }
        this.lookAi = new EntityAILook(this);
        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)this.lookAi);
        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIWorldLines(this));
        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIJob(this));
        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAIRole(this));
        this.animateAi = new EntityAIAnimation(this);
        this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)this.animateAi);
        if (this.transform.isValid()) {
            this.field_70714_bg.func_75776_a(this.taskCount++, (EntityAIBase)new EntityAITransform(this));
        }
    }

    public float getSpeed() {
        return (float)this.ais.getWalkingSpeed() / 20.0f;
    }

    public float func_180484_a(BlockPos pos) {
        if (this.ais.movementType == 2) {
            return this.field_70170_p.func_180495_p(pos).func_185904_a() == Material.field_151586_h ? 10.0f : 0.0f;
        }
        float weight = this.field_70170_p.func_175724_o(pos) - 0.5f;
        if (this.field_70170_p.func_180495_p(pos).func_185914_p()) {
            weight += 10.0f;
        }
        return weight;
    }

    protected int func_70682_h(int par1) {
        if (!this.stats.canDrown) {
            return par1;
        }
        return super.func_70682_h(par1);
    }

    public EnumCreatureAttribute func_70668_bt() {
        return this.stats == null ? null : this.stats.creatureType;
    }

    public int func_70627_aG() {
        return 160;
    }

    public void func_70642_aH() {
        if (!this.func_70089_S()) {
            return;
        }
        this.advanced.playSound(this.func_70638_az() != null ? 1 : 0, this.func_70599_aP(), this.func_70647_i());
    }

    protected void func_184581_c(DamageSource source) {
        this.advanced.playSound(2, this.func_70599_aP(), this.func_70647_i());
    }

    public SoundEvent func_184615_bR() {
        return null;
    }

    protected float func_70647_i() {
        if (this.advanced.disablePitch) {
            return 1.0f;
        }
        return super.func_70647_i();
    }

    protected void func_180429_a(BlockPos pos, Block block) {
        if (this.advanced.getSound(4) != null) {
            this.advanced.playSound(4, 0.15f, 1.0f);
        } else {
            super.func_180429_a(pos, block);
        }
    }

    public EntityPlayerMP getFakeChatPlayer() {
        if (this.field_70170_p.field_72995_K) {
            return null;
        }
        EntityUtil.Copy((EntityLivingBase)this, (EntityLivingBase)ChatEventPlayer);
        EntityNPCInterface.ChatEventProfile.npc = this;
        ChatEventPlayer.refreshDisplayName();
        ChatEventPlayer.func_70029_a(this.field_70170_p);
        ChatEventPlayer.func_70107_b(this.field_70165_t, this.field_70163_u, this.field_70161_v);
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
            line.text = event.getComponent().func_150260_c().replace("%%", "%");
        }
        List inRange = this.field_70170_p.func_72872_a(EntityPlayer.class, this.func_174813_aQ().func_72314_b(20.0, 20.0, 20.0));
        for (EntityPlayer player : inRange) {
            this.say(player, line);
        }
    }

    public void say(EntityPlayer player, Line line) {
        if (line == null || !this.canSee((Entity)player)) {
            return;
        }
        if (!line.sound.isEmpty()) {
            BlockPos pos = this.func_180425_c();
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.PLAY_SOUND, line.sound, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), Float.valueOf(this.func_70599_aP()), Float.valueOf(this.func_70647_i()));
        }
        if (line.text != null && !line.text.isEmpty()) {
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.CHATBUBBLE, this.func_145782_y(), line.text, !line.hideText);
        }
    }

    public boolean func_94059_bO() {
        return true;
    }

    public void func_70024_g(double d, double d1, double d2) {
        if (this.isWalking() && !this.isKilled()) {
            super.func_70024_g(d, d1, d2);
        }
    }

    public void func_70037_a(NBTTagCompound compound) {
        super.func_70037_a(compound);
        this.npcVersion = compound.func_74762_e("ModRev");
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
        this.killedtime = compound.func_74763_f("KilledTime");
        this.totalTicksAlive = compound.func_74763_f("TotalTicksAlive");
        this.linkedName = compound.func_74779_i("LinkedNpcName");
        if (!this.isRemote()) {
            LinkedNpcController.Instance.loadNpcData(this);
        }
        this.func_110148_a(SharedMonsterAttributes.field_111265_b).func_111128_a((double)CustomNpcs.NpcNavRange);
        this.updateAI = true;
    }

    public void func_70014_b(NBTTagCompound compound) {
        super.func_70014_b(compound);
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
        compound.func_74772_a("KilledTime", this.killedtime);
        compound.func_74772_a("TotalTicksAlive", this.totalTicksAlive);
        compound.func_74768_a("ModRev", this.npcVersion);
        compound.func_74778_a("LinkedNpcName", this.linkedName);
    }

    public void updateHitbox() {
        if (this.currentAnimation == 2 || this.currentAnimation == 7 || this.field_70725_aQ > 0) {
            this.field_70130_N = 0.8f;
            this.field_70131_O = 0.4f;
        } else if (this.func_184218_aH()) {
            this.field_70130_N = 0.6f;
            this.field_70131_O = this.baseHeight * 0.77f;
        } else {
            this.field_70130_N = 0.6f;
            this.field_70131_O = this.baseHeight;
        }
        this.field_70130_N = this.field_70130_N / 5.0f * (float)this.display.getSize();
        this.field_70131_O = this.field_70131_O / 5.0f * (float)this.display.getSize();
        if (!this.display.getHasHitbox() || this.isKilled() && this.stats.hideKilledBody) {
            this.field_70130_N = 1.0E-5f;
        }
        if ((double)(this.field_70130_N / 2.0f) > World.MAX_ENTITY_RADIUS) {
            World.MAX_ENTITY_RADIUS = this.field_70130_N / 2.0f;
        }
        this.func_70107_b(this.field_70165_t, this.field_70163_u, this.field_70161_v);
    }

    public void func_70609_aI() {
        if (this.stats.spawnCycle == 3 || this.stats.spawnCycle == 4) {
            super.func_70609_aI();
            return;
        }
        ++this.field_70725_aQ;
        if (this.field_70170_p.field_72995_K) {
            return;
        }
        if (!this.hasDied) {
            this.func_70106_y();
        }
        if (this.killedtime < System.currentTimeMillis() && (this.stats.spawnCycle == 0 || this.field_70170_p.func_72935_r() && this.stats.spawnCycle == 1 || !this.field_70170_p.func_72935_r() && this.stats.spawnCycle == 2)) {
            this.reset();
        }
    }

    public void reset() {
        this.hasDied = false;
        this.field_70128_L = false;
        this.wasKilled = false;
        this.func_70031_b(false);
        this.func_70606_j(this.func_110138_aP());
        this.field_70180_af.func_187227_b(Animation, (Object)0);
        this.field_70180_af.func_187227_b(Walking, (Object)false);
        this.field_70180_af.func_187227_b(IsDead, (Object)false);
        this.field_70180_af.func_187227_b(Interacting, (Object)false);
        this.interactingEntities.clear();
        this.combatHandler.reset();
        this.func_70624_b(null);
        this.func_70604_c(null);
        this.field_70725_aQ = 0;
        if (this.ais.returnToStart && !this.hasOwner() && !this.isRemote() && !this.func_184218_aH()) {
            this.func_70012_b(this.getStartXPos(), this.getStartYPos(), this.getStartZPos(), this.field_70177_z, this.field_70125_A);
        }
        this.killedtime = 0L;
        this.func_70066_B();
        this.func_70674_bp();
        this.func_191986_a(0.0f, 0.0f, 0.0f);
        this.field_70140_Q = 0.0f;
        this.func_70661_as().func_75499_g();
        this.currentAnimation = 0;
        this.updateHitbox();
        this.updateAI = true;
        this.ais.movingPos = 0;
        if (this.getOwner() != null) {
            this.getOwner().func_130011_c(null);
        }
        this.bossInfo.func_186758_d(this.display.getBossbar() == 1);
        if (this.jobInterface != null) {
            this.jobInterface.reset();
        }
        EventHooks.onNPCInit(this);
    }

    public void onCollide() {
        if (!this.func_70089_S() || this.field_70173_aa % 4 != 0 || this.field_70170_p.field_72995_K) {
            return;
        }
        AxisAlignedBB axisalignedbb = null;
        axisalignedbb = this.func_184187_bx() != null && this.func_184187_bx().func_70089_S() ? this.func_174813_aQ().func_111270_a(this.func_184187_bx().func_174813_aQ()).func_72314_b(1.0, 0.0, 1.0) : this.func_174813_aQ().func_72314_b(1.0, 0.5, 1.0);
        List list = this.field_70170_p.func_72872_a(EntityLivingBase.class, axisalignedbb);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity)list.get(i);
            if (entity == this || !entity.func_70089_S()) continue;
            EventHooks.onNPCCollide(this, entity);
        }
    }

    public void func_181015_d(BlockPos pos) {
    }

    public void cloakUpdate() {
        this.field_20066_r = this.field_20063_u;
        this.field_20065_s = this.field_20062_v;
        this.field_20064_t = this.field_20061_w;
        double d = this.field_70165_t - this.field_20063_u;
        double d1 = this.field_70163_u - this.field_20062_v;
        double d2 = this.field_70161_v - this.field_20061_w;
        double d3 = 10.0;
        if (d > d3) {
            this.field_20066_r = this.field_20063_u = this.field_70165_t;
        }
        if (d2 > d3) {
            this.field_20064_t = this.field_20061_w = this.field_70161_v;
        }
        if (d1 > d3) {
            this.field_20065_s = this.field_20062_v = this.field_70163_u;
        }
        if (d < -d3) {
            this.field_20066_r = this.field_20063_u = this.field_70165_t;
        }
        if (d2 < -d3) {
            this.field_20064_t = this.field_20061_w = this.field_70161_v;
        }
        if (d1 < -d3) {
            this.field_20065_s = this.field_20062_v = this.field_70163_u;
        }
        this.field_20063_u += d * 0.25;
        this.field_20061_w += d2 * 0.25;
        this.field_20062_v += d1 * 0.25;
    }

    protected boolean func_70692_ba() {
        return this.stats.spawnCycle == 4;
    }

    public ItemStack func_184614_ca() {
        IItemStack item = null;
        item = this.isAttacking() ? this.inventory.getRightHand() : (this.advanced.role == 6 ? ((RoleCompanion)this.roleInterface).getHeldItem() : (this.jobInterface != null && this.jobInterface.overrideMainHand ? this.jobInterface.getMainhand() : this.inventory.getRightHand()));
        return ItemStackWrapper.MCItem(item);
    }

    public ItemStack func_184592_cb() {
        IItemStack item = null;
        item = this.isAttacking() ? this.inventory.getLeftHand() : (this.jobInterface != null && this.jobInterface.overrideOffHand ? this.jobInterface.getOffhand() : this.inventory.getLeftHand());
        return ItemStackWrapper.MCItem(item);
    }

    public ItemStack func_184582_a(EntityEquipmentSlot slot) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            return this.func_184614_ca();
        }
        if (slot == EntityEquipmentSlot.OFFHAND) {
            return this.func_184592_cb();
        }
        return ItemStackWrapper.MCItem(this.inventory.getArmor(3 - slot.func_188454_b()));
    }

    public void func_184201_a(EntityEquipmentSlot slot, ItemStack item) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            this.inventory.weapons.put(0, NpcAPI.Instance().getIItemStack(item));
        } else if (slot == EntityEquipmentSlot.OFFHAND) {
            this.inventory.weapons.put(2, NpcAPI.Instance().getIItemStack(item));
        } else {
            this.inventory.armor.put(3 - slot.func_188454_b(), NpcAPI.Instance().getIItemStack(item));
        }
    }

    public Iterable<ItemStack> func_184193_aE() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        for (int i = 0; i < 4; ++i) {
            list.add(ItemStackWrapper.MCItem(this.inventory.armor.get(3 - i)));
        }
        return list;
    }

    public Iterable<ItemStack> func_184214_aD() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        list.add(ItemStackWrapper.MCItem(this.inventory.weapons.get(0)));
        list.add(ItemStackWrapper.MCItem(this.inventory.weapons.get(2)));
        return list;
    }

    protected void func_82160_b(boolean wasRecentlyHit, int lootingModifier) {
    }

    protected void func_70628_a(boolean wasRecentlyHit, int lootingModifier) {
    }

    public void func_70645_a(DamageSource damagesource) {
        this.func_70031_b(false);
        this.func_70661_as().func_75499_g();
        this.func_70066_B();
        this.func_70674_bp();
        if (!this.isRemote()) {
            this.advanced.playSound(3, this.func_70599_aP(), this.func_70647_i());
            Entity attackingEntity = NoppesUtilServer.GetDamageSourcee(damagesource);
            EventHooks.onNPCDied(this, attackingEntity, damagesource);
            this.bossInfo.func_186758_d(false);
            this.inventory.dropStuff(attackingEntity, damagesource);
            Line line = this.advanced.getKilledLine();
            if (line != null) {
                this.saySurrounding(line.formatTarget(attackingEntity instanceof EntityLivingBase ? (EntityLivingBase)attackingEntity : null));
            }
        }
        super.func_70645_a(damagesource);
    }

    public void func_184178_b(EntityPlayerMP player) {
        super.func_184178_b(player);
        this.bossInfo.func_186760_a(player);
    }

    public void func_184203_c(EntityPlayerMP player) {
        super.func_184203_c(player);
        this.bossInfo.func_186761_b(player);
    }

    public void func_70106_y() {
        this.hasDied = true;
        this.func_184226_ay();
        this.func_184210_p();
        if (this.field_70170_p.field_72995_K || this.stats.spawnCycle == 3 || this.stats.spawnCycle == 4) {
            this.delete();
        } else {
            this.func_70606_j(-1.0f);
            this.func_70031_b(false);
            this.func_70661_as().func_75499_g();
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
        super.func_70106_y();
    }

    public float getStartXPos() {
        return (float)this.ais.startPos().func_177958_n() + this.ais.bodyOffsetX / 10.0f;
    }

    public float getStartZPos() {
        return (float)this.ais.startPos().func_177952_p() + this.ais.bodyOffsetZ / 10.0f;
    }

    public boolean isVeryNearAssignedPlace() {
        double xx = this.field_70165_t - (double)this.getStartXPos();
        double zz = this.field_70161_v - (double)this.getStartZPos();
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
        while (pos.func_177956_o() > 0) {
            IBlockState state = this.field_70170_p.func_180495_p(pos);
            AxisAlignedBB bb = state.func_185900_c((IBlockAccess)this.field_70170_p, pos).func_186670_a(pos);
            if (bb != null) {
                if (this.ais.movementType == 2 && startPos.func_177956_o() <= pos.func_177956_o() && state.func_185904_a() == Material.field_151586_h) {
                    pos = pos.func_177977_b();
                    continue;
                }
                return bb.field_72337_e;
            }
            pos = pos.func_177977_b();
        }
        return 0.0;
    }

    private BlockPos calculateTopPos(BlockPos pos) {
        BlockPos check = pos;
        while (check.func_177956_o() > 0) {
            IBlockState state = this.field_70170_p.func_180495_p(pos);
            AxisAlignedBB bb = state.func_185900_c((IBlockAccess)this.field_70170_p, pos).func_186670_a(pos);
            if (bb != null) {
                return check;
            }
            check = check.func_177977_b();
        }
        return pos;
    }

    public boolean isInRange(Entity entity, double range) {
        return this.isInRange(entity.field_70165_t, entity.field_70163_u, entity.field_70161_v, range);
    }

    public boolean isInRange(double posX, double posY, double posZ, double range) {
        double y = Math.abs(this.field_70163_u - posY);
        if (posY >= 0.0 && y > range) {
            return false;
        }
        double x = Math.abs(this.field_70165_t - posX);
        double z = Math.abs(this.field_70161_v - posZ);
        return x <= range && z <= range;
    }

    public void givePlayerItem(EntityPlayer player, ItemStack item) {
        if (this.field_70170_p.field_72995_K) {
            return;
        }
        item = item.func_77946_l();
        float f = 0.7f;
        double d = (double)(this.field_70170_p.field_73012_v.nextFloat() * f) + (double)(1.0f - f);
        double d1 = (double)(this.field_70170_p.field_73012_v.nextFloat() * f) + (double)(1.0f - f);
        double d2 = (double)(this.field_70170_p.field_73012_v.nextFloat() * f) + (double)(1.0f - f);
        EntityItem entityitem = new EntityItem(this.field_70170_p, this.field_70165_t + d, this.field_70163_u + d1, this.field_70161_v + d2, item);
        entityitem.func_174867_a(2);
        this.field_70170_p.func_72838_d((Entity)entityitem);
        int i = item.func_190916_E();
        if (player.field_71071_by.func_70441_a(item)) {
            this.field_70170_p.func_184148_a(null, this.field_70165_t, this.field_70163_u, this.field_70161_v, SoundEvents.field_187638_cR, SoundCategory.PLAYERS, 0.2f, ((this.field_70146_Z.nextFloat() - this.field_70146_Z.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            player.func_71001_a((Entity)entityitem, i);
            if (item.func_190916_E() <= 0) {
                entityitem.func_70106_y();
            }
        }
    }

    public boolean func_70608_bn() {
        return this.currentAnimation == 2 && !this.isAttacking();
    }

    public boolean isWalking() {
        return this.ais.getMovingType() != 0 || this.isAttacking() || this.isFollower() || (Boolean)this.field_70180_af.func_187225_a(Walking) != false;
    }

    public boolean func_70093_af() {
        return this.currentAnimation == 4;
    }

    public void func_70653_a(Entity par1Entity, float strength, double ratioX, double ratioZ) {
        super.func_70653_a(par1Entity, strength * (2.0f - this.stats.resistances.knockback), ratioX, ratioZ);
    }

    public Faction getFaction() {
        Faction fac = FactionController.instance.getFaction((Integer)this.field_70180_af.func_187225_a(FactionData));
        if (fac == null) {
            return FactionController.instance.getFaction(FactionController.instance.getFirstFactionId());
        }
        return fac;
    }

    public boolean isRemote() {
        return this.field_70170_p == null || this.field_70170_p.field_72995_K;
    }

    public void setFaction(int id) {
        if (id < 0 || this.isRemote()) {
            return;
        }
        this.field_70180_af.func_187227_b(FactionData, (Object)id);
    }

    public boolean func_70687_e(PotionEffect effect) {
        if (this.stats.potionImmune) {
            return false;
        }
        if (this.func_70668_bt() == EnumCreatureAttribute.ARTHROPOD && effect.func_188419_a() == MobEffects.field_76436_u) {
            return false;
        }
        return super.func_70687_e(effect);
    }

    public boolean isAttacking() {
        return (Boolean)this.field_70180_af.func_187225_a(Attacking);
    }

    public boolean isKilled() {
        return this.field_70128_L || (Boolean)this.field_70180_af.func_187225_a(IsDead) != false;
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
        compound.func_74768_a("MaxHealth", this.stats.maxHealth);
        compound.func_74782_a("Armor", (NBTBase)NBTTags.nbtIItemStackMap(this.inventory.armor));
        compound.func_74782_a("Weapons", (NBTBase)NBTTags.nbtIItemStackMap(this.inventory.weapons));
        compound.func_74768_a("Speed", this.ais.getWalkingSpeed());
        compound.func_74757_a("DeadBody", this.stats.hideKilledBody);
        compound.func_74768_a("StandingState", this.ais.getStandingType());
        compound.func_74768_a("MovingState", this.ais.getMovingType());
        compound.func_74768_a("Orientation", this.ais.orientation);
        compound.func_74776_a("PositionXOffset", this.ais.bodyOffsetX);
        compound.func_74776_a("PositionYOffset", this.ais.bodyOffsetY);
        compound.func_74776_a("PositionZOffset", this.ais.bodyOffsetZ);
        compound.func_74768_a("Role", this.advanced.role);
        compound.func_74768_a("Job", this.advanced.job);
        if (this.advanced.job == 1) {
            bard = new NBTTagCompound();
            this.jobInterface.writeToNBT(bard);
            compound.func_74782_a("Bard", (NBTBase)bard);
        }
        if (this.advanced.job == 9) {
            bard = new NBTTagCompound();
            this.jobInterface.writeToNBT(bard);
            compound.func_74782_a("Puppet", (NBTBase)bard);
        }
        if (this.advanced.role == 6) {
            bard = new NBTTagCompound();
            this.roleInterface.writeToNBT(bard);
            compound.func_74782_a("Companion", (NBTBase)bard);
        }
        if (this instanceof EntityCustomNpc) {
            compound.func_74782_a("ModelData", (NBTBase)((EntityCustomNpc)this).modelData.writeToNBT());
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
        this.stats.setMaxHealth(compound.func_74762_e("MaxHealth"));
        this.ais.setWalkingSpeed(compound.func_74762_e("Speed"));
        this.stats.hideKilledBody = compound.func_74767_n("DeadBody");
        this.ais.setStandingType(compound.func_74762_e("StandingState"));
        this.ais.setMovingType(compound.func_74762_e("MovingState"));
        this.ais.orientation = compound.func_74762_e("Orientation");
        this.ais.bodyOffsetX = compound.func_74760_g("PositionXOffset");
        this.ais.bodyOffsetY = compound.func_74760_g("PositionYOffset");
        this.ais.bodyOffsetZ = compound.func_74760_g("PositionZOffset");
        this.inventory.armor = NBTTags.getIItemStackMap(compound.func_150295_c("Armor", 10));
        this.inventory.weapons = NBTTags.getIItemStackMap(compound.func_150295_c("Weapons", 10));
        this.advanced.setRole(compound.func_74762_e("Role"));
        this.advanced.setJob(compound.func_74762_e("Job"));
        if (this.advanced.job == 1) {
            NBTTagCompound bard = compound.func_74775_l("Bard");
            this.jobInterface.readFromNBT(bard);
        }
        if (this.advanced.job == 9) {
            puppet = compound.func_74775_l("Puppet");
            this.jobInterface.readFromNBT(puppet);
        }
        if (this.advanced.role == 6) {
            puppet = compound.func_74775_l("Companion");
            this.roleInterface.readFromNBT(puppet);
        }
        if (this instanceof EntityCustomNpc) {
            ((EntityCustomNpc)this).modelData.readFromNBT(compound.func_74775_l("ModelData"));
        }
        this.display.readToNBT(compound);
    }

    public Entity func_174793_f() {
        if (this.field_70170_p.field_72995_K) {
            return this;
        }
        EntityUtil.Copy((EntityLivingBase)this, (EntityLivingBase)CommandPlayer);
        CommandPlayer.func_70029_a(this.field_70170_p);
        CommandPlayer.func_70107_b(this.field_70165_t, this.field_70163_u, this.field_70161_v);
        return CommandPlayer;
    }

    public String func_70005_c_() {
        return this.display.getName();
    }

    public BlockPos func_180425_c() {
        return new BlockPos(this.field_70165_t, this.field_70163_u, this.field_70161_v);
    }

    public Vec3d func_174791_d() {
        return new Vec3d(this.field_70165_t, this.field_70163_u, this.field_70161_v);
    }

    public boolean func_70686_a(Class par1Class) {
        return EntityBat.class != par1Class;
    }

    public void setImmuneToFire(boolean immuneToFire) {
        this.field_70178_ae = immuneToFire;
        this.stats.immuneToFire = immuneToFire;
    }

    public void func_180430_e(float distance, float modifier) {
        if (!this.stats.noFallDamage) {
            super.func_180430_e(distance, modifier);
        }
    }

    public void func_70110_aj() {
        if (!this.stats.ignoreCobweb) {
            super.func_70110_aj();
        }
    }

    public boolean func_70067_L() {
        return !this.isKilled() && this.display.getHasHitbox();
    }

    public boolean func_70104_M() {
        return super.func_70104_M() && this.display.getHasHitbox();
    }

    public EnumPushReaction func_184192_z() {
        return this.display.getHasHitbox() ? super.func_184192_z() : EnumPushReaction.IGNORE;
    }

    public EntityAIRangedAttack getRangedTask() {
        return this.aiRange;
    }

    public String getRoleData() {
        return (String)this.field_70180_af.func_187225_a(RoleData);
    }

    public void setRoleData(String s) {
        this.field_70180_af.func_187227_b(RoleData, (Object)s);
    }

    public String getJobData() {
        return (String)this.field_70180_af.func_187225_a(RoleData);
    }

    public void setJobData(String s) {
        this.field_70180_af.func_187227_b(RoleData, (Object)s);
    }

    public World func_130014_f_() {
        return this.field_70170_p;
    }

    public boolean func_98034_c(EntityPlayer player) {
        return this.display.getVisible() == 1 && (player.func_184614_ca().func_190926_b() || player.func_184614_ca().func_77973_b() != CustomItems.wand);
    }

    public boolean func_82150_aj() {
        return this.display.getVisible() != 0;
    }

    public void func_145747_a(ITextComponent var1) {
    }

    public void setCurrentAnimation(int animation) {
        this.currentAnimation = animation;
        this.field_70180_af.func_187227_b(Animation, (Object)animation);
    }

    public boolean canSee(Entity entity) {
        return this.func_70635_at().func_75522_a(entity);
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

    public void func_175449_a(BlockPos pos, int range) {
        super.func_175449_a(pos, range);
        this.ais.setStartPos(pos);
    }

    protected float func_70655_b(DamageSource source, float damage) {
        if (this.advanced.role == 6) {
            damage = ((RoleCompanion)this.roleInterface).applyArmorCalculations(source, damage);
        }
        return damage;
    }

    public boolean func_184191_r(Entity entity) {
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
        return super.func_184191_r(entity);
    }

    public void setDataWatcher(EntityDataManager dataManager) {
        this.field_70180_af = dataManager;
    }

    public void func_191986_a(float f1, float f2, float f3) {
        double d0 = this.field_70165_t;
        double d1 = this.field_70163_u;
        double d2 = this.field_70161_v;
        super.func_191986_a(f1, f2, f3);
        if (this.advanced.role == 6 && !this.isRemote()) {
            ((RoleCompanion)this.roleInterface).addMovementStat(this.field_70165_t - d0, this.field_70163_u - d1, this.field_70161_v - d2);
        }
    }

    public boolean func_184652_a(EntityPlayer player) {
        return false;
    }

    public boolean func_110167_bD() {
        return false;
    }

    public boolean nearPosition(BlockPos pos) {
        BlockPos npcpos = this.func_180425_c();
        float x = npcpos.func_177958_n() - pos.func_177958_n();
        float z = npcpos.func_177952_p() - pos.func_177952_p();
        float y = npcpos.func_177956_o() - pos.func_177956_o();
        float height = MathHelper.func_76123_f((float)(this.field_70131_O + 1.0f)) * MathHelper.func_76123_f((float)(this.field_70131_O + 1.0f));
        return (double)(x * x + z * z) < 2.5 && (double)(y * y) < (double)height + 2.5;
    }

    public void tpTo(EntityLivingBase owner) {
        if (owner == null) {
            return;
        }
        EnumFacing facing = owner.func_174811_aO().func_176734_d();
        BlockPos pos = new BlockPos(owner.field_70165_t, owner.func_174813_aQ().field_72338_b, owner.field_70161_v);
        pos = pos.func_177982_a(facing.func_82601_c(), 0, facing.func_82599_e());
        pos = this.calculateTopPos(pos);
        block0: for (int i = -1; i < 2; ++i) {
            for (int j = 0; j < 3; ++j) {
                BlockPos check = facing.func_82601_c() == 0 ? pos.func_177982_a(i, 0, j * facing.func_82599_e()) : pos.func_177982_a(j * facing.func_82601_c(), 0, i);
                if (this.field_70170_p.func_180495_p(check = this.calculateTopPos(check)).func_185913_b() || this.field_70170_p.func_180495_p(check.func_177984_a()).func_185913_b()) continue;
                this.func_70012_b((float)check.func_177958_n() + 0.5f, check.func_177956_o(), (float)check.func_177952_p() + 0.5f, this.field_70177_z, this.field_70125_A);
                this.func_70661_as().func_75499_g();
                continue block0;
            }
        }
    }

    public void func_184724_a(boolean swingingArms) {
    }

    public boolean func_70601_bi() {
        return this.func_180484_a(new BlockPos(this.field_70165_t, this.func_174813_aQ().field_72338_b, this.field_70161_v)) >= 0.0f && this.field_70170_p.func_180495_p(new BlockPos((Entity)this).func_177977_b()).func_189884_a((Entity)this);
    }

    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }
}

