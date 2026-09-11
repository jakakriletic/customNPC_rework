/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.World
 */
package noppes.npcs.items;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.IPermission;

public class ItemNpcMovingPath
extends Item
implements IPermission {
    public ItemNpcMovingPath() {
        this.field_77777_bU = 1;
        this.func_77637_a(CustomItems.tab);
    }

    public ActionResult<ItemStack> func_77659_a(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack;
        block5: {
            block4: {
                itemstack = player.func_184586_b(hand);
                if (world.field_72995_K) break block4;
                if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_MOUNTER)) break block5;
            }
            return new ActionResult(EnumActionResult.PASS, (Object)itemstack);
        }
        EntityNPCInterface npc = this.getNpc(itemstack, world);
        if (npc != null) {
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.MovingPath, npc);
        }
        return new ActionResult(EnumActionResult.SUCCESS, (Object)itemstack);
    }

    public EnumActionResult func_180614_a(EntityPlayer player, World world, BlockPos bpos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        block6: {
            block5: {
                if (world.field_72995_K) break block5;
                if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_MOUNTER)) break block6;
            }
            return EnumActionResult.FAIL;
        }
        ItemStack stack = player.func_184586_b(hand);
        EntityNPCInterface npc = this.getNpc(stack, world);
        if (npc == null) {
            return EnumActionResult.PASS;
        }
        List<int[]> list = npc.ais.getMovingPath();
        int[] pos = list.get(list.size() - 1);
        int x = bpos.func_177958_n();
        int y = bpos.func_177956_o();
        int z = bpos.func_177952_p();
        list.add(new int[]{x, y, z});
        double d3 = x - pos[0];
        double d4 = y - pos[1];
        double d5 = z - pos[2];
        double distance = MathHelper.func_76133_a((double)(d3 * d3 + d4 * d4 + d5 * d5));
        player.func_145747_a((ITextComponent)new TextComponentString("Added point x:" + x + " y:" + y + " z:" + z + " to npc " + npc.func_70005_c_()));
        if (distance > (double)CustomNpcs.NpcNavRange) {
            player.func_145747_a((ITextComponent)new TextComponentString("Warning: point is too far away from previous point. Max block walk distance = " + CustomNpcs.NpcNavRange));
        }
        return EnumActionResult.SUCCESS;
    }

    private EntityNPCInterface getNpc(ItemStack item, World world) {
        if (world.field_72995_K || item.func_77978_p() == null) {
            return null;
        }
        Entity entity = world.func_73045_a(item.func_77978_p().func_74762_e("NPCID"));
        if (entity == null || !(entity instanceof EntityNPCInterface)) {
            return null;
        }
        return (EntityNPCInterface)entity;
    }

    public Item func_77655_b(String name) {
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return super.func_77655_b(name);
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.MovingPathGet || e == EnumPacketServer.MovingPathSave;
    }
}

