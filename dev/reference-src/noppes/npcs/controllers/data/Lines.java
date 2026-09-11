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
import java.util.Map;
import java.util.Random;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.controllers.data.Line;

public class Lines {
    private static final Random random = new Random();
    private int lastLine = -1;
    public HashMap<Integer, Line> lines = new HashMap();

    public NBTTagCompound writeToNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList nbttaglist = new NBTTagList();
        for (int slot : this.lines.keySet()) {
            Line line = this.lines.get(slot);
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("Slot", slot);
            nbttagcompound.setString("Line", line.text);
            nbttagcompound.setString("Song", line.sound);
            nbttaglist.appendTag((NBTBase)nbttagcompound);
        }
        compound.setTag("Lines", (NBTBase)nbttaglist);
        return compound;
    }

    public void readNBT(NBTTagCompound compound) {
        NBTTagList nbttaglist = compound.getTagList("Lines", 10);
        HashMap<Integer, Line> map = new HashMap<Integer, Line>();
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            Line line = new Line();
            line.text = nbttagcompound.getString("Line");
            line.sound = nbttagcompound.getString("Song");
            map.put(nbttagcompound.getInteger("Slot"), line);
        }
        this.lines = map;
    }

    public Line getLine(boolean isRandom) {
        if (this.lines.isEmpty()) {
            return null;
        }
        if (isRandom) {
            int i = random.nextInt(this.lines.size());
            for (Map.Entry<Integer, Line> e : this.lines.entrySet()) {
                if (--i >= 0) continue;
                return e.getValue().copy();
            }
        }
        ++this.lastLine;
        while (true) {
            this.lastLine %= 8;
            Line line = this.lines.get(this.lastLine);
            if (line != null) {
                return line.copy();
            }
            ++this.lastLine;
        }
    }

    public boolean isEmpty() {
        return this.lines.isEmpty();
    }
}

