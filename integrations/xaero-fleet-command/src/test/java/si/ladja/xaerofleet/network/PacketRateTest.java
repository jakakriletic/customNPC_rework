package si.ladja.xaerofleet.network;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PacketRateTest {

    /** One second, the interval the snapshot requests actually use. */
    private static final long SECOND = 1000L;

    @Test
    public void theFirstAskIsAlwaysAnswered() {
        assertTrue(new PacketRate(SECOND).allow(UUID.randomUUID(), 0));
    }

    @Test
    public void aSecondAskInsideTheIntervalIsRefused() {
        PacketRate rate = new PacketRate(SECOND);
        UUID player = UUID.randomUUID();
        assertTrue(rate.allow(player, 5000));
        assertFalse(rate.allow(player, 5001));
        assertFalse(rate.allow(player, 5999));
        assertTrue(rate.allow(player, 6000));
    }

    @Test
    public void oneFloodDoesNotSilenceEverybodyElse() {
        PacketRate rate = new PacketRate(SECOND);
        UUID loud = UUID.randomUUID();
        UUID quiet = UUID.randomUUID();
        rate.allow(loud, 0);
        for (long at = 50; at < SECOND; at += 50) {
            assertFalse(rate.allow(loud, at));
        }
        assertTrue(rate.allow(quiet, 250));
    }

    @Test
    public void aClockSetBackwardsLetsHimThroughRatherThanLockingHimOut() {
        PacketRate rate = new PacketRate(SECOND);
        UUID player = UUID.randomUUID();
        assertTrue(rate.allow(player, 500000));
        assertTrue(rate.allow(player, 5));
    }

    @Test
    public void aFrozenWorldClockCannotLockAPlayerOut() {
        // The reason this is measured in real time at all: in single player
        // the world stops ticking while the map screen is open, so a world
        // clock would hand the same number to every click of a session.
        PacketRate rate = new PacketRate(SECOND);
        UUID player = UUID.randomUUID();
        assertTrue(rate.allow(player, 10000));
        assertTrue(rate.allow(player, 11000));
        assertTrue(rate.allow(player, 12000));
    }

    @Test
    public void itCannotGrowPastItsBoundHoweverManyPlayersPassThrough() {
        PacketRate rate = new PacketRate(SECOND);
        for (int i = 0; i < 5000; i++) {
            rate.allow(UUID.randomUUID(), i * SECOND);
        }
        assertTrue(rate.remembered() <= 256);
    }

    @Test
    public void nobodyIsNotSomebody() {
        assertFalse(new PacketRate(SECOND).allow((UUID) null, 0));
    }
}
