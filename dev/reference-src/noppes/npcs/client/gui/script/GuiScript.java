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
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataScript;

public class GuiScript
extends GuiScriptInterface {
    private DataScript script;

    public GuiScript(EntityNPCInterface npc) {
        this.script = npc.script;
        this.handler = this.script;
        Client.sendData(EnumPacketServer.ScriptDataGet, new Object[0]);
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.script.readFromNBT(compound);
        super.setGuiData(compound);
    }

    @Override
    public void save() {
        super.save();
        Client.sendData(EnumPacketServer.ScriptDataSave, this.script.writeToNBT(new NBTTagCompound()));
    }
}

