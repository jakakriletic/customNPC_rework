/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.client.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.LogWriter;
import noppes.npcs.client.controllers.Preset;
import noppes.npcs.rework.data.CompressedNbtFile;

public class PresetController {
    public HashMap<String, Preset> presets = new HashMap();
    private File dir;
    public static PresetController instance;

    public PresetController(File dir) {
        instance = this;
        this.dir = dir;
        this.load();
    }

    public Preset getPreset(String username) {
        if (this.presets.isEmpty()) {
            this.load();
        }
        return this.presets.get(username.toLowerCase());
    }

    public void load() {
        NBTTagCompound compound = this.loadPreset();
        HashMap<String, Preset> presets = new HashMap<String, Preset>();
        if (compound != null) {
            NBTTagList list = compound.getTagList("Presets", 10);
            for (int i = 0; i < list.tagCount(); ++i) {
                NBTTagCompound comp = list.getCompoundTagAt(i);
                Preset preset = new Preset();
                preset.readFromNBT(comp);
                presets.put(preset.name.toLowerCase(), preset);
            }
        }
        Preset.FillDefault(presets);
        this.presets = presets;
    }

    private NBTTagCompound loadPreset() {
        String filename = "presets.dat";
        try {
            File file = new File(this.dir, filename);
            if (!file.exists()) {
                return null;
            }
            return CompressedStreamTools.readCompressed((InputStream)new FileInputStream(file));
        }
        catch (Exception e) {
            LogWriter.except(e);
            try {
                File file = new File(this.dir, filename + "_old");
                if (!file.exists()) {
                    return null;
                }
                return CompressedStreamTools.readCompressed((InputStream)new FileInputStream(file));
            }
            catch (Exception e2) {
                LogWriter.except(e2);
                return null;
            }
        }
    }

    public void save() {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Preset preset : this.presets.values()) {
            list.appendTag((NBTBase)preset.writeToNBT());
        }
        compound.setTag("Presets", (NBTBase)list);
        this.savePreset(compound);
    }

    private void savePreset(NBTTagCompound compound) {
        String filename = "presets.dat";
        try {
            File file = new File(this.dir, filename);
            CompressedNbtFile.save(file, compound);
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
    }

    public void addPreset(Preset preset) {
        while (this.presets.containsKey(preset.name.toLowerCase())) {
            preset.name = preset.name + "_";
        }
        this.presets.put(preset.name.toLowerCase(), preset);
        this.save();
    }

    public void removePreset(String preset) {
        if (preset == null) {
            return;
        }
        this.presets.remove(preset.toLowerCase());
        this.save();
    }
}

