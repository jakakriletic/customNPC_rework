/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundEvent
 */
package noppes.npcs.entity.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import noppes.npcs.api.entity.data.INPCRanged;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class DataRanged
implements INPCRanged {
    private EntityNPCInterface npc;
    private int burstCount = 1;
    private int pDamage = 4;
    private int pSpeed = 10;
    private int pImpact = 0;
    private int pSize = 5;
    private int pArea = 0;
    private int pTrail = 0;
    private int minDelay = 20;
    private int maxDelay = 40;
    private int rangedRange = 15;
    private int fireRate = 5;
    private int shotCount = 1;
    private int accuracy = 60;
    private int meleeDistance = 0;
    private int canFireIndirect = 0;
    private boolean pRender3D = true;
    private boolean pSpin = false;
    private boolean pStick = false;
    private boolean pPhysics = true;
    private boolean pXlr8 = false;
    private boolean pGlows = false;
    private boolean aimWhileShooting = false;
    private int pEffect = 0;
    private int pDur = 5;
    private int pEffAmp = 0;
    private String fireSound = "minecraft:entity.arrow.shoot";
    private String hitSound = "minecraft:entity.arrow.hit";
    private String groundSound = "minecraft:block.stone.break";

    public DataRanged(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public void readFromNBT(NBTTagCompound compound) {
        this.pDamage = compound.getInteger("pDamage");
        this.pSpeed = compound.getInteger("pSpeed");
        this.burstCount = compound.getInteger("BurstCount");
        this.pImpact = compound.getInteger("pImpact");
        this.pSize = compound.getInteger("pSize");
        this.pArea = compound.getInteger("pArea");
        this.pTrail = compound.getInteger("pTrail");
        this.rangedRange = compound.getInteger("MaxFiringRange");
        this.fireRate = compound.getInteger("FireRate");
        this.minDelay = ValueUtil.CorrectInt(compound.getInteger("minDelay"), 1, 9999);
        this.maxDelay = ValueUtil.CorrectInt(compound.getInteger("maxDelay"), 1, 9999);
        this.shotCount = ValueUtil.CorrectInt(compound.getInteger("ShotCount"), 1, 10);
        this.accuracy = compound.getInteger("Accuracy");
        this.pRender3D = compound.getBoolean("pRender3D");
        this.pSpin = compound.getBoolean("pSpin");
        this.pStick = compound.getBoolean("pStick");
        this.pPhysics = compound.getBoolean("pPhysics");
        this.pXlr8 = compound.getBoolean("pXlr8");
        this.pGlows = compound.getBoolean("pGlows");
        this.aimWhileShooting = compound.getBoolean("AimWhileShooting");
        this.pEffect = compound.getInteger("pEffect");
        this.pDur = compound.getInteger("pDur");
        this.pEffAmp = compound.getInteger("pEffAmp");
        this.fireSound = compound.getString("FiringSound");
        this.hitSound = compound.getString("HitSound");
        this.groundSound = compound.getString("GroundSound");
        this.canFireIndirect = compound.getInteger("FireIndirect");
        this.meleeDistance = compound.getInteger("DistanceToMelee");
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("BurstCount", this.burstCount);
        compound.setInteger("pSpeed", this.pSpeed);
        compound.setInteger("pDamage", this.pDamage);
        compound.setInteger("pImpact", this.pImpact);
        compound.setInteger("pSize", this.pSize);
        compound.setInteger("pArea", this.pArea);
        compound.setInteger("pTrail", this.pTrail);
        compound.setInteger("MaxFiringRange", this.rangedRange);
        compound.setInteger("FireRate", this.fireRate);
        compound.setInteger("minDelay", this.minDelay);
        compound.setInteger("maxDelay", this.maxDelay);
        compound.setInteger("ShotCount", this.shotCount);
        compound.setInteger("Accuracy", this.accuracy);
        compound.setBoolean("pRender3D", this.pRender3D);
        compound.setBoolean("pSpin", this.pSpin);
        compound.setBoolean("pStick", this.pStick);
        compound.setBoolean("pPhysics", this.pPhysics);
        compound.setBoolean("pXlr8", this.pXlr8);
        compound.setBoolean("pGlows", this.pGlows);
        compound.setBoolean("AimWhileShooting", this.aimWhileShooting);
        compound.setInteger("pEffect", this.pEffect);
        compound.setInteger("pDur", this.pDur);
        compound.setInteger("pEffAmp", this.pEffAmp);
        compound.setString("FiringSound", this.fireSound);
        compound.setString("HitSound", this.hitSound);
        compound.setString("GroundSound", this.groundSound);
        compound.setInteger("FireIndirect", this.canFireIndirect);
        compound.setInteger("DistanceToMelee", this.meleeDistance);
        return compound;
    }

    @Override
    public int getStrength() {
        return this.pDamage;
    }

    @Override
    public void setStrength(int strength) {
        this.pDamage = strength;
    }

    @Override
    public int getSpeed() {
        return this.pSpeed;
    }

    @Override
    public void setSpeed(int speed) {
        this.pSpeed = ValueUtil.CorrectInt(speed, 0, 100);
    }

    @Override
    public int getKnockback() {
        return this.pImpact;
    }

    @Override
    public void setKnockback(int punch) {
        this.pImpact = punch;
    }

    @Override
    public int getSize() {
        return this.pSize;
    }

    @Override
    public void setSize(int size) {
        this.pSize = size;
    }

    @Override
    public boolean getRender3D() {
        return this.pRender3D;
    }

    @Override
    public void setRender3D(boolean render3d) {
        this.pRender3D = render3d;
    }

    @Override
    public boolean getSpins() {
        return this.pSpin;
    }

    @Override
    public void setSpins(boolean spins) {
        this.pSpin = spins;
    }

    @Override
    public boolean getSticks() {
        return this.pStick;
    }

    @Override
    public void setSticks(boolean sticks) {
        this.pStick = sticks;
    }

    @Override
    public boolean getHasGravity() {
        return this.pPhysics;
    }

    @Override
    public void setHasGravity(boolean hasGravity) {
        this.pPhysics = hasGravity;
    }

    @Override
    public boolean getAccelerate() {
        return this.pXlr8;
    }

    @Override
    public void setAccelerate(boolean accelerate) {
        this.pXlr8 = accelerate;
    }

    @Override
    public int getExplodeSize() {
        return this.pArea;
    }

    @Override
    public void setExplodeSize(int size) {
        this.pArea = size;
    }

    @Override
    public int getEffectType() {
        return this.pEffect;
    }

    @Override
    public int getEffectTime() {
        return this.pDur;
    }

    @Override
    public int getEffectStrength() {
        return this.pEffAmp;
    }

    @Override
    public void setEffect(int type, int strength, int time) {
        this.pEffect = type;
        this.pDur = time;
        this.pEffAmp = strength;
    }

    @Override
    public boolean getGlows() {
        return this.pGlows;
    }

    @Override
    public void setGlows(boolean glows) {
        this.pGlows = glows;
    }

    @Override
    public int getParticle() {
        return this.pTrail;
    }

    @Override
    public void setParticle(int type) {
        this.pTrail = type;
    }

    @Override
    public int getAccuracy() {
        return this.accuracy;
    }

    @Override
    public void setAccuracy(int accuracy) {
        this.accuracy = ValueUtil.CorrectInt(accuracy, 1, 100);
    }

    @Override
    public int getRange() {
        return this.rangedRange;
    }

    @Override
    public void setRange(int range) {
        this.rangedRange = ValueUtil.CorrectInt(range, 1, 64);
    }

    @Override
    public int getDelayMin() {
        return this.minDelay;
    }

    @Override
    public int getDelayMax() {
        return this.maxDelay;
    }

    @Override
    public int getDelayRNG() {
        int delay = this.minDelay;
        if (this.maxDelay - this.minDelay > 0) {
            delay += this.npc.world.rand.nextInt(this.maxDelay - this.minDelay);
        }
        return delay;
    }

    @Override
    public void setDelay(int min, int max) {
        this.minDelay = min = Math.min(min, max);
        this.maxDelay = max;
    }

    @Override
    public int getBurst() {
        return this.burstCount;
    }

    @Override
    public void setBurst(int count) {
        this.burstCount = count;
    }

    @Override
    public int getBurstDelay() {
        return this.fireRate;
    }

    @Override
    public void setBurstDelay(int delay) {
        this.fireRate = delay;
    }

    @Override
    public String getSound(int type) {
        String sound = null;
        if (type == 0) {
            sound = this.fireSound;
        }
        if (type == 1) {
            sound = this.hitSound;
        }
        if (type == 2) {
            sound = this.groundSound;
        }
        if (sound == null || sound.isEmpty()) {
            return null;
        }
        return sound;
    }

    public SoundEvent getSoundEvent(int type) {
        String sound = this.getSound(type);
        if (sound == null) {
            return null;
        }
        ResourceLocation res = new ResourceLocation(sound);
        SoundEvent ev = (SoundEvent)SoundEvent.REGISTRY.getObject((Object)res);
        if (ev != null) {
            return ev;
        }
        return new SoundEvent(res);
    }

    @Override
    public void setSound(int type, String sound) {
        if (sound == null) {
            sound = "";
        }
        if (type == 0) {
            this.fireSound = sound;
        }
        if (type == 1) {
            this.hitSound = sound;
        }
        if (type == 2) {
            this.groundSound = sound;
        }
        this.npc.updateClient = true;
    }

    @Override
    public int getShotCount() {
        return this.shotCount;
    }

    @Override
    public void setShotCount(int count) {
        this.shotCount = count;
    }

    @Override
    public boolean getHasAimAnimation() {
        return this.aimWhileShooting;
    }

    @Override
    public void setHasAimAnimation(boolean aim) {
        this.aimWhileShooting = aim;
    }

    @Override
    public int getFireType() {
        return this.canFireIndirect;
    }

    @Override
    public void setFireType(int type) {
        this.canFireIndirect = type;
    }

    @Override
    public int getMeleeRange() {
        return this.meleeDistance;
    }

    @Override
    public void setMeleeRange(int range) {
        this.meleeDistance = range;
        this.npc.updateAI = true;
    }
}

