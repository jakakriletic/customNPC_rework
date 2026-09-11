/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.world.World
 */
package noppes.npcs.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.IPermission;

public class ItemNpcWand
extends Item
implements IPermission {
    public ItemNpcWand() {
        this.maxStackSize = 1;
        this.setCreativeTab(CustomItems.tab);
    }

    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.getHeldItem(hand);
        if (!world.isRemote) {
            return new ActionResult(EnumActionResult.SUCCESS, (Object)itemstack);
        }
        CustomNpcs.proxy.openGui(0, 0, 0, EnumGuiType.NpcRemote, player);
        return new ActionResult(EnumActionResult.SUCCESS, (Object)itemstack);
    }

    public int getMaxItemUseDuration(ItemStack par1ItemStack) {
        return 72000;
    }

    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos bpos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (CustomNpcs.OpsOnly && !player.getServer().getPlayerList().canSendCommands(player.getGameProfile())) {
            player.sendMessage((ITextComponent)new TextComponentTranslation("availability.permission", new Object[0]));
        } else if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_CREATE)) {
            EntityCustomNpc npc = new EntityCustomNpc(world);
            npc.ais.setStartPos(bpos.up());
            npc.setLocationAndAngles((float)bpos.getX() + 0.5f, npc.getStartYPos(), (float)bpos.getZ() + 0.5f, player.rotationYaw, player.rotationPitch);
            world.spawnEntity((Entity)npc);
            npc.setHealth(npc.getMaxHealth());
            CustomNPCsScheduler.runTack(() -> NoppesUtilServer.sendOpenGui(player, EnumGuiType.MainMenuDisplay, npc), 100);
        } else {
            player.sendMessage((ITextComponent)new TextComponentTranslation("availability.permission", new Object[0]));
        }
        return EnumActionResult.SUCCESS;
    }

    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase playerIn) {
        return stack;
    }

    public Item setUnlocalizedName(String name) {
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return super.setUnlocalizedName(name);
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return true;
    }
}

