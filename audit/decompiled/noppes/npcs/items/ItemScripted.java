/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.items;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.CustomItems;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.util.IPermission;

public class ItemScripted
extends Item
implements IPermission {
    public static Map<Integer, String> Resources = new HashMap<Integer, String>();

    public ItemScripted() {
        this.field_77777_bU = 1;
        this.func_77637_a(CustomItems.tab);
        this.func_77627_a(true);
    }

    public Item func_77655_b(String name) {
        super.func_77655_b(name);
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return this;
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.ScriptItemDataGet || e == EnumPacketServer.ScriptItemDataSave;
    }

    public static ItemScriptedWrapper GetWrapper(ItemStack stack) {
        return (ItemScriptedWrapper)NpcAPI.Instance().getIItemStack(stack);
    }

    public boolean showDurabilityBar(ItemStack stack) {
        IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
        if (istack instanceof ItemScriptedWrapper) {
            return ((ItemScriptedWrapper)istack).durabilityShow;
        }
        return super.showDurabilityBar(stack);
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
        if (istack instanceof ItemScriptedWrapper) {
            return 1.0 - ((ItemScriptedWrapper)istack).durabilityValue;
        }
        return super.getDurabilityForDisplay(stack);
    }

    public int getRGBDurabilityForDisplay(ItemStack stack) {
        IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
        if (!(istack instanceof ItemScriptedWrapper)) {
            return super.getRGBDurabilityForDisplay(stack);
        }
        int color = ((ItemScriptedWrapper)istack).durabilityColor;
        if (color >= 0) {
            return color;
        }
        return MathHelper.func_181758_c((float)(Math.max(0.0f, (float)(1.0 - this.getDurabilityForDisplay(stack))) / 3.0f), (float)1.0f, (float)1.0f);
    }

    public int getItemStackLimit(ItemStack stack) {
        IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
        if (istack instanceof ItemScriptedWrapper) {
            return ((ItemScriptedWrapper)istack).getMaxStackSize();
        }
        return super.getItemStackLimit(stack);
    }

    public boolean func_77644_a(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        return true;
    }
}

