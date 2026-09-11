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
    public static final PropertyInteger ROTATION = PropertyInteger.func_177719_a((String)"rotation", (int)0, (int)3);
    public static final PropertyInteger TYPE = PropertyInteger.func_177719_a((String)"type", (int)0, (int)2);

    public BlockMailbox() {
        super(Material.field_151573_f);
        this.func_149672_a(SoundType.field_185852_e);
    }

    public void func_149666_a(CreativeTabs par2CreativeTabs, NonNullList<ItemStack> par3List) {
        par3List.add((Object)new ItemStack((Block)this, 1, 0));
        par3List.add((Object)new ItemStack((Block)this, 1, 1));
        par3List.add((Object)new ItemStack((Block)this, 1, 2));
    }

    public boolean func_180639_a(World par1World, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!par1World.field_72995_K) {
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.GUI, new Object[]{EnumGuiType.PlayerMailbox, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p()});
        }
        return true;
    }

    public ArrayList<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        int damage = (Integer)state.func_177229_b((IProperty)TYPE);
        ret.add(new ItemStack((Block)this, 1, damage));
        return ret;
    }

    public int func_180651_a(IBlockState state) {
        return (Integer)state.func_177229_b((IProperty)TYPE);
    }

    public int func_176201_c(IBlockState state) {
        return (Integer)state.func_177229_b((IProperty)ROTATION) | (Integer)state.func_177229_b((IProperty)TYPE) << 2;
    }

    public IBlockState func_176203_a(int meta) {
        return this.func_176223_P().func_177226_a((IProperty)TYPE, (Comparable)Integer.valueOf((Integer.valueOf(meta) >> 2) % 3)).func_177226_a((IProperty)ROTATION, (Comparable)Integer.valueOf((meta | 4) % 4));
    }

    protected BlockStateContainer func_180661_e() {
        return new BlockStateContainer((Block)this, new IProperty[]{TYPE, ROTATION});
    }

    public void func_180633_a(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
        int l = MathHelper.func_76128_c((double)((double)(entity.field_70177_z * 4.0f / 360.0f) + 0.5)) & 3;
        world.func_180501_a(pos, state.func_177226_a((IProperty)TYPE, (Comparable)Integer.valueOf(stack.func_77952_i())).func_177226_a((IProperty)ROTATION, (Comparable)Integer.valueOf(l % 4)), 2);
    }

    public boolean func_149662_c(IBlockState state) {
        return false;
    }

    public boolean func_149686_d(IBlockState state) {
        return false;
    }

    public TileEntity func_149915_a(World var1, int var2) {
        return new TileMailbox();
    }
}

