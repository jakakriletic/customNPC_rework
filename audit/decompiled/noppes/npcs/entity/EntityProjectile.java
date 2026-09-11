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
    private static final DataParameter<Boolean> Gravity = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<Boolean> Arrow = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<Boolean> Is3d = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<Boolean> Glows = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<Boolean> Rotating = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<Boolean> Sticks = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187198_h);
    private static final DataParameter<ItemStack> ItemStackThrown = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187196_f);
    private static final DataParameter<Integer> Velocity = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187192_b);
    private static final DataParameter<Integer> Size = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187192_b);
    private static final DataParameter<Integer> Particle = EntityDataManager.func_187226_a(EntityProjectile.class, (DataSerializer)DataSerializers.field_187192_b);
    private BlockPos tilePos = BlockPos.field_177992_a;
    private Block inTile;
    protected boolean field_174854_a = false;
    private int inData = 0;
    public int field_70191_b = 0;
    public int arrowShake = 0;
    public boolean canBePickedUp = false;
    public boolean destroyedOnEntityHit = true;
    private EntityLivingBase thrower;
    private EntityNPCInterface npc;
    private String throwerName = null;
    private int ticksInGround;
    public int field_70195_i = 0;
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
        this.func_70105_a(0.25f, 0.25f);
    }

    protected void func_70088_a() {
        this.field_70180_af.func_187214_a(ItemStackThrown, (Object)ItemStack.field_190927_a);
        this.field_70180_af.func_187214_a(Velocity, (Object)10);
        this.field_70180_af.func_187214_a(Size, (Object)10);
        this.field_70180_af.func_187214_a(Particle, (Object)0);
        this.field_70180_af.func_187214_a(Gravity, (Object)false);
        this.field_70180_af.func_187214_a(Glows, (Object)false);
        this.field_70180_af.func_187214_a(Arrow, (Object)false);
        this.field_70180_af.func_187214_a(Is3d, (Object)false);
        this.field_70180_af.func_187214_a(Rotating, (Object)false);
        this.field_70180_af.func_187214_a(Sticks, (Object)false);
    }

    @SideOnly(value=Side.CLIENT)
    public boolean func_70112_a(double par1) {
        double d1 = this.func_174813_aQ().func_72320_b() * 4.0;
        return par1 < (d1 *= 64.0) * d1;
    }

    public EntityProjectile(World par1World, EntityLivingBase par2EntityLiving, ItemStack item, boolean isNPC) {
        super(par1World);
        this.thrower = par2EntityLiving;
        if (this.thrower != null) {
            this.throwerName = this.thrower.func_110124_au().toString();
        }
        this.setThrownItem(item);
        this.field_70180_af.func_187227_b(Arrow, (Object)(this.getItem() == Items.field_151032_g ? 1 : 0));
        this.func_70105_a((float)this.getSize() / 10.0f, (float)this.getSize() / 10.0f);
        this.func_70012_b(par2EntityLiving.field_70165_t, par2EntityLiving.field_70163_u + (double)par2EntityLiving.func_70047_e(), par2EntityLiving.field_70161_v, par2EntityLiving.field_70177_z, par2EntityLiving.field_70125_A);
        this.field_70165_t -= (double)(MathHelper.func_76134_b((float)(this.field_70177_z / 180.0f * (float)Math.PI)) * 0.1f);
        this.field_70163_u -= (double)0.1f;
        this.field_70161_v -= (double)(MathHelper.func_76126_a((float)(this.field_70177_z / 180.0f * (float)Math.PI)) * 0.1f);
        this.func_70107_b(this.field_70165_t, this.field_70163_u, this.field_70161_v);
        if (isNPC) {
            this.npc = (EntityNPCInterface)this.thrower;
            this.getStatProperties(this.npc.stats.ranged);
        }
    }

    public void setThrownItem(ItemStack item) {
        this.field_70180_af.func_187227_b(ItemStackThrown, (Object)item);
    }

    public int getSize() {
        return (Integer)this.field_70180_af.func_187225_a(Size);
    }

    public void func_70186_c(double par1, double par3, double par5, float par7, float par8) {
        float f2 = MathHelper.func_76133_a((double)(par1 * par1 + par3 * par3 + par5 * par5));
        float f3 = MathHelper.func_76133_a((double)(par1 * par1 + par5 * par5));
        float yaw = (float)(Math.atan2(par1, par5) * 180.0 / Math.PI);
        float pitch = this.hasGravity() ? par7 : (float)(Math.atan2(par3, f3) * 180.0 / Math.PI);
        this.field_70126_B = this.field_70177_z = yaw;
        this.field_70127_C = this.field_70125_A = pitch;
        this.field_70159_w = MathHelper.func_76126_a((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(pitch / 180.0f * (float)Math.PI));
        this.field_70179_y = MathHelper.func_76134_b((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(pitch / 180.0f * (float)Math.PI));
        this.field_70181_x = MathHelper.func_76126_a((float)((pitch + 1.0f) / 180.0f * (float)Math.PI));
        this.field_70159_w += this.field_70146_Z.nextGaussian() * (double)0.0075f * (double)par8;
        this.field_70179_y += this.field_70146_Z.nextGaussian() * (double)0.0075f * (double)par8;
        this.field_70181_x += this.field_70146_Z.nextGaussian() * (double)0.0075f * (double)par8;
        this.field_70159_w *= (double)this.getSpeed();
        this.field_70179_y *= (double)this.getSpeed();
        this.field_70181_x *= (double)this.getSpeed();
        this.accelerationX = par1 / (double)f2 * 0.1;
        this.accelerationY = par3 / (double)f2 * 0.1;
        this.accelerationZ = par5 / (double)f2 * 0.1;
        this.ticksInGround = 0;
    }

    public float getAngleForXYZ(double varX, double varY, double varZ, double horiDist, boolean arc) {
        float g = this.func_70185_h();
        float var1 = this.getSpeed() * this.getSpeed();
        double var2 = (double)g * horiDist;
        double var3 = (double)g * horiDist * horiDist + 2.0 * varY * (double)var1;
        double var4 = (double)(var1 * var1) - (double)g * var3;
        if (var4 < 0.0) {
            return 30.0f;
        }
        float var6 = arc ? var1 + MathHelper.func_76133_a((double)var4) : var1 - MathHelper.func_76133_a((double)var4);
        float var7 = (float)(Math.atan2(var6, var2) * 180.0 / Math.PI);
        return var7;
    }

    public void shoot(float speed) {
        double varX = -MathHelper.func_76126_a((float)(this.field_70177_z / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(this.field_70125_A / 180.0f * (float)Math.PI));
        double varZ = MathHelper.func_76134_b((float)(this.field_70177_z / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(this.field_70125_A / 180.0f * (float)Math.PI));
        double varY = -MathHelper.func_76126_a((float)(this.field_70125_A / 180.0f * (float)Math.PI));
        this.func_70186_c(varX, varY, varZ, -this.field_70125_A, speed);
    }

    @SideOnly(value=Side.CLIENT)
    public void func_180426_a(double par1, double par3, double par5, float par7, float par8, int par9, boolean bo) {
        if (this.field_70170_p.field_72995_K && this.field_174854_a) {
            return;
        }
        this.func_70107_b(par1, par3, par5);
        this.func_70101_b(par7, par8);
    }

    public void func_70071_h_() {
        AxisAlignedBB axisalignedbb;
        super.func_70030_z();
        if (++this.field_70173_aa % 10 == 0) {
            EventHooks.onProjectileTick(this);
        }
        if (this.effect == 1 && !this.field_174854_a) {
            this.func_70015_d(1);
        }
        IBlockState state = this.field_70170_p.func_180495_p(this.tilePos);
        Block block = state.func_177230_c();
        if ((this.isArrow() || this.sticksToWalls()) && this.tilePos != BlockPos.field_177992_a && (axisalignedbb = state.func_185890_d((IBlockAccess)this.field_70170_p, this.tilePos)) != null && axisalignedbb.func_72318_a(new Vec3d(this.field_70165_t, this.field_70163_u, this.field_70161_v))) {
            this.field_174854_a = true;
        }
        if (this.arrowShake > 0) {
            --this.arrowShake;
        }
        if (this.field_174854_a) {
            int j = block.func_176201_c(state);
            if (block == this.inTile && j == this.inData) {
                ++this.ticksInGround;
                if (this.ticksInGround == 1200) {
                    this.func_70106_y();
                }
            } else {
                this.field_174854_a = false;
                this.field_70159_w *= (double)(this.field_70146_Z.nextFloat() * 0.2f);
                this.field_70181_x *= (double)(this.field_70146_Z.nextFloat() * 0.2f);
                this.field_70179_y *= (double)(this.field_70146_Z.nextFloat() * 0.2f);
                this.ticksInGround = 0;
                this.field_70195_i = 0;
            }
        } else {
            ++this.field_70195_i;
            if (this.field_70195_i == 1200) {
                this.func_70106_y();
            }
            Vec3d vec3 = new Vec3d(this.field_70165_t, this.field_70163_u, this.field_70161_v);
            Vec3d vec31 = new Vec3d(this.field_70165_t + this.field_70159_w, this.field_70163_u + this.field_70181_x, this.field_70161_v + this.field_70179_y);
            RayTraceResult movingobjectposition = this.field_70170_p.func_147447_a(vec3, vec31, false, true, false);
            vec3 = new Vec3d(this.field_70165_t, this.field_70163_u, this.field_70161_v);
            vec31 = new Vec3d(this.field_70165_t + this.field_70159_w, this.field_70163_u + this.field_70181_x, this.field_70161_v + this.field_70179_y);
            if (movingobjectposition != null) {
                vec31 = new Vec3d(movingobjectposition.field_72307_f.field_72450_a, movingobjectposition.field_72307_f.field_72448_b, movingobjectposition.field_72307_f.field_72449_c);
            }
            if (!this.field_70170_p.field_72995_K) {
                Entity entity = null;
                List list = this.field_70170_p.func_72839_b((Entity)this, this.func_174813_aQ().func_72314_b(this.field_70159_w, this.field_70181_x, this.field_70179_y).func_72314_b(1.0, 1.0, 1.0));
                double d0 = 0.0;
                EntityLivingBase entityliving = this.func_85052_h();
                for (int k = 0; k < list.size(); ++k) {
                    double d1;
                    Entity entity1 = (Entity)list.get(k);
                    if (!entity1.func_70067_L() || entity1.func_70028_i((Entity)this.thrower) && this.field_70195_i < 25) continue;
                    float f = 0.3f;
                    AxisAlignedBB axisalignedbb2 = entity1.func_174813_aQ().func_72314_b((double)f, (double)f, (double)f);
                    RayTraceResult movingobjectposition1 = axisalignedbb2.func_72327_a(vec3, vec31);
                    if (movingobjectposition1 == null || !((d1 = vec3.func_72438_d(movingobjectposition1.field_72307_f)) < d0) && d0 != 0.0) continue;
                    entity = entity1;
                    d0 = d1;
                }
                if (entity != null) {
                    movingobjectposition = new RayTraceResult(entity);
                }
                if (movingobjectposition != null && movingobjectposition.field_72308_g != null) {
                    if (this.npc != null && movingobjectposition.field_72308_g instanceof EntityLivingBase && this.npc.func_184191_r((Entity)((EntityLivingBase)movingobjectposition.field_72308_g))) {
                        movingobjectposition = null;
                    } else if (movingobjectposition.field_72308_g instanceof EntityPlayer) {
                        EntityPlayer entityplayer = (EntityPlayer)movingobjectposition.field_72308_g;
                        if (entityplayer.field_71075_bZ.field_75102_a || this.thrower instanceof EntityPlayer && !((EntityPlayer)this.thrower).func_96122_a(entityplayer)) {
                            movingobjectposition = null;
                        }
                    }
                }
            }
            if (movingobjectposition != null) {
                if (movingobjectposition.field_72313_a == RayTraceResult.Type.BLOCK && this.field_70170_p.func_180495_p(movingobjectposition.func_178782_a()).func_177230_c() == Blocks.field_150427_aO) {
                    this.func_181015_d(movingobjectposition.func_178782_a());
                } else {
                    this.field_70180_af.func_187227_b(Rotating, (Object)false);
                    this.func_70184_a(movingobjectposition);
                }
            }
            this.field_70165_t += this.field_70159_w;
            this.field_70163_u += this.field_70181_x;
            this.field_70161_v += this.field_70179_y;
            float f1 = MathHelper.func_76133_a((double)(this.field_70159_w * this.field_70159_w + this.field_70179_y * this.field_70179_y));
            this.field_70177_z = (float)(Math.atan2(this.field_70159_w, this.field_70179_y) * 180.0 / Math.PI);
            this.field_70125_A = (float)(Math.atan2(this.field_70181_x, f1) * 180.0 / Math.PI);
            while (this.field_70125_A - this.field_70127_C < -180.0f) {
                this.field_70127_C -= 360.0f;
            }
            while (this.field_70125_A - this.field_70127_C >= 180.0f) {
                this.field_70127_C += 360.0f;
            }
            while (this.field_70177_z - this.field_70126_B < -180.0f) {
                this.field_70126_B -= 360.0f;
            }
            while (this.field_70177_z - this.field_70126_B >= 180.0f) {
                this.field_70126_B += 360.0f;
            }
            this.field_70125_A = this.field_70127_C + (this.field_70125_A - this.field_70127_C);
            this.field_70177_z = this.field_70126_B + (this.field_70177_z - this.field_70126_B);
            if (this.isRotating()) {
                int spin = this.isBlock() ? 10 : 20;
                this.field_70125_A -= (float)(this.field_70195_i % 15 * spin) * this.getSpeed();
            }
            float f2 = this.getMotionFactor();
            float f3 = this.func_70185_h();
            if (this.func_70090_H()) {
                if (this.field_70170_p.field_72995_K) {
                    for (int k = 0; k < 4; ++k) {
                        float f4 = 0.25f;
                        this.field_70170_p.func_175688_a(EnumParticleTypes.WATER_BUBBLE, this.field_70165_t - this.field_70159_w * (double)f4, this.field_70163_u - this.field_70181_x * (double)f4, this.field_70161_v - this.field_70179_y * (double)f4, this.field_70159_w, this.field_70181_x, this.field_70179_y, new int[0]);
                    }
                }
                f2 = 0.8f;
            }
            this.field_70159_w *= (double)f2;
            this.field_70181_x *= (double)f2;
            this.field_70179_y *= (double)f2;
            if (this.hasGravity()) {
                this.field_70181_x -= (double)f3;
            }
            if (this.accelerate) {
                this.field_70159_w += this.accelerationX;
                this.field_70181_x += this.accelerationY;
                this.field_70179_y += this.accelerationZ;
            }
            if (this.field_70170_p.field_72995_K && (Integer)this.field_70180_af.func_187225_a(Particle) > 0) {
                this.field_70170_p.func_175688_a(ParticleType.getMCType((Integer)this.field_70180_af.func_187225_a(Particle)), this.field_70165_t, this.field_70163_u, this.field_70161_v, 0.0, 0.0, 0.0, new int[0]);
            }
            this.func_70107_b(this.field_70165_t, this.field_70163_u, this.field_70161_v);
            this.func_145775_I();
        }
    }

    public boolean isBlock() {
        ItemStack item = this.getItemDisplay();
        if (item.func_190926_b()) {
            return false;
        }
        return item.func_77973_b() instanceof ItemBlock;
    }

    private Item getItem() {
        ItemStack item = this.getItemDisplay();
        if (item.func_190926_b()) {
            return Items.field_190931_a;
        }
        return item.func_77973_b();
    }

    protected float getMotionFactor() {
        return this.accelerate ? 0.95f : 1.0f;
    }

    protected void func_70184_a(RayTraceResult movingobjectposition) {
        block33: {
            block31: {
                block32: {
                    float f3;
                    if (!this.field_70170_p.field_72995_K) {
                        ProjectileEvent.ImpactEvent event;
                        BlockPos pos = null;
                        if (movingobjectposition.field_72308_g != null) {
                            pos = movingobjectposition.field_72308_g.func_180425_c();
                            event = new ProjectileEvent.ImpactEvent((IProjectile)NpcAPI.Instance().getIEntity((Entity)this), 0, movingobjectposition.field_72308_g);
                        } else {
                            pos = movingobjectposition.func_178782_a();
                            event = new ProjectileEvent.ImpactEvent((IProjectile)NpcAPI.Instance().getIEntity((Entity)this), 1, NpcAPI.Instance().getIBlock(this.field_70170_p, pos));
                        }
                        if (pos == BlockPos.field_177992_a) {
                            pos = new BlockPos(movingobjectposition.field_72307_f);
                        }
                        if (this.callback != null && this.callback.onImpact(this, pos, movingobjectposition.field_72308_g)) {
                            return;
                        }
                        EventHooks.onProjectileImpact(this, event);
                    }
                    if (movingobjectposition.field_72308_g == null) break block31;
                    float damage = this.damage;
                    if (damage == 0.0f) {
                        damage = 0.001f;
                    }
                    if (!movingobjectposition.field_72308_g.func_70097_a(DamageSource.func_76356_a((Entity)this, (Entity)this.func_85052_h()), damage)) break block32;
                    if (movingobjectposition.field_72308_g instanceof EntityLivingBase && (this.isArrow() || this.sticksToWalls())) {
                        EntityLivingBase entityliving = (EntityLivingBase)movingobjectposition.field_72308_g;
                        if (!this.field_70170_p.field_72995_K) {
                            entityliving.func_85034_r(entityliving.func_85035_bI() + 1);
                        }
                        if (this.destroyedOnEntityHit && !(movingobjectposition.field_72308_g instanceof EntityEnderman)) {
                            this.func_70106_y();
                        }
                    }
                    if (this.isBlock()) {
                        this.field_70170_p.func_180498_a((EntityPlayer)null, 2001, movingobjectposition.field_72308_g.func_180425_c(), Item.func_150891_b((Item)this.getItem()));
                    } else if (!this.isArrow() && !this.sticksToWalls()) {
                        int[] intArr = new int[]{Item.func_150891_b((Item)this.getItem())};
                        if (this.getItem().func_77614_k()) {
                            intArr = new int[]{Item.func_150891_b((Item)this.getItem()), this.getItemDisplay().func_77960_j()};
                        }
                        for (int i = 0; i < 8; ++i) {
                            this.field_70170_p.func_175688_a(EnumParticleTypes.ITEM_CRACK, this.field_70165_t, this.field_70163_u, this.field_70161_v, this.field_70146_Z.nextGaussian() * 0.15, this.field_70146_Z.nextGaussian() * 0.2, this.field_70146_Z.nextGaussian() * 0.15, intArr);
                        }
                    }
                    if (this.punch > 0 && (f3 = MathHelper.func_76133_a((double)(this.field_70159_w * this.field_70159_w + this.field_70179_y * this.field_70179_y))) > 0.0f) {
                        movingobjectposition.field_72308_g.func_70024_g(this.field_70159_w * (double)this.punch * (double)0.6f / (double)f3, 0.1, this.field_70179_y * (double)this.punch * (double)0.6f / (double)f3);
                    }
                    if (this.effect != 0 && movingobjectposition.field_72308_g instanceof EntityLivingBase) {
                        if (this.effect != 1) {
                            Potion p = PotionEffectType.getMCType(this.effect);
                            ((EntityLivingBase)movingobjectposition.field_72308_g).func_70690_d(new PotionEffect(p, this.duration * 20, this.amplify));
                        } else {
                            movingobjectposition.field_72308_g.func_70015_d(this.duration);
                        }
                    }
                    break block33;
                }
                if (!this.hasGravity() || !this.isArrow() && !this.sticksToWalls()) break block33;
                this.field_70159_w *= (double)-0.1f;
                this.field_70181_x *= (double)-0.1f;
                this.field_70179_y *= (double)-0.1f;
                this.field_70177_z += 180.0f;
                this.field_70126_B += 180.0f;
                this.field_70195_i = 0;
                break block33;
            }
            if (this.isArrow() || this.sticksToWalls()) {
                this.tilePos = movingobjectposition.func_178782_a();
                IBlockState state = this.field_70170_p.func_180495_p(this.tilePos);
                this.inTile = state.func_177230_c();
                this.inData = this.inTile.func_176201_c(state);
                this.field_70159_w = (float)(movingobjectposition.field_72307_f.field_72450_a - this.field_70165_t);
                this.field_70181_x = (float)(movingobjectposition.field_72307_f.field_72448_b - this.field_70163_u);
                this.field_70179_y = (float)(movingobjectposition.field_72307_f.field_72449_c - this.field_70161_v);
                float f2 = MathHelper.func_76133_a((double)(this.field_70159_w * this.field_70159_w + this.field_70181_x * this.field_70181_x + this.field_70179_y * this.field_70179_y));
                this.field_70165_t -= this.field_70159_w / (double)f2 * (double)0.05f;
                this.field_70163_u -= this.field_70181_x / (double)f2 * (double)0.05f;
                this.field_70161_v -= this.field_70179_y / (double)f2 * (double)0.05f;
                this.field_174854_a = true;
                this.arrowShake = 7;
                if (!this.hasGravity()) {
                    this.field_70180_af.func_187227_b(Gravity, (Object)true);
                }
                if (this.inTile != null) {
                    this.inTile.func_180634_a(this.field_70170_p, this.tilePos, state, (Entity)this);
                }
            } else if (this.isBlock()) {
                this.field_70170_p.func_180498_a((EntityPlayer)null, 2001, this.func_180425_c(), Item.func_150891_b((Item)this.getItem()));
            } else {
                int[] intArr = new int[]{Item.func_150891_b((Item)this.getItem())};
                if (this.getItem().func_77614_k()) {
                    intArr = new int[]{Item.func_150891_b((Item)this.getItem()), this.getItemDisplay().func_77960_j()};
                }
                for (int i = 0; i < 8; ++i) {
                    this.field_70170_p.func_175688_a(EnumParticleTypes.ITEM_CRACK, this.field_70165_t, this.field_70163_u, this.field_70161_v, this.field_70146_Z.nextGaussian() * 0.15, this.field_70146_Z.nextGaussian() * 0.2, this.field_70146_Z.nextGaussian() * 0.15, intArr);
                }
            }
        }
        if (this.explosiveRadius > 0) {
            boolean terraindamage = this.field_70170_p.func_82736_K().func_82766_b("mobGriefing") && this.explosiveDamage;
            this.field_70170_p.func_72885_a((Entity)(this.func_85052_h() == null ? this : this.func_85052_h()), this.field_70165_t, this.field_70163_u, this.field_70161_v, (float)this.explosiveRadius, this.effect == 1, terraindamage);
            if (this.effect != 0) {
                AxisAlignedBB axisalignedbb = this.func_174813_aQ().func_72314_b((double)(this.explosiveRadius * 2), (double)(this.explosiveRadius * 2), (double)(this.explosiveRadius * 2));
                List list1 = this.field_70170_p.func_72872_a(EntityLivingBase.class, axisalignedbb);
                for (EntityLivingBase entity : list1) {
                    if (this.effect != 1) {
                        Potion p = PotionEffectType.getMCType(this.effect);
                        if (p == null) continue;
                        entity.func_70690_d(new PotionEffect(p, this.duration * 20, this.amplify));
                        continue;
                    }
                    entity.func_70015_d(this.duration);
                }
                this.field_70170_p.func_180498_a((EntityPlayer)null, 2002, this.func_180425_c(), this.getPotionColor(this.effect));
            }
            this.func_70106_y();
        }
        if (!(this.field_70170_p.field_72995_K || this.isArrow() || this.sticksToWalls())) {
            this.func_70106_y();
        }
    }

    private void blockParticles() {
    }

    public void func_70014_b(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.func_74777_a("xTile", (short)this.tilePos.func_177958_n());
        par1NBTTagCompound.func_74777_a("yTile", (short)this.tilePos.func_177956_o());
        par1NBTTagCompound.func_74777_a("zTile", (short)this.tilePos.func_177952_p());
        par1NBTTagCompound.func_74774_a("inTile", (byte)Block.func_149682_b((Block)this.inTile));
        par1NBTTagCompound.func_74774_a("inData", (byte)this.inData);
        par1NBTTagCompound.func_74774_a("shake", (byte)this.field_70191_b);
        par1NBTTagCompound.func_74757_a("inGround", this.field_174854_a);
        par1NBTTagCompound.func_74757_a("isArrow", this.isArrow());
        par1NBTTagCompound.func_74782_a("direction", (NBTBase)this.func_70087_a(new double[]{this.field_70159_w, this.field_70181_x, this.field_70179_y}));
        par1NBTTagCompound.func_74757_a("canBePickedUp", this.canBePickedUp);
        if ((this.throwerName == null || this.throwerName.length() == 0) && this.thrower != null && this.thrower instanceof EntityPlayer) {
            this.throwerName = this.thrower.func_110124_au().toString();
        }
        par1NBTTagCompound.func_74778_a("ownerName", this.throwerName == null ? "" : this.throwerName);
        par1NBTTagCompound.func_74782_a("Item", (NBTBase)this.getItemDisplay().func_77955_b(new NBTTagCompound()));
        par1NBTTagCompound.func_74776_a("damagev2", this.damage);
        par1NBTTagCompound.func_74768_a("punch", this.punch);
        par1NBTTagCompound.func_74768_a("size", ((Integer)this.field_70180_af.func_187225_a(Size)).intValue());
        par1NBTTagCompound.func_74768_a("velocity", ((Integer)this.field_70180_af.func_187225_a(Velocity)).intValue());
        par1NBTTagCompound.func_74768_a("explosiveRadius", this.explosiveRadius);
        par1NBTTagCompound.func_74768_a("effectDuration", this.duration);
        par1NBTTagCompound.func_74757_a("gravity", this.hasGravity());
        par1NBTTagCompound.func_74757_a("accelerate", this.accelerate);
        par1NBTTagCompound.func_74757_a("glows", ((Boolean)this.field_70180_af.func_187225_a(Glows)).booleanValue());
        par1NBTTagCompound.func_74768_a("PotionEffect", this.effect);
        par1NBTTagCompound.func_74768_a("trailenum", ((Integer)this.field_70180_af.func_187225_a(Particle)).intValue());
        par1NBTTagCompound.func_74757_a("Render3D", ((Boolean)this.field_70180_af.func_187225_a(Is3d)).booleanValue());
        par1NBTTagCompound.func_74757_a("Spins", ((Boolean)this.field_70180_af.func_187225_a(Rotating)).booleanValue());
        par1NBTTagCompound.func_74757_a("Sticks", ((Boolean)this.field_70180_af.func_187225_a(Sticks)).booleanValue());
        par1NBTTagCompound.func_74768_a("accuracy", this.accuracy);
    }

    public void func_70037_a(NBTTagCompound compound) {
        NBTTagCompound var2;
        ItemStack item;
        this.tilePos = new BlockPos((int)compound.func_74765_d("xTile"), (int)compound.func_74765_d("yTile"), (int)compound.func_74765_d("zTile"));
        this.inTile = Block.func_149729_e((int)(compound.func_74771_c("inTile") & 0xFF));
        this.inData = compound.func_74771_c("inData") & 0xFF;
        this.field_70191_b = compound.func_74771_c("shake") & 0xFF;
        this.field_174854_a = compound.func_74771_c("inGround") == 1;
        this.field_70180_af.func_187227_b(Arrow, (Object)compound.func_74767_n("isArrow"));
        this.throwerName = compound.func_74779_i("ownerName");
        this.canBePickedUp = compound.func_74767_n("canBePickedUp");
        this.damage = compound.func_74760_g("damagev2");
        this.punch = compound.func_74762_e("punch");
        this.explosiveRadius = compound.func_74762_e("explosiveRadius");
        this.duration = compound.func_74762_e("effectDuration");
        this.accelerate = compound.func_74767_n("accelerate");
        this.effect = compound.func_74762_e("PotionEffect");
        this.accuracy = compound.func_74762_e("accuracy");
        this.field_70180_af.func_187227_b(Particle, (Object)compound.func_74762_e("trailenum"));
        this.field_70180_af.func_187227_b(Size, (Object)compound.func_74762_e("size"));
        this.field_70180_af.func_187227_b(Glows, (Object)compound.func_74767_n("glows"));
        this.field_70180_af.func_187227_b(Velocity, (Object)compound.func_74762_e("velocity"));
        this.field_70180_af.func_187227_b(Gravity, (Object)compound.func_74767_n("gravity"));
        this.field_70180_af.func_187227_b(Is3d, (Object)compound.func_74767_n("Render3D"));
        this.field_70180_af.func_187227_b(Rotating, (Object)compound.func_74767_n("Spins"));
        this.field_70180_af.func_187227_b(Sticks, (Object)compound.func_74767_n("Sticks"));
        if (this.throwerName != null && this.throwerName.length() == 0) {
            this.throwerName = null;
        }
        if (compound.func_74764_b("direction")) {
            NBTTagList nbttaglist = compound.func_150295_c("direction", 6);
            this.field_70159_w = nbttaglist.func_150309_d(0);
            this.field_70181_x = nbttaglist.func_150309_d(1);
            this.field_70179_y = nbttaglist.func_150309_d(2);
        }
        if ((item = new ItemStack(var2 = compound.func_74775_l("Item"))).func_190926_b()) {
            this.func_70106_y();
        } else {
            this.field_70180_af.func_187227_b(ItemStackThrown, (Object)item);
        }
    }

    public EntityLivingBase func_85052_h() {
        if (this.throwerName == null || this.throwerName.isEmpty()) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(this.throwerName);
            if (this.thrower == null && uuid != null) {
                this.thrower = this.field_70170_p.func_152378_a(uuid);
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
        this.field_70180_af.func_187227_b(Size, (Object)stats.getSize());
        this.field_70180_af.func_187227_b(Glows, (Object)stats.getGlows());
        this.setSpeed(stats.getSpeed());
        this.setHasGravity(stats.getHasGravity());
        this.setIs3D(stats.getRender3D());
        this.setRotating(stats.getSpins());
        this.setStickInWall(stats.getSticks());
    }

    public void setParticleEffect(int type) {
        this.field_70180_af.func_187227_b(Particle, (Object)type);
    }

    public void setHasGravity(boolean bo) {
        this.field_70180_af.func_187227_b(Gravity, (Object)bo);
    }

    public void setIs3D(boolean bo) {
        this.field_70180_af.func_187227_b(Is3d, (Object)bo);
    }

    public void setStickInWall(boolean bo) {
        this.field_70180_af.func_187227_b(Sticks, (Object)bo);
    }

    public ItemStack getItemDisplay() {
        return (ItemStack)this.field_70180_af.func_187225_a(ItemStackThrown);
    }

    public float func_70013_c() {
        return (Boolean)this.field_70180_af.func_187225_a(Glows) != false ? 1.0f : super.func_70013_c();
    }

    @SideOnly(value=Side.CLIENT)
    public int func_70070_b() {
        return (Boolean)this.field_70180_af.func_187225_a(Glows) != false ? 0xF000F0 : super.func_70070_b();
    }

    public boolean hasGravity() {
        return (Boolean)this.field_70180_af.func_187225_a(Gravity);
    }

    public void setSpeed(int speed) {
        this.field_70180_af.func_187227_b(Velocity, (Object)speed);
    }

    public float getSpeed() {
        return (float)((Integer)this.field_70180_af.func_187225_a(Velocity)).intValue() / 10.0f;
    }

    public boolean isArrow() {
        return (Boolean)this.field_70180_af.func_187225_a(Arrow);
    }

    public void setRotating(boolean bo) {
        this.field_70180_af.func_187227_b(Rotating, (Object)bo);
    }

    public boolean isRotating() {
        return (Boolean)this.field_70180_af.func_187225_a(Rotating);
    }

    public boolean glows() {
        return (Boolean)this.field_70180_af.func_187225_a(Glows);
    }

    public boolean is3D() {
        return (Boolean)this.field_70180_af.func_187225_a(Is3d) != false || this.isBlock();
    }

    public boolean sticksToWalls() {
        return this.is3D() && (Boolean)this.field_70180_af.func_187225_a(Sticks) != false;
    }

    public void func_70100_b_(EntityPlayer par1EntityPlayer) {
        if (this.field_70170_p.field_72995_K || !this.canBePickedUp || !this.field_174854_a || this.arrowShake > 0) {
            return;
        }
        if (par1EntityPlayer.field_71071_by.func_70441_a(this.getItemDisplay())) {
            this.field_174854_a = false;
            this.func_184185_a(SoundEvents.field_187638_cR, 0.2f, ((this.field_70146_Z.nextFloat() - this.field_70146_Z.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            par1EntityPlayer.func_71001_a((Entity)this, 1);
            this.func_70106_y();
        }
    }

    protected boolean func_70041_e_() {
        return false;
    }

    public ITextComponent func_145748_c_() {
        if (!this.getItemDisplay().func_190926_b()) {
            return new TextComponentTranslation(this.getItemDisplay().func_82833_r(), new Object[0]);
        }
        return super.func_145748_c_();
    }

    public static interface IProjectileCallback {
        public boolean onImpact(EntityProjectile var1, BlockPos var2, Entity var3);
    }
}

