/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.SoundType
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving$SpawnPlacementType
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.Explosion
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.BlockInterface;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.util.IPermission;

public class BlockScripted
extends BlockInterface
implements IPermission {
    public static final AxisAlignedBB AABB = new AxisAlignedBB((double)0.001f, (double)0.001f, (double)0.001f, (double)0.998f, (double)0.998f, (double)0.998f);
    public static final AxisAlignedBB AABB_EMPTY = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    public BlockScripted() {
        super(Material.ROCK);
        this.setSoundType(SoundType.STONE);
    }

    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileScripted();
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return AABB;
    }

    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess world, BlockPos pos) {
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        if (tile != null && tile.isPassible) {
            return AABB_EMPTY;
        }
        return AABB;
    }

    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        ItemStack currentItem = player.inventory.getCurrentItem();
        if (currentItem != null && (currentItem.getItem() == CustomItems.wand || currentItem.getItem() == CustomItems.scripter)) {
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.ScriptBlock, null, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        return !EventHooks.onScriptBlockInteract(tile, player, side.getIndex(), hitX, hitY, hitZ);
    }

    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
        if (entity instanceof EntityPlayer && !world.isRemote) {
            NoppesUtilServer.sendOpenGui((EntityPlayer)entity, EnumGuiType.ScriptBlock, null, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entityIn) {
        if (world.isRemote) {
            return;
        }
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        EventHooks.onScriptBlockCollide(tile, entityIn);
    }

    public void fillWithRain(World world, BlockPos pos) {
        if (world.isRemote) {
            return;
        }
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        EventHooks.onScriptBlockRainFill(tile);
    }

    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        if (world.isRemote) {
            return;
        }
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        fallDistance = EventHooks.onScriptBlockFallenUpon(tile, entity, fallDistance);
        super.onFallenUpon(world, pos, entity, fallDistance);
    }

    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        EventHooks.onScriptBlockClicked(tile, player);
    }

    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileScripted tile = (TileScripted)world.getTileEntity(pos);
            EventHooks.onScriptBlockBreak(tile);
        }
        super.breakBlock(world, pos, state);
    }

    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        TileScripted tile;
        if (!world.isRemote && EventHooks.onScriptBlockHarvest(tile = (TileScripted)world.getTileEntity(pos), player)) {
            return false;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileScripted tile;
        if (!world.isRemote && EventHooks.onScriptBlockExploded(tile = (TileScripted)world.getTileEntity(pos))) {
            return;
        }
        super.onBlockExploded(world, pos, explosion);
    }

    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos pos2) {
        if (world.isRemote) {
            return;
        }
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        EventHooks.onScriptBlockNeighborChanged(tile, pos2);
        int power = 0;
        for (EnumFacing enumfacing : EnumFacing.values()) {
            int p = world.getRedstonePower(pos.offset(enumfacing), enumfacing);
            if (p <= power) continue;
            power = p;
        }
        if (tile.prevPower != power && tile.powering <= 0) {
            tile.newPower = power;
        }
    }

    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    public int getWeakPower(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return this.getStrongPower(state, worldIn, pos, side);
    }

    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return ((TileScripted)world.getTileEntity((BlockPos)pos)).activePowering;
    }

    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return ((TileScripted)world.getTileEntity((BlockPos)pos)).isLadder;
    }

    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return true;
    }

    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileScripted tile = (TileScripted)world.getTileEntity(pos);
        if (tile == null) {
            return 0;
        }
        return tile.lightValue;
    }

    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return ((TileScripted)world.getTileEntity((BlockPos)pos)).isPassible;
    }

    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return super.canEntityDestroy(state, world, pos, entity);
    }

    public float getEnchantPowerBonus(World world, BlockPos pos) {
        return super.getEnchantPowerBonus(world, pos);
    }

    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return ((TileScripted)world.getTileEntity((BlockPos)pos)).blockHardness;
    }

    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        return ((TileScripted)world.getTileEntity((BlockPos)pos)).blockResistance;
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.SaveTileEntity || e == EnumPacketServer.ScriptBlockDataSave;
    }
}

