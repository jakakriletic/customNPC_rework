/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 */
package noppes.npcs.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.util.IPermission;

public class ItemNpcScripter
extends Item
implements IPermission {
    public ItemNpcScripter() {
        this.maxStackSize = 1;
        this.setCreativeTab(CustomItems.tab);
    }

    public Item setUnlocalizedName(String name) {
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return super.setUnlocalizedName(name);
    }

    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.getHeldItem(hand);
        if (!world.isRemote || hand != EnumHand.MAIN_HAND) {
            return new ActionResult(EnumActionResult.SUCCESS, (Object)itemstack);
        }
        CustomNpcs.proxy.openGui(0, 0, 0, EnumGuiType.ScriptPlayers, player);
        return new ActionResult(EnumActionResult.SUCCESS, (Object)itemstack);
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.ScriptDataGet || e == EnumPacketServer.ScriptDataSave || e == EnumPacketServer.ScriptBlockDataSave || e == EnumPacketServer.ScriptDoorDataSave || e == EnumPacketServer.ScriptPlayerGet || e == EnumPacketServer.ScriptPlayerSave || e == EnumPacketServer.ScriptForgeGet || e == EnumPacketServer.ScriptForgeSave;
    }
}

