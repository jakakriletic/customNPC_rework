/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.HashMap;
import java.util.Vector;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.controllers.data.TransportLocation;

public class TransportCategory {
    public int id = -1;
    public String title = "";
    public HashMap<Integer, TransportLocation> locations = new HashMap();

    public Vector<TransportLocation> getDefaultLocations() {
        Vector<TransportLocation> list = new Vector<TransportLocation>();
        for (TransportLocation loc : this.locations.values()) {
            if (!loc.isDefault()) continue;
            list.add(loc);
        }
        return list;
    }

    public void readNBT(NBTTagCompound compound) {
        this.id = compound.getInteger("CategoryId");
        this.title = compound.getString("CategoryTitle");
        NBTTagList locs = compound.getTagList("CategoryLocations", 10);
        if (locs == null || locs.tagCount() == 0) {
            return;
        }
        for (int ii = 0; ii < locs.tagCount(); ++ii) {
            TransportLocation location = new TransportLocation();
            location.readNBT(locs.getCompoundTagAt(ii));
            location.category = this;
            this.locations.put(location.id, location);
        }
    }

    public void writeNBT(NBTTagCompound compound) {
        compound.setInteger("CategoryId", this.id);
        compound.setString("CategoryTitle", this.title);
        NBTTagList locs = new NBTTagList();
        for (TransportLocation location : this.locations.values()) {
            locs.appendTag((NBTBase)location.writeNBT());
        }
        compound.setTag("CategoryLocations", (NBTBase)locs);
    }
}

