/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.NBTTags;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.NBTJsonUtil;

public class LinkedNpcController {
    public static LinkedNpcController Instance;
    public List<LinkedData> list = new ArrayList<LinkedData>();

    public LinkedNpcController() {
        Instance = this;
        this.load();
    }

    private void load() {
        try {
            this.loadNpcs();
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
    }

    public File getDir() {
        File dir = new File(CustomNpcs.getWorldSaveDirectory(), "linkednpcs");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    private void loadNpcs() {
        LogWriter.info("Loading Linked Npcs");
        File dir = this.getDir();
        if (dir.exists()) {
            ArrayList<LinkedData> list = new ArrayList<LinkedData>();
            for (File file : dir.listFiles()) {
                if (!file.getName().endsWith(".json")) continue;
                try {
                    NBTTagCompound compound = NBTJsonUtil.LoadFile(file);
                    LinkedData linked = new LinkedData();
                    linked.setNBT(compound);
                    list.add(linked);
                }
                catch (Exception e) {
                    LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
                }
            }
            this.list = list;
        }
        LogWriter.info("Done loading Linked Npcs");
    }

    public void save() {
        for (LinkedData npc : this.list) {
            try {
                this.saveNpc(npc);
            }
            catch (IOException e) {
                LogWriter.except(e);
            }
        }
    }

    private void saveNpc(LinkedData npc) throws IOException {
        File file = new File(this.getDir(), npc.name + ".json_new");
        File file1 = new File(this.getDir(), npc.name + ".json");
        try {
            NBTJsonUtil.SaveFile(file, npc.getNBT());
            if (file1.exists()) {
                file1.delete();
            }
            file.renameTo(file1);
        }
        catch (NBTJsonUtil.JsonException e) {
            LogWriter.except(e);
        }
    }

    public void loadNpcData(EntityNPCInterface npc) {
        if (npc.linkedName.isEmpty()) {
            return;
        }
        LinkedData data = this.getData(npc.linkedName);
        if (data == null) {
            npc.linkedLast = 0L;
            npc.linkedName = "";
            npc.linkedData = null;
        } else {
            npc.linkedData = data;
            if (npc.field_70165_t == 0.0 && npc.field_70163_u == 0.0 && npc.field_70165_t == 0.0) {
                return;
            }
            npc.linkedLast = data.time;
            List<int[]> points = npc.ais.getMovingPath();
            NBTTagCompound compound = NBTTags.NBTMerge(this.readNpcData(npc), data.data);
            npc.display.readToNBT(compound);
            npc.stats.readToNBT(compound);
            npc.advanced.readToNBT(compound);
            npc.inventory.readEntityFromNBT(compound);
            if (compound.func_74764_b("ModelData")) {
                ((EntityCustomNpc)npc).modelData.readFromNBT(compound.func_74775_l("ModelData"));
            }
            npc.ais.readToNBT(compound);
            npc.transform.readToNBT(compound);
            npc.ais.setMovingPath(points);
            npc.updateClient = true;
        }
    }

    private void cleanTags(NBTTagCompound compound) {
        compound.func_82580_o("MovingPathNew");
    }

    public LinkedData getData(String name) {
        for (LinkedData data : this.list) {
            if (!data.name.equalsIgnoreCase(name)) continue;
            return data;
        }
        return null;
    }

    private NBTTagCompound readNpcData(EntityNPCInterface npc) {
        NBTTagCompound compound = new NBTTagCompound();
        npc.display.writeToNBT(compound);
        npc.inventory.writeEntityToNBT(compound);
        npc.stats.writeToNBT(compound);
        npc.ais.writeToNBT(compound);
        npc.advanced.writeToNBT(compound);
        npc.transform.writeToNBT(compound);
        compound.func_74782_a("ModelData", (NBTBase)((EntityCustomNpc)npc).modelData.writeToNBT());
        return compound;
    }

    public void saveNpcData(EntityNPCInterface npc) {
        NBTTagCompound compound = this.readNpcData(npc);
        this.cleanTags(compound);
        if (npc.linkedData.data.equals((Object)compound)) {
            return;
        }
        npc.linkedData.data = compound;
        npc.linkedData.time = System.currentTimeMillis();
        this.save();
    }

    public void removeData(String name) {
        Iterator<LinkedData> ita = this.list.iterator();
        while (ita.hasNext()) {
            if (!ita.next().name.equalsIgnoreCase(name)) continue;
            ita.remove();
        }
        this.save();
    }

    public void addData(String name) {
        if (this.getData(name) != null || name.isEmpty()) {
            return;
        }
        LinkedData data = new LinkedData();
        data.name = name;
        this.list.add(data);
        this.save();
    }

    public static class LinkedData {
        public String name = "LinkedNpc";
        public long time = 0L;
        public NBTTagCompound data = new NBTTagCompound();

        public LinkedData() {
            this.time = System.currentTimeMillis();
        }

        public void setNBT(NBTTagCompound compound) {
            this.name = compound.func_74779_i("LinkedName");
            this.data = compound.func_74775_l("NPCData");
        }

        public NBTTagCompound getNBT() {
            NBTTagCompound compound = new NBTTagCompound();
            compound.func_74778_a("LinkedName", this.name);
            compound.func_74782_a("NPCData", (NBTBase)this.data);
            return compound;
        }
    }
}

