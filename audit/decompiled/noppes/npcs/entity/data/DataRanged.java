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
        this.pDamage = compound.func_74762_e("pDamage");
        this.pSpeed = compound.func_74762_e("pSpeed");
        this.burstCount = compound.func_74762_e("BurstCount");
        this.pImpact = compound.func_74762_e("pImpact");
        this.pSize = compound.func_74762_e("pSize");
        this.pArea = compound.func_74762_e("pArea");
        this.pTrail = compound.func_74762_e("pTrail");
        this.rangedRange = compound.func_74762_e("MaxFiringRange");
        this.fireRate = compound.func_74762_e("FireRate");
        this.minDelay = ValueUtil.CorrectInt(compound.func_74762_e("minDelay"), 1, 9999);
        this.maxDelay = ValueUtil.CorrectInt(compound.func_74762_e("maxDelay"), 1, 9999);
        this.shotCount = ValueUtil.CorrectInt(compound.func_74762_e("ShotCount"), 1, 10);
        this.accuracy = compound.func_74762_e("Accuracy");
        this.pRender3D = compound.func_74767_n("pRender3D");
        this.pSpin = compound.func_74767_n("pSpin");
        this.pStick = compound.func_74767_n("pStick");
        this.pPhysics = compound.func_74767_n("pPhysics");
        this.pXlr8 = compound.func_74767_n("pXlr8");
        this.pGlows = compound.func_74767_n("pGlows");
        this.aimWhileShooting = compound.func_74767_n("AimWhileShooting");
        this.pEffect = compound.func_74762_e("pEffect");
        this.pDur = compound.func_74762_e("pDur");
        this.pEffAmp = compound.func_74762_e("pEffAmp");
        this.fireSound = compound.func_74779_i("FiringSound");
        this.hitSound = compound.func_74779_i("HitSound");
        this.groundSound = compound.func_74779_i("GroundSound");
        this.canFireIndirect = compound.func_74762_e("FireIndirect");
        this.meleeDistance = compound.func_74762_e("DistanceToMelee");
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.func_74768_a("BurstCount", this.burstCount);
        compound.func_74768_a("pSpeed", this.pSpeed);
        compound.func_74768_a("pDamage", this.pDamage);
        compound.func_74768_a("pImpact", this.pImpact);
        compound.func_74768_a("pSize", this.pSize);
        compound.func_74768_a("pArea", this.pArea);
        compound.func_74768_a("pTrail", this.pTrail);
        compound.func_74768_a("MaxFiringRange", this.rangedRange);
        compound.func_74768_a("FireRate", this.fireRate);
        compound.func_74768_a("minDelay", this.minDelay);
        compound.func_74768_a("maxDelay", this.maxDelay);
        compound.func_74768_a("ShotCount", this.shotCount);
        compound.func_74768_a("Accuracy", this.accuracy);
        compound.func_74757_a("pRender3D", this.pRender3D);
        compound.func_74757_a("pSpin", this.pSpin);
        compound.func_74757_a("pStick", this.pStick);
        compound.func_74757_a("pPhysics", this.pPhysics);
        compound.func_74757_a("pXlr8", this.pXlr8);
        compound.func_74757_a("pGlows", this.pGlows);
        compound.func_74757_a("AimWhileShooting", this.aimWhileShooting);
        compound.func_74768_a("pEffect", this.pEffect);
        compound.func_74768_a("pDur", this.pDur);
        compound.func_74768_a("pEffAmp", this.pEffAmp);
        compound.func_74778_a("FiringSound", this.fireSound);
        compound.func_74778_a("HitSound", this.hitSound);
        compound.func_74778_a("GroundSound", this.groundSound);
        compound.func_74768_a("FireIndirect", this.canFireIndirect);
        compound.func_74768_a("DistanceToMelee", this.meleeDistance);
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
            delay += this.npc.field_70170_p.field_73012_v.nextInt(this.maxDelay - this.minDelay);
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
        SoundEvent ev = (SoundEvent)SoundEvent.field_187505_a.func_82594_a((Object)res);
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

