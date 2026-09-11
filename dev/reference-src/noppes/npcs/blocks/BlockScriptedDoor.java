/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.BlockDoor$EnumDoorHalf
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumBlockRenderType
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.Explosion
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.BlockNpcDoorInterface;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.util.IPermission;

public class BlockScriptedDoor
extends BlockNpcDoorInterface
implements IPermission {
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileScriptedDoor();
    }

    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        IBlockState iblockstate1;
        if (world.isRemote) {
            return true;
        }
        BlockPos blockpos1 = state.getValue((IProperty)HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
        IBlockState iBlockState = iblockstate1 = pos.equals((Object)blockpos1) ? state : world.getBlockState(blockpos1);
        if (iblockstate1.getBlock() != this) {
            return false;
        }
        ItemStack currentItem = player.inventory.getCurrentItem();
        if (currentItem != null && (currentItem.getItem() == CustomItems.wand || currentItem.getItem() == CustomItems.scripter || currentItem.getItem() == CustomItems.scriptedDoorTool)) {
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.ScriptDoor, null, blockpos1.getX(), blockpos1.getY(), blockpos1.getZ());
            return true;
        }
        TileScriptedDoor tile = (TileScriptedDoor)world.getTileEntity(blockpos1);
        if (EventHooks.onScriptBlockInteract(tile, player, side.getIndex(), hitX, hitY, hitZ)) {
            return false;
        }
        this.toggleDoor(world, blockpos1, ((Boolean)iblockstate1.getValue((IProperty)BlockDoor.OPEN)).equals(false));
        return true;
    }

    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block neighborBlock, BlockPos pos2) {
        if (state.getValue((IProperty)HALF) == BlockDoor.EnumDoorHalf.UPPER) {
            BlockPos blockpos1 = pos.down();
            IBlockState iblockstate1 = worldIn.getBlockState(blockpos1);
            if (iblockstate1.getBlock() != this) {
                worldIn.setBlockToAir(pos);
            } else if (neighborBlock != this) {
                this.neighborChanged(iblockstate1, worldIn, blockpos1, neighborBlock, blockpos1);
            }
        } else {
            BlockPos blockpos2 = pos.up();
            IBlockState iblockstate2 = worldIn.getBlockState(blockpos2);
            if (iblockstate2.getBlock() != this) {
                worldIn.setBlockToAir(pos);
            } else {
                boolean flag;
                TileScriptedDoor tile = (TileScriptedDoor)worldIn.getTileEntity(pos);
                if (!worldIn.isRemote) {
                    EventHooks.onScriptBlockNeighborChanged(tile, pos2);
                }
                boolean bl = flag = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(blockpos2);
                if ((flag || neighborBlock.getDefaultState().canProvidePower()) && neighborBlock != this && flag != (Boolean)iblockstate2.getValue((IProperty)POWERED)) {
                    worldIn.setBlockState(blockpos2, iblockstate2.withProperty((IProperty)POWERED, (Comparable)Boolean.valueOf(flag)), 2);
                    if (flag != (Boolean)state.getValue((IProperty)OPEN)) {
                        this.toggleDoor(worldIn, pos, flag);
                    }
                }
                int power = 0;
                for (EnumFacing enumfacing : EnumFacing.values()) {
                    int p = worldIn.getRedstonePower(pos.offset(enumfacing), enumfacing);
                    if (p <= power) continue;
                    power = p;
                }
                tile.newPower = power;
            }
        }
    }

    public void toggleDoor(World worldIn, BlockPos pos, boolean open) {
        TileScriptedDoor tile = (TileScriptedDoor)worldIn.getTileEntity(pos);
        if (EventHooks.onScriptBlockDoorToggle(tile)) {
            return;
        }
        super.toggleDoor(worldIn, pos, open);
    }

    public void onBlockClicked(World world, BlockPos pos, EntityPlayer playerIn) {
        IBlockState iblockstate1;
        if (world.isRemote) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        BlockPos blockpos1 = state.getValue((IProperty)HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
        IBlockState iBlockState = iblockstate1 = pos.equals((Object)blockpos1) ? state : world.getBlockState(blockpos1);
        if (iblockstate1.getBlock() != this) {
            return;
        }
        TileScriptedDoor tile = (TileScriptedDoor)world.getTileEntity(blockpos1);
        EventHooks.onScriptBlockClicked(tile, playerIn);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        IBlockState iblockstate1;
        BlockPos blockpos1 = state.getValue((IProperty)HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
        IBlockState iBlockState = iblockstate1 = pos.equals((Object)blockpos1) ? state : world.getBlockState(blockpos1);
        if (!world.isRemote && iblockstate1.getBlock() == this) {
            TileScriptedDoor tile = (TileScriptedDoor)world.getTileEntity(pos);
            EventHooks.onScriptBlockBreak(tile);
        }
        super.breakBlock(world, pos, state);
    }

    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        TileScriptedDoor tile;
        if (!world.isRemote && EventHooks.onScriptBlockHarvest(tile = (TileScriptedDoor)world.getTileEntity(pos), player)) {
            return false;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entityIn) {
        if (world.isRemote) {
            return;
        }
        TileScriptedDoor tile = (TileScriptedDoor)world.getTileEntity(pos);
        EventHooks.onScriptBlockCollide(tile, entityIn);
    }

    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        IBlockState iblockstate1;
        BlockPos blockpos1 = state.getValue((IProperty)HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
        IBlockState iBlockState = iblockstate1 = pos.equals((Object)blockpos1) ? state : world.getBlockState(blockpos1);
        if (player.capabilities.isCreativeMode && iblockstate1.getValue((IProperty)HALF) == BlockDoor.EnumDoorHalf.LOWER && iblockstate1.getBlock() == this) {
            world.setBlockToAir(blockpos1);
        }
    }

    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return ((TileScriptedDoor)world.getTileEntity((BlockPos)pos)).blockHardness;
    }

    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        return ((TileScriptedDoor)world.getTileEntity((BlockPos)pos)).blockResistance;
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.ScriptDoorDataSave;
    }
}

