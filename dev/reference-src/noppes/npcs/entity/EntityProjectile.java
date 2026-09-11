/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.monster.EntityEnderman
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.projectile.EntityThrowable
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.network.datasync.DataParameter
 *  net.minecraft.network.datasync.DataSerializer
 *  net.minecraft.network.datasync.DataSerializers
 *  net.minecraft.network.datasync.EntityDataManager
 *  net.minecraft.potion.Potion
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.EnumParticleTypes
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.RayTraceResult
 *  net.minecraft.util.math.RayTraceResult$Type
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.ParticleType;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.event.ProjectileEvent;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;

public class EntityProjectile
extends EntityThrowable {
    private static final DataParameter<Boolean> Gravity = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> Arrow = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> Is3d = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> Glows = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> Rotating = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> Sticks = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.BOOLEAN);
    private static final DataParameter<ItemStack> ItemStackThrown = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.ITEM_STACK);
    private static final DataParameter<Integer> Velocity = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.VARINT);
    private static final DataParameter<Integer> Size = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.VARINT);
    private static final DataParameter<Integer> Particle = EntityDataManager.createKey(EntityProjectile.class, (DataSerializer)DataSerializers.VARINT);
    private BlockPos tilePos = BlockPos.ORIGIN;
    private Block inTile;
    protected boolean inGround = false;
    private int inData = 0;
    public int throwableShake = 0;
    public int arrowShake = 0;
    public boolean canBePickedUp = false;
    public boolean destroyedOnEntityHit = true;
    private EntityLivingBase thrower;
    private EntityNPCInterface npc;
    private String throwerName = null;
    private int ticksInGround;
    public int ticksInAir = 0;
    private double accelerationX;
    private double accelerationY;
    private double accelerationZ;
    public float damage = 5.0f;
    public int punch = 0;
    public boolean accelerate = false;
    public boolean explosiveDamage = true;
    public int explosiveRadius = 0;
    public int effect = 0;
    public int duration = 5;
    public int amplify = 0;
    public int accuracy = 60;
    public IProjectileCallback callback;
    public List<ScriptContainer> scripts = new ArrayList<ScriptContainer>();

    public EntityProjectile(World par1World) {
        super(par1World);
        this.setSize(0.25f, 0.25f);
    }

    protected void entityInit() {
        this.dataManager.register(ItemStackThrown, (Object)ItemStack.EMPTY);
        this.dataManager.register(Velocity, (Object)10);
        this.dataManager.register(Size, (Object)10);
        this.dataManager.register(Particle, (Object)0);
        this.dataManager.register(Gravity, (Object)false);
        this.dataManager.register(Glows, (Object)false);
        this.dataManager.register(Arrow, (Object)false);
        this.dataManager.register(Is3d, (Object)false);
        this.dataManager.register(Rotating, (Object)false);
        this.dataManager.register(Sticks, (Object)false);
    }

    @SideOnly(value=Side.CLIENT)
    public boolean isInRangeToRenderDist(double par1) {
        double d1 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0;
        return par1 < (d1 *= 64.0) * d1;
    }

    public EntityProjectile(World par1World, EntityLivingBase par2EntityLiving, ItemStack item, boolean isNPC) {
        super(par1World);
        this.thrower = par2EntityLiving;
        if (this.thrower != null) {
            this.throwerName = this.thrower.getUniqueID().toString();
        }
        this.setThrownItem(item);
        this.dataManager.set(Arrow, (Object)(this.getItem() == Items.ARROW ? 1 : 0));
        this.setSize((float)this.getSize() / 10.0f, (float)this.getSize() / 10.0f);
        this.setLocationAndAngles(par2EntityLiving.posX, par2EntityLiving.posY + (double)par2EntityLiving.getEyeHeight(), par2EntityLiving.posZ, par2EntityLiving.rotationYaw, par2EntityLiving.rotationPitch);
        this.posX -= (double)(MathHelper.cos((float)(this.rotationYaw / 180.0f * (float)Math.PI)) * 0.1f);
        this.posY -= (double)0.1f;
        this.posZ -= (double)(MathHelper.sin((float)(this.rotationYaw / 180.0f * (float)Math.PI)) * 0.1f);
        this.setPosition(this.posX, this.posY, this.posZ);
        if (isNPC) {
            this.npc = (EntityNPCInterface)this.thrower;
            this.getStatProperties(this.npc.stats.ranged);
        }
    }

    public void setThrownItem(ItemStack item) {
        this.dataManager.set(ItemStackThrown, (Object)item);
    }

    public int getSize() {
        return (Integer)this.dataManager.get(Size);
    }

    public void shoot(double par1, double par3, double par5, float par7, float par8) {
        float f2 = MathHelper.sqrt((double)(par1 * par1 + par3 * par3 + par5 * par5));
        float f3 = MathHelper.sqrt((double)(par1 * par1 + par5 * par5));
        float yaw = (float)(Math.atan2(par1, par5) * 180.0 / Math.PI);
        float pitch = this.hasGravity() ? par7 : (float)(Math.atan2(par3, f3) * 180.0 / Math.PI);
        this.prevRotationYaw = this.rotationYaw = yaw;
        this.prevRotationPitch = this.rotationPitch = pitch;
        this.motionX = MathHelper.sin((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(pitch / 180.0f * (float)Math.PI));
        this.motionZ = MathHelper.cos((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(pitch / 180.0f * (float)Math.PI));
        this.motionY = MathHelper.sin((float)((pitch + 1.0f) / 180.0f * (float)Math.PI));
        this.motionX += this.rand.nextGaussian() * (double)0.0075f * (double)par8;
        this.motionZ += this.rand.nextGaussian() * (double)0.0075f * (double)par8;
        this.motionY += this.rand.nextGaussian() * (double)0.0075f * (double)par8;
        this.motionX *= (double)this.getSpeed();
        this.motionZ *= (double)this.getSpeed();
        this.motionY *= (double)this.getSpeed();
        this.accelerationX = par1 / (double)f2 * 0.1;
        this.accelerationY = par3 / (double)f2 * 0.1;
        this.accelerationZ = par5 / (double)f2 * 0.1;
        this.ticksInGround = 0;
    }

    public float getAngleForXYZ(double varX, double varY, double varZ, double horiDist, boolean arc) {
        float g = this.getGravityVelocity();
        float var1 = this.getSpeed() * this.getSpeed();
        double var2 = (double)g * horiDist;
        double var3 = (double)g * horiDist * horiDist + 2.0 * varY * (double)var1;
        double var4 = (double)(var1 * var1) - (double)g * var3;
        if (var4 < 0.0) {
            return 30.0f;
        }
        float var6 = arc ? var1 + MathHelper.sqrt((double)var4) : var1 - MathHelper.sqrt((double)var4);
        float var7 = (float)(Math.atan2(var6, var2) * 180.0 / Math.PI);
        return var7;
    }

    public void shoot(float speed) {
        double varX = -MathHelper.sin((float)(this.rotationYaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(this.rotationPitch / 180.0f * (float)Math.PI));
        double varZ = MathHelper.cos((float)(this.rotationYaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(this.rotationPitch / 180.0f * (float)Math.PI));
        double varY = -MathHelper.sin((float)(this.rotationPitch / 180.0f * (float)Math.PI));
        this.shoot(varX, varY, varZ, -this.rotationPitch, speed);
    }

    @SideOnly(value=Side.CLIENT)
    public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9, boolean bo) {
        if (this.world.isRemote && this.inGround) {
            return;
        }
        this.setPosition(par1, par3, par5);
        this.setRotation(par7, par8);
    }

    public void onUpdate() {
        AxisAlignedBB axisalignedbb;
        super.onEntityUpdate();
        if (++this.ticksExisted % 10 == 0) {
            EventHooks.onProjectileTick(this);
        }
        if (this.effect == 1 && !this.inGround) {
            this.setFire(1);
        }
        IBlockState state = this.world.getBlockState(this.tilePos);
        Block block = state.getBlock();
        if ((this.isArrow() || this.sticksToWalls()) && this.tilePos != BlockPos.ORIGIN && (axisalignedbb = state.getCollisionBoundingBox((IBlockAccess)this.world, this.tilePos)) != null && axisalignedbb.contains(new Vec3d(this.posX, this.posY, this.posZ))) {
            this.inGround = true;
        }
        if (this.arrowShake > 0) {
            --this.arrowShake;
        }
        if (this.inGround) {
            int j = block.getMetaFromState(state);
            if (block == this.inTile && j == this.inData) {
                ++this.ticksInGround;
                if (this.ticksInGround == 1200) {
                    this.setDead();
                }
            } else {
                this.inGround = false;
                this.motionX *= (double)(this.rand.nextFloat() * 0.2f);
                this.motionY *= (double)(this.rand.nextFloat() * 0.2f);
                this.motionZ *= (double)(this.rand.nextFloat() * 0.2f);
                this.ticksInGround = 0;
                this.ticksInAir = 0;
            }
        } else {
            ++this.ticksInAir;
            if (this.ticksInAir == 1200) {
                this.setDead();
            }
            Vec3d vec3 = new Vec3d(this.posX, this.posY, this.posZ);
            Vec3d vec31 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            RayTraceResult movingobjectposition = this.world.rayTraceBlocks(vec3, vec31, false, true, false);
            vec3 = new Vec3d(this.posX, this.posY, this.posZ);
            vec31 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            if (movingobjectposition != null) {
                vec31 = new Vec3d(movingobjectposition.hitVec.x, movingobjectposition.hitVec.y, movingobjectposition.hitVec.z);
            }
            if (!this.world.isRemote) {
                Entity entity = null;
                List list = this.world.getEntitiesWithinAABBExcludingEntity((Entity)this, this.getEntityBoundingBox().grow(this.motionX, this.motionY, this.motionZ).grow(1.0, 1.0, 1.0));
                double d0 = 0.0;
                EntityLivingBase entityliving = this.getThrower();
                for (int k = 0; k < list.size(); ++k) {
                    double d1;
                    Entity entity1 = (Entity)list.get(k);
                    if (!entity1.canBeCollidedWith() || entity1.isEntityEqual((Entity)this.thrower) && this.ticksInAir < 25) continue;
                    float f = 0.3f;
                    AxisAlignedBB axisalignedbb2 = entity1.getEntityBoundingBox().grow((double)f, (double)f, (double)f);
                    RayTraceResult movingobjectposition1 = axisalignedbb2.calculateIntercept(vec3, vec31);
                    if (movingobjectposition1 == null || !((d1 = vec3.distanceTo(movingobjectposition1.hitVec)) < d0) && d0 != 0.0) continue;
                    entity = entity1;
                    d0 = d1;
                }
                if (entity != null) {
                    movingobjectposition = new RayTraceResult(entity);
                }
                if (movingobjectposition != null && movingobjectposition.entityHit != null) {
                    if (this.npc != null && movingobjectposition.entityHit instanceof EntityLivingBase && this.npc.isOnSameTeam((Entity)((EntityLivingBase)movingobjectposition.entityHit))) {
                        movingobjectposition = null;
                    } else if (movingobjectposition.entityHit instanceof EntityPlayer) {
                        EntityPlayer entityplayer = (EntityPlayer)movingobjectposition.entityHit;
                        if (entityplayer.capabilities.disableDamage || this.thrower instanceof EntityPlayer && !((EntityPlayer)this.thrower).canAttackPlayer(entityplayer)) {
                            movingobjectposition = null;
                        }
                    }
                }
            }
            if (movingobjectposition != null) {
                if (movingobjectposition.typeOfHit == RayTraceResult.Type.BLOCK && this.world.getBlockState(movingobjectposition.getBlockPos()).getBlock() == Blocks.PORTAL) {
                    this.setPortal(movingobjectposition.getBlockPos());
                } else {
                    this.dataManager.set(Rotating, (Object)false);
                    this.onImpact(movingobjectposition);
                }
            }
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;
            float f1 = MathHelper.sqrt((double)(this.motionX * this.motionX + this.motionZ * this.motionZ));
            this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0 / Math.PI);
            this.rotationPitch = (float)(Math.atan2(this.motionY, f1) * 180.0 / Math.PI);
            while (this.rotationPitch - this.prevRotationPitch < -180.0f) {
                this.prevRotationPitch -= 360.0f;
            }
            while (this.rotationPitch - this.prevRotationPitch >= 180.0f) {
                this.prevRotationPitch += 360.0f;
            }
            while (this.rotationYaw - this.prevRotationYaw < -180.0f) {
                this.prevRotationYaw -= 360.0f;
            }
            while (this.rotationYaw - this.prevRotationYaw >= 180.0f) {
                this.prevRotationYaw += 360.0f;
            }
            this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch);
            this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw);
            if (this.isRotating()) {
                int spin = this.isBlock() ? 10 : 20;
                this.rotationPitch -= (float)(this.ticksInAir % 15 * spin) * this.getSpeed();
            }
            float f2 = this.getMotionFactor();
            float f3 = this.getGravityVelocity();
            if (this.isInWater()) {
                if (this.world.isRemote) {
                    for (int k = 0; k < 4; ++k) {
                        float f4 = 0.25f;
                        this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * (double)f4, this.posY - this.motionY * (double)f4, this.posZ - this.motionZ * (double)f4, this.motionX, this.motionY, this.motionZ, new int[0]);
                    }
                }
                f2 = 0.8f;
            }
            this.motionX *= (double)f2;
            this.motionY *= (double)f2;
            this.motionZ *= (double)f2;
            if (this.hasGravity()) {
                this.motionY -= (double)f3;
            }
            if (this.accelerate) {
                this.motionX += this.accelerationX;
                this.motionY += this.accelerationY;
                this.motionZ += this.accelerationZ;
            }
            if (this.world.isRemote && (Integer)this.dataManager.get(Particle) > 0) {
                this.world.spawnParticle(ParticleType.getMCType((Integer)this.dataManager.get(Particle)), this.posX, this.posY, this.posZ, 0.0, 0.0, 0.0, new int[0]);
            }
            this.setPosition(this.posX, this.posY, this.posZ);
            this.doBlockCollisions();
        }
    }

    public boolean isBlock() {
        ItemStack item = this.getItemDisplay();
        if (item.isEmpty()) {
            return false;
        }
        return item.getItem() instanceof ItemBlock;
    }

    private Item getItem() {
        ItemStack item = this.getItemDisplay();
        if (item.isEmpty()) {
            return Items.AIR;
        }
        return item.getItem();
    }

    protected float getMotionFactor() {
        return this.accelerate ? 0.95f : 1.0f;
    }

    protected void onImpact(RayTraceResult movingobjectposition) {
        block33: {
            block31: {
                block32: {
                    float f3;
                    if (!this.world.isRemote) {
                        ProjectileEvent.ImpactEvent event;
                        BlockPos pos = null;
                        if (movingobjectposition.entityHit != null) {
                            pos = movingobjectposition.entityHit.getPosition();
                            event = new ProjectileEvent.ImpactEvent((IProjectile)NpcAPI.Instance().getIEntity((Entity)this), 0, movingobjectposition.entityHit);
                        } else {
                            pos = movingobjectposition.getBlockPos();
                            event = new ProjectileEvent.ImpactEvent((IProjectile)NpcAPI.Instance().getIEntity((Entity)this), 1, NpcAPI.Instance().getIBlock(this.world, pos));
                        }
                        if (pos == BlockPos.ORIGIN) {
                            pos = new BlockPos(movingobjectposition.hitVec);
                        }
                        if (this.callback != null && this.callback.onImpact(this, pos, movingobjectposition.entityHit)) {
                            return;
                        }
                        EventHooks.onProjectileImpact(this, event);
                    }
                    if (movingobjectposition.entityHit == null) break block31;
                    float damage = this.damage;
                    if (damage == 0.0f) {
                        damage = 0.001f;
                    }
                    if (!movingobjectposition.entityHit.attackEntityFrom(DamageSource.causeThrownDamage((Entity)this, (Entity)this.getThrower()), damage)) break block32;
                    if (movingobjectposition.entityHit instanceof EntityLivingBase && (this.isArrow() || this.sticksToWalls())) {
                        EntityLivingBase entityliving = (EntityLivingBase)movingobjectposition.entityHit;
                        if (!this.world.isRemote) {
                            entityliving.setArrowCountInEntity(entityliving.getArrowCountInEntity() + 1);
                        }
                        if (this.destroyedOnEntityHit && !(movingobjectposition.entityHit instanceof EntityEnderman)) {
                            this.setDead();
                        }
                    }
                    if (this.isBlock()) {
                        this.world.playEvent((EntityPlayer)null, 2001, movingobjectposition.entityHit.getPosition(), Item.getIdFromItem((Item)this.getItem()));
                    } else if (!this.isArrow() && !this.sticksToWalls()) {
                        int[] intArr = new int[]{Item.getIdFromItem((Item)this.getItem())};
                        if (this.getItem().getHasSubtypes()) {
                            intArr = new int[]{Item.getIdFromItem((Item)this.getItem()), this.getItemDisplay().getMetadata()};
                        }
                        for (int i = 0; i < 8; ++i) {
                            this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, this.posX, this.posY, this.posZ, this.rand.nextGaussian() * 0.15, this.rand.nextGaussian() * 0.2, this.rand.nextGaussian() * 0.15, intArr);
                        }
                    }
                    if (this.punch > 0 && (f3 = MathHelper.sqrt((double)(this.motionX * this.motionX + this.motionZ * this.motionZ))) > 0.0f) {
                        movingobjectposition.entityHit.addVelocity(this.motionX * (double)this.punch * (double)0.6f / (double)f3, 0.1, this.motionZ * (double)this.punch * (double)0.6f / (double)f3);
                    }
                    if (this.effect != 0 && movingobjectposition.entityHit instanceof EntityLivingBase) {
                        if (this.effect != 1) {
                            Potion p = PotionEffectType.getMCType(this.effect);
                            ((EntityLivingBase)movingobjectposition.entityHit).addPotionEffect(new PotionEffect(p, this.duration * 20, this.amplify));
                        } else {
                            movingobjectposition.entityHit.setFire(this.duration);
                        }
                    }
                    break block33;
                }
                if (!this.hasGravity() || !this.isArrow() && !this.sticksToWalls()) break block33;
                this.motionX *= (double)-0.1f;
                this.motionY *= (double)-0.1f;
                this.motionZ *= (double)-0.1f;
                this.rotationYaw += 180.0f;
                this.prevRotationYaw += 180.0f;
                this.ticksInAir = 0;
                break block33;
            }
            if (this.isArrow() || this.sticksToWalls()) {
                this.tilePos = movingobjectposition.getBlockPos();
                IBlockState state = this.world.getBlockState(this.tilePos);
                this.inTile = state.getBlock();
                this.inData = this.inTile.getMetaFromState(state);
                this.motionX = (float)(movingobjectposition.hitVec.x - this.posX);
                this.motionY = (float)(movingobjectposition.hitVec.y - this.posY);
                this.motionZ = (float)(movingobjectposition.hitVec.z - this.posZ);
                float f2 = MathHelper.sqrt((double)(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ));
                this.posX -= this.motionX / (double)f2 * (double)0.05f;
                this.posY -= this.motionY / (double)f2 * (double)0.05f;
                this.posZ -= this.motionZ / (double)f2 * (double)0.05f;
                this.inGround = true;
                this.arrowShake = 7;
                if (!this.hasGravity()) {
                    this.dataManager.set(Gravity, (Object)true);
                }
                if (this.inTile != null) {
                    this.inTile.onEntityCollidedWithBlock(this.world, this.tilePos, state, (Entity)this);
                }
            } else if (this.isBlock()) {
                this.world.playEvent((EntityPlayer)null, 2001, this.getPosition(), Item.getIdFromItem((Item)this.getItem()));
            } else {
                int[] intArr = new int[]{Item.getIdFromItem((Item)this.getItem())};
                if (this.getItem().getHasSubtypes()) {
                    intArr = new int[]{Item.getIdFromItem((Item)this.getItem()), this.getItemDisplay().getMetadata()};
                }
                for (int i = 0; i < 8; ++i) {
                    this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, this.posX, this.posY, this.posZ, this.rand.nextGaussian() * 0.15, this.rand.nextGaussian() * 0.2, this.rand.nextGaussian() * 0.15, intArr);
                }
            }
        }
        if (this.explosiveRadius > 0) {
            boolean terraindamage = this.world.getGameRules().getBoolean("mobGriefing") && this.explosiveDamage;
            this.world.newExplosion((Entity)(this.getThrower() == null ? this : this.getThrower()), this.posX, this.posY, this.posZ, (float)this.explosiveRadius, this.effect == 1, terraindamage);
            if (this.effect != 0) {
                AxisAlignedBB axisalignedbb = this.getEntityBoundingBox().grow((double)(this.explosiveRadius * 2), (double)(this.explosiveRadius * 2), (double)(this.explosiveRadius * 2));
                List list1 = this.world.getEntitiesWithinAABB(EntityLivingBase.class, axisalignedbb);
                for (EntityLivingBase entity : list1) {
                    if (this.effect != 1) {
                        Potion p = PotionEffectType.getMCType(this.effect);
                        if (p == null) continue;
                        entity.addPotionEffect(new PotionEffect(p, this.duration * 20, this.amplify));
                        continue;
                    }
                    entity.setFire(this.duration);
                }
                this.world.playEvent((EntityPlayer)null, 2002, this.getPosition(), this.getPotionColor(this.effect));
            }
            this.setDead();
        }
        if (!(this.world.isRemote || this.isArrow() || this.sticksToWalls())) {
            this.setDead();
        }
    }

    private void blockParticles() {
    }

    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setShort("xTile", (short)this.tilePos.getX());
        par1NBTTagCompound.setShort("yTile", (short)this.tilePos.getY());
        par1NBTTagCompound.setShort("zTile", (short)this.tilePos.getZ());
        par1NBTTagCompound.setByte("inTile", (byte)Block.getIdFromBlock((Block)this.inTile));
        par1NBTTagCompound.setByte("inData", (byte)this.inData);
        par1NBTTagCompound.setByte("shake", (byte)this.throwableShake);
        par1NBTTagCompound.setBoolean("inGround", this.inGround);
        par1NBTTagCompound.setBoolean("isArrow", this.isArrow());
        par1NBTTagCompound.setTag("direction", (NBTBase)this.newDoubleNBTList(new double[]{this.motionX, this.motionY, this.motionZ}));
        par1NBTTagCompound.setBoolean("canBePickedUp", this.canBePickedUp);
        if ((this.throwerName == null || this.throwerName.length() == 0) && this.thrower != null && this.thrower instanceof EntityPlayer) {
            this.throwerName = this.thrower.getUniqueID().toString();
        }
        par1NBTTagCompound.setString("ownerName", this.throwerName == null ? "" : this.throwerName);
        par1NBTTagCompound.setTag("Item", (NBTBase)this.getItemDisplay().writeToNBT(new NBTTagCompound()));
        par1NBTTagCompound.setFloat("damagev2", this.damage);
        par1NBTTagCompound.setInteger("punch", this.punch);
        par1NBTTagCompound.setInteger("size", ((Integer)this.dataManager.get(Size)).intValue());
        par1NBTTagCompound.setInteger("velocity", ((Integer)this.dataManager.get(Velocity)).intValue());
        par1NBTTagCompound.setInteger("explosiveRadius", this.explosiveRadius);
        par1NBTTagCompound.setInteger("effectDuration", this.duration);
        par1NBTTagCompound.setBoolean("gravity", this.hasGravity());
        par1NBTTagCompound.setBoolean("accelerate", this.accelerate);
        par1NBTTagCompound.setBoolean("glows", ((Boolean)this.dataManager.get(Glows)).booleanValue());
        par1NBTTagCompound.setInteger("PotionEffect", this.effect);
        par1NBTTagCompound.setInteger("trailenum", ((Integer)this.dataManager.get(Particle)).intValue());
        par1NBTTagCompound.setBoolean("Render3D", ((Boolean)this.dataManager.get(Is3d)).booleanValue());
        par1NBTTagCompound.setBoolean("Spins", ((Boolean)this.dataManager.get(Rotating)).booleanValue());
        par1NBTTagCompound.setBoolean("Sticks", ((Boolean)this.dataManager.get(Sticks)).booleanValue());
        par1NBTTagCompound.setInteger("accuracy", this.accuracy);
    }

    public void readEntityFromNBT(NBTTagCompound compound) {
        NBTTagCompound var2;
        ItemStack item;
        this.tilePos = new BlockPos((int)compound.getShort("xTile"), (int)compound.getShort("yTile"), (int)compound.getShort("zTile"));
        this.inTile = Block.getBlockById((int)(compound.getByte("inTile") & 0xFF));
        this.inData = compound.getByte("inData") & 0xFF;
        this.throwableShake = compound.getByte("shake") & 0xFF;
        this.inGround = compound.getByte("inGround") == 1;
        this.dataManager.set(Arrow, (Object)compound.getBoolean("isArrow"));
        this.throwerName = compound.getString("ownerName");
        this.canBePickedUp = compound.getBoolean("canBePickedUp");
        this.damage = compound.getFloat("damagev2");
        this.punch = compound.getInteger("punch");
        this.explosiveRadius = compound.getInteger("explosiveRadius");
        this.duration = compound.getInteger("effectDuration");
        this.accelerate = compound.getBoolean("accelerate");
        this.effect = compound.getInteger("PotionEffect");
        this.accuracy = compound.getInteger("accuracy");
        this.dataManager.set(Particle, (Object)compound.getInteger("trailenum"));
        this.dataManager.set(Size, (Object)compound.getInteger("size"));
        this.dataManager.set(Glows, (Object)compound.getBoolean("glows"));
        this.dataManager.set(Velocity, (Object)compound.getInteger("velocity"));
        this.dataManager.set(Gravity, (Object)compound.getBoolean("gravity"));
        this.dataManager.set(Is3d, (Object)compound.getBoolean("Render3D"));
        this.dataManager.set(Rotating, (Object)compound.getBoolean("Spins"));
        this.dataManager.set(Sticks, (Object)compound.getBoolean("Sticks"));
        if (this.throwerName != null && this.throwerName.length() == 0) {
            this.throwerName = null;
        }
        if (compound.hasKey("direction")) {
            NBTTagList nbttaglist = compound.getTagList("direction", 6);
            this.motionX = nbttaglist.getDoubleAt(0);
            this.motionY = nbttaglist.getDoubleAt(1);
            this.motionZ = nbttaglist.getDoubleAt(2);
        }
        if ((item = new ItemStack(var2 = compound.getCompoundTag("Item"))).isEmpty()) {
            this.setDead();
        } else {
            this.dataManager.set(ItemStackThrown, (Object)item);
        }
    }

    public EntityLivingBase getThrower() {
        if (this.throwerName == null || this.throwerName.isEmpty()) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(this.throwerName);
            if (this.thrower == null && uuid != null) {
                this.thrower = this.world.getPlayerEntityByUUID(uuid);
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
        return this.thrower;
    }

    private int getPotionColor(int p) {
        switch (p) {
            case 2: {
                return 32660;
            }
            case 3: {
                return 32660;
            }
            case 4: {
                return 32696;
            }
            case 5: {
                return 32698;
            }
            case 6: {
                return 32732;
            }
            case 7: {
                return 15;
            }
            case 8: {
                return 32732;
            }
        }
        return 0;
    }

    public void getStatProperties(DataRanged stats) {
        this.damage = stats.getStrength();
        this.punch = stats.getKnockback();
        this.accelerate = stats.getAccelerate();
        this.explosiveRadius = stats.getExplodeSize();
        this.effect = stats.getEffectType();
        this.duration = stats.getEffectTime();
        this.amplify = stats.getEffectStrength();
        this.setParticleEffect(stats.getParticle());
        this.dataManager.set(Size, (Object)stats.getSize());
        this.dataManager.set(Glows, (Object)stats.getGlows());
        this.setSpeed(stats.getSpeed());
        this.setHasGravity(stats.getHasGravity());
        this.setIs3D(stats.getRender3D());
        this.setRotating(stats.getSpins());
        this.setStickInWall(stats.getSticks());
    }

    public void setParticleEffect(int type) {
        this.dataManager.set(Particle, (Object)type);
    }

    public void setHasGravity(boolean bo) {
        this.dataManager.set(Gravity, (Object)bo);
    }

    public void setIs3D(boolean bo) {
        this.dataManager.set(Is3d, (Object)bo);
    }

    public void setStickInWall(boolean bo) {
        this.dataManager.set(Sticks, (Object)bo);
    }

    public ItemStack getItemDisplay() {
        return (ItemStack)this.dataManager.get(ItemStackThrown);
    }

    public float getBrightness() {
        return (Boolean)this.dataManager.get(Glows) != false ? 1.0f : super.getBrightness();
    }

    @SideOnly(value=Side.CLIENT)
    public int getBrightnessForRender() {
        return (Boolean)this.dataManager.get(Glows) != false ? 0xF000F0 : super.getBrightnessForRender();
    }

    public boolean hasGravity() {
        return (Boolean)this.dataManager.get(Gravity);
    }

    public void setSpeed(int speed) {
        this.dataManager.set(Velocity, (Object)speed);
    }

    public float getSpeed() {
        return (float)((Integer)this.dataManager.get(Velocity)).intValue() / 10.0f;
    }

    public boolean isArrow() {
        return (Boolean)this.dataManager.get(Arrow);
    }

    public void setRotating(boolean bo) {
        this.dataManager.set(Rotating, (Object)bo);
    }

    public boolean isRotating() {
        return (Boolean)this.dataManager.get(Rotating);
    }

    public boolean glows() {
        return (Boolean)this.dataManager.get(Glows);
    }

    public boolean is3D() {
        return (Boolean)this.dataManager.get(Is3d) != false || this.isBlock();
    }

    public boolean sticksToWalls() {
        return this.is3D() && (Boolean)this.dataManager.get(Sticks) != false;
    }

    public void onCollideWithPlayer(EntityPlayer par1EntityPlayer) {
        if (this.world.isRemote || !this.canBePickedUp || !this.inGround || this.arrowShake > 0) {
            return;
        }
        if (par1EntityPlayer.inventory.addItemStackToInventory(this.getItemDisplay())) {
            this.inGround = false;
            this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.2f, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            par1EntityPlayer.onItemPickup((Entity)this, 1);
            this.setDead();
        }
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    public ITextComponent getDisplayName() {
        if (!this.getItemDisplay().isEmpty()) {
            return new TextComponentTranslation(this.getItemDisplay().getDisplayName(), new Object[0]);
        }
        return super.getDisplayName();
    }

    public static interface IProjectileCallback {
        public boolean onImpact(EntityProjectile var1, BlockPos var2, Entity var3);
    }
}

