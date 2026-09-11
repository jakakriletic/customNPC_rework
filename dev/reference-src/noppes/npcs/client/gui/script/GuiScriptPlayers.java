/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.client.gui.script;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.script.GuiScriptInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.PlayerScriptData;

public class GuiScriptPlayers
extends GuiScriptInterface {
    private PlayerScriptData script = new PlayerScriptData(null);

    public GuiScriptPlayers() {
        this.handler = this.script;
        Client.sendData(EnumPacketServer.ScriptPlayerGet, new Object[0]);
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.script.readFromNBT(compound);
        super.setGuiData(compound);
    }

    @Override
    public void save() {
        super.save();
        Client.sendData(EnumPacketServer.ScriptPlayerSave, this.script.writeToNBT(new NBTTagCompound()));
    }
}

