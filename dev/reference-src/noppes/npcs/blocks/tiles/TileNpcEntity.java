/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntity
 */
package noppes.npcs.blocks.tiles;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileNpcEntity
extends TileEntity {
    public Map<String, Object> tempData = new HashMap<String, Object>();

    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        NBTTagCompound extraData = compound.getCompoundTag("ExtraData");
        if (extraData.getSize() > 0) {
            this.getTileData().setTag("CustomNPCsData", (NBTBase)extraData);
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return super.writeToNBT(compound);
    }
}

