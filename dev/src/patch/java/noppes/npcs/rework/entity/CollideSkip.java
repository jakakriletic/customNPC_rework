package noppes.npcs.rework.entity;

import java.lang.reflect.Field;

import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.ListenerList;
import noppes.npcs.LogWriter;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M5.12 (S4): {@code EntityNPCInterface.onCollide} brez opazovalca preskoci poizvedbo entitet.
 *
 * <p>Original vsak 4. tick poisce zive entitete v kvadru okoli NPC-ja in za vsako poklice
 * {@code EventHooks.onNPCCollide}: ustvari {@code NpcEvent.CollideEvent}, {@code runScript(COLLIDE)}
 * in {@code WrapperNpcAPI.EVENT_BUS.post}. Zakaj je preskok enak originalu (CNPC 01Oct19, Forge
 * 14.23.5.2847) [K]:
 * <ul>
 *   <li>{@code DataScript.runScript} pri {@code !isEnabled()} takoj vrne (tudi brez ponovne
 *       inicializacije po nalaganju skript);</li>
 *   <li>{@code EventBus.post} brez poslusalcev za dogodek ne naredi nicesar;</li>
 *   <li>konstruktor dogodka samo prebere ovoj entitete (capability, pripet ob nastanku);</li>
 *   <li>{@code World.getEntitiesWithinAABB} in {@code isEntityAlive} nimata stranskih ucinkov.</li>
 * </ul>
 * Ce ima NPC vklopljene skripte, ostane original: {@code runScript} lahko sprozi
 * {@code onNPCInit} po ponovnem nalaganju in prvo evalvacijo kode, preskok bi to premaknil v casu.
 *
 * <p>Poslusalci se preverijo ob vsakem klicu prek {@link ListenerList} dogodka (vkljucuje
 * poslusalce nadrazredov), zato registracija med tekom velja takoj. Ce refleksija na
 * {@code EventBus.busID} odpove, se steje, da poslusalec obstaja (nikoli preskok).
 *
 * <ul>
 *   <li>0 = original</li>
 *   <li>1 = preskok brez opazovalca</li>
 *   <li>2 = preverba: original, steje klice in dogodke, ki bi bili preskoceni</li>
 * </ul>
 * Stanje bere samo strezniska nit.
 */
public final class CollideSkip {
    public static final int ORIGINAL = 0;
    public static final int SKIP = 1;
    public static final int VERIFY = 2;

    /** Izid {@link #decide}: original brez stetja. */
    public static final int RUN = 0;
    /** Izid {@link #decide}: preskoci poizvedbo. */
    public static final int SKIP_QUERY = 1;
    /** Izid {@link #decide}: original, a brez opazovalca (nacin 2 steje dogodke). */
    public static final int RUN_UNOBSERVED = 2;

    private static int mode = ORIGINAL;
    private static boolean listenerLookupFailed;
    private static ListenerList collideListeners;
    private static int busId = -1;

    static long calls;
    static long skipped;
    static long unobservedCalls;
    static long unobservedEvents;

    private CollideSkip() {
    }

    public static boolean isValidMode(int m) {
        return m >= ORIGINAL && m <= VERIFY;
    }

    public static void setMode(int m) {
        mode = isValidMode(m) ? m : ORIGINAL;
    }

    public static int mode() {
        return mode;
    }

    public static String describe(int m) {
        switch (m) {
            case SKIP:
                return "preskok brez opazovalca";
            case VERIFY:
                return "preverba: original, steje preskocljive";
            default:
                return "original";
        }
    }

    /**
     * Cista odlocitev (testirana brez sveta).
     *
     * @param m nacin
     * @param scriptsEnabled {@code npc.script.isEnabled()}
     * @param hasListeners na vodilu je poslusalec za {@code CollideEvent} (ali ga ni bilo mogoce preveriti)
     */
    public static int decide(int m, boolean scriptsEnabled, boolean hasListeners) {
        if (m == ORIGINAL || scriptsEnabled || hasListeners) {
            return RUN;
        }
        return m == SKIP ? SKIP_QUERY : RUN_UNOBSERVED;
    }

    /** Klic iz {@code onCollide} po preverbi zivosti in 4. ticka; steje in vrne izid {@link #decide}. */
    public static int decide(EntityNPCInterface npc) {
        if (mode == ORIGINAL) {
            return RUN;
        }
        ++calls;
        int d = decide(mode, npc.script.isEnabled(), hasCollideListeners());
        if (d == SKIP_QUERY) {
            ++skipped;
        } else if (d == RUN_UNOBSERVED) {
            ++unobservedCalls;
        }
        return d;
    }

    /** Nacin 2: en dogodek, ki je bil poslan brez opazovalca. */
    public static void unobservedEvent() {
        ++unobservedEvents;
    }

    /** Ali ima {@code WrapperNpcAPI.EVENT_BUS} poslusalca za {@code NpcEvent.CollideEvent}. */
    public static boolean hasCollideListeners() {
        if (listenerLookupFailed) {
            return true;
        }
        if (collideListeners == null) {
            try {
                Field f = EventBus.class.getDeclaredField("busID");
                f.setAccessible(true);
                busId = f.getInt(WrapperNpcAPI.EVENT_BUS);
                collideListeners = new NpcEvent.CollideEvent(null, null).getListenerList();
            } catch (Throwable t) {
                listenerLookupFailed = true;
                LogWriter.info("RWCOLLIDE poslusalcev ni mogoce preveriti, preskoka ne bo: " + t);
                return true;
            }
        }
        return collideListeners.getListeners(busId).length > 0;
    }

    public static void reset() {
        calls = 0;
        skipped = 0;
        unobservedCalls = 0;
        unobservedEvents = 0;
    }

    public static long calls() {
        return calls;
    }

    public static long skipped() {
        return skipped;
    }

    public static long unobservedCalls() {
        return unobservedCalls;
    }

    public static long unobservedEvents() {
        return unobservedEvents;
    }
}
