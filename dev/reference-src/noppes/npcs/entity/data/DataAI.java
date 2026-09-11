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
        this.canSwim = compound.getBoolean("CanSwim");
        this.reactsToFire = compound.getBoolean("ReactsToFire");
        this.setAvoidsWater(compound.getBoolean("AvoidsWater"));
        this.avoidsSun = compound.getBoolean("AvoidsSun");
        this.returnToStart = compound.getBoolean("ReturnToStart");
        this.onAttack = compound.getInteger("OnAttack");
        this.doorInteract = compound.getInteger("DoorInteract");
        this.findShelter = compound.getInteger("FindShelter");
        this.directLOS = compound.getBoolean("DirectLOS");
        this.canLeap = compound.getBoolean("CanLeap");
        this.canSprint = compound.getBoolean("CanSprint");
        this.tacticalRadius = compound.getInteger("TacticalRadius");
        this.movingPause = compound.getBoolean("MovingPause");
        this.npcInteracting = compound.getBoolean("npcInteracting");
        this.stopAndInteract = compound.getBoolean("stopAndInteract");
        this.movementType = compound.getInteger("MovementType");
        this.animationType = compound.getInteger("MoveState");
        this.standingType = compound.getInteger("StandingState");
        this.movingType = compound.getInteger("MovingState");
        this.tacticalVariant = compound.getInteger("TacticalVariant");
        this.orientation = compound.getInteger("Orientation");
        this.bodyOffsetY = compound.getFloat("PositionOffsetY");
        this.bodyOffsetZ = compound.getFloat("PositionOffsetZ");
        this.bodyOffsetX = compound.getFloat("PositionOffsetX");
        this.walkingRange = compound.getInteger("WalkingRange");
        this.setWalkingSpeed(compound.getInteger("MoveSpeed"));
        this.setMovingPath(NBTTags.getIntegerArraySet(compound.getTagList("MovingPathNew", 10)));
        this.movingPos = compound.getInteger("MovingPos");
        this.movingPattern = compound.getInteger("MovingPatern");
        this.attackInvisible = compound.getBoolean("AttackInvisible");
        if (compound.hasKey("StartPosNew")) {
            int[] startPos = compound.getIntArray("StartPosNew");
            this.startPos = new BlockPos(startPos[0], startPos[1], startPos[2]);
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("CanSwim", this.canSwim);
        compound.setBoolean("ReactsToFire", this.reactsToFire);
        compound.setBoolean("AvoidsWater", this.avoidsWater);
        compound.setBoolean("AvoidsSun", this.avoidsSun);
        compound.setBoolean("ReturnToStart", this.returnToStart);
        compound.setInteger("OnAttack", this.onAttack);
        compound.setInteger("DoorInteract", this.doorInteract);
        compound.setInteger("FindShelter", this.findShelter);
        compound.setBoolean("DirectLOS", this.directLOS);
        compound.setBoolean("CanLeap", this.canLeap);
        compound.setBoolean("CanSprint", this.canSprint);
        compound.setInteger("TacticalRadius", this.tacticalRadius);
        compound.setBoolean("MovingPause", this.movingPause);
        compound.setBoolean("npcInteracting", this.npcInteracting);
        compound.setBoolean("stopAndInteract", this.stopAndInteract);
        compound.setInteger("MoveState", this.animationType);
        compound.setInteger("StandingState", this.standingType);
        compound.setInteger("MovingState", this.movingType);
        compound.setInteger("TacticalVariant", this.tacticalVariant);
        compound.setInteger("MovementType", this.movementType);
        compound.setInteger("Orientation", this.orientation);
        compound.setFloat("PositionOffsetX", this.bodyOffsetX);
        compound.setFloat("PositionOffsetY", this.bodyOffsetY);
        compound.setFloat("PositionOffsetZ", this.bodyOffsetZ);
        compound.setInteger("WalkingRange", this.walkingRange);
        compound.setInteger("MoveSpeed", this.moveSpeed);
        compound.setTag("MovingPathNew", (NBTBase)NBTTags.nbtIntegerArraySet(this.movingPath));
        compound.setInteger("MovingPos", this.movingPos);
        compound.setInteger("MovingPatern", this.movingPattern);
        this.setAvoidsWater(this.avoidsWater);
        compound.setIntArray("StartPosNew", this.getStartArray());
        compound.setBoolean("AttackInvisible", this.attackInvisible);
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
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
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
        return this.npc.getDistanceSq((double)pos[0] + 0.5, pos[1], (double)pos[2] + 0.5);
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
        this.npc.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue((double)this.npc.getSpeed());
        this.npc.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue((double)(this.npc.getSpeed() * 2.0f));
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
        if (this.npc.getNavigator() instanceof PathNavigateGround) {
            this.npc.setPathPriority(PathNodeType.WATER, enabled ? PathNodeType.WATER.getPriority() : 0.0f);
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

