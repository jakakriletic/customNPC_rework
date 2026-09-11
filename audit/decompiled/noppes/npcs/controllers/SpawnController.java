/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.WeightedRandom
 */
package noppes.npcs.controllers;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.WeightedRandom;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.controllers.data.SpawnData;

public class SpawnController {
    public HashMap<String, List<SpawnData>> biomes = new HashMap();
    public ArrayList<SpawnData> data = new ArrayList();
    public Random random = new Random();
    public static SpawnController instance;
    private int lastUsedID = 0;

    public SpawnController() {
        instance = this;
        this.loadData();
    }

    private void loadData() {
        File saveDir = CustomNpcs.getWorldSaveDirectory();
        if (saveDir == null) {
            return;
        }
        try {
            File file = new File(saveDir, "spawns.dat");
            if (file.exists()) {
                this.loadDataFile(file);
            }
        }
        catch (Exception e) {
            try {
                File file = new File(saveDir, "spawns.dat_old");
                if (file.exists()) {
                    this.loadDataFile(file);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void loadDataFile(File file) throws IOException {
        DataInputStream var1 = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
        this.loadData(var1);
        var1.close();
    }

    public void loadData(DataInputStream stream) throws IOException {
        ArrayList<SpawnData> data = new ArrayList<SpawnData>();
        NBTTagCompound nbttagcompound1 = CompressedStreamTools.func_74794_a((DataInputStream)stream);
        this.lastUsedID = nbttagcompound1.func_74762_e("lastID");
        NBTTagList nbtlist = nbttagcompound1.func_150295_c("NPCSpawnData", 10);
        if (nbtlist != null) {
            for (int i = 0; i < nbtlist.func_74745_c(); ++i) {
                NBTTagCompound nbttagcompound = nbtlist.func_150305_b(i);
                SpawnData spawn = new SpawnData();
                spawn.readNBT(nbttagcompound);
                data.add(spawn);
            }
        }
        this.data = data;
        this.fillBiomeData();
    }

    public NBTTagCompound getNBT() {
        NBTTagList list = new NBTTagList();
        for (SpawnData spawn : this.data) {
            NBTTagCompound nbtfactions = new NBTTagCompound();
            spawn.writeNBT(nbtfactions);
            list.func_74742_a((NBTBase)nbtfactions);
        }
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.func_74768_a("lastID", this.lastUsedID);
        nbttagcompound.func_74782_a("NPCSpawnData", (NBTBase)list);
        return nbttagcompound;
    }

    public void saveData() {
        try {
            File saveDir = CustomNpcs.getWorldSaveDirectory();
            File file = new File(saveDir, "spawns.dat_new");
            File file1 = new File(saveDir, "spawns.dat_old");
            File file2 = new File(saveDir, "spawns.dat");
            CompressedStreamTools.func_74799_a((NBTTagCompound)this.getNBT(), (OutputStream)new FileOutputStream(file));
            if (file1.exists()) {
                file1.delete();
            }
            file2.renameTo(file1);
            if (file2.exists()) {
                file2.delete();
            }
            file.renameTo(file2);
            if (file.exists()) {
                file.delete();
            }
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
    }

    public SpawnData getSpawnData(int id) {
        for (SpawnData spawn : this.data) {
            if (spawn.id != id) continue;
            return spawn;
        }
        return null;
    }

    public void saveSpawnData(SpawnData spawn) {
        SpawnData original;
        if (spawn.id < 0) {
            spawn.id = this.getUnusedId();
        }
        if ((original = this.getSpawnData(spawn.id)) == null) {
            this.data.add(spawn);
        } else {
            original.readNBT(spawn.writeNBT(new NBTTagCompound()));
        }
        this.fillBiomeData();
        this.saveData();
    }

    private void fillBiomeData() {
        HashMap<String, ArrayList<SpawnData>> biomes = new HashMap<String, ArrayList<SpawnData>>();
        for (SpawnData spawn : this.data) {
            for (String s : spawn.biomes) {
                ArrayList<SpawnData> list = (ArrayList<SpawnData>)biomes.get(s);
                if (list == null) {
                    list = new ArrayList<SpawnData>();
                    biomes.put(s, list);
                }
                list.add(spawn);
            }
        }
        this.biomes = biomes;
    }

    public int getUnusedId() {
        ++this.lastUsedID;
        return this.lastUsedID;
    }

    public void removeSpawnData(int id) {
        ArrayList<SpawnData> data = new ArrayList<SpawnData>();
        for (SpawnData spawn : this.data) {
            if (spawn.id == id) continue;
            data.add(spawn);
        }
        this.data = data;
        this.fillBiomeData();
        this.saveData();
    }

    public List<SpawnData> getSpawnList(String biome) {
        return this.biomes.get(biome);
    }

    public SpawnData getRandomSpawnData(String biome, boolean isAir) {
        List<SpawnData> list = this.getSpawnList(biome);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return (SpawnData)WeightedRandom.func_76271_a((Random)this.random, list);
    }

    public Map<String, Integer> getScroll() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (SpawnData spawn : this.data) {
            map.put(spawn.name, spawn.id);
        }
        return map;
    }
}

