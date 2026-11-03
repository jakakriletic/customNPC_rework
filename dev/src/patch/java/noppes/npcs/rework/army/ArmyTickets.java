package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.rework.diag.DiagChunkPlan;

/**
 * M11.6 (Q301, Q349-Q353): NORMAL tickets of CustomNpcs that travel with marching armies. At most
 * {@code RwArmyMaxMarches} armies hold chunks at once; the others march as before, without tickets.
 * Server thread only. Tickets never outlive the server: they are released at stopping, and a ticket
 * left in forcedchunks.dat by a crash is released when the world loads ({@link #callback}).
 */
public final class ArmyTickets {
    static final String TAG = "RwArmyTicket";
    private static volatile int maxMarches = ArmyTicketPlan.DEFAULT_MAX_MARCHES;

    private final Map<String, Hold> holds = new LinkedHashMap<String, Hold>();
    private int refused;

    private static final class Hold {
        final String army;
        final WorldServer world;
        final List<ForgeChunkManager.Ticket> tickets = new ArrayList<ForgeChunkManager.Ticket>();
        final LinkedHashSet<Long> forced = new LinkedHashSet<Long>();
        final Map<Long, ForgeChunkManager.Ticket> owner = new HashMap<Long, ForgeChunkManager.Ticket>();
        final Map<Long, Integer> misses = new HashMap<Long, Integer>();
        boolean resume;
        Hold(String army, WorldServer world) { this.army = army; this.world = world; }
    }

    /** Copy of CustomNpcs.RwArmyMaxMarches, set at server start (0 = no tickets, the M11.3 behaviour). */
    public static void setMaxMarches(int value) { maxMarches = ArmyTicketPlan.clampMarches(value); }
    public static int maxMarches() { return maxMarches; }

    /** Army keys that hold tickets, in grant order. */
    List<String> holders() { return new ArrayList<String>(holds.keySet()); }

    /** Saved army tickets are never valid after a restart; the rest goes to the original callback unchanged. */
    public static ForgeChunkManager.LoadingCallback callback(final ForgeChunkManager.LoadingCallback original) {
        return new ForgeChunkManager.LoadingCallback() {
            @Override
            public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
                List<ForgeChunkManager.Ticket> rest = new ArrayList<ForgeChunkManager.Ticket>();
                int stale = 0;
                for (ForgeChunkManager.Ticket t : tickets) {
                    if (t.getModData().getBoolean(TAG)) { ForgeChunkManager.releaseTicket(t); stale++; }
                    else { rest.add(t); }
                }
                if (stale > 0) { LogWriter.info("RWARMY tickets stale-released=" + stale); }
                original.ticketsLoaded(rest, world);
            }
        };
    }

    /**
     * One scan per second. {@code wanted} maps every holder army key to its chunks (empty = nothing to hold yet),
     * {@code resume} marks the ones held at a saved position.
     */
    void apply(List<String> granted, Map<String, String> names, Map<String, WorldServer> worlds,
            Map<String, LinkedHashSet<Long>> wanted, Map<String, Boolean> resume) {
        for (String key : new ArrayList<String>(holds.keySet())) {
            Hold h = holds.get(key);
            if (!granted.contains(key) || worlds.get(key) != h.world) { release(key, "released"); }
        }
        for (String key : granted) {
            Hold h = holds.get(key);
            if (h == null) {
                h = new Hold(names.get(key), worlds.get(key));
                holds.put(key, h);
                LogWriter.info("RWARMY tickets " + h.army + " granted slots=" + holds.size() + "/" + maxMarches);
            }
            LinkedHashSet<Long> want = wanted.get(key);
            if (want == null) { want = new LinkedHashSet<Long>(); }
            for (Long packed : DiagChunkPlan.stale(new ArrayList<Long>(h.forced), want, h.misses,
                    ArmyTicketPlan.GRACE_SCANS)) {
                unforce(h, packed);
            }
            int added = 0;
            for (Long packed : want) {
                if (h.forced.contains(packed)) { continue; }
                if (!force(h, packed.longValue())) { refused++; break; }
                added++;
            }
            boolean nowResume = Boolean.TRUE.equals(resume.get(key));
            if (added > 0 || nowResume != h.resume) {
                h.resume = nowResume;
                LogWriter.info("RWARMY tickets " + h.army + " chunks=" + h.forced.size() + " tickets="
                        + h.tickets.size() + " added=" + added + " mode=" + (nowResume ? "resume" : "members")
                        + " centre=" + centre(want) + " refused=" + refused);
            }
        }
    }

    /** The first wanted chunk: the chunk of the first member, or of the saved position. */
    private static String centre(LinkedHashSet<Long> want) {
        if (want.isEmpty()) { return "none"; }
        long first = want.iterator().next();
        return DiagChunkPlan.unpackX(first) + "," + DiagChunkPlan.unpackZ(first);
    }

    private boolean force(Hold h, long packed) {
        ForgeChunkManager.Ticket ticket = null;
        for (ForgeChunkManager.Ticket t : h.tickets) {
            // A full ticket would silently evict its oldest chunk, so count its real list.
            if (t.getChunkList().size() < t.getMaxChunkListDepth()) { ticket = t; break; }
        }
        if (ticket == null) {
            ticket = ForgeChunkManager.requestTicket(CustomNpcs.instance, h.world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null || ticket.getMaxChunkListDepth() < 1) {
                if (ticket != null) { ForgeChunkManager.releaseTicket(ticket); }
                return false;
            }
            ticket.getModData().setBoolean(TAG, true);
            ticket.getModData().setString("Army", h.army);
            h.tickets.add(ticket);
        }
        int x = DiagChunkPlan.unpackX(packed), z = DiagChunkPlan.unpackZ(packed);
        ForgeChunkManager.forceChunk(ticket, new ChunkPos(x, z));
        // Forcing alone does not load: load from disk (never generate) so members there tick and are found.
        h.world.getChunkProvider().loadChunk(x, z);
        h.forced.add(Long.valueOf(packed));
        h.owner.put(Long.valueOf(packed), ticket);
        return true;
    }

    private void unforce(Hold h, Long packed) {
        ForgeChunkManager.Ticket ticket = h.owner.remove(packed);
        h.forced.remove(packed);
        if (ticket == null) { return; }
        ForgeChunkManager.unforceChunk(ticket, new ChunkPos(DiagChunkPlan.unpackX(packed), DiagChunkPlan.unpackZ(packed)));
        if (ticket.getChunkList().isEmpty()) {
            ForgeChunkManager.releaseTicket(ticket);
            h.tickets.remove(ticket);
        }
    }

    void release(String key, String why) {
        Hold h = holds.remove(key);
        if (h == null) { return; }
        // Q241: after a world unload Forge already dropped its tickets and releaseTicket would throw.
        if (h.world != null && h.world.provider != null
                && DimensionManager.getWorld(h.world.provider.getDimension()) == h.world) {
            for (ForgeChunkManager.Ticket t : h.tickets) {
                try {
                    ForgeChunkManager.releaseTicket(t);
                } catch (Exception problem) {
                    LogWriter.error("RWARMY ticket could not be released", problem);
                }
            }
        }
        LogWriter.info("RWARMY tickets " + h.army + " " + why + " chunks=" + h.forced.size());
    }

    void releaseAll(String why) {
        for (String key : new ArrayList<String>(holds.keySet())) { release(key, why); }
    }

    String status(String key) {
        Hold h = holds.get(Army.key(key));
        return h == null ? "tickets=0 chunks=0" : "tickets=" + h.tickets.size() + " chunks=" + h.forced.size()
                + (h.resume ? " resume" : "");
    }
}
