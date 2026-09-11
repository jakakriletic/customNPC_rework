/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.controllers.GlobalDataController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerItemGiverData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;

public class JobItemGiver
extends JobInterface {
    public int cooldownType = 0;
    public int givingMethod = 0;
    public int cooldown = 10;
    public NpcMiscInventory inventory;
    public int itemGiverId = 0;
    public List<String> lines = new ArrayList<String>();
    private int ticks = 10;
    private List<EntityPlayer> recentlyChecked = new ArrayList<EntityPlayer>();
    private List<EntityPlayer> toCheck;
    public Availability availability = new Availability();

    public JobItemGiver(EntityNPCInterface npc) {
        super(npc);
        this.inventory = new NpcMiscInventory(9);
        this.lines.add("Have these items {player}");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.func_74768_a("igCooldownType", this.cooldownType);
        nbttagcompound.func_74768_a("igGivingMethod", this.givingMethod);
        nbttagcompound.func_74768_a("igCooldown", this.cooldown);
        nbttagcompound.func_74768_a("ItemGiverId", this.itemGiverId);
        nbttagcompound.func_74782_a("igLines", (NBTBase)NBTTags.nbtStringList(this.lines));
        nbttagcompound.func_74782_a("igJobInventory", (NBTBase)this.inventory.getToNBT());
        nbttagcompound.func_74782_a("igAvailability", (NBTBase)this.availability.writeToNBT(new NBTTagCompound()));
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        this.itemGiverId = nbttagcompound.func_74762_e("ItemGiverId");
        this.cooldownType = nbttagcompound.func_74762_e("igCooldownType");
        this.givingMethod = nbttagcompound.func_74762_e("igGivingMethod");
        this.cooldown = nbttagcompound.func_74762_e("igCooldown");
        this.lines = NBTTags.getStringList(nbttagcompound.func_150295_c("igLines", 10));
        this.inventory.setFromNBT(nbttagcompound.func_74775_l("igJobInventory"));
        if (this.itemGiverId == 0 && GlobalDataController.instance != null) {
            this.itemGiverId = GlobalDataController.instance.incrementItemGiverId();
        }
        this.availability.readFromNBT(nbttagcompound.func_74775_l("igAvailability"));
    }

    public NBTTagList newHashMapNBTList(HashMap<String, Long> lines) {
        NBTTagList nbttaglist = new NBTTagList();
        HashMap<String, Long> lines2 = lines;
        for (String s : lines2.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74778_a("Line", s);
            nbttagcompound.func_74772_a("Time", lines.get(s).longValue());
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public HashMap<String, Long> getNBTLines(NBTTagList tagList) {
        HashMap<String, Long> map = new HashMap<String, Long>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            String line = nbttagcompound.func_74779_i("Line");
            long time = nbttagcompound.func_74763_f("Time");
            map.put(line, time);
        }
        return map;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private boolean giveItems(EntityPlayer player) {
        PlayerItemGiverData data = PlayerData.get((EntityPlayer)player).itemgiverData;
        if (!this.canPlayerInteract(data)) {
            return false;
        }
        Vector<ItemStack> items = new Vector<ItemStack>();
        Vector<Object> toGive = new Vector<ItemStack>();
        for (Object is : this.inventory.items) {
            if (is.func_190926_b()) continue;
            items.add(is.func_77946_l());
        }
        if (items.isEmpty()) {
            return false;
        }
        if (this.isAllGiver()) {
            toGive = items;
        } else if (this.isRemainingGiver()) {
            for (Object is : items) {
                if (this.playerHasItem(player, is.func_77973_b())) continue;
                toGive.add((ItemStack)is);
            }
        } else if (this.isRandomGiver()) {
            toGive.add(((ItemStack)items.get(this.npc.field_70170_p.field_73012_v.nextInt(items.size()))).func_77946_l());
        } else if (this.isGiverWhenNotOwnedAny()) {
            boolean ownsItems = false;
            for (ItemStack is : items) {
                if (!this.playerHasItem(player, is.func_77973_b())) continue;
                return false;
            }
            if (ownsItems) return false;
            toGive = items;
        } else if (this.isChainedGiver()) {
            int itemIndex = data.getItemIndex(this);
            int i = 0;
            for (ItemStack item : this.inventory.items) {
                if (i == itemIndex) {
                    toGive.add(item);
                    break;
                }
                ++i;
            }
        }
        if (toGive.isEmpty()) {
            return false;
        }
        if (!this.givePlayerItems(player, toGive)) return false;
        if (!this.lines.isEmpty()) {
            this.npc.say(player, new Line(this.lines.get(this.npc.func_70681_au().nextInt(this.lines.size()))));
        }
        if (this.isDaily()) {
            data.setTime(this, this.getDay());
        } else {
            data.setTime(this, System.currentTimeMillis());
        }
        if (!this.isChainedGiver()) return true;
        data.setItemIndex(this, (data.getItemIndex(this) + 1) % this.inventory.items.size());
        return true;
    }

    private int getDay() {
        return (int)(this.npc.field_70170_p.func_82737_E() / 24000L);
    }

    private boolean canPlayerInteract(PlayerItemGiverData data) {
        if (this.inventory.items.isEmpty()) {
            return false;
        }
        if (this.isOnTimer()) {
            if (!data.hasInteractedBefore(this)) {
                return true;
            }
            return data.getTime(this) + (long)(this.cooldown * 1000) < System.currentTimeMillis();
        }
        if (this.isGiveOnce()) {
            return !data.hasInteractedBefore(this);
        }
        if (this.isDaily()) {
            if (!data.hasInteractedBefore(this)) {
                return true;
            }
            return (long)this.getDay() > data.getTime(this);
        }
        return false;
    }

    private boolean givePlayerItems(EntityPlayer player, Vector<ItemStack> toGive) {
        if (toGive.isEmpty()) {
            return false;
        }
        if (this.freeInventorySlots(player) < toGive.size()) {
            return false;
        }
        for (ItemStack is : toGive) {
            this.npc.givePlayerItem(player, is);
        }
        return true;
    }

    private boolean playerHasItem(EntityPlayer player, Item item) {
        for (ItemStack is : player.field_71071_by.field_70462_a) {
            if (is.func_190926_b() || is.func_77973_b() != item) continue;
            return true;
        }
        for (ItemStack is : player.field_71071_by.field_70460_b) {
            if (is.func_190926_b() || is.func_77973_b() != item) continue;
            return true;
        }
        return false;
    }

    private int freeInventorySlots(EntityPlayer player) {
        int i = 0;
        for (ItemStack is : player.field_71071_by.field_70462_a) {
            if (!NoppesUtilServer.IsItemStackNull(is)) continue;
            ++i;
        }
        return i;
    }

    private boolean isRandomGiver() {
        return this.givingMethod == 0;
    }

    private boolean isAllGiver() {
        return this.givingMethod == 1;
    }

    private boolean isRemainingGiver() {
        return this.givingMethod == 2;
    }

    private boolean isGiverWhenNotOwnedAny() {
        return this.givingMethod == 3;
    }

    private boolean isChainedGiver() {
        return this.givingMethod == 4;
    }

    public boolean isOnTimer() {
        return this.cooldownType == 0;
    }

    private boolean isGiveOnce() {
        return this.cooldownType == 1;
    }

    private boolean isDaily() {
        return this.cooldownType == 2;
    }

    @Override
    public boolean aiShouldExecute() {
        if (this.npc.isAttacking()) {
            return false;
        }
        --this.ticks;
        if (this.ticks > 0) {
            return false;
        }
        this.ticks = 10;
        this.toCheck = this.npc.field_70170_p.func_72872_a(EntityPlayer.class, this.npc.func_174813_aQ().func_72314_b(3.0, 3.0, 3.0));
        this.toCheck.removeAll(this.recentlyChecked);
        List listMax = this.npc.field_70170_p.func_72872_a(EntityPlayer.class, this.npc.func_174813_aQ().func_72314_b(10.0, 10.0, 10.0));
        this.recentlyChecked.retainAll(listMax);
        this.recentlyChecked.addAll(this.toCheck);
        return this.toCheck.size() > 0;
    }

    @Override
    public boolean aiContinueExecute() {
        return false;
    }

    @Override
    public void aiStartExecuting() {
        for (EntityPlayer player : this.toCheck) {
            if (!this.npc.canSee((Entity)player) || !this.availability.isAvailable(player)) continue;
            this.recentlyChecked.add(player);
            this.interact(player);
        }
    }

    @Override
    public void killed() {
    }

    private boolean interact(EntityPlayer player) {
        if (!this.giveItems(player)) {
            this.npc.say(player, this.npc.advanced.getInteractLine());
        }
        return true;
    }

    @Override
    public void delete() {
    }
}

