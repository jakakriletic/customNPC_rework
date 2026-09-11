/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.quests.QuestItem;

public class ContainerNpcQuestTypeItem
extends Container {
    public ContainerNpcQuestTypeItem(EntityPlayer player) {
        int i1;
        Quest quest = NoppesUtilServer.getEditingQuest(player);
        for (i1 = 0; i1 < 3; ++i1) {
            this.addSlotToContainer(new Slot((IInventory)((QuestItem)quest.questInterface).items, i1, 44, 39 + i1 * 25));
        }
        for (i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.addSlotToContainer(new Slot((IInventory)player.inventory, l1 + i1 * 9 + 9, 8 + l1 * 18, 113 + i1 * 18));
            }
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, j1, 8 + j1 * 18, 171));
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int i) {
        return null;
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return true;
    }
}

