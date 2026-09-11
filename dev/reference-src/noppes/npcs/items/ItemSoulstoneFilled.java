/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.util.ITooltipFlag
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.items;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;

public class ItemSoulstoneFilled
extends Item {
    public ItemSoulstoneFilled() {
        this.setMaxStackSize(1);
    }

    public Item setUnlocalizedName(String name) {
        super.setUnlocalizedName(name);
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return this;
    }

    @SideOnly(value=Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        NBTTagCompound compound = stack.getTagCompound();
        if (compound == null || !compound.hasKey("Entity", 10)) {
            list.add(TextFormatting.RED + "Error");
            return;
        }
        String name = I18n.translateToLocal((String)compound.getString("Name"));
        if (compound.hasKey("DisplayName")) {
            name = compound.getString("DisplayName") + " (" + name + ")";
        }
        list.add(TextFormatting.BLUE + name);
        if (stack.getTagCompound().hasKey("ExtraText")) {
            String[] split;
            String text = "";
            for (String s : split = compound.getString("ExtraText").split(",")) {
                text = text + I18n.translateToLocal((String)s);
            }
            list.add(text);
        }
    }

    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (ItemSoulstoneFilled.Spawn(player, stack, world, pos) == null) {
            return EnumActionResult.FAIL;
        }
        if (!player.capabilities.isCreativeMode) {
            stack.splitStack(1);
        }
        return EnumActionResult.SUCCESS;
    }

    public static Entity Spawn(EntityPlayer player, ItemStack stack, World world, BlockPos pos) {
        if (world.isRemote) {
            return null;
        }
        if (stack.getTagCompound() == null || !stack.getTagCompound().hasKey("Entity", 10)) {
            return null;
        }
        NBTTagCompound compound = stack.getTagCompound().getCompoundTag("Entity");
        Entity entity = EntityList.createEntityFromNBT((NBTTagCompound)compound, (World)world);
        if (entity == null) {
            return null;
        }
        entity.setPosition((double)pos.getX() + 0.5, (double)((float)(pos.getY() + 1) + 0.2f), (double)pos.getZ() + 0.5);
        if (entity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            npc.ais.setStartPos(pos);
            npc.setHealth(npc.getMaxHealth());
            npc.setPosition((float)pos.getX() + 0.5f, npc.getStartYPos(), (float)pos.getZ() + 0.5f);
            if (npc.advanced.role == 6 && player != null) {
                PlayerData data = PlayerData.get(player);
                if (data.hasCompanion()) {
                    return null;
                }
                ((RoleCompanion)npc.roleInterface).setOwner(player);
                data.setCompanion(npc);
            }
            if (npc.advanced.role == 2 && player != null) {
                ((RoleFollower)npc.roleInterface).setOwner(player);
            }
        }
        if (!world.spawnEntity(entity)) {
            player.sendMessage((ITextComponent)new TextComponentTranslation("error.failedToSpawn", new Object[0]));
            return null;
        }
        return entity;
    }
}

