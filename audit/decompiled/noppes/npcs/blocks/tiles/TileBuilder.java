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

    public void func_145839_a(NBTTagCompound compound) {
        super.func_145839_a(compound);
        if (compound.func_74764_b("SchematicName")) {
            this.schematic = SchematicController.Instance.load(compound.func_74779_i("SchematicName"));
        }
        Stack<Integer> positions = new Stack<Integer>();
        positions.addAll(NBTTags.getIntegerList(compound.func_150295_c("Positions", 10)));
        this.positions = positions;
        positions = new Stack();
        positions.addAll(NBTTags.getIntegerList(compound.func_150295_c("PositionsSecond", 10)));
        this.positionsSecond = positions;
        this.readPartNBT(compound);
    }

    public void readPartNBT(NBTTagCompound compound) {
        this.rotation = compound.func_74762_e("Rotation");
        this.yOffest = compound.func_74762_e("YOffset");
        this.enabled = compound.func_74767_n("Enabled");
        this.started = compound.func_74767_n("Started");
        this.finished = compound.func_74767_n("Finished");
        this.availability.readFromNBT(compound.func_74775_l("Availability"));
    }

    public NBTTagCompound func_189515_b(NBTTagCompound compound) {
        super.func_189515_b(compound);
        if (this.schematic != null) {
            compound.func_74778_a("SchematicName", this.schematic.schema.getName());
        }
        compound.func_74782_a("Positions", (NBTBase)NBTTags.nbtIntegerCollection(new ArrayList<Integer>(this.positions)));
        compound.func_74782_a("PositionsSecond", (NBTBase)NBTTags.nbtIntegerCollection(new ArrayList<Integer>(this.positionsSecond)));
        this.writePartNBT(compound);
        return compound;
    }

    public NBTTagCompound writePartNBT(NBTTagCompound compound) {
        compound.func_74768_a("Rotation", this.rotation);
        compound.func_74768_a("YOffset", this.yOffest);
        compound.func_74757_a("Enabled", this.enabled);
        compound.func_74757_a("Started", this.started);
        compound.func_74757_a("Finished", this.finished);
        compound.func_74782_a("Availability", (NBTBase)this.availability.writeToNBT(new NBTTagCompound()));
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

    public void func_73660_a() {
        if (this.field_145850_b.field_72995_K || !this.hasSchematic() || this.finished) {
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
        List list = this.field_145850_b.func_72872_a(EntityNPCInterface.class, new AxisAlignedBB(this.func_174877_v(), this.func_174877_v()).func_72314_b(32.0, 32.0, 32.0));
        for (EntityNPCInterface npc : list) {
            if (npc.advanced.job != 10) continue;
            JobBuilder job = (JobBuilder)npc.jobInterface;
            if (job.build != null) continue;
            job.build = this;
        }
    }

    private List<EntityPlayer> getPlayerList() {
        return this.field_145850_b.func_72872_a(EntityPlayer.class, new AxisAlignedBB((double)this.field_174879_c.func_177958_n(), (double)this.field_174879_c.func_177956_o(), (double)this.field_174879_c.func_177952_p(), (double)(this.field_174879_c.func_177958_n() + 1), (double)(this.field_174879_c.func_177956_o() + 1), (double)(this.field_174879_c.func_177952_p() + 1)).func_72314_b(10.0, 10.0, 10.0));
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
            if (!state.func_185913_b() && !bo && state.func_177230_c() != Blocks.field_150350_a) {
                this.positionsSecond.add(0, pos);
                continue;
            }
            BlockPos blockPos = this.func_174877_v().func_177982_a(1, this.yOffest, 1).func_177971_a((Vec3i)this.schematic.rotatePos(x, y, z, this.rotation));
            IBlockState original = this.field_145850_b.func_180495_p(blockPos);
            if (Block.func_176210_f((IBlockState)state) == Block.func_176210_f((IBlockState)original)) continue;
            state = this.schematic.rotationState(state, this.rotation);
            NBTTagCompound tile = null;
            if (state.func_177230_c() instanceof ITileEntityProvider) {
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

