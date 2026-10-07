package noppes.npcs.rework.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Diagnostika (scenarij M3.9): {@code /rwhitbox track <predpona> <tickov>} vsak tick sveta zapise
 * polozaj NPC-jev s predpono in ob koncu za vsakega izpise vrstico {@code RWHITBOX-TRACK}:
 * najvecji premik od zacetka, najvecji skok v enem ticku (teleport iz {@code EntityAIReturn}),
 * ticke, ko se njegov hitbox prekriva s hitboxom drugega sledenega NPC-ja (takrat tece trk), in
 * vodoravni premik v teh tickih. Brez vpliva na igro; poslusalec se po koncu odjavi.
 */
public final class HitboxTrack {
    private static HitboxTrack active;

    private final World world;
    private final List<EntityNPCInterface> npcs;
    private final double[][] start;
    private final double[][] last;
    private final double[] maxDisp;
    private final double[] maxJump;
    private final double[] minDist;
    private final double[] contactMove;
    private final int[] contactTicks;
    private int ticksLeft;

    private HitboxTrack(World world, List<EntityNPCInterface> npcs, int ticks) {
        this.world = world;
        this.npcs = npcs;
        int n = npcs.size();
        this.start = new double[n][];
        this.last = new double[n][];
        this.maxDisp = new double[n];
        this.maxJump = new double[n];
        this.minDist = new double[n];
        this.contactMove = new double[n];
        this.contactTicks = new int[n];
        for (int i = 0; i < n; i++) {
            Entity e = npcs.get(i);
            this.start[i] = new double[] {e.posX, e.posZ};
            this.last[i] = new double[] {e.posX, e.posZ};
            this.minDist[i] = Double.MAX_VALUE;
        }
        this.ticksLeft = ticks;
    }

    /** Zacne sledenje; prejsnje se prekine brez izpisa. Vrne stevilo sledenih NPC-jev. */
    public static int start(World world, String prefix, int ticks) {
        stop();
        List<EntityNPCInterface> npcs = new ArrayList<EntityNPCInterface>();
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof EntityNPCInterface && entity.isEntityAlive() && entity.getName().startsWith(prefix)) {
                npcs.add((EntityNPCInterface) entity);
            }
        }
        if (npcs.isEmpty() || ticks <= 0) {
            return 0;
        }
        active = new HitboxTrack(world, npcs, ticks);
        MinecraftForge.EVENT_BUS.register(active);
        return npcs.size();
    }

    public static void stop() {
        if (active != null) {
            MinecraftForge.EVENT_BUS.unregister(active);
            active = null;
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END || event.world != this.world) {
            return;
        }
        int n = this.npcs.size();
        boolean[] contact = new boolean[n];
        for (int i = 0; i < n; i++) {
            EntityNPCInterface a = this.npcs.get(i);
            for (int j = i + 1; j < n; j++) {
                EntityNPCInterface b = this.npcs.get(j);
                double d = Math.hypot(a.posX - b.posX, a.posZ - b.posZ);
                this.minDist[i] = Math.min(this.minDist[i], d);
                this.minDist[j] = Math.min(this.minDist[j], d);
                if (a.getEntityBoundingBox().intersects(b.getEntityBoundingBox())) {
                    contact[i] = true;
                    contact[j] = true;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            Entity e = this.npcs.get(i);
            double step = Math.hypot(e.posX - this.last[i][0], e.posZ - this.last[i][1]);
            this.maxJump[i] = Math.max(this.maxJump[i], step);
            this.maxDisp[i] = Math.max(this.maxDisp[i], Math.hypot(e.posX - this.start[i][0], e.posZ - this.start[i][1]));
            if (contact[i]) {
                this.contactTicks[i]++;
                this.contactMove[i] += step;
            }
            this.last[i][0] = e.posX;
            this.last[i][1] = e.posZ;
        }
        if (--this.ticksLeft <= 0) {
            report();
            stop();
        }
    }

    private void report() {
        for (int i = 0; i < this.npcs.size(); i++) {
            Entity e = this.npcs.get(i);
            say(String.format(Locale.ROOT,
                    "RWHITBOX-TRACK ime=%s premik=%.4f skok=%.4f razdalja=%.4f stik=%d premikObStiku=%.4f x=%.4f z=%.4f",
                    e.getName(), this.maxDisp[i], this.maxJump[i], this.minDist[i], this.contactTicks[i], this.contactMove[i], e.posX, e.posZ));
        }
        say("RWHITBOX-TRACK-KONEC n=" + this.npcs.size());
    }

    /** V konzolo strezika (log scenarija), ker ukaza ob koncu sledenja ni vec. */
    private static void say(String line) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            server.sendMessage(new TextComponentString(line));
        }
        LogWriter.info(line);
    }
}
