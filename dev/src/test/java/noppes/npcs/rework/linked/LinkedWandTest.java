package noppes.npcs.rework.linked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/** M13.3: messages of the wand list and the multiple selection without Minecraft. */
public class LinkedWandTest {
    private static final char Z_CARON = (char) 0x17E;

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static List<LinkedGuiCodec.Group> groups() {
        return Arrays.asList(new LinkedGuiCodec.Group("Stra" + Z_CARON + "a", 3), new LinkedGuiCodec.Group("LG2", 0));
    }

    /** Ids 10..15: Guard at 2, 5 and 40 blocks, Archer, Cook, Guard at 8. */
    private static List<LinkedGuiCodec.Row> rows() {
        return Arrays.asList(new LinkedGuiCodec.Row(10, 2f, 0, "Guard"), new LinkedGuiCodec.Row(11, 3f, -1, "Archer"),
                new LinkedGuiCodec.Row(12, 5f, 0, "Guard"), new LinkedGuiCodec.Row(13, 6f, 1, "Cook"),
                new LinkedGuiCodec.Row(14, 8f, -1, "Guard"), new LinkedGuiCodec.Row(15, 40f, -1, "Guard"));
    }

    private static WandSelection selection() {
        WandSelection s = new WandSelection();
        s.setRows(rows(), groups());
        return s;
    }

    @Test public void batchRoundTripKeepsEveryField() {
        byte[] data = new LinkedGuiCodec.Batch(9, LinkedGuiCodec.BatchOp.COPY, LinkedCategories.STATS, 42, "LG" + Z_CARON, new int[]{1, -2, 3}).encode();
        LinkedGuiCodec.Batch b = LinkedGuiCodec.batch(data);
        assertEquals(9, b.id);
        assertEquals(LinkedGuiCodec.BatchOp.COPY, b.op);
        assertEquals(LinkedCategories.STATS, b.mask);
        assertEquals(42, b.source);
        assertEquals("LG" + Z_CARON, b.group);
        assertArrayEquals(new int[]{1, -2, 3}, b.ids);
        assertNull("a batch is no request", LinkedGuiCodec.request(data));
    }

    @Test public void largestBatchFitsAndOneMoreIdIsRefused() {
        byte[] data = new LinkedGuiCodec.Batch(1, LinkedGuiCodec.BatchOp.LINK, 0, -1, repeat('a', LinkedGuiCodec.MAX_NAME), new int[LinkedGuiCodec.MAX_IDS]).encode();
        assertEquals(LinkedGuiCodec.MAX_BATCH, data.length);
        assertNotNull(LinkedGuiCodec.batch(data));
        try {
            new LinkedGuiCodec.Batch(1, LinkedGuiCodec.BatchOp.LINK, 0, -1, "", new int[LinkedGuiCodec.MAX_IDS + 1]).encode();
            fail("too many ids");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test public void malformedBatchesAreRefused() {
        byte[] good = new LinkedGuiCodec.Batch(1, LinkedGuiCodec.BatchOp.DELETE, 0, -1, "G", new int[]{5, 6}).encode();
        for (int length = 0; length < good.length; length++) {
            assertNull("cut at " + length, LinkedGuiCodec.batch(Arrays.copyOf(good, length)));
        }
        assertNull("trailing byte", LinkedGuiCodec.batch(Arrays.copyOf(good, good.length + 1)));
        byte[] op = good.clone();
        op[6] = (byte) LinkedGuiCodec.BatchOp.values().length;
        assertNull("unknown op", LinkedGuiCodec.batch(op));
        byte[] mask = good.clone();
        mask[7] = (byte) 0x80;
        assertNull("unknown category", LinkedGuiCodec.batch(mask));
        byte[] id = good.clone();
        id[2] = (byte) 0x80;
        assertNull("negative request id", LinkedGuiCodec.batch(id));
    }

    @Test public void listRoundTripKeepsGroupsAndRows() {
        byte[] data = new LinkedGuiCodec.ListResponse(4, 1500, groups(), rows()).encode();
        LinkedGuiCodec.ListResponse l = LinkedGuiCodec.list(data);
        assertEquals(4, l.id);
        assertEquals(1500, l.total);
        assertEquals("Stra" + Z_CARON + "a", l.groups.get(0).name);
        assertEquals(3, l.groups.get(0).loaded);
        assertEquals(6, l.rows.size());
        assertEquals(13, l.rows.get(3).id);
        assertEquals(6f, l.rows.get(3).distance, 0f);
        assertEquals(1, l.rows.get(3).group);
        assertEquals(-1, l.rows.get(1).group);
        assertEquals("Cook", l.rows.get(3).name);
        assertNull("a list is no response", LinkedGuiCodec.response(data));
    }

    @Test public void largestListFitsItsLimit() {
        List<LinkedGuiCodec.Group> groups = new ArrayList<LinkedGuiCodec.Group>();
        for (int i = 0; i < LinkedGuiCodec.MAX_GROUPS; i++) {
            groups.add(new LinkedGuiCodec.Group(repeat('g', LinkedGuiCodec.MAX_NAME), i));
        }
        List<LinkedGuiCodec.Row> rows = new ArrayList<LinkedGuiCodec.Row>();
        for (int i = 0; i < LinkedGuiCodec.MAX_ROWS; i++) {
            rows.add(new LinkedGuiCodec.Row(i, i, i % LinkedGuiCodec.MAX_GROUPS, repeat('n', LinkedGuiCodec.MAX_NAME)));
        }
        byte[] data = new LinkedGuiCodec.ListResponse(1, 5000, groups, rows).encode();
        assertEquals(LinkedGuiCodec.MAX_LIST, data.length);
        assertTrue("under the 1 MB packet limit", data.length < 1000000);
        assertNotNull(LinkedGuiCodec.list(data));
    }

    @Test public void malformedListsAreRefused() {
        byte[] good = new LinkedGuiCodec.ListResponse(4, 6, groups(), rows()).encode();
        for (int length = 0; length < good.length; length++) {
            assertNull("cut at " + length, LinkedGuiCodec.list(Arrays.copyOf(good, length)));
        }
        assertNull("trailing byte", LinkedGuiCodec.list(Arrays.copyOf(good, good.length + 1)));
        List<LinkedGuiCodec.Row> bad = Arrays.asList(new LinkedGuiCodec.Row(1, 1f, 2, "x"));
        assertNull("group index outside the groups", LinkedGuiCodec.list(new LinkedGuiCodec.ListResponse(1, 1, groups(), bad).encode()));
        List<LinkedGuiCodec.Row> nan = Arrays.asList(new LinkedGuiCodec.Row(1, Float.NaN, -1, "x"));
        assertNull("distance not a number", LinkedGuiCodec.list(new LinkedGuiCodec.ListResponse(1, 1, groups(), nan).encode()));
    }

    @Test public void fitCutsWholeCharactersOnly() {
        String text = repeat(Z_CARON, 200);
        String cut = LinkedGuiCodec.fit(text, LinkedGuiCodec.MAX_NAME);
        assertEquals(127, cut.length());
        assertTrue(LinkedGuiCodec.validGroup(cut));
        String pair = new String(Character.toChars(0x1F600));
        assertEquals("a", LinkedGuiCodec.fit("a" + pair, 4));
        assertEquals("abc", LinkedGuiCodec.fit("abc", 3));
    }

    @Test public void plainClickReplacesTheSelection() {
        WandSelection s = selection();
        s.click(0, false, false);
        s.click(3, false, false);
        assertArrayEquals(new int[]{13}, s.ids());
        assertEquals(3, s.primaryIndex());
    }

    @Test public void ctrlTogglesAndKeepsALastPrimary() {
        WandSelection s = selection();
        s.click(0, false, false);
        s.click(2, false, true);
        s.click(4, false, true);
        assertArrayEquals(new int[]{10, 12, 14}, s.ids());
        assertEquals(14, s.primaryRow().id);
        s.click(4, false, true);
        assertArrayEquals(new int[]{10, 12}, s.ids());
        assertEquals(12, s.primaryRow().id);
    }

    @Test public void shiftTakesTheRangeFromTheAnchor() {
        WandSelection s = selection();
        s.click(1, false, false);
        s.click(4, true, false);
        assertArrayEquals(new int[]{11, 12, 13, 14}, s.ids());
        s.click(0, true, false);
        assertArrayEquals("anchor stays at the plain click", new int[]{10, 11}, s.ids());
        s.click(5, false, true);
        s.click(3, true, true);
        assertEquals("ctrl+shift adds the range from the new anchor", 5, s.count());
        assertTrue(s.isSelected(0) && s.isSelected(1) && s.isSelected(3) && s.isSelected(4) && s.isSelected(5));
        assertFalse(s.isSelected(2));
    }

    @Test public void filterHidesRowsAndDropsThemFromTheSelection() {
        WandSelection s = selection();
        s.click(1, false, false);
        s.click(3, false, true);
        s.setFilter(" GUA ");
        assertEquals(4, s.visibleCount());
        assertEquals("hidden rows never take part in an action", 0, s.count());
        assertNull(s.primaryRow());
        s.click(0, false, false);
        s.setFilter("");
        assertArrayEquals(new int[]{10}, s.ids());
        assertEquals(0, s.primaryIndex());
    }

    @Test public void sameNameUsesTheRadiusOfThePlayer() {
        WandSelection s = selection();
        s.click(2, false, false);
        s.sameName(6);
        assertArrayEquals(new int[]{10, 12}, s.ids());
        s.sameName(0);
        assertArrayEquals(new int[]{10, 12, 14, 15}, s.ids());
        assertEquals("primary stays the clicked row", 12, s.primaryRow().id);
        s.click(5, false, false);
        s.sameName(1);
        assertArrayEquals("the primary itself is always kept", new int[]{15}, s.ids());
    }

    @Test public void copyTargetsLeaveOutTheSource() {
        WandSelection s = selection();
        s.click(0, false, false);
        s.click(2, false, true);
        s.click(4, false, true);
        s.click(2, false, false);
        s.click(0, false, true);
        s.click(4, false, true);
        assertEquals(14, s.primaryRow().id);
        assertArrayEquals(new int[]{12, 10}, s.targets());
    }

    @Test public void selectionSurvivesARefreshByEntityId() {
        WandSelection s = selection();
        s.click(2, false, false);
        s.click(4, false, true);
        List<LinkedGuiCodec.Row> moved = new ArrayList<LinkedGuiCodec.Row>(rows());
        moved.remove(0);
        moved.add(0, new LinkedGuiCodec.Row(99, 1f, -1, "Guard"));
        java.util.Collections.swap(moved, 2, 4);
        s.setRows(moved, groups());
        assertArrayEquals(new int[]{12, 14}, s.ids());
        assertEquals(14, s.primaryRow().id);
        assertEquals(2, s.primaryIndex());
        List<LinkedGuiCodec.Row> gone = new ArrayList<LinkedGuiCodec.Row>(moved);
        gone.remove(2);
        s.setRows(gone, groups());
        assertArrayEquals("a deleted NPC leaves the selection", new int[]{12}, s.ids());
        assertEquals(12, s.primaryRow().id);
    }

    @Test public void rowTextShowsSelectionGroupAndLoadedMembers() {
        WandSelection s = selection();
        s.click(0, false, false);
        assertEquals("[x] 2 : Guard [Stra" + Z_CARON + "a 3]", s.text(0));
        assertEquals("3 : Archer", s.text(1));
        assertEquals("LG2 0", s.group(s.row(3)));
        assertNull(s.group(s.row(1)));
    }
}
