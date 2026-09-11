/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.roles.RoleInterface;

public class ItemSoulstoneEmpty
extends Item {
    public ItemSoulstoneEmpty() {
        this.setMaxStackSize(64);
    }

    public Item setUnlocalizedName(String name) {
        super.setUnlocalizedName(name);
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return this;
    }

    public boolean store(EntityLivingBase entity, ItemStack stack, EntityPlayer player) {
        if (!this.hasPermission(entity, player) || entity instanceof EntityPlayer) {
            return false;
        }
        ItemStack stone = new ItemStack(CustomItems.soulstoneFull);
        NBTTagCompound compound = new NBTTagCompound();
        if (!entity.writeToNBTAtomically(compound)) {
            return false;
        }
        ServerCloneController.Instance.cleanTags(compound);
        stone.setTagInfo("Entity", (NBTBase)compound);
        String name = EntityList.getEntityString((Entity)entity);
        if (name == null) {
            name = "generic";
        }
        stone.setTagInfo("Name", (NBTBase)new NBTTagString("entity." + name + ".name"));
        if (entity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            stone.setTagInfo("DisplayName", (NBTBase)new NBTTagString(entity.getName()));
            if (npc.advanced.role == 6) {
                RoleCompanion role = (RoleCompanion)npc.roleInterface;
                stone.setTagInfo("ExtraText", (NBTBase)new NBTTagString("companion.stage,: ," + role.stage.name));
            }
        } else if (entity instanceof EntityLiving && ((EntityLiving)entity).hasCustomName()) {
            stone.setTagInfo("DisplayName", (NBTBase)new NBTTagString(((EntityLiving)entity).getCustomNameTag()));
        }
        NoppesUtilServer.GivePlayerItem((Entity)player, player, stone);
        if (!player.capabilities.isCreativeMode) {
            stack.splitStack(1);
            if (stack.getCount() <= 0) {
                player.inventory.deleteStack(stack);
            }
        }
        entity.isDead = true;
        return true;
    }

    public boolean hasPermission(EntityLivingBase entity, EntityPlayer player) {
        if (NoppesUtilServer.isOp(player)) {
            return true;
        }
        if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.SOULSTONE_ALL)) {
            return true;
        }
        if (entity instanceof EntityNPCInterface) {
            RoleInterface role;
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            if (npc.advanced.role == 6 && ((RoleCompanion)(role = (RoleCompanion)npc.roleInterface)).getOwner() == player) {
                return true;
            }
            if (npc.advanced.role == 2 && ((RoleFollower)(role = (RoleFollower)npc.roleInterface)).getOwner() == player) {
                return !((RoleFollower)role).refuseSoulStone;
            }
            return CustomNpcs.SoulStoneNPCs;
        }
        if (entity instanceof EntityAnimal) {
            return CustomNpcs.SoulStoneAnimals;
        }
        return false;
    }
}

