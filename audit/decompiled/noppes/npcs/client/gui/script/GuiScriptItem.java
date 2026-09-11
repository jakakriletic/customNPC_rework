/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.client.gui.script;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomItems;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.script.GuiScriptInterface;
import noppes.npcs.constants.EnumPacketServer;

public class GuiScriptItem
extends GuiScriptInterface {
    private ItemScriptedWrapper item = new ItemScriptedWrapper(new ItemStack((Item)CustomItems.scripted_item));

    public GuiScriptItem(EntityPlayer player) {
        this.handler = this.item;
        Client.sendData(EnumPacketServer.ScriptItemDataGet, new Object[0]);
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.item.setMCNbt(compound);
        super.setGuiData(compound);
    }

    @Override
    public void save() {
        super.save();
        Client.sendData(EnumPacketServer.ScriptItemDataSave, this.item.getMCNbt());
    }
}

