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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.NBTTags;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.linked.LinkedBudget;
import noppes.npcs.rework.linked.LinkedCategories;
import noppes.npcs.rework.linked.LinkedOwnFields;
import noppes.npcs.util.NBTJsonUtil;

public class LinkedNpcController {
    public static LinkedNpcController Instance;
    public List<LinkedData> list = new ArrayList<LinkedData>();
    public final LinkedBudget budget = new LinkedBudget();

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
        File file = new File(this.getDir(), npc.name + ".json");
        try {
            NBTJsonUtil.SaveFile(file, npc.getNBT());
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
            if (npc.posX == 0.0 && npc.posY == 0.0 && npc.posX == 0.0) {
                return;
            }
            npc.linkedLast = data.time;
            if (CustomNpcs.RwLinkedGroups == 1) {
                long start = System.nanoTime();
                this.applyCategories(npc, data, CustomNpcs.RwLinkedOwnFields == 1);
                this.budget.nanos += System.nanoTime() - start;
                return;
            }
            List<int[]> points = npc.ais.getMovingPath();
            NBTTagCompound own = this.readNpcData(npc);
            NBTTagCompound compound = NBTTags.NBTMerge(own, data.data);
            if (CustomNpcs.RwLinkedOwnFields == 1) {
                LinkedOwnFields.restore(own, compound);
            }
            npc.display.readToNBT(compound);
            npc.stats.readToNBT(compound);
            npc.advanced.readToNBT(compound);
            npc.inventory.readEntityFromNBT(compound);
            if (compound.hasKey("ModelData")) {
                ((EntityCustomNpc)npc).modelData.readFromNBT(compound.getCompoundTag("ModelData"));
            }
            npc.ais.readToNBT(compound);
            npc.transform.readToNBT(compound);
            npc.ais.setMovingPath(points);
            if (CustomNpcs.RwLinkedOwnFields == 1) {
                // setMovingPath moves the home to the first path point (Q384); put the member's own home back.
                BlockPos home = LinkedOwnFields.home(own);
                if (home != null) {
                    npc.ais.setStartPos(home);
                }
            }
            npc.updateClient = true;
        }
    }

    /**
     * M13.2 (Q389): only the group's selected categories reach the member, and only their readers run. With AI off
     * setMovingPath is skipped too, because it would move the member's home (Q384).
     */
    private void applyCategories(EntityNPCInterface npc, LinkedData data, boolean keepOwn) {
        int mask = data.categories;
        List<int[]> points = npc.ais.getMovingPath();
        NBTTagCompound own = this.readNpcData(npc);
        NBTTagCompound compound = NBTTags.NBTMerge(own, LinkedCategories.filter(data.data, mask));
        if (keepOwn) {
            LinkedOwnFields.restore(own, compound);
        }
        if (LinkedCategories.has(mask, LinkedCategories.APPEARANCE | LinkedCategories.NAME)) {
            npc.display.readToNBT(compound);
        }
        if (LinkedCategories.has(mask, LinkedCategories.STATS)) {
            npc.stats.readToNBT(compound);
        }
        if (LinkedCategories.has(mask, LinkedCategories.ADVANCED)) {
            npc.advanced.readToNBT(compound);
        }
        if (LinkedCategories.has(mask, LinkedCategories.INVENTORY)) {
            npc.inventory.readEntityFromNBT(compound);
        }
        if (LinkedCategories.has(mask, LinkedCategories.APPEARANCE) && compound.hasKey("ModelData")) {
            ((EntityCustomNpc)npc).modelData.readFromNBT(compound.getCompoundTag("ModelData"));
        }
        if (LinkedCategories.has(mask, LinkedCategories.AI)) {
            npc.ais.readToNBT(compound);
            npc.ais.setMovingPath(points);
            if (keepOwn) {
                BlockPos home = LinkedOwnFields.home(own);
                if (home != null) {
                    npc.ais.setStartPos(home);
                }
            }
        }
        if (LinkedCategories.has(mask, LinkedCategories.TRANSFORM)) {
            npc.transform.readToNBT(compound);
        }
        if (LinkedCategories.has(mask, LinkedCategories.SCRIPTS) && data.data.hasKey("Scripts")) {
            NBTTagCompound scripts = LinkedCategories.filter(data.data, LinkedCategories.SCRIPTS);
            // Rereading would drop the running engines, so a member with the same scripts keeps them (Q390).
            if (!scripts.equals(this.readScripts(npc))) {
                npc.script.readFromNBT(scripts);
                npc.updateAI = true;
                npc.script.lastInited = -1L;
            }
        }
        npc.updateClient = true;
    }

    /** The NPC's scripts without the per-NPC console text. */
    private NBTTagCompound readScripts(EntityNPCInterface npc) {
        NBTTagCompound compound = npc.script.writeToNBT(new NBTTagCompound());
        NBTTagList tabs = compound.getTagList("Scripts", 10);
        for (int i = 0; i < tabs.tagCount(); i++) {
            tabs.getCompoundTagAt(i).removeTag("Console");
        }
        return compound;
    }

    /** M13.3: a snapshot of an NPC for a one-time copy with the given categories. */
    public LinkedData copyOf(EntityNPCInterface source, int mask) {
        LinkedData once = new LinkedData();
        once.data = this.readNpcData(source);
        this.cleanTags(once.data);
        if (LinkedCategories.has(mask, LinkedCategories.SCRIPTS)) {
            this.addScripts(source, once.data);
        }
        once.categories = mask;
        return once;
    }

    /** M13.3 (Q403): no link is made, and a copy always keeps the target's own home, orientation and path. */
    public void copyTo(EntityNPCInterface target, LinkedData once) {
        this.applyCategories(target, once, true);
    }

    private void addScripts(EntityNPCInterface npc, NBTTagCompound compound) {
        NBTTagCompound scripts = this.readScripts(npc);
        for (String key : scripts.getKeySet()) {
            compound.setTag(key, scripts.getTag(key));
        }
    }

    /** False when a member over this tick's budget has to wait for its next check (Q391). */
    public boolean mayApply(EntityNPCInterface npc) {
        if (CustomNpcs.RwLinkedGroups != 1) {
            return true;
        }
        return this.budget.tryAcquire(npc.world.getMinecraftServer().getTickCounter(), CustomNpcs.RwLinkedBudget);
    }

    private File previousFile(LinkedData data) {
        return new File(this.getDir(), data.name + ".json_prev");
    }

    public boolean hasPrevious(LinkedData data) {
        return this.previousFile(data).isFile();
    }

    private void keepPrevious(LinkedData data) {
        if (data.data.hasNoTags()) {
            return;
        }
        try {
            NBTJsonUtil.SaveFile(this.previousFile(data), data.getNBT());
        }
        catch (IOException | NBTJsonUtil.JsonException e) {
            LogWriter.except(e);
        }
    }

    /** M13.2 (Q390): swaps the group data with the kept version, so a second undo brings the change back. */
    public boolean undo(LinkedData data) {
        File file = this.previousFile(data);
        if (!file.isFile()) {
            return false;
        }
        NBTTagCompound previous;
        try {
            previous = NBTJsonUtil.LoadFile(file).getCompoundTag("NPCData");
        }
        catch (IOException | NBTJsonUtil.JsonException e) {
            LogWriter.except(e);
            return false;
        }
        if (previous.hasNoTags()) {
            return false;
        }
        this.keepPrevious(data);
        data.data = previous;
        this.changed(data);
        return true;
    }

    public void setCategories(LinkedData data, int mask) {
        data.categories = mask;
        this.changed(data);
    }

    private void changed(LinkedData data) {
        data.time = Math.max(System.currentTimeMillis(), data.time + 1L);
        this.save();
    }

    private void cleanTags(NBTTagCompound compound) {
        compound.removeTag("MovingPathNew");
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
        compound.setTag("ModelData", (NBTBase)((EntityCustomNpc)npc).modelData.writeToNBT());
        return compound;
    }

    public void saveNpcData(EntityNPCInterface npc) {
        NBTTagCompound compound = this.readNpcData(npc);
        this.cleanTags(compound);
        if (CustomNpcs.RwLinkedGroups == 1) {
            this.saveCategories(npc, compound);
            return;
        }
        if (npc.linkedData.data.equals((Object)compound)) {
            return;
        }
        npc.linkedData.data = compound;
        npc.linkedData.time = System.currentTimeMillis();
        this.save();
    }

    private void saveCategories(EntityNPCInterface npc, NBTTagCompound compound) {
        LinkedData data = npc.linkedData;
        if (LinkedCategories.has(data.categories, LinkedCategories.SCRIPTS)) {
            this.addScripts(npc, compound);
        }
        // With own fields on, a member walking its path is no change of the group and must not use up the undo (Q390).
        boolean same = CustomNpcs.RwLinkedOwnFields == 1 ? LinkedOwnFields.sameShared(data.data, compound) : data.data.equals((Object)compound);
        if (same) {
            return;
        }
        this.keepPrevious(data);
        data.data = compound;
        this.changed(data);
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
        /** M13.2: read by RwLinkedGroups=1 only; the file gets the key only when it differs from the default. */
        public int categories = LinkedCategories.DEFAULT;

        public LinkedData() {
            this.time = System.currentTimeMillis();
        }

        public void setNBT(NBTTagCompound compound) {
            this.name = compound.getString("LinkedName");
            this.data = compound.getCompoundTag("NPCData");
            int mask = compound.hasKey("RwCategories", 8) ? LinkedCategories.parse(compound.getString("RwCategories")) : -1;
            this.categories = mask < 0 ? LinkedCategories.DEFAULT : mask;
        }

        public NBTTagCompound getNBT() {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setString("LinkedName", this.name);
            compound.setTag("NPCData", (NBTBase)this.data);
            if (this.categories != LinkedCategories.DEFAULT) {
                compound.setString("RwCategories", LinkedCategories.toText(this.categories));
            }
            return compound;
        }
    }
}

