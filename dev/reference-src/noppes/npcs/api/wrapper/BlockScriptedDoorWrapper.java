/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package noppes.npcs.api.wrapper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.api.ITimers;
import noppes.npcs.api.block.IBlockScriptedDoor;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.blocks.tiles.TileScriptedDoor;

public class BlockScriptedDoorWrapper
extends BlockWrapper
implements IBlockScriptedDoor {
    private TileScriptedDoor tile;

    public BlockScriptedDoorWrapper(World world, Block block, BlockPos pos) {
        super(world, block, pos);
        this.tile = (TileScriptedDoor)((BlockWrapper)this).tile;
    }

    @Override
    public boolean getOpen() {
        IBlockState state = this.world.getMCWorld().getBlockState(this.pos);
        return ((Boolean)state.getValue((IProperty)BlockDoor.OPEN)).equals(true);
    }

    @Override
    public void setOpen(boolean open) {
        if (this.getOpen() == open || this.isRemoved()) {
            return;
        }
        IBlockState state = this.world.getMCWorld().getBlockState(this.pos);
        ((BlockDoor)this.block).toggleDoor((World)this.world.getMCWorld(), this.pos, open);
    }

    @Override
    public void setBlockModel(String name) {
        Block b = null;
        if (name != null) {
            b = Block.getBlockFromName((String)name);
        }
        this.tile.setItemModel(b);
    }

    @Override
    public String getBlockModel() {
        return Block.REGISTRY.getNameForObject((Object)this.tile.blockModel) + "";
    }

    @Override
    public ITimers getTimers() {
        return this.tile.timers;
    }

    @Override
    public float getHardness() {
        return this.tile.blockHardness;
    }

    @Override
    public void setHardness(float hardness) {
        this.tile.blockHardness = hardness;
    }

    @Override
    public float getResistance() {
        return this.tile.blockResistance;
    }

    @Override
    public void setResistance(float resistance) {
        this.tile.blockResistance = resistance;
    }

    @Override
    protected void setTile(TileEntity tile) {
        this.tile = (TileScriptedDoor)tile;
        super.setTile(tile);
    }
}

