/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraftforge.fml.common.eventhandler.Event
 */
package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.item.IItemScripted;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.items.ItemScripted;

public class ItemScriptedWrapper
extends ItemStackWrapper
implements IItemScripted,
IScriptHandler {
    public List<ScriptContainer> scripts = new ArrayList<ScriptContainer>();
    public String scriptLanguage = "ECMAScript";
    public boolean enabled = false;
    public long lastInited = -1L;
    public boolean updateClient = false;
    public boolean durabilityShow = true;
    public double durabilityValue = 1.0;
    public int durabilityColor = -1;
    public int itemColor = -1;
    public int stackSize = 64;
    public boolean loaded = false;

    public ItemScriptedWrapper(ItemStack item) {
        super(item);
    }

    @Override
    public boolean hasTexture(int damage) {
        return ItemScripted.Resources.containsKey(damage);
    }

    @Override
    public String getTexture(int damage) {
        return ItemScripted.Resources.get(damage);
    }

    @Override
    public void setTexture(int damage, String texture) {
        if (damage == 0) {
            throw new CustomNPCsException("Can't set texture for 0", new Object[0]);
        }
        String old = ItemScripted.Resources.get(damage);
        if (old == texture || old != null && texture != null && old.equals(texture)) {
            return;
        }
        ItemScripted.Resources.put(damage, texture);
        SyncController.syncScriptItemsEverybody();
    }

    public NBTTagCompound getScriptNBT(NBTTagCompound compound) {
        compound.func_74782_a("Scripts", (NBTBase)NBTTags.NBTScript(this.scripts));
        compound.func_74778_a("ScriptLanguage", this.scriptLanguage);
        compound.func_74757_a("ScriptEnabled", this.enabled);
        return compound;
    }

    @Override
    public NBTTagCompound getMCNbt() {
        NBTTagCompound compound = super.getMCNbt();
        this.getScriptNBT(compound);
        compound.func_74757_a("DurabilityShow", this.durabilityShow);
        compound.func_74780_a("DurabilityValue", this.durabilityValue);
        compound.func_74768_a("DurabilityColor", this.durabilityColor);
        compound.func_74768_a("ItemColor", this.itemColor);
        compound.func_74768_a("MaxStackSize", this.stackSize);
        return compound;
    }

    public void setScriptNBT(NBTTagCompound compound) {
        if (!compound.func_74764_b("Scripts")) {
            return;
        }
        this.scripts = NBTTags.GetScript(compound.func_150295_c("Scripts", 10), this);
        this.scriptLanguage = compound.func_74779_i("ScriptLanguage");
        this.enabled = compound.func_74767_n("ScriptEnabled");
    }

    @Override
    public void setMCNbt(NBTTagCompound compound) {
        super.setMCNbt(compound);
        this.setScriptNBT(compound);
        this.durabilityShow = compound.func_74767_n("DurabilityShow");
        this.durabilityValue = compound.func_74769_h("DurabilityValue");
        if (compound.func_74764_b("DurabilityColor")) {
            this.durabilityColor = compound.func_74762_e("DurabilityColor");
        }
        this.itemColor = compound.func_74762_e("ItemColor");
        this.stackSize = compound.func_74762_e("MaxStackSize");
    }

    @Override
    public int getType() {
        return 6;
    }

    @Override
    public void runScript(EnumScriptType type, Event event) {
        if (!this.loaded) {
            this.loadScriptData();
            this.loaded = true;
        }
        if (!this.isEnabled()) {
            return;
        }
        if (ScriptController.Instance.lastLoaded > this.lastInited) {
            this.lastInited = ScriptController.Instance.lastLoaded;
            if (type != EnumScriptType.INIT) {
                EventHooks.onScriptItemInit(this);
            }
        }
        for (ScriptContainer script : this.scripts) {
            script.run(type, event);
        }
    }

    private boolean isEnabled() {
        return this.enabled && ScriptController.HasStart;
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public boolean getEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean bo) {
        this.enabled = bo;
    }

    @Override
    public String getLanguage() {
        return this.scriptLanguage;
    }

    @Override
    public void setLanguage(String lang) {
        this.scriptLanguage = lang;
    }

    @Override
    public List<ScriptContainer> getScripts() {
        return this.scripts;
    }

    @Override
    public String noticeString() {
        return "ScriptedItem";
    }

    @Override
    public Map<Long, String> getConsoleText() {
        TreeMap<Long, String> map = new TreeMap<Long, String>();
        int tab = 0;
        for (ScriptContainer script : this.getScripts()) {
            ++tab;
            for (Map.Entry<Long, String> entry : script.console.entrySet()) {
                map.put(entry.getKey(), " tab " + tab + ":\n" + entry.getValue());
            }
        }
        return map;
    }

    @Override
    public void clearConsole() {
        for (ScriptContainer script : this.getScripts()) {
            script.console.clear();
        }
    }

    @Override
    public int getMaxStackSize() {
        return this.stackSize;
    }

    @Override
    public void setMaxStackSize(int size) {
        if (size < 1 || size > 64) {
            throw new CustomNPCsException("Stacksize has to be between 1 and 64", new Object[0]);
        }
        this.stackSize = size;
    }

    @Override
    public double getDurabilityValue() {
        return this.durabilityValue;
    }

    @Override
    public void setDurabilityValue(float value) {
        if ((double)value != this.durabilityValue) {
            this.updateClient = true;
        }
        this.durabilityValue = value;
    }

    @Override
    public boolean getDurabilityShow() {
        return this.durabilityShow;
    }

    @Override
    public void setDurabilityShow(boolean bo) {
        if (bo != this.durabilityShow) {
            this.updateClient = true;
        }
        this.durabilityShow = bo;
    }

    @Override
    public int getDurabilityColor() {
        return this.durabilityColor;
    }

    @Override
    public void setDurabilityColor(int color) {
        if (color != this.durabilityColor) {
            this.updateClient = true;
        }
        this.durabilityColor = color;
    }

    @Override
    public int getColor() {
        return this.itemColor;
    }

    @Override
    public void setColor(int color) {
        if (color != this.itemColor) {
            this.updateClient = true;
        }
        this.itemColor = color;
    }

    public void saveScriptData() {
        NBTTagCompound c = this.item.func_77978_p();
        if (c == null) {
            c = new NBTTagCompound();
            this.item.func_77982_d(c);
        }
        c.func_74782_a("ScriptedData", (NBTBase)this.getScriptNBT(new NBTTagCompound()));
    }

    public void loadScriptData() {
        NBTTagCompound c = this.item.func_77978_p();
        if (c == null) {
            return;
        }
        this.setScriptNBT(c.func_74775_l("ScriptedData"));
    }
}

