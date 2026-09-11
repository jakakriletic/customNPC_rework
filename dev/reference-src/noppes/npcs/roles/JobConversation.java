/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;

public class JobConversation
extends JobInterface {
    public Availability availability = new Availability();
    private ArrayList<String> names = new ArrayList();
    private HashMap<String, EntityNPCInterface> npcs = new HashMap();
    public HashMap<Integer, ConversationLine> lines = new HashMap();
    public int quest = -1;
    public String questTitle = "";
    public int generalDelay = 400;
    public int ticks = 100;
    public int range = 20;
    private ConversationLine nextLine;
    private boolean hasStarted = false;
    private int startedTicks = 20;
    public int mode = 0;

    public JobConversation(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("ConversationAvailability", (NBTBase)this.availability.writeToNBT(new NBTTagCompound()));
        compound.setInteger("ConversationQuest", this.quest);
        compound.setInteger("ConversationDelay", this.generalDelay);
        compound.setInteger("ConversationRange", this.range);
        compound.setInteger("ConversationMode", this.mode);
        NBTTagList nbttaglist = new NBTTagList();
        for (int slot : this.lines.keySet()) {
            ConversationLine line = this.lines.get(slot);
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("Slot", slot);
            line.writeEntityToNBT(nbttagcompound);
            nbttaglist.appendTag((NBTBase)nbttagcompound);
        }
        compound.setTag("ConversationLines", (NBTBase)nbttaglist);
        if (this.hasQuest()) {
            compound.setString("ConversationQuestTitle", this.getQuest().title);
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.names.clear();
        this.availability.readFromNBT(compound.getCompoundTag("ConversationAvailability"));
        this.quest = compound.getInteger("ConversationQuest");
        this.generalDelay = compound.getInteger("ConversationDelay");
        this.questTitle = compound.getString("ConversationQuestTitle");
        this.range = compound.getInteger("ConversationRange");
        this.mode = compound.getInteger("ConversationMode");
        NBTTagList nbttaglist = compound.getTagList("ConversationLines", 10);
        HashMap<Integer, ConversationLine> map = new HashMap<Integer, ConversationLine>();
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            ConversationLine line = new ConversationLine();
            line.readEntityFromNBT(nbttagcompound);
            if (!line.npc.isEmpty() && !this.names.contains(line.npc.toLowerCase())) {
                this.names.add(line.npc.toLowerCase());
            }
            map.put(nbttagcompound.getInteger("Slot"), line);
        }
        this.lines = map;
        this.ticks = this.generalDelay;
    }

    public boolean hasQuest() {
        return this.getQuest() != null;
    }

    public Quest getQuest() {
        if (this.npc.isRemote()) {
            return null;
        }
        return QuestController.instance.quests.get(this.quest);
    }

    @Override
    public void aiUpdateTask() {
        --this.ticks;
        if (this.ticks > 0 || this.nextLine == null) {
            return;
        }
        this.say(this.nextLine);
        boolean seenNext = false;
        ConversationLine compare = this.nextLine;
        this.nextLine = null;
        for (ConversationLine line : this.lines.values()) {
            if (line.isEmpty()) continue;
            if (seenNext) {
                this.nextLine = line;
                break;
            }
            if (line != compare) continue;
            seenNext = true;
        }
        if (this.nextLine != null) {
            this.ticks = this.nextLine.delay;
        } else if (this.hasQuest()) {
            List inRange = this.npc.world.getEntitiesWithinAABB(EntityPlayer.class, this.npc.getEntityBoundingBox().grow((double)this.range, (double)this.range, (double)this.range));
            for (EntityPlayer player : inRange) {
                if (!this.availability.isAvailable(player)) continue;
                PlayerQuestController.addActiveQuest(this.getQuest(), player);
            }
        }
    }

    @Override
    public boolean aiShouldExecute() {
        if (this.lines.isEmpty() || this.npc.isKilled() || this.npc.isAttacking() || !this.shouldRun()) {
            return false;
        }
        if (!this.hasStarted && this.mode == 1) {
            if (this.startedTicks-- > 0) {
                return false;
            }
            this.startedTicks = 10;
            if (this.npc.world.getEntitiesWithinAABB(EntityPlayer.class, this.npc.getEntityBoundingBox().grow((double)this.range, (double)this.range, (double)this.range)).isEmpty()) {
                return false;
            }
        }
        for (ConversationLine line : this.lines.values()) {
            if (line == null || line.isEmpty()) continue;
            this.nextLine = line;
            break;
        }
        return this.nextLine != null;
    }

    private boolean shouldRun() {
        boolean bo;
        --this.ticks;
        if (this.ticks > 0) {
            return false;
        }
        this.npcs.clear();
        List list = this.npc.world.getEntitiesWithinAABB(EntityNPCInterface.class, this.npc.getEntityBoundingBox().grow(10.0, 10.0, 10.0));
        for (EntityNPCInterface npc : list) {
            if (npc.isKilled() || npc.isAttacking() || !this.names.contains(npc.getName().toLowerCase())) continue;
            this.npcs.put(npc.getName().toLowerCase(), npc);
        }
        boolean bl = bo = this.names.size() == this.npcs.size();
        if (!bo) {
            this.ticks = 20;
        }
        return bo;
    }

    @Override
    public boolean aiContinueExecute() {
        for (EntityNPCInterface npc : this.npcs.values()) {
            if (!npc.isKilled() && !npc.isAttacking()) continue;
            return false;
        }
        return this.nextLine != null;
    }

    @Override
    public void resetTask() {
        this.nextLine = null;
        this.ticks = this.generalDelay;
        this.hasStarted = false;
    }

    @Override
    public void aiStartExecuting() {
        this.startedTicks = 20;
        this.hasStarted = true;
    }

    private void say(ConversationLine line) {
        List inRange = this.npc.world.getEntitiesWithinAABB(EntityPlayer.class, this.npc.getEntityBoundingBox().grow((double)this.range, (double)this.range, (double)this.range));
        EntityNPCInterface npc = this.npcs.get(line.npc.toLowerCase());
        if (npc == null) {
            return;
        }
        for (EntityPlayer player : inRange) {
            if (!this.availability.isAvailable(player)) continue;
            npc.say(player, line);
        }
    }

    @Override
    public void reset() {
        this.hasStarted = false;
        this.resetTask();
        this.ticks = 60;
    }

    @Override
    public void killed() {
        this.reset();
    }

    public ConversationLine getLine(int slot) {
        if (this.lines.containsKey(slot)) {
            return this.lines.get(slot);
        }
        ConversationLine line = new ConversationLine();
        this.lines.put(slot, line);
        return line;
    }

    public class ConversationLine
    extends Line {
        public String npc = "";
        public int delay = 40;

        public void writeEntityToNBT(NBTTagCompound compound) {
            compound.setString("Line", this.text);
            compound.setString("Npc", this.npc);
            compound.setString("Sound", this.sound);
            compound.setInteger("Delay", this.delay);
        }

        public void readEntityFromNBT(NBTTagCompound compound) {
            this.text = compound.getString("Line");
            this.npc = compound.getString("Npc");
            this.sound = compound.getString("Sound");
            this.delay = compound.getInteger("Delay");
        }

        public boolean isEmpty() {
            return this.npc.isEmpty() || this.text.isEmpty();
        }
    }
}

