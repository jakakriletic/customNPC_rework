/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.client.gui.script;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.script.GuiScriptInterface;
import noppes.npcs.constants.EnumPacketServer;

public class GuiScriptDoor
extends GuiScriptInterface {
    private TileScriptedDoor script;

    public GuiScriptDoor(int x, int y, int z) {
        this.script = (TileScriptedDoor)this.player.field_70170_p.func_175625_s(new BlockPos(x, y, z));
        this.handler = this.script;
        Client.sendData(EnumPacketServer.ScriptDoorDataGet, x, y, z);
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.script.setNBT(compound);
        super.setGuiData(compound);
    }

    @Override
    public void save() {
        super.save();
        BlockPos pos = this.script.func_174877_v();
        Client.sendData(EnumPacketServer.ScriptDoorDataSave, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), this.script.getNBT(new NBTTagCompound()));
    }
}

