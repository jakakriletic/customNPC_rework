/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.ITileEntityProvider
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ITickable
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3i
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.blocks.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.NBTTags;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.BlockData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobBuilder;
import noppes.npcs.schematics.SchematicWrapper;

public class TileBuilder
extends TileEntity
implements ITickable {
    private SchematicWrapper schematic = null;
    public int rotation = 0;
    public int yOffest = 0;
    public boolean enabled = false;
    public boolean started = false;
    public boolean finished = false;
    public Availability availability = new Availability();
    private Stack<Integer> positions = new Stack();
    private Stack<Integer> positionsSecond = new Stack();
    public static BlockPos DrawPos = null;
    public static boolean Compiled = false;
    private int ticks = 20;

    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("SchematicName")) {
            this.schematic = SchematicController.Instance.load(compound.getString("SchematicName"));
        }
        Stack<Integer> positions = new Stack<Integer>();
        positions.addAll(NBTTags.getIntegerList(compound.getTagList("Positions", 10)));
        this.positions = positions;
        positions = new Stack();
        positions.addAll(NBTTags.getIntegerList(compound.getTagList("PositionsSecond", 10)));
        this.positionsSecond = positions;
        this.readPartNBT(compound);
    }

    public void readPartNBT(NBTTagCompound compound) {
        this.rotation = compound.getInteger("Rotation");
        this.yOffest = compound.getInteger("YOffset");
        this.enabled = compound.getBoolean("Enabled");
        this.started = compound.getBoolean("Started");
        this.finished = compound.getBoolean("Finished");
        this.availability.readFromNBT(compound.getCompoundTag("Availability"));
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (this.schematic != null) {
            compound.setString("SchematicName", this.schematic.schema.getName());
        }
        compound.setTag("Positions", (NBTBase)NBTTags.nbtIntegerCollection(new ArrayList<Integer>(this.positions)));
        compound.setTag("PositionsSecond", (NBTBase)NBTTags.nbtIntegerCollection(new ArrayList<Integer>(this.positionsSecond)));
        this.writePartNBT(compound);
        return compound;
    }

    public NBTTagCompound writePartNBT(NBTTagCompound compound) {
        compound.setInteger("Rotation", this.rotation);
        compound.setInteger("YOffset", this.yOffest);
        compound.setBoolean("Enabled", this.enabled);
        compound.setBoolean("Started", this.started);
        compound.setBoolean("Finished", this.finished);
        compound.setTag("Availability", (NBTBase)this.availability.writeToNBT(new NBTTagCompound()));
        return compound;
    }

    @SideOnly(value=Side.CLIENT)
    public void setDrawSchematic(SchematicWrapper schematics) {
        this.schematic = schematics;
    }

    public void setSchematic(SchematicWrapper schematics) {
        this.schematic = schematics;
        if (schematics == null) {
            this.positions.clear();
            this.positionsSecond.clear();
            return;
        }
        Stack<Integer> positions = new Stack<Integer>();
        for (int y = 0; y < schematics.schema.getHeight(); ++y) {
            int x;
            int z;
            for (z = 0; z < schematics.schema.getLength() / 2; ++z) {
                for (x = 0; x < schematics.schema.getWidth() / 2; ++x) {
                    positions.add(0, this.xyzToIndex(x, y, z));
                }
            }
            for (z = 0; z < schematics.schema.getLength() / 2; ++z) {
                for (x = schematics.schema.getWidth() / 2; x < schematics.schema.getWidth(); ++x) {
                    positions.add(0, this.xyzToIndex(x, y, z));
                }
            }
            for (z = schematics.schema.getLength() / 2; z < schematics.schema.getLength(); ++z) {
                for (x = 0; x < schematics.schema.getWidth() / 2; ++x) {
                    positions.add(0, this.xyzToIndex(x, y, z));
                }
            }
            for (z = schematics.schema.getLength() / 2; z < schematics.schema.getLength(); ++z) {
                for (x = schematics.schema.getWidth() / 2; x < schematics.schema.getWidth(); ++x) {
                    positions.add(0, this.xyzToIndex(x, y, z));
                }
            }
        }
        this.positions = positions;
        this.positionsSecond.clear();
    }

    public int xyzToIndex(int x, int y, int z) {
        return (y * this.schematic.schema.getLength() + z) * this.schematic.schema.getWidth() + x;
    }

    public SchematicWrapper getSchematic() {
        return this.schematic;
    }

    public boolean hasSchematic() {
        return this.schematic != null;
    }

    public void update() {
        if (this.world.isRemote || !this.hasSchematic() || this.finished) {
            return;
        }
        --this.ticks;
        if (this.ticks > 0) {
            return;
        }
        this.ticks = 200;
        if (this.positions.isEmpty() && this.positionsSecond.isEmpty()) {
            this.finished = true;
            return;
        }
        if (!this.started) {
            for (EntityPlayer player : this.getPlayerList()) {
                if (!this.availability.isAvailable(player)) continue;
                this.started = true;
                break;
            }
            if (!this.started) {
                return;
            }
        }
        List list = this.world.getEntitiesWithinAABB(EntityNPCInterface.class, new AxisAlignedBB(this.getPos(), this.getPos()).grow(32.0, 32.0, 32.0));
        for (EntityNPCInterface npc : list) {
            if (npc.advanced.job != 10) continue;
            JobBuilder job = (JobBuilder)npc.jobInterface;
            if (job.build != null) continue;
            job.build = this;
        }
    }

    private List<EntityPlayer> getPlayerList() {
        return this.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB((double)this.pos.getX(), (double)this.pos.getY(), (double)this.pos.getZ(), (double)(this.pos.getX() + 1), (double)(this.pos.getY() + 1), (double)(this.pos.getZ() + 1)).grow(10.0, 10.0, 10.0));
    }

    public Stack<BlockData> getBlock() {
        if (!this.enabled || this.finished || !this.hasSchematic()) {
            return null;
        }
        boolean bo = this.positions.isEmpty();
        Stack<BlockData> list = new Stack<BlockData>();
        int size = this.schematic.schema.getWidth() * this.schematic.schema.getLength() / 4;
        if (size > 30) {
            size = 30;
        }
        for (int i = 0; i < size; ++i) {
            int pos;
            if (this.positions.isEmpty() && !bo || this.positionsSecond.isEmpty() && bo) {
                return list;
            }
            int n = pos = bo ? this.positionsSecond.pop().intValue() : this.positions.pop().intValue();
            if (pos >= this.schematic.size) continue;
            int x = pos % this.schematic.schema.getWidth();
            int z = (pos - x) / this.schematic.schema.getWidth() % this.schematic.schema.getLength();
            int y = ((pos - x) / this.schematic.schema.getWidth() - z) / this.schematic.schema.getLength();
            IBlockState state = this.schematic.schema.getBlockState(x, y, z);
            if (!state.isFullBlock() && !bo && state.getBlock() != Blocks.AIR) {
                this.positionsSecond.add(0, pos);
                continue;
            }
            BlockPos blockPos = this.getPos().add(1, this.yOffest, 1).add((Vec3i)this.schematic.rotatePos(x, y, z, this.rotation));
            IBlockState original = this.world.getBlockState(blockPos);
            if (Block.getStateId((IBlockState)state) == Block.getStateId((IBlockState)original)) continue;
            state = this.schematic.rotationState(state, this.rotation);
            NBTTagCompound tile = null;
            if (state.getBlock() instanceof ITileEntityProvider) {
                tile = this.schematic.getTileEntity(x, y, z, blockPos);
            }
            list.add(0, new BlockData(blockPos, state, tile));
        }
        return list;
    }

    public static void SetDrawPos(BlockPos pos) {
        DrawPos = pos;
        Compiled = false;
    }
}

