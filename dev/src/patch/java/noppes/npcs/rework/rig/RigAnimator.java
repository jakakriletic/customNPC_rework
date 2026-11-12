package noppes.npcs.rework.rig;

import java.util.List;
import net.minecraft.entity.Entity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.anim.IAnimator;

/** M16: {@link IAnimator} of an NPC that wears a rig ({@code RwRig}), on the built-in core. Server thread. */
public final class RigAnimator implements IAnimator {
    private final EntityNPCInterface npc;

    public RigAnimator(EntityNPCInterface npc) {
        this.npc = npc;
    }

    /** Whether this NPC is animated by the core (switch on and a rig). */
    public static boolean handles(Entity e) {
        return CustomNpcs.RwAnimCore == 1 && e instanceof EntityNPCInterface && ((EntityNPCInterface) e).rwRig.active();
    }

    private boolean ok() {
        return handles(npc) && !npc.world.isRemote && !npc.isDead && npc.world.getMinecraftServer() != null
                && npc.world.getMinecraftServer().isCallingFromMinecraftThread();
    }

    @Override public boolean isAvailable() { return handles(npc) && !npc.isDead; }
    @Override public boolean play(String clip) { return ok() && clip != null && npc.rwRigPlayer.play(npc, clip, 0); }
    @Override public boolean queue(String clip) { return ok() && clip != null && npc.rwRigPlayer.queue(npc, clip); }
    @Override public boolean stop() { return ok() && npc.rwRigPlayer.stop(npc); }
    @Override public boolean blendTo(String clip, int ticks) {
        return ok() && clip != null && ticks >= 0 && ticks <= 200 && npc.rwRigPlayer.play(npc, clip, ticks);
    }
    @Override public String getClip() { return isAvailable() ? npc.rwRigPlayer.clip() : ""; }
    @Override public int getTick() { return isAvailable() ? npc.rwRigPlayer.age() : 0; }
    @Override public String[] getClips() {
        if (!isAvailable()) { return new String[0]; }
        List<String> names = npc.rwRig.library().sortedClipNames();
        return names.toArray(new String[0]);
    }
}
