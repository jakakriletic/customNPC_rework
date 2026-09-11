/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.math.BlockPos
 *  net.minecraftforge.fml.common.eventhandler.Event
 */
package noppes.npcs.controllers.data;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;

public class PlayerScriptData
implements IScriptHandler {
    private List<ScriptContainer> scripts = new ArrayList<ScriptContainer>();
    private String scriptLanguage = "ECMAScript";
    private EntityPlayer player;
    private IPlayer playerAPI;
    private long lastPlayerUpdate = 0L;
    public long lastInited = -1L;
    public boolean hadInteract = true;
    private boolean enabled = false;
    private static Map<Long, String> console = new TreeMap<Long, String>();
    private static List<Integer> errored = new ArrayList<Integer>();

    public PlayerScriptData(EntityPlayer player) {
        this.player = player;
    }

    public void clear() {
        console = new TreeMap<Long, String>();
        errored = new ArrayList<Integer>();
        this.scripts = new ArrayList<ScriptContainer>();
    }

    public void readFromNBT(NBTTagCompound compound) {
        this.scripts = NBTTags.GetScript(compound.getTagList("Scripts", 10), this);
        this.scriptLanguage = compound.getString("ScriptLanguage");
        this.enabled = compound.getBoolean("ScriptEnabled");
        console = NBTTags.GetLongStringMap(compound.getTagList("ScriptConsole", 10));
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("Scripts", (NBTBase)NBTTags.NBTScript(this.scripts));
        compound.setString("ScriptLanguage", this.scriptLanguage);
        compound.setBoolean("ScriptEnabled", this.enabled);
        compound.setTag("ScriptConsole", (NBTBase)NBTTags.NBTLongStringMap(console));
        return compound;
    }

    @Override
    public void runScript(EnumScriptType type, Event event) {
        if (!this.isEnabled()) {
            return;
        }
        if (ScriptController.Instance.lastLoaded > this.lastInited || ScriptController.Instance.lastPlayerUpdate > this.lastPlayerUpdate) {
            this.lastInited = ScriptController.Instance.lastLoaded;
            errored.clear();
            if (this.player != null) {
                this.scripts.clear();
                for (ScriptContainer script : ScriptController.Instance.playerScripts.scripts) {
                    ScriptContainer s = new ScriptContainer(this);
                    s.readFromNBT(script.writeToNBT(new NBTTagCompound()));
                    this.scripts.add(s);
                }
            }
            this.lastPlayerUpdate = ScriptController.Instance.lastPlayerUpdate;
            if (type != EnumScriptType.INIT) {
                EventHooks.onPlayerInit(this);
            }
        }
        for (int i = 0; i < this.scripts.size(); ++i) {
            ScriptContainer script;
            script = this.scripts.get(i);
            if (errored.contains(i)) continue;
            script.run(type, event);
            if (script.errored) {
                errored.add(i);
            }
            for (Map.Entry<Long, String> entry : script.console.entrySet()) {
                if (console.containsKey(entry.getKey())) continue;
                console.put(entry.getKey(), " tab " + (i + 1) + ":\n" + entry.getValue());
            }
            script.console.clear();
        }
    }

    public boolean isEnabled() {
        return ScriptController.Instance.playerScripts.enabled && ScriptController.HasStart && (this.player == null || !this.player.world.isRemote);
    }

    @Override
    public boolean isClient() {
        return !this.player.isServerWorld();
    }

    @Override
    public boolean getEnabled() {
        return ScriptController.Instance.playerScripts.enabled;
    }

    @Override
    public void setEnabled(boolean bo) {
        this.enabled = bo;
    }

    @Override
    public String getLanguage() {
        return ScriptController.Instance.playerScripts.scriptLanguage;
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
        if (this.player == null) {
            return "Global script";
        }
        BlockPos pos = this.player.getPosition();
        return MoreObjects.toStringHelper((Object)this.player).add("x", pos.getX()).add("y", pos.getY()).add("z", pos.getZ()).toString();
    }

    public IPlayer getPlayer() {
        if (this.playerAPI == null) {
            this.playerAPI = (IPlayer)NpcAPI.Instance().getIEntity((Entity)this.player);
        }
        return this.playerAPI;
    }

    @Override
    public Map<Long, String> getConsoleText() {
        return console;
    }

    @Override
    public void clearConsole() {
        console.clear();
    }
}

