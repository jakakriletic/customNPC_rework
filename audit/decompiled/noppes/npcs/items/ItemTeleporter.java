/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.RayTraceResult
 *  net.minecraft.util.math.RayTraceResult$Type
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.world.World
 */
package noppes.npcs.items;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.IPermission;

public class ItemTeleporter
extends Item
implements IPermission {
    public ItemTeleporter() {
        this.field_77777_bU = 1;
        this.func_77637_a(CustomItems.tab);
    }

    public ActionResult<ItemStack> func_77659_a(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.func_184586_b(hand);
        if (!world.field_72995_K) {
            return new ActionResult(EnumActionResult.SUCCESS, (Object)itemstack);
        }
        CustomNpcs.proxy.openGui((EntityNPCInterface)null, EnumGuiType.NpcDimensions);
        return new ActionResult(EnumActionResult.SUCCESS, (Object)itemstack);
    }

    public boolean onEntitySwing(EntityLivingBase par3EntityPlayer, ItemStack stack) {
        float f8;
        float f6;
        double d3;
        float f5;
        if (par3EntityPlayer.field_70170_p.field_72995_K) {
            return false;
        }
        float f = 1.0f;
        float f1 = par3EntityPlayer.field_70127_C + (par3EntityPlayer.field_70125_A - par3EntityPlayer.field_70127_C) * f;
        float f2 = par3EntityPlayer.field_70126_B + (par3EntityPlayer.field_70177_z - par3EntityPlayer.field_70126_B) * f;
        double d0 = par3EntityPlayer.field_70169_q + (par3EntityPlayer.field_70165_t - par3EntityPlayer.field_70169_q) * (double)f;
        double d1 = par3EntityPlayer.field_70167_r + (par3EntityPlayer.field_70163_u - par3EntityPlayer.field_70167_r) * (double)f + 1.62;
        double d2 = par3EntityPlayer.field_70166_s + (par3EntityPlayer.field_70161_v - par3EntityPlayer.field_70166_s) * (double)f;
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        float f3 = MathHelper.func_76134_b((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f4 = MathHelper.func_76126_a((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f7 = f4 * (f5 = -MathHelper.func_76134_b((float)(-f1 * ((float)Math.PI / 180))));
        Vec3d vec31 = vec3.func_72441_c((double)f7 * (d3 = 80.0), (double)(f6 = MathHelper.func_76126_a((float)(-f1 * ((float)Math.PI / 180)))) * d3, (double)(f8 = f3 * f5) * d3);
        RayTraceResult movingobjectposition = par3EntityPlayer.field_70170_p.func_72901_a(vec3, vec31, true);
        if (movingobjectposition == null) {
            return false;
        }
        Vec3d vec32 = par3EntityPlayer.func_70676_i(f);
        boolean flag = false;
        float f9 = 1.0f;
        List list = par3EntityPlayer.field_70170_p.func_72839_b((Entity)par3EntityPlayer, par3EntityPlayer.func_174813_aQ().func_72314_b(vec32.field_72450_a * d3, vec32.field_72448_b * d3, vec32.field_72449_c * d3).func_72314_b((double)f9, (double)f9, (double)f9));
        for (int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity)list.get(i);
            if (!entity.func_70067_L()) continue;
            float f10 = entity.func_70111_Y();
            AxisAlignedBB axisalignedbb = entity.func_174813_aQ().func_72314_b((double)f10, (double)f10, (double)f10);
            if (!axisalignedbb.func_72318_a(vec3)) continue;
            flag = true;
        }
        if (flag) {
            return false;
        }
        if (movingobjectposition.field_72313_a == RayTraceResult.Type.BLOCK) {
            BlockPos pos = movingobjectposition.func_178782_a();
            while (par3EntityPlayer.field_70170_p.func_180495_p(pos).func_177230_c() != Blocks.field_150350_a) {
                pos = pos.func_177984_a();
            }
            par3EntityPlayer.func_70634_a((double)((float)pos.func_177958_n() + 0.5f), (double)((float)pos.func_177956_o() + 1.0f), (double)((float)pos.func_177952_p() + 0.5f));
        }
        return true;
    }

    public Item func_77655_b(String name) {
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return super.func_77655_b(name);
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.DimensionsGet || e == EnumPacketServer.DimensionTeleport;
    }
}

