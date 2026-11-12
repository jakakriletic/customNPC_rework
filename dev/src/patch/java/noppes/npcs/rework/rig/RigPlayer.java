package noppes.npcs.rework.rig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.anim.AnimationEvent;

/**
 * M16: the action clip a rigged NPC plays (an attack, a roar, a wave) on top of its idle and walk.
 * The server owns it: it starts and ends clips, fires the clip's timed events (built-in {@code hit},
 * {@code sound}, {@code lunge}, {@code effect}; every event also goes to the script hook
 * {@code animationEvent}) and tells the tracking clients ({@link RigNet}). A client only keeps the age
 * for drawing. Not saved in NBT: a reloaded NPC starts idle.
 */
public final class RigPlayer {
    public static final int MAX_QUEUE = 32;
    private String clip = "";
    private int age;
    /** Client: ticks of the cross-fade from the previous pose into this clip (0 = the default fade). */
    private int blend;
    /** Server: counts every start and end; a client ignores older news (CustomNPCs sends NPC updates late, async). */
    private int seq;
    private final ArrayDeque<String> queue = new ArrayDeque<String>();
    private final List<AnimEvent> fired = new ArrayList<AnimEvent>();

    public String clip() { return clip; }

    public int age() { return age; }

    public int blend() { return blend; }

    public boolean playing() { return !clip.isEmpty(); }

    /** Server: starts a known clip now (clears the queue). False for an unknown clip or a dead NPC. */
    public boolean play(EntityNPCInterface npc, String name, int blendTicks) {
        if (npc.world.isRemote || !npc.isEntityAlive() || !npc.rwRig.active() || npc.rwRig.library().clipOrNull(name) == null) { return false; }
        queue.clear();
        begin(npc, name, blendTicks);
        return true;
    }

    /** Server: starts now when idle, else waits behind the others (at most {@link #MAX_QUEUE}). */
    public boolean queue(EntityNPCInterface npc, String name) {
        if (npc.world.isRemote || !npc.rwRig.active() || npc.rwRig.library().clipOrNull(name) == null || queue.size() >= MAX_QUEUE) { return false; }
        if (!playing()) { begin(npc, name, 0); }
        else { queue.add(name); }
        return true;
    }

    /** Server: stops the clip and clears the queue. */
    public boolean stop(EntityNPCInterface npc) {
        if (npc.world.isRemote) { return false; }
        queue.clear();
        if (playing()) { end(npc, false); }
        return true;
    }

    private void begin(EntityNPCInterface npc, String name, int blendTicks) {
        if (playing()) { end(npc, false); }
        clip = name; age = 0; blend = blendTicks; seq++;
        RigNet.sendStart(npc, name, 0, blendTicks, seq);
        if (npc.script.isEnabled()) { npc.script.runScript("animationStart", new AnimationEvent.Started(npc.wrappedNPC, name)); }
    }

    private void end(EntityNPCInterface npc, boolean finished) {
        String was = clip;
        int at = age;
        clip = ""; age = 0; blend = 0; seq++;
        RigNet.sendStart(npc, "", 0, 0, seq);
        if (npc.script.isEnabled()) { npc.script.runScript("animationEnd", new AnimationEvent.Ended(npc.wrappedNPC, was, at, finished)); }
    }

    /** Every tick of a rigged NPC, both sides. */
    public void tick(EntityNPCInterface npc) {
        if (!playing()) { return; }
        boolean remote = npc.world.isRemote;
        if (!npc.isEntityAlive() || npc.deathTime > 0) {
            if (remote) { clip = ""; age = 0; } else { queue.clear(); end(npc, false); }
            return;
        }
        AnimClip c = npc.rwRig.library().clipOrNull(clip);
        if (c == null) {
            if (remote) { age++; } else { end(npc, false); }
            return;
        }
        float from = age == 0 ? -1 : age / 20F;
        age++;
        if (remote) { return; }
        fired.clear();
        c.eventsBetween(from, Math.min(age / 20F, c.length), fired);
        String current = clip;
        for (AnimEvent e : fired) {
            try {
                RigEvents.server(npc, e);
            } catch (RuntimeException ex) {
                LogWriter.error("RWRIG event " + e.type + " of " + current + " failed on " + npc.getName(), ex);
            }
            if (npc.script.isEnabled()) {
                npc.script.runScript("animationEvent", new AnimationEvent.ClipEvent(npc.wrappedNPC, current, age, e.type, e.time, e.toString()));
            }
            if (!current.equals(clip)) { return; } // a hook started or stopped a clip
        }
        if (age >= Math.max(1, c.lengthTicks()) && current.equals(clip)) {
            end(npc, true);
            String next = queue.poll();
            while (next != null && npc.rwRig.library().clipOrNull(next) == null) { next = queue.poll(); }
            if (next != null) { begin(npc, next, 0); }
        }
    }

    /** Client: the server started (or, with an empty name, ended) a clip; older news than we have is ignored. */
    public void accept(String name, int startAge, int blendTicks, int number) {
        if (number < seq) { return; }
        seq = number;
        clip = name == null ? "" : name; age = startAge; blend = blendTicks;
    }

    /** Spawn data for a late joiner. */
    public void writeSpawn(NBTTagCompound tag) {
        if (playing()) { tag.setString("RwRigClip", clip); tag.setInteger("RwRigAge", age); }
        tag.setInteger("RwRigSeq", seq);
    }

    public void readSpawn(NBTTagCompound tag) {
        accept(tag.getString("RwRigClip"), tag.getInteger("RwRigAge"), 0, tag.getInteger("RwRigSeq"));
    }
}
