/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.ITileEntityProvider
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package noppes.npcs.schematics;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.schematics.ISchematic;

public class Schematic
implements ISchematic {
    public String name;
    public short width;
    public short height;
    public short length;
    private NBTTagList entityList;
    public NBTTagList tileList;
    public short[] blockArray;
    public byte[] blockDataArray;

    public Schematic(String name) {
        this.name = name;
    }

    public void load(NBTTagCompound compound) {
        this.width = compound.func_74765_d("Width");
        this.height = compound.func_74765_d("Height");
        this.length = compound.func_74765_d("Length");
        byte[] addId = compound.func_74764_b("AddBlocks") ? compound.func_74770_j("AddBlocks") : new byte[]{};
        this.setBlockBytes(compound.func_74770_j("Blocks"), addId);
        this.blockDataArray = compound.func_74770_j("Data");
        this.entityList = compound.func_150295_c("Entities", 10);
        this.tileList = compound.func_150295_c("TileEntities", 10);
    }

    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74777_a("Width", this.width);
        compound.func_74777_a("Height", this.height);
        compound.func_74777_a("Length", this.length);
        byte[][] arr = this.getBlockBytes();
        compound.func_74773_a("Blocks", arr[0]);
        if (arr.length > 1) {
            compound.func_74773_a("AddBlocks", arr[1]);
        }
        compound.func_74773_a("Data", this.blockDataArray);
        compound.func_74782_a("TileEntities", (NBTBase)this.tileList);
        return compound;
    }

    public void setBlockBytes(byte[] blockId, byte[] addId) {
        this.blockArray = new short[blockId.length];
        for (int index = 0; index < blockId.length; ++index) {
            short id = (short)(blockId[index] & 0xFF);
            if (index >> 1 < addId.length) {
                id = (index & 1) == 0 ? (short)(id + (short)((addId[index >> 1] & 0xF) << 8)) : (short)(id + (short)((addId[index >> 1] & 0xF0) << 4));
            }
            this.blockArray[index] = id;
        }
    }

    public byte[][] getBlockBytes() {
        byte[] blocks = new byte[this.blockArray.length];
        byte[] addBlocks = null;
        for (int i = 0; i < blocks.length; ++i) {
            short id = this.blockArray[i];
            if (id > 255) {
                if (addBlocks == null) {
                    addBlocks = new byte[(blocks.length >> 1) + 1];
                }
                addBlocks[i >> 1] = (i & 1) == 0 ? (byte)(addBlocks[i >> 1] & 0xF0 | id >> 8 & 0xF) : (byte)(addBlocks[i >> 1] & 0xF | (id >> 8 & 0xF) << 4);
            }
            blocks[i] = (byte)id;
        }
        if (addBlocks == null) {
            return new byte[][]{blocks};
        }
        return new byte[][]{blocks, addBlocks};
    }

    public int xyzToIndex(int x, int y, int z) {
        return (y * this.length + z) * this.width + x;
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z) {
        int i = this.xyzToIndex(x, y, z);
        Block b = Block.func_149729_e((int)this.blockArray[i]);
        if (b == null) {
            return Blocks.field_150350_a.func_176223_P();
        }
        return b.func_176203_a((int)this.blockDataArray[i]);
    }

    @Override
    public IBlockState getBlockState(int i) {
        Block b = Block.func_149729_e((int)this.blockArray[i]);
        if (b == null) {
            return Blocks.field_150350_a.func_176223_P();
        }
        return b.func_176203_a((int)this.blockDataArray[i]);
    }

    @Override
    public short getWidth() {
        return this.width;
    }

    @Override
    public short getHeight() {
        return this.height;
    }

    @Override
    public short getLength() {
        return this.length;
    }

    @Override
    public int getTileEntitySize() {
        if (this.entityList == null) {
            return 0;
        }
        return this.entityList.func_74745_c();
    }

    @Override
    public NBTTagCompound getTileEntity(int i) {
        return this.entityList.func_150305_b(i);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public static Schematic Create(World world, String name, BlockPos pos, short height, short width, short length) {
        Schematic schema = new Schematic(name);
        schema.height = height;
        schema.width = width;
        schema.length = length;
        int size = height * width * length;
        schema.blockArray = new short[size];
        schema.blockDataArray = new byte[size];
        NoppesUtilServer.NotifyOPs("Creating schematic at: " + pos + " might lag slightly", new Object[0]);
        schema.tileList = new NBTTagList();
        for (int i = 0; i < size; ++i) {
            int x = i % width;
            int z = (i - x) / width % length;
            int y = ((i - x) / width - z) / length;
            IBlockState state = world.func_180495_p(pos.func_177982_a(x, y, z));
            if (state.func_177230_c() == Blocks.field_150350_a || state.func_177230_c() == CustomItems.copy) continue;
            schema.blockArray[i] = (short)Block.field_149771_c.func_148757_b((Object)state.func_177230_c());
            schema.blockDataArray[i] = (byte)state.func_177230_c().func_176201_c(state);
            if (!(state.func_177230_c() instanceof ITileEntityProvider)) continue;
            TileEntity tile = world.func_175625_s(pos.func_177982_a(x, y, z));
            NBTTagCompound compound = new NBTTagCompound();
            tile.func_189515_b(compound);
            compound.func_74768_a("x", x);
            compound.func_74768_a("y", y);
            compound.func_74768_a("z", z);
            schema.tileList.func_74742_a((NBTBase)compound);
        }
        return schema;
    }
}

