/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.fluids.BlockFluidBase
 */
package noppes.npcs.api.wrapper;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import noppes.npcs.api.block.IBlockFluidContainer;
import noppes.npcs.api.wrapper.BlockWrapper;

public class BlockFluidContainerWrapper
extends BlockWrapper
implements IBlockFluidContainer {
    private BlockFluidBase block;

    public BlockFluidContainerWrapper(World world, Block block, BlockPos pos) {
        super(world, block, pos);
        this.block = (BlockFluidBase)block;
    }

    @Override
    public float getFluidPercentage() {
        return this.block.getFilledPercentage((World)this.world.getMCWorld(), this.pos);
    }

    @Override
    public float getFuildDensity() {
        return BlockFluidBase.getDensity((IBlockAccess)this.world.getMCWorld(), (BlockPos)this.pos);
    }

    @Override
    public float getFuildTemperature() {
        return BlockFluidBase.getTemperature((IBlockAccess)this.world.getMCWorld(), (BlockPos)this.pos);
    }

    @Override
    public float getFluidValue() {
        return this.block.getQuantaValue((IBlockAccess)this.world.getMCWorld(), this.pos);
    }

    @Override
    public String getFluidName() {
        return this.block.getFluid().getName();
    }
}

