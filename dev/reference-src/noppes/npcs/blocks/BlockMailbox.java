/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.SoundType
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.properties.PropertyInteger
 *  net.minecraft.block.state.BlockStateContainer
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.creativetab.CreativeTabs
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.NonNullList
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import noppes.npcs.Server;
import noppes.npcs.blocks.BlockInterface;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;

public class BlockMailbox
extends BlockInterface {
    public static final PropertyInteger ROTATION = PropertyInteger.create((String)"rotation", (int)0, (int)3);
    public static final PropertyInteger TYPE = PropertyInteger.create((String)"type", (int)0, (int)2);

    public BlockMailbox() {
        super(Material.IRON);
        this.setSoundType(SoundType.METAL);
    }

    public void getSubBlocks(CreativeTabs par2CreativeTabs, NonNullList<ItemStack> par3List) {
        par3List.add((Object)new ItemStack((Block)this, 1, 0));
        par3List.add((Object)new ItemStack((Block)this, 1, 1));
        par3List.add((Object)new ItemStack((Block)this, 1, 2));
    }

    public boolean onBlockActivated(World par1World, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!par1World.isRemote) {
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.GUI, new Object[]{EnumGuiType.PlayerMailbox, pos.getX(), pos.getY(), pos.getZ()});
        }
        return true;
    }

    public ArrayList<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        int damage = (Integer)state.getValue((IProperty)TYPE);
        ret.add(new ItemStack((Block)this, 1, damage));
        return ret;
    }

    public int damageDropped(IBlockState state) {
        return (Integer)state.getValue((IProperty)TYPE);
    }

    public int getMetaFromState(IBlockState state) {
        return (Integer)state.getValue((IProperty)ROTATION) | (Integer)state.getValue((IProperty)TYPE) << 2;
    }

    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty((IProperty)TYPE, (Comparable)Integer.valueOf((Integer.valueOf(meta) >> 2) % 3)).withProperty((IProperty)ROTATION, (Comparable)Integer.valueOf((meta | 4) % 4));
    }

    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer((Block)this, new IProperty[]{TYPE, ROTATION});
    }

    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
        int l = MathHelper.floor((double)((double)(entity.rotationYaw * 4.0f / 360.0f) + 0.5)) & 3;
        world.setBlockState(pos, state.withProperty((IProperty)TYPE, (Comparable)Integer.valueOf(stack.getItemDamage())).withProperty((IProperty)ROTATION, (Comparable)Integer.valueOf(l % 4)), 2);
    }

    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileMailbox();
    }
}

