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
        this.script = (TileScriptedDoor)this.player.world.getTileEntity(new BlockPos(x, y, z));
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
        BlockPos pos = this.script.getPos();
        Client.sendData(EnumPacketServer.ScriptDoorDataSave, pos.getX(), pos.getY(), pos.getZ(), this.script.getNBT(new NBTTagCompound()));
    }
}

