package si.ladja.xaerofleet.client;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectionModelTest {

    private static List<UUID> ids(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) ids.add(new UUID(7, i));
        return ids;
    }

    @Test
    public void aBoxTakesEveryShipInsideItNotJustOne() {
        SelectionModel model = new SelectionModel(16);
        List<UUID> fleet = ids(5);
        assertEquals(0, model.apply(fleet, SelectionModel.Mode.REPLACE));
        assertEquals(5, model.size());
        assertEquals(fleet, model.ids());
    }

    @Test
    public void aClickReplacesAndShiftClickToggles() {
        SelectionModel model = new SelectionModel(16);
        List<UUID> fleet = ids(3);
        model.apply(fleet.get(0), SelectionModel.Mode.REPLACE);
        model.apply(fleet.get(1), SelectionModel.Mode.TOGGLE);
        assertEquals(2, model.size());
        model.apply(fleet.get(0), SelectionModel.Mode.TOGGLE);
        assertEquals(Arrays.asList(fleet.get(1)), model.ids());
        model.apply(fleet.get(2), SelectionModel.Mode.REPLACE);
        assertEquals(Arrays.asList(fleet.get(2)), model.ids());
    }

    @Test
    public void aShiftBoxAddsToWhatWasSelected() {
        SelectionModel model = new SelectionModel(16);
        List<UUID> fleet = ids(6);
        model.apply(fleet.subList(0, 2), SelectionModel.Mode.REPLACE);
        model.apply(fleet.subList(1, 6), SelectionModel.Mode.ADD);
        assertEquals(6, model.size());
    }

    @Test
    public void theSelectionNeverOutgrowsOneOrder() {
        SelectionModel model = new SelectionModel(16);
        List<UUID> armada = ids(20);
        assertEquals(4, model.apply(armada, SelectionModel.Mode.REPLACE));
        assertEquals(16, model.size());
        // The first ones asked for are the ones kept.
        assertEquals(armada.subList(0, 16), model.ids());
    }

    @Test
    public void groupsComeBackWithoutTheShipsThatAreGone() {
        SelectionModel model = new SelectionModel(16);
        List<UUID> fleet = ids(4);
        model.apply(fleet, SelectionModel.Mode.REPLACE);
        model.saveGroup(2);
        model.clear();
        assertTrue(model.hasGroup(2));
        assertEquals(2, model.groupOf(fleet.get(3)));
        int back = model.recallGroup(2, false,
                new HashSet<>(Arrays.asList(fleet.get(0), fleet.get(2))));
        assertEquals(2, back);
        assertEquals(Arrays.asList(fleet.get(0), fleet.get(2)), model.ids());
    }

    @Test
    public void retainDropsCaptainsThatCanNoLongerBeCommanded() {
        SelectionModel model = new SelectionModel(16);
        List<UUID> fleet = ids(3);
        model.apply(fleet, SelectionModel.Mode.REPLACE);
        model.retain(new HashSet<>(fleet.subList(1, 3)));
        assertFalse(model.contains(fleet.get(0)));
        assertEquals(2, model.size());
    }

    @Test
    public void aBoxWorksWhicheverWayItIsDragged() {
        assertTrue(SelectionModel.inBox(5, 5, 0, 0, 10, 10));
        assertTrue(SelectionModel.inBox(5, 5, 10, 10, 0, 0));
        assertTrue(SelectionModel.inBox(5, 5, 10, 0, 0, 10));
        assertFalse(SelectionModel.inBox(11, 5, 0, 0, 10, 10));
    }

    @Test
    public void anOrderForSeveralShipsLeavesRoomForRealHulls() {
        // Two sloops need more than thirty blocks between stations; the old
        // twenty-block zone was why only one ship would ever take an order.
        assertTrue(SelectionModel.orderRadius(2) >= 64);
        assertTrue(SelectionModel.orderRadius(16) > SelectionModel.orderRadius(7));
        assertTrue(SelectionModel.orderRadius(16) <= 1024);
    }
}
