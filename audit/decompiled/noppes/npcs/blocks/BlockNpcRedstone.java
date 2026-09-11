/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.properties.PropertyBool
 *  net.minecraft.block.state.BlockStateContainer
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumBlockRenderType
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.BlockInterface;
import noppes.npcs.blocks.tiles.TileRedstoneBlock;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.util.IPermission;

public class BlockNpcRedstone
extends BlockInterface
implements IPermission {
    public static final PropertyBool ACTIVE = PropertyBool.func_177716_a((String)"active");

    public BlockNpcRedstone() {
        super(Material.field_151576_e);
    }

    public boolean func_180639_a(World par1World, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (par1World.field_72995_K) {
            return false;
        }
        ItemStack currentItem = player.field_71071_by.func_70448_g();
        if (currentItem != null && currentItem.func_77973_b() == CustomItems.wand && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_BLOCKS)) {
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.RedstoneBlock, null, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
            return true;
        }
        return false;
    }

    public void func_176213_c(World par1World, BlockPos pos, IBlockState state) {
        par1World.func_175685_c(pos, (Block)this, false);
        par1World.func_175685_c(pos.func_177977_b(), (Block)this, false);
        par1World.func_175685_c(pos.func_177984_a(), (Block)this, false);
        par1World.func_175685_c(pos.func_177976_e(), (Block)this, false);
        par1World.func_175685_c(pos.func_177974_f(), (Block)this, false);
        par1World.func_175685_c(pos.func_177968_d(), (Block)this, false);
        par1World.func_175685_c(pos.func_177978_c(), (Block)this, false);
    }

    public void func_180633_a(World world, BlockPos pos, IBlockState state, EntityLivingBase entityliving, ItemStack item) {
        if (entityliving instanceof EntityPlayer && !world.field_72995_K) {
            NoppesUtilServer.sendOpenGui((EntityPlayer)entityliving, EnumGuiType.RedstoneBlock, null, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
        }
    }

    public void func_176206_d(World par1World, BlockPos pos, IBlockState state) {
        this.func_176213_c(par1World, pos, state);
    }

    public int func_180656_a(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return this.isActivated(state);
    }

    public int func_176211_b(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return this.isActivated(state);
    }

    public boolean func_149744_f(IBlockState state) {
        return true;
    }

    public int func_176201_c(IBlockState state) {
        return (Boolean)state.func_177229_b((IProperty)ACTIVE) != false ? 1 : 0;
    }

    public IBlockState func_176203_a(int meta) {
        return this.func_176223_P().func_177226_a((IProperty)ACTIVE, (Comparable)Boolean.valueOf(false));
    }

    protected BlockStateContainer func_180661_e() {
        return new BlockStateContainer((Block)this, new IProperty[]{ACTIVE});
    }

    public int isActivated(IBlockState state) {
        return (Boolean)state.func_177229_b((IProperty)ACTIVE) != false ? 15 : 0;
    }

    public TileEntity func_149915_a(World var1, int var2) {
        return new TileRedstoneBlock();
    }

    public EnumBlockRenderType func_149645_b(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.SaveTileEntity;
    }
}

