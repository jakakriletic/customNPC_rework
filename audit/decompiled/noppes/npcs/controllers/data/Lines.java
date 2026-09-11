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
            nbttagcompound.func_74768_a("Slot", slot);
            nbttagcompound.func_74778_a("Line", line.text);
            nbttagcompound.func_74778_a("Song", line.sound);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        compound.func_74782_a("Lines", (NBTBase)nbttaglist);
        return compound;
    }

    public void readNBT(NBTTagCompound compound) {
        NBTTagList nbttaglist = compound.func_150295_c("Lines", 10);
        HashMap<Integer, Line> map = new HashMap<Integer, Line>();
        for (int i = 0; i < nbttaglist.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = nbttaglist.func_150305_b(i);
            Line line = new Line();
            line.text = nbttagcompound.func_74779_i("Line");
            line.sound = nbttagcompound.func_74779_i("Song");
            map.put(nbttagcompound.func_74762_e("Slot"), line);
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

