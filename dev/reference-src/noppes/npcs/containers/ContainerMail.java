/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.containers.ContainerNpcInterface;
import noppes.npcs.containers.SlotValid;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.PlayerMailData;

public class ContainerMail
extends ContainerNpcInterface {
    public static PlayerMail staticmail = new PlayerMail();
    public PlayerMail mail = staticmail;
    private boolean canEdit;
    private boolean canSend;

    public ContainerMail(EntityPlayer player, boolean canEdit, boolean canSend) {
        super(player);
        int j;
        int k;
        staticmail = new PlayerMail();
        this.canEdit = canEdit;
        this.canSend = canSend;
        player.inventory.openInventory(player);
        for (k = 0; k < 4; ++k) {
            this.addSlotToContainer(new SlotValid(this.mail, k, 179 + k * 24, 138, canEdit));
        }
        for (j = 0; j < 3; ++j) {
            for (k = 0; k < 9; ++k) {
                this.addSlotToContainer(new Slot((IInventory)player.inventory, k + j * 9 + 9, 28 + k * 18, 175 + j * 18));
            }
        }
        for (j = 0; j < 9; ++j) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, j, 28 + j * 18, 230));
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot)this.inventorySlots.get(par2);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (par2 < 4) {
                if (!this.mergeItemStack(itemstack1, 4, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.canEdit || !this.mergeItemStack(itemstack1, 0, 4, false)) {
                return null;
            }
            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }

    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!this.canEdit && !player.world.isRemote) {
            PlayerMailData data = PlayerData.get((EntityPlayer)player).mailData;
            for (PlayerMail mail : data.playermail) {
                if (mail.time != this.mail.time || !mail.sender.equals(this.mail.sender)) continue;
                mail.readNBT(this.mail.writeNBT());
                break;
            }
        }
    }
}

