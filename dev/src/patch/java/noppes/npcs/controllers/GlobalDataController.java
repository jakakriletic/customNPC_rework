/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomNpcs;
import noppes.npcs.rework.data.CompressedNbtFile;

public class GlobalDataController {
    public static GlobalDataController instance;
    private int itemGiverId = 0;

    public GlobalDataController() {
        instance = this;
        this.load();
    }

    private void load() {
        File saveDir = CustomNpcs.getWorldSaveDirectory();
        try {
            File file = new File(saveDir, "global.dat");
            if (file.exists()) {
                this.loadData(file);
            }
        }
        catch (Exception e) {
            try {
                File file = new File(saveDir, "global.dat_old");
                if (file.exists()) {
                    this.loadData(file);
                }
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
    }

    private void loadData(File file) throws Exception {
        NBTTagCompound nbttagcompound1 = CompressedStreamTools.readCompressed((InputStream)new FileInputStream(file));
        this.itemGiverId = nbttagcompound1.getInteger("itemGiverId");
    }

    public void saveData() {
        try {
            File saveDir = CustomNpcs.getWorldSaveDirectory();
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("itemGiverId", this.itemGiverId);
            File file = new File(saveDir, "global.dat");
            CompressedNbtFile.save(file, nbttagcompound);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int incrementItemGiverId() {
        ++this.itemGiverId;
        this.saveData();
        return this.itemGiverId;
    }
}

