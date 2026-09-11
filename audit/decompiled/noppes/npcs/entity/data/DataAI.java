/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.pathfinding.PathNavigateGround
 *  net.minecraft.pathfinding.PathNodeType
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.NBTTags;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.data.INPCAi;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobBuilder;
import noppes.npcs.roles.JobFarmer;

public class DataAI
implements INPCAi {
    private EntityNPCInterface npc;
    public int onAttack = 0;
    public int doorInteract = 2;
    public int findShelter = 2;
    public boolean canSwim = true;
    public boolean reactsToFire = false;
    public boolean avoidsWater = false;
    public boolean avoidsSun = false;
    public boolean returnToStart = true;
    public boolean directLOS = true;
    public boolean canLeap = false;
    public boolean canSprint = false;
    public boolean stopAndInteract = true;
    public boolean attackInvisible = false;
    public int tacticalVariant = 0;
    private int tacticalRadius = 8;
    public int movementType = 0;
    public int animationType = 0;
    private int standingType = 0;
    private int movingType = 0;
    public boolean npcInteracting = true;
    public int orientation = 0;
    public float bodyOffsetX = 5.0f;
    public float bodyOffsetY = 5.0f;
    public float bodyOffsetZ = 5.0f;
    public int walkingRange = 10;
    private int moveSpeed = 5;
    private List<int[]> movingPath = new ArrayList<int[]>();
    private BlockPos startPos = null;
    public int movingPos = 0;
    public int movingPattern = 0;
    public boolean movingPause = true;

    public DataAI(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public void readToNBT(NBTTagCompound compound) {
        this.canSwim = compound.func_74767_n("CanSwim");
        this.reactsToFire = compound.func_74767_n("ReactsToFire");
        this.setAvoidsWater(compound.func_74767_n("AvoidsWater"));
        this.avoidsSun = compound.func_74767_n("AvoidsSun");
        this.returnToStart = compound.func_74767_n("ReturnToStart");
        this.onAttack = compound.func_74762_e("OnAttack");
        this.doorInteract = compound.func_74762_e("DoorInteract");
        this.findShelter = compound.func_74762_e("FindShelter");
        this.directLOS = compound.func_74767_n("DirectLOS");
        this.canLeap = compound.func_74767_n("CanLeap");
        this.canSprint = compound.func_74767_n("CanSprint");
        this.tacticalRadius = compound.func_74762_e("TacticalRadius");
        this.movingPause = compound.func_74767_n("MovingPause");
        this.npcInteracting = compound.func_74767_n("npcInteracting");
        this.stopAndInteract = compound.func_74767_n("stopAndInteract");
        this.movementType = compound.func_74762_e("MovementType");
        this.animationType = compound.func_74762_e("MoveState");
        this.standingType = compound.func_74762_e("StandingState");
        this.movingType = compound.func_74762_e("MovingState");
        this.tacticalVariant = compound.func_74762_e("TacticalVariant");
        this.orientation = compound.func_74762_e("Orientation");
        this.bodyOffsetY = compound.func_74760_g("PositionOffsetY");
        this.bodyOffsetZ = compound.func_74760_g("PositionOffsetZ");
        this.bodyOffsetX = compound.func_74760_g("PositionOffsetX");
        this.walkingRange = compound.func_74762_e("WalkingRange");
        this.setWalkingSpeed(compound.func_74762_e("MoveSpeed"));
        this.setMovingPath(NBTTags.getIntegerArraySet(compound.func_150295_c("MovingPathNew", 10)));
        this.movingPos = compound.func_74762_e("MovingPos");
        this.movingPattern = compound.func_74762_e("MovingPatern");
        this.attackInvisible = compound.func_74767_n("AttackInvisible");
        if (compound.func_74764_b("StartPosNew")) {
            int[] startPos = compound.func_74759_k("StartPosNew");
            this.startPos = new BlockPos(startPos[0], startPos[1], startPos[2]);
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.func_74757_a("CanSwim", this.canSwim);
        compound.func_74757_a("ReactsToFire", this.reactsToFire);
        compound.func_74757_a("AvoidsWater", this.avoidsWater);
        compound.func_74757_a("AvoidsSun", this.avoidsSun);
        compound.func_74757_a("ReturnToStart", this.returnToStart);
        compound.func_74768_a("OnAttack", this.onAttack);
        compound.func_74768_a("DoorInteract", this.doorInteract);
        compound.func_74768_a("FindShelter", this.findShelter);
        compound.func_74757_a("DirectLOS", this.directLOS);
        compound.func_74757_a("CanLeap", this.canLeap);
        compound.func_74757_a("CanSprint", this.canSprint);
        compound.func_74768_a("TacticalRadius", this.tacticalRadius);
        compound.func_74757_a("MovingPause", this.movingPause);
        compound.func_74757_a("npcInteracting", this.npcInteracting);
        compound.func_74757_a("stopAndInteract", this.stopAndInteract);
        compound.func_74768_a("MoveState", this.animationType);
        compound.func_74768_a("StandingState", this.standingType);
        compound.func_74768_a("MovingState", this.movingType);
        compound.func_74768_a("TacticalVariant", this.tacticalVariant);
        compound.func_74768_a("MovementType", this.movementType);
        compound.func_74768_a("Orientation", this.orientation);
        compound.func_74776_a("PositionOffsetX", this.bodyOffsetX);
        compound.func_74776_a("PositionOffsetY", this.bodyOffsetY);
        compound.func_74776_a("PositionOffsetZ", this.bodyOffsetZ);
        compound.func_74768_a("WalkingRange", this.walkingRange);
        compound.func_74768_a("MoveSpeed", this.moveSpeed);
        compound.func_74782_a("MovingPathNew", (NBTBase)NBTTags.nbtIntegerArraySet(this.movingPath));
        compound.func_74768_a("MovingPos", this.movingPos);
        compound.func_74768_a("MovingPatern", this.movingPattern);
        this.setAvoidsWater(this.avoidsWater);
        compound.func_74783_a("StartPosNew", this.getStartArray());
        compound.func_74757_a("AttackInvisible", this.attackInvisible);
        return compound;
    }

    public List<int[]> getMovingPath() {
        if (this.movingPath.isEmpty() && this.startPos != null) {
            this.movingPath.add(this.getStartArray());
        }
        return this.movingPath;
    }

    public void setMovingPath(List<int[]> list) {
        this.movingPath = list;
        if (!this.movingPath.isEmpty()) {
            int[] startPos = this.movingPath.get(0);
            this.startPos = new BlockPos(startPos[0], startPos[1], startPos[2]);
        }
    }

    public BlockPos startPos() {
        if (this.startPos == null) {
            this.startPos = new BlockPos((Entity)this.npc);
        }
        return this.startPos;
    }

    private int[] getStartArray() {
        BlockPos pos = this.startPos();
        return new int[]{pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p()};
    }

    public int[] getCurrentMovingPath() {
        List<int[]> list = this.getMovingPath();
        int size = list.size();
        if (size == 1) {
            return list.get(0);
        }
        int pos = this.movingPos;
        if (this.movingPattern == 0 && pos >= size) {
            this.movingPos = 0;
            pos = 0;
        }
        if (this.movingPattern == 1) {
            int size2 = size * 2 - 1;
            if (pos >= size2) {
                this.movingPos = 0;
                pos = 0;
            } else if (pos >= size) {
                pos = size2 - pos;
            }
        }
        return list.get(pos);
    }

    public void incrementMovingPath() {
        List<int[]> list = this.getMovingPath();
        if (list.size() == 1) {
            this.movingPos = 0;
            return;
        }
        ++this.movingPos;
        if (this.movingPattern == 0) {
            this.movingPos %= list.size();
        } else if (this.movingPattern == 1) {
            int size = list.size() * 2 - 1;
            this.movingPos %= size;
        }
    }

    public void decreaseMovingPath() {
        List<int[]> list = this.getMovingPath();
        if (list.size() == 1) {
            this.movingPos = 0;
            return;
        }
        --this.movingPos;
        if (this.movingPos < 0) {
            if (this.movingPattern == 0) {
                this.movingPos = list.size() - 1;
            } else if (this.movingPattern == 1) {
                this.movingPos = list.size() * 2 - 2;
            }
        }
    }

    public double getDistanceSqToPathPoint() {
        int[] pos = this.getCurrentMovingPath();
        return this.npc.func_70092_e((double)pos[0] + 0.5, pos[1], (double)pos[2] + 0.5);
    }

    public void setStartPos(BlockPos pos) {
        this.startPos = pos;
    }

    @Override
    public void setReturnsHome(boolean bo) {
        this.returnToStart = bo;
    }

    @Override
    public boolean getReturnsHome() {
        return this.returnToStart;
    }

    public boolean shouldReturnHome() {
        if (this.npc.advanced.job == 10 && ((JobBuilder)this.npc.jobInterface).isBuilding()) {
            return false;
        }
        if (this.npc.advanced.job == 11 && ((JobFarmer)this.npc.jobInterface).isPlucking()) {
            return false;
        }
        return this.returnToStart;
    }

    @Override
    public int getAnimation() {
        return this.animationType;
    }

    @Override
    public int getCurrentAnimation() {
        return this.npc.currentAnimation;
    }

    @Override
    public void setAnimation(int type) {
        this.animationType = type;
    }

    @Override
    public int getRetaliateType() {
        return this.onAttack;
    }

    @Override
    public void setRetaliateType(int type) {
        if (type < 0 || type > 3) {
            throw new CustomNPCsException("Unknown retaliation type: " + type, new Object[0]);
        }
        this.onAttack = type;
        this.npc.updateAI = true;
    }

    @Override
    public int getMovingType() {
        return this.movingType;
    }

    @Override
    public void setMovingType(int type) {
        if (type < 0 || type > 2) {
            throw new CustomNPCsException("Unknown moving type: " + type, new Object[0]);
        }
        this.movingType = type;
        this.npc.updateAI = true;
    }

    @Override
    public int getStandingType() {
        return this.standingType;
    }

    @Override
    public void setStandingType(int type) {
        if (type < 0 || type > 3) {
            throw new CustomNPCsException("Unknown standing type: " + type, new Object[0]);
        }
        this.standingType = type;
        this.npc.updateAI = true;
    }

    @Override
    public boolean getAttackInvisible() {
        return this.attackInvisible;
    }

    @Override
    public void setAttackInvisible(boolean attack) {
        this.attackInvisible = attack;
    }

    @Override
    public int getWanderingRange() {
        return this.walkingRange;
    }

    @Override
    public void setWanderingRange(int range) {
        if (range < 1 || range > 50) {
            throw new CustomNPCsException("Bad wandering range: " + range, new Object[0]);
        }
        this.walkingRange = range;
    }

    @Override
    public boolean getInteractWithNPCs() {
        return this.npcInteracting;
    }

    @Override
    public void setInteractWithNPCs(boolean interact) {
        this.npcInteracting = interact;
    }

    @Override
    public boolean getStopOnInteract() {
        return this.stopAndInteract;
    }

    @Override
    public void setStopOnInteract(boolean stopOnInteract) {
        this.stopAndInteract = stopOnInteract;
    }

    @Override
    public int getWalkingSpeed() {
        return this.moveSpeed;
    }

    @Override
    public void setWalkingSpeed(int speed) {
        if (speed < 0 || speed > 10) {
            throw new CustomNPCsException("Wrong speed: " + speed, new Object[0]);
        }
        this.moveSpeed = speed;
        this.npc.func_110148_a(SharedMonsterAttributes.field_111263_d).func_111128_a((double)this.npc.getSpeed());
        this.npc.func_110148_a(SharedMonsterAttributes.field_193334_e).func_111128_a((double)(this.npc.getSpeed() * 2.0f));
    }

    @Override
    public int getMovingPathType() {
        return this.movingPattern;
    }

    @Override
    public boolean getMovingPathPauses() {
        return this.movingPause;
    }

    @Override
    public void setMovingPathType(int type, boolean pauses) {
        if (type < 0 && type > 1) {
            throw new CustomNPCsException("Moving path type: " + type, new Object[0]);
        }
        this.movingPattern = type;
        this.movingPause = pauses;
    }

    @Override
    public int getDoorInteract() {
        return this.doorInteract;
    }

    @Override
    public void setDoorInteract(int type) {
        this.doorInteract = type;
        this.npc.updateAI = true;
    }

    @Override
    public boolean getCanSwim() {
        return this.canSwim;
    }

    @Override
    public void setCanSwim(boolean canSwim) {
        this.canSwim = canSwim;
    }

    @Override
    public int getSheltersFrom() {
        return this.findShelter;
    }

    @Override
    public void setSheltersFrom(int type) {
        this.findShelter = type;
        this.npc.updateAI = true;
    }

    @Override
    public boolean getAttackLOS() {
        return this.directLOS;
    }

    @Override
    public void setAttackLOS(boolean enabled) {
        this.directLOS = enabled;
        this.npc.updateAI = true;
    }

    @Override
    public boolean getAvoidsWater() {
        return this.avoidsWater;
    }

    @Override
    public void setAvoidsWater(boolean enabled) {
        if (this.npc.func_70661_as() instanceof PathNavigateGround) {
            this.npc.func_184644_a(PathNodeType.WATER, enabled ? PathNodeType.WATER.func_186289_a() : 0.0f);
        }
        this.avoidsWater = enabled;
    }

    @Override
    public boolean getLeapAtTarget() {
        return this.canLeap;
    }

    @Override
    public void setLeapAtTarget(boolean leap) {
        this.canLeap = leap;
        this.npc.updateAI = true;
    }

    @Override
    public int getTacticalType() {
        return this.tacticalVariant;
    }

    @Override
    public void setTacticalType(int type) {
        this.tacticalVariant = type;
        this.npc.updateAI = true;
    }

    @Override
    public int getTacticalRange() {
        return this.tacticalRadius;
    }

    @Override
    public void setTacticalRange(int range) {
        this.tacticalRadius = range;
    }

    @Override
    public int getNavigationType() {
        return this.movementType;
    }

    @Override
    public void setNavigationType(int type) {
        this.movementType = type;
    }
}

