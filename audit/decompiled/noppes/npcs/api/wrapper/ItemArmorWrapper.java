/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.api.wrapper;

import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import noppes.npcs.api.item.IItemArmor;
import noppes.npcs.api.wrapper.ItemStackWrapper;

public class ItemArmorWrapper
extends ItemStackWrapper
implements IItemArmor {
    protected ItemArmor armor;

    protected ItemArmorWrapper(ItemStack item) {
        super(item);
        this.armor = (ItemArmor)item.func_77973_b();
    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public int getArmorSlot() {
        return this.armor.func_185083_B_().func_188452_c();
    }

    @Override
    public String getArmorMaterial() {
        return this.armor.func_82812_d().func_179242_c();
    }
}

