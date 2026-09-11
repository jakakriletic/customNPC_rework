/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.World
 */
package noppes.npcs.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.handler.ICloneHandler;
import noppes.npcs.util.NBTJsonUtil;

public class ServerCloneController
implements ICloneHandler {
    public static ServerCloneController Instance;

    public ServerCloneController() {
        this.loadClones();
    }

    private void loadClones() {
        try {
            File dir = new File(this.getDir(), "..");
            File file = new File(dir, "clonednpcs.dat");
            if (file.exists()) {
                Map<Integer, Map<String, NBTTagCompound>> clones = this.loadOldClones(file);
                file.delete();
                file = new File(dir, "clonednpcs.dat_old");
                if (file.exists()) {
                    file.delete();
                }
                for (int tab : clones.keySet()) {
                    Map<String, NBTTagCompound> map = clones.get(tab);
                    for (String name : map.keySet()) {
                        this.saveClone(tab, name, map.get(name));
                    }
                }
            }
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
    }

    public File getDir() {
        File dir = new File(CustomNpcs.getWorldSaveDirectory(), "clones");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    private Map<Integer, Map<String, NBTTagCompound>> loadOldClones(File file) throws Exception {
        HashMap<Integer, Map<String, NBTTagCompound>> clones = new HashMap<Integer, Map<String, NBTTagCompound>>();
        NBTTagCompound nbttagcompound1 = CompressedStreamTools.func_74796_a((InputStream)new FileInputStream(file));
        NBTTagList list = nbttagcompound1.func_150295_c("Data", 10);
        if (list == null) {
            return clones;
        }
        for (int i = 0; i < list.func_74745_c(); ++i) {
            HashMap<String, NBTTagCompound> tab;
            NBTTagCompound compound = list.func_150305_b(i);
            if (!compound.func_74764_b("ClonedTab")) {
                compound.func_74768_a("ClonedTab", 1);
            }
            if ((tab = (HashMap<String, NBTTagCompound>)clones.get(compound.func_74762_e("ClonedTab"))) == null) {
                tab = new HashMap<String, NBTTagCompound>();
                clones.put(compound.func_74762_e("ClonedTab"), tab);
            }
            String name = compound.func_74779_i("ClonedName");
            int number = 1;
            while (tab.containsKey(name)) {
                name = String.format("%s%s", compound.func_74779_i("ClonedName"), ++number);
            }
            compound.func_82580_o("ClonedName");
            compound.func_82580_o("ClonedTab");
            compound.func_82580_o("ClonedDate");
            this.cleanTags(compound);
            tab.put(name, compound);
        }
        return clones;
    }

    public NBTTagCompound getCloneData(ICommandSender player, String name, int tab) {
        File file = new File(new File(this.getDir(), tab + ""), name + ".json");
        if (!file.exists()) {
            if (player != null) {
                player.func_145747_a((ITextComponent)new TextComponentString("Could not find clone file"));
            }
            return null;
        }
        try {
            return NBTJsonUtil.LoadFile(file);
        }
        catch (Exception e) {
            LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
            if (player != null) {
                player.func_145747_a((ITextComponent)new TextComponentString(e.getMessage()));
            }
            return null;
        }
    }

    public void saveClone(int tab, String name, NBTTagCompound compound) {
        try {
            File dir = new File(this.getDir(), tab + "");
            if (!dir.exists()) {
                dir.mkdir();
            }
            String filename = name + ".json";
            File file = new File(dir, filename + "_new");
            File file2 = new File(dir, filename);
            NBTJsonUtil.SaveFile(file, compound);
            if (file2.exists()) {
                file2.delete();
            }
            file.renameTo(file2);
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
    }

    public List<String> getClones(int tab) {
        ArrayList<String> list = new ArrayList<String>();
        File dir = new File(this.getDir(), tab + "");
        if (!dir.exists() || !dir.isDirectory()) {
            return list;
        }
        for (String file : dir.list()) {
            if (!file.endsWith(".json")) continue;
            list.add(file.substring(0, file.length() - 5));
        }
        return list;
    }

    public boolean removeClone(String name, int tab) {
        File file = new File(new File(this.getDir(), tab + ""), name + ".json");
        if (!file.exists()) {
            return false;
        }
        file.delete();
        return true;
    }

    public String addClone(NBTTagCompound nbttagcompound, String name, int tab) {
        this.cleanTags(nbttagcompound);
        this.saveClone(tab, name, nbttagcompound);
        return name;
    }

    public void cleanTags(NBTTagCompound nbttagcompound) {
        NBTTagCompound adv;
        if (nbttagcompound.func_74764_b("ItemGiverId")) {
            nbttagcompound.func_74768_a("ItemGiverId", 0);
        }
        if (nbttagcompound.func_74764_b("TransporterId")) {
            nbttagcompound.func_74768_a("TransporterId", -1);
        }
        nbttagcompound.func_82580_o("StartPosNew");
        nbttagcompound.func_82580_o("StartPos");
        nbttagcompound.func_82580_o("MovingPathNew");
        nbttagcompound.func_82580_o("Pos");
        nbttagcompound.func_82580_o("Riding");
        nbttagcompound.func_82580_o("UUID");
        nbttagcompound.func_82580_o("UUIDMost");
        nbttagcompound.func_82580_o("UUIDLeast");
        if (!nbttagcompound.func_74764_b("ModRev")) {
            nbttagcompound.func_74768_a("ModRev", 1);
        }
        if (nbttagcompound.func_74764_b("TransformRole")) {
            adv = nbttagcompound.func_74775_l("TransformRole");
            adv.func_74768_a("TransporterId", -1);
            nbttagcompound.func_74782_a("TransformRole", (NBTBase)adv);
        }
        if (nbttagcompound.func_74764_b("TransformJob")) {
            adv = nbttagcompound.func_74775_l("TransformJob");
            adv.func_74768_a("ItemGiverId", 0);
            nbttagcompound.func_74782_a("TransformJob", (NBTBase)adv);
        }
        if (nbttagcompound.func_74764_b("TransformAI")) {
            adv = nbttagcompound.func_74775_l("TransformAI");
            adv.func_82580_o("StartPosNew");
            adv.func_82580_o("StartPos");
            adv.func_82580_o("MovingPathNew");
            nbttagcompound.func_74782_a("TransformAI", (NBTBase)adv);
        }
        if (nbttagcompound.func_74764_b("id")) {
            String id = nbttagcompound.func_74779_i("id");
            id = id.replace("customnpcs.", "customnpcs:");
            nbttagcompound.func_74778_a("id", id);
        }
    }

    @Override
    public IEntity spawn(double x, double y, double z, int tab, String name, IWorld world) {
        NBTTagCompound compound = this.getCloneData(null, name, tab);
        if (compound == null) {
            throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name, new Object[0]);
        }
        Entity entity = NoppesUtilServer.spawnClone(compound, x, y, z, (World)world.getMCWorld());
        if (entity == null) {
            return null;
        }
        return NpcAPI.Instance().getIEntity(entity);
    }

    @Override
    public IEntity get(int tab, String name, IWorld world) {
        NBTTagCompound compound = this.getCloneData(null, name, tab);
        if (compound == null) {
            throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name, new Object[0]);
        }
        Instance.cleanTags(compound);
        Entity entity = EntityList.func_75615_a((NBTTagCompound)compound, (World)world.getMCWorld());
        if (entity == null) {
            return null;
        }
        return NpcAPI.Instance().getIEntity(entity);
    }

    @Override
    public void set(int tab, String name, IEntity entity) {
        NBTTagCompound compound = new NBTTagCompound();
        if (!entity.getMCEntity().func_184198_c(compound)) {
            throw new CustomNPCsException("Cannot save dead entities", new Object[0]);
        }
        this.cleanTags(compound);
        this.saveClone(tab, name, compound);
    }

    @Override
    public void remove(int tab, String name) {
        this.removeClone(name, tab);
    }
}

