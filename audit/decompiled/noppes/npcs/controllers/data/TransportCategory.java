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
        this.id = compound.func_74762_e("CategoryId");
        this.title = compound.func_74779_i("CategoryTitle");
        NBTTagList locs = compound.func_150295_c("CategoryLocations", 10);
        if (locs == null || locs.func_74745_c() == 0) {
            return;
        }
        for (int ii = 0; ii < locs.func_74745_c(); ++ii) {
            TransportLocation location = new TransportLocation();
            location.readNBT(locs.func_150305_b(ii));
            location.category = this;
            this.locations.put(location.id, location);
        }
    }

    public void writeNBT(NBTTagCompound compound) {
        compound.func_74768_a("CategoryId", this.id);
        compound.func_74778_a("CategoryTitle", this.title);
        NBTTagList locs = new NBTTagList();
        for (TransportLocation location : this.locations.values()) {
            locs.func_74742_a((NBTBase)location.writeNBT());
        }
        compound.func_74782_a("CategoryLocations", (NBTBase)locs);
    }
}

