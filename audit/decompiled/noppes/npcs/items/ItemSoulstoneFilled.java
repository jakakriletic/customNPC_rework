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
        this.func_77625_d(1);
    }

    public Item func_77655_b(String name) {
        super.func_77655_b(name);
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return this;
    }

    @SideOnly(value=Side.CLIENT)
    public void func_77624_a(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        NBTTagCompound compound = stack.func_77978_p();
        if (compound == null || !compound.func_150297_b("Entity", 10)) {
            list.add(TextFormatting.RED + "Error");
            return;
        }
        String name = I18n.func_74838_a((String)compound.func_74779_i("Name"));
        if (compound.func_74764_b("DisplayName")) {
            name = compound.func_74779_i("DisplayName") + " (" + name + ")";
        }
        list.add(TextFormatting.BLUE + name);
        if (stack.func_77978_p().func_74764_b("ExtraText")) {
            String[] split;
            String text = "";
            for (String s : split = compound.func_74779_i("ExtraText").split(",")) {
                text = text + I18n.func_74838_a((String)s);
            }
            list.add(text);
        }
    }

    public EnumActionResult func_180614_a(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) {
            return EnumActionResult.SUCCESS;
        }
        ItemStack stack = player.func_184586_b(hand);
        if (ItemSoulstoneFilled.Spawn(player, stack, world, pos) == null) {
            return EnumActionResult.FAIL;
        }
        if (!player.field_71075_bZ.field_75098_d) {
            stack.func_77979_a(1);
        }
        return EnumActionResult.SUCCESS;
    }

    public static Entity Spawn(EntityPlayer player, ItemStack stack, World world, BlockPos pos) {
        if (world.field_72995_K) {
            return null;
        }
        if (stack.func_77978_p() == null || !stack.func_77978_p().func_150297_b("Entity", 10)) {
            return null;
        }
        NBTTagCompound compound = stack.func_77978_p().func_74775_l("Entity");
        Entity entity = EntityList.func_75615_a((NBTTagCompound)compound, (World)world);
        if (entity == null) {
            return null;
        }
        entity.func_70107_b((double)pos.func_177958_n() + 0.5, (double)((float)(pos.func_177956_o() + 1) + 0.2f), (double)pos.func_177952_p() + 0.5);
        if (entity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            npc.ais.setStartPos(pos);
            npc.func_70606_j(npc.func_110138_aP());
            npc.func_70107_b((float)pos.func_177958_n() + 0.5f, npc.getStartYPos(), (float)pos.func_177952_p() + 0.5f);
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
        if (!world.func_72838_d(entity)) {
            player.func_145747_a((ITextComponent)new TextComponentTranslation("error.failedToSpawn", new Object[0]));
            return null;
        }
        return entity;
    }
}

