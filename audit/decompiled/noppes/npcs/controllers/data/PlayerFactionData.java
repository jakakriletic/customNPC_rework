/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.HashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerScriptData;

public class PlayerFactionData {
    public HashMap<Integer, Integer> factionData = new HashMap();

    public void loadNBTData(NBTTagCompound compound) {
        HashMap<Integer, Integer> factionData = new HashMap<Integer, Integer>();
        if (compound == null) {
            return;
        }
        NBTTagList list = compound.func_150295_c("FactionData", 10);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = list.func_150305_b(i);
            factionData.put(nbttagcompound.func_74762_e("Faction"), nbttagcompound.func_74762_e("Points"));
        }
        this.factionData = factionData;
    }

    public void saveNBTData(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (int faction : this.factionData.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Faction", faction);
            nbttagcompound.func_74768_a("Points", this.factionData.get(faction).intValue());
            list.func_74742_a((NBTBase)nbttagcompound);
        }
        compound.func_74782_a("FactionData", (NBTBase)list);
    }

    public int getFactionPoints(EntityPlayer player, int factionId) {
        Faction faction = FactionController.instance.getFaction(factionId);
        if (faction == null) {
            return 0;
        }
        if (!this.factionData.containsKey(factionId)) {
            if (player.field_70170_p.field_72995_K) {
                return faction.defaultPoints;
            }
            PlayerScriptData handler = PlayerData.get((EntityPlayer)player).scriptData;
            PlayerWrapper wrapper = (PlayerWrapper)NpcAPI.Instance().getIEntity((Entity)player);
            PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, faction.defaultPoints, true);
            EventHooks.OnPlayerFactionChange(handler, event);
            this.factionData.put(factionId, event.points);
        }
        return this.factionData.get(factionId);
    }

    public void increasePoints(EntityPlayer player, int factionId, int points) {
        PlayerEvent.FactionUpdateEvent event;
        Faction faction = FactionController.instance.getFaction(factionId);
        if (faction == null || player == null || player.field_70170_p.field_72995_K) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)player).scriptData;
        PlayerWrapper wrapper = (PlayerWrapper)NpcAPI.Instance().getIEntity((Entity)player);
        if (!this.factionData.containsKey(factionId)) {
            event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, faction.defaultPoints, true);
            EventHooks.OnPlayerFactionChange(handler, event);
            this.factionData.put(factionId, event.points);
        }
        event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, points, false);
        EventHooks.OnPlayerFactionChange(handler, event);
        this.factionData.put(factionId, this.factionData.get(factionId) + points);
    }

    public NBTTagCompound getPlayerGuiData() {
        NBTTagCompound compound = new NBTTagCompound();
        this.saveNBTData(compound);
        NBTTagList list = new NBTTagList();
        for (int id : this.factionData.keySet()) {
            Faction faction = FactionController.instance.getFaction(id);
            if (faction == null || faction.hideFaction) continue;
            NBTTagCompound com = new NBTTagCompound();
            faction.writeNBT(com);
            list.func_74742_a((NBTBase)com);
        }
        compound.func_74782_a("FactionList", (NBTBase)list);
        return compound;
    }
}

