/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package noppes.npcs.schematics;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.schematics.BlueprintUtil;
import noppes.npcs.schematics.ISchematic;

public class Blueprint
implements ISchematic {
    private List<String> requiredMods;
    private short sizeX;
    private short sizeY;
    private short sizeZ;
    private short palleteSize;
    private IBlockState[] pallete;
    private String name;
    private String[] architects;
    private short[][][] structure;
    private NBTTagCompound[] tileEntities;

    public Blueprint(short sizeX, short sizeY, short sizeZ, short palleteSize, IBlockState[] pallete, short[][][] structure, NBTTagCompound[] tileEntities, List<String> requiredMods) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palleteSize = palleteSize;
        this.pallete = pallete;
        this.structure = structure;
        this.tileEntities = tileEntities;
        this.requiredMods = requiredMods;
    }

    public void build(World world, BlockPos pos) {
        IBlockState state;
        short x;
        short z;
        short y;
        IBlockState[] pallete = this.getPallete();
        short[][][] structure = this.getStructure();
        for (y = 0; y < this.getSizeY(); y = (short)(y + 1)) {
            for (z = 0; z < this.getSizeZ(); z = (short)(z + 1)) {
                for (x = 0; x < this.getSizeX(); x = (short)(x + 1)) {
                    state = pallete[structure[y][z][x] & 0xFFFF];
                    if (state.func_177230_c() == Blocks.field_189881_dj || !state.func_185917_h()) continue;
                    world.func_180501_a(pos.func_177982_a((int)x, (int)y, (int)z), state, 2);
                }
            }
        }
        for (y = 0; y < this.getSizeY(); y = (short)(y + 1)) {
            for (z = 0; z < this.getSizeZ(); z = (short)(z + 1)) {
                for (x = 0; x < this.getSizeX(); x = (short)(x + 1)) {
                    state = pallete[structure[y][z][x]];
                    if (state.func_177230_c() == Blocks.field_189881_dj || state.func_185917_h()) continue;
                    world.func_180501_a(pos.func_177982_a((int)x, (int)y, (int)z), state, 2);
                }
            }
        }
        if (this.getTileEntities() != null) {
            for (NBTTagCompound tag : this.getTileEntities()) {
                TileEntity te = world.func_175625_s(pos.func_177982_a((int)tag.func_74765_d("x"), (int)tag.func_74765_d("y"), (int)tag.func_74765_d("z")));
                tag.func_74768_a("x", pos.func_177958_n() + tag.func_74765_d("x"));
                tag.func_74768_a("y", pos.func_177956_o() + tag.func_74765_d("y"));
                tag.func_74768_a("z", pos.func_177952_p() + tag.func_74765_d("z"));
                te.deserializeNBT(tag);
            }
        }
    }

    public short getSizeX() {
        return this.sizeX;
    }

    public short getSizeY() {
        return this.sizeY;
    }

    public short getSizeZ() {
        return this.sizeZ;
    }

    public short getPalleteSize() {
        return this.palleteSize;
    }

    public IBlockState[] getPallete() {
        return this.pallete;
    }

    public short[][][] getStructure() {
        return this.structure;
    }

    public NBTTagCompound[] getTileEntities() {
        return this.tileEntities;
    }

    public List<String> getRequiredMods() {
        return this.requiredMods;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getArchitects() {
        return this.architects;
    }

    public void setArchitects(String[] architects) {
        this.architects = architects;
    }

    @Override
    public short getWidth() {
        return this.getSizeX();
    }

    @Override
    public short getHeight() {
        return this.getSizeZ();
    }

    @Override
    public short getLength() {
        return this.getSizeY();
    }

    @Override
    public int getTileEntitySize() {
        return this.tileEntities.length;
    }

    @Override
    public NBTTagCompound getTileEntity(int i) {
        return this.tileEntities[i];
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z) {
        return this.pallete[this.structure[y][z][x]];
    }

    @Override
    public IBlockState getBlockState(int i) {
        int x = i % this.getWidth();
        int z = (i - x) / this.getWidth() % this.getLength();
        int y = ((i - x) / this.getWidth() - z) / this.getLength();
        return this.getBlockState(x, y, z);
    }

    @Override
    public NBTTagCompound getNBT() {
        return BlueprintUtil.writeBlueprintToNBT(this);
    }
}

