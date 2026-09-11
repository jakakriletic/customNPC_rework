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
        this.maxStackSize = 1;
        this.setCreativeTab(CustomItems.tab);
    }

    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.getHeldItem(hand);
        if (!world.isRemote) {
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
        if (par3EntityPlayer.world.isRemote) {
            return false;
        }
        float f = 1.0f;
        float f1 = par3EntityPlayer.prevRotationPitch + (par3EntityPlayer.rotationPitch - par3EntityPlayer.prevRotationPitch) * f;
        float f2 = par3EntityPlayer.prevRotationYaw + (par3EntityPlayer.rotationYaw - par3EntityPlayer.prevRotationYaw) * f;
        double d0 = par3EntityPlayer.prevPosX + (par3EntityPlayer.posX - par3EntityPlayer.prevPosX) * (double)f;
        double d1 = par3EntityPlayer.prevPosY + (par3EntityPlayer.posY - par3EntityPlayer.prevPosY) * (double)f + 1.62;
        double d2 = par3EntityPlayer.prevPosZ + (par3EntityPlayer.posZ - par3EntityPlayer.prevPosZ) * (double)f;
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        float f3 = MathHelper.cos((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f4 = MathHelper.sin((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f7 = f4 * (f5 = -MathHelper.cos((float)(-f1 * ((float)Math.PI / 180))));
        Vec3d vec31 = vec3.addVector((double)f7 * (d3 = 80.0), (double)(f6 = MathHelper.sin((float)(-f1 * ((float)Math.PI / 180)))) * d3, (double)(f8 = f3 * f5) * d3);
        RayTraceResult movingobjectposition = par3EntityPlayer.world.rayTraceBlocks(vec3, vec31, true);
        if (movingobjectposition == null) {
            return false;
        }
        Vec3d vec32 = par3EntityPlayer.getLook(f);
        boolean flag = false;
        float f9 = 1.0f;
        List list = par3EntityPlayer.world.getEntitiesWithinAABBExcludingEntity((Entity)par3EntityPlayer, par3EntityPlayer.getEntityBoundingBox().grow(vec32.x * d3, vec32.y * d3, vec32.z * d3).grow((double)f9, (double)f9, (double)f9));
        for (int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity)list.get(i);
            if (!entity.canBeCollidedWith()) continue;
            float f10 = entity.getCollisionBorderSize();
            AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().grow((double)f10, (double)f10, (double)f10);
            if (!axisalignedbb.contains(vec3)) continue;
            flag = true;
        }
        if (flag) {
            return false;
        }
        if (movingobjectposition.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos pos = movingobjectposition.getBlockPos();
            while (par3EntityPlayer.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                pos = pos.up();
            }
            par3EntityPlayer.setPositionAndUpdate((double)((float)pos.getX() + 0.5f), (double)((float)pos.getY() + 1.0f), (double)((float)pos.getZ() + 0.5f));
        }
        return true;
    }

    public Item setUnlocalizedName(String name) {
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return super.setUnlocalizedName(name);
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.DimensionsGet || e == EnumPacketServer.DimensionTeleport;
    }
}

