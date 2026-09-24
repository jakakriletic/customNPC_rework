package noppes.npcs.rework.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import noppes.npcs.rework.entity.RiderState.Role;
import noppes.npcs.rework.entity.RiderState.Steering;
import noppes.npcs.rework.entity.RiderState.Teleport;

import org.junit.After;
import org.junit.Test;

/**
 * M3.3: odlocitev, kdo krmili nosilca. Tabela je namenoma izcrpna (3 nacini x 2^3
 * vhodov): napaka tu spremeni obnasanje vseh jahajocih NPC-jev.
 */
public class RiderStateTest {

    @After
    public void restoreDefault() {
        RiderState.setMode(RiderState.ORIGINAL);
    }

    @Test
    public void defaultIsOriginal() {
        assertEquals(RiderState.ORIGINAL, RiderState.mode());
    }

    @Test
    public void originalModeAlwaysLetsRiderSteer() {
        for (int i = 0; i < 8; i++) {
            assertEquals(Steering.RIDER, RiderState.decide(RiderState.ORIGINAL,
                    (i & 1) != 0, (i & 2) != 0, (i & 4) != 0));
        }
    }

    @Test
    public void nonNpcMountIsVanillaInEveryMode() {
        for (int m = 0; m <= 2; m++) {
            for (int i = 0; i < 4; i++) {
                assertEquals(Steering.RIDER, RiderState.decide(m, false, (i & 1) != 0, (i & 2) != 0));
            }
        }
    }

    /** R1: nosilec ima pot, jahac ne — v originalu jo jahac izbrise. */
    @Test
    public void r1CaseMountKeepsItsPathInFixModes() {
        assertEquals(Steering.RIDER, RiderState.decide(RiderState.ORIGINAL, true, true, false));
        assertEquals(Steering.MOUNT, RiderState.decide(RiderState.MOUNT_WHEN_OWN_PATH, true, true, false));
        assertEquals(Steering.MOUNT, RiderState.decide(RiderState.MOUNT_ALWAYS, true, true, false));
    }

    /** Faza C (M3.2): pot ima samo jahac — v nacinu 1 ostane vanilla spider jockey. */
    @Test
    public void onlyRiderHasPathRiderSteersInMode1ButNotInMode2() {
        assertEquals(Steering.RIDER, RiderState.decide(RiderState.MOUNT_WHEN_OWN_PATH, true, false, true));
        assertEquals(Steering.MOUNT, RiderState.decide(RiderState.MOUNT_ALWAYS, true, false, true));
    }

    @Test
    public void bothHavePathMountWinsInFixModes() {
        assertEquals(Steering.MOUNT, RiderState.decide(RiderState.MOUNT_WHEN_OWN_PATH, true, true, true));
        assertEquals(Steering.MOUNT, RiderState.decide(RiderState.MOUNT_ALWAYS, true, true, true));
    }

    @Test
    public void neitherHasPathMountKeepsItsIdleState() {
        assertEquals(Steering.MOUNT, RiderState.decide(RiderState.MOUNT_WHEN_OWN_PATH, true, false, false));
    }

    @Test
    public void invalidModeFallsBackToOriginal() {
        assertEquals(RiderState.ORIGINAL, RiderState.setMode(7));
        assertEquals(RiderState.ORIGINAL, RiderState.setMode(-1));
        assertEquals(Steering.RIDER, RiderState.decide(7, true, true, false));
        assertFalse(RiderState.isValidMode(3));
        assertTrue(RiderState.isValidMode(2));
        assertEquals(RiderState.MOUNT_ALWAYS, RiderState.setMode(2));
    }

    @Test
    public void roles() {
        assertEquals(Role.NONE, RiderState.role(false, false));
        assertEquals(Role.RIDER, RiderState.role(true, false));
        assertEquals(Role.MOUNT, RiderState.role(false, true));
        assertEquals(Role.RIDER_AND_MOUNT, RiderState.role(true, true));
    }

    // --- M3.4 ---

    @Test
    public void riderMovementNeverBlockedInOriginalOrOnNonNpcMount() {
        for (int i = 0; i < 4; i++) {
            assertFalse(RiderState.riderMovementBlocked(RiderState.ORIGINAL, (i & 1) != 0, (i & 2) != 0));
        }
        for (int m = 0; m <= 2; m++) {
            assertFalse(RiderState.riderMovementBlocked(m, false, true));
        }
    }

    @Test
    public void riderMovementBlockedExactlyWhenMountSteers() {
        assertTrue(RiderState.riderMovementBlocked(RiderState.MOUNT_WHEN_OWN_PATH, true, true));
        assertFalse(RiderState.riderMovementBlocked(RiderState.MOUNT_WHEN_OWN_PATH, true, false));
        assertTrue(RiderState.riderMovementBlocked(RiderState.MOUNT_ALWAYS, true, false));
        assertTrue(RiderState.riderMovementBlocked(RiderState.MOUNT_ALWAYS, true, true));
    }

    /** Blokada gibanja se mora ujemati z decide: ko je blokirano, bi pot jahaca itak zavrgli. */
    @Test
    public void movementBlockConsistentWithSteering() {
        for (int m = 0; m <= 2; m++) {
            for (int i = 0; i < 4; i++) {
                boolean npc = (i & 1) != 0, mountPath = (i & 2) != 0;
                if (RiderState.riderMovementBlocked(m, npc, mountPath)) {
                    assertEquals(Steering.MOUNT, RiderState.decide(m, npc, mountPath, true));
                }
            }
        }
    }

    @Test
    public void teleportTable() {
        assertEquals(Teleport.RIDER, RiderState.teleport(RiderState.ORIGINAL, true, true));
        assertEquals(Teleport.RIDER, RiderState.teleport(RiderState.ORIGINAL, true, false));
        assertEquals(Teleport.RIDER, RiderState.teleport(9, true, true));
        for (int m = 1; m <= 2; m++) {
            assertEquals(Teleport.RIDER, RiderState.teleport(m, false, false));
            assertEquals(Teleport.MOUNT, RiderState.teleport(m, true, true));
            assertEquals(Teleport.NONE, RiderState.teleport(m, true, false));
        }
    }
}
