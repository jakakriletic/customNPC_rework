package noppes.npcs.rework.diag;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Zbiralnik meritev, ki tece na Forge dogodkih.
 *
 * <p>Zakaj na dogodkih in ne v {@code EntityNPCInterface}: ta razred se v M2 se ni
 * prenesen v {@code src/patch/java}. Prenese se v M3.1, skupaj s celotnim {@code ai}
 * paketom. Do takrat Forge dogodki dajo dovolj za osnovni posnetek: koliko NPC-jev tika,
 * koliko traja server tick in kaksen delez casa gre v NPC-je.
 *
 * <p>Zbiralnik je na event bus prijavljen <b>samo dokler je merjenje vklopljeno</b>.
 * Izklopljena instrumentacija zato ne stane niti enega klica na entiteto na tick.
 */
public final class DiagEventCollector {
    private static DiagEventCollector registered;

    private long serverTickStart;
    private long windowStart;
    private DiagKey windowKey;
    private int npcsThisTick;

    private DiagEventCollector() {
    }

    public static synchronized boolean isEnabled() {
        return registered != null;
    }

    /** Vklopi merjenje in prijavi zbiralnik na event bus. Stevci se ob vklopu pocistijo. */
    public static synchronized void enable() {
        if (registered != null) {
            Diag.reset();
            return;
        }
        DiagKeys.touch();
        DiagEventCollector collector = new DiagEventCollector();
        MinecraftForge.EVENT_BUS.register(collector);
        registered = collector;
        Diag.setEnabled(true);
    }

    /** Izklopi merjenje in odjavi zbiralnik. Zadnji posnetek ostane berljiv do vklopa. */
    public static synchronized void disable() {
        Diag.setEnabled(false);
        if (registered == null) {
            return;
        }
        MinecraftForge.EVENT_BUS.unregister(registered);
        registered = null;
    }

    /** Ce je bil zahtevan {@code -Drwdiag=on}, vklopi merjenje ob zagonu serverja. */
    public static void enableIfRequestedByProperty() {
        if (Diag.requestedByProperty()) {
            enable();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.side != Side.SERVER) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            closeWindow();
            this.serverTickStart = System.nanoTime();
            this.npcsThisTick = 0;
            return;
        }
        closeWindow();
        Diag.record(DiagKeys.NPCS_PER_TICK, this.npcsThisTick);
        if (this.serverTickStart != 0L) {
            Diag.tick(System.nanoTime() - this.serverTickStart);
            this.serverTickStart = 0L;
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null) {
            return;
        }
        World world = entity.world;
        if (world == null || world.isRemote) {
            return;
        }
        // Okno prejsnje entitete se zapre sele tu, ker je razmik do naslednje posodobitve
        // najboljsa ocena trajanja prejsnje, ki je na voljo brez prenosa entitete.
        closeWindow();
        if (entity instanceof EntityNPCInterface) {
            DiagKeys.NPC_UPDATE.increment();
            this.npcsThisTick++;
            this.windowKey = DiagKeys.NPC_UPDATE_WINDOW;
            this.windowStart = System.nanoTime();
        } else {
            DiagKeys.OTHER_LIVING_UPDATE.increment();
        }
    }

    private void closeWindow() {
        if (this.windowKey == null) {
            return;
        }
        long elapsed = System.nanoTime() - this.windowStart;
        this.windowKey.record(elapsed < 0L ? 0L : elapsed);
        this.windowKey = null;
    }
}
