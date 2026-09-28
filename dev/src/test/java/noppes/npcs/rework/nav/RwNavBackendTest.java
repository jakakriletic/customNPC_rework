package noppes.npcs.rework.nav;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RwNavBackendTest {
    @Test
    public void bothSwitchesAreRequiredAndVanillaIsDefault() {
        assertFalse(RwNavBackend.selected(0, 0, 0, false, false, false, false));
        assertFalse(RwNavBackend.selected(1, 0, 0, false, false, false, false));
        assertFalse(RwNavBackend.selected(0, 1, 0, false, false, false, false));
        assertTrue(RwNavBackend.selected(1, 1, 0, false, false, false, false));
    }

    @Test
    public void unsupportedMovementRiderCarrierSquadAndDeadNpcStayVanilla() {
        assertFalse(RwNavBackend.selected(1, 1, 1, false, false, false, false));
        assertFalse(RwNavBackend.selected(1, 1, 2, false, false, false, false));
        assertFalse(RwNavBackend.selected(1, 1, 0, true, false, false, false));
        assertFalse(RwNavBackend.selected(1, 1, 0, false, true, false, false));
        assertFalse(RwNavBackend.selected(1, 1, 0, false, false, true, false));
        assertFalse(RwNavBackend.selected(1, 1, 0, false, false, false, true));
    }

    @Test
    public void explicitTestOverrideCanSwitchBothWays() {
        String previous = System.getProperty("rwnavbackend");
        try {
            System.setProperty("rwnavbackend", "1");
            assertTrue(RwNavBackend.globalMode() == 1);
            System.setProperty("rwnavbackend", "0");
            assertTrue(RwNavBackend.globalMode() == 0);
        } finally {
            if (previous == null) {
                System.clearProperty("rwnavbackend");
            } else {
                System.setProperty("rwnavbackend", previous);
            }
        }
    }
}
