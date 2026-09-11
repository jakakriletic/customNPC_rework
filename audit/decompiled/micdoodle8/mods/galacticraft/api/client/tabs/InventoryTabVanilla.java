/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 */
package micdoodle8.mods.galacticraft.api.client.tabs;

import micdoodle8.mods.galacticraft.api.client.tabs.AbstractTab;
import micdoodle8.mods.galacticraft.api.client.tabs.TabRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class InventoryTabVanilla
extends AbstractTab {
    public InventoryTabVanilla() {
        super(0, 0, 0, new ItemStack(Blocks.field_150462_ai));
    }

    @Override
    public void onTabClicked() {
        TabRegistry.openInventoryGui();
    }

    @Override
    public boolean shouldAddToList() {
        return true;
    }
}

