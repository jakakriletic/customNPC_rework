package noppes.npcs.rework.entity;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * M10.4 (Q449): pri 0 seznam ostane nespremenjen; nad mejo ostane zadnjih N vnosov v istem vrstnem redu.
 */
public class CombatEntryCapTest {
    private static List<Integer> seznam(int n) {
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < n; ++i) {
            l.add(i);
        }
        return l;
    }

    @Test
    public void nicJeOriginal() {
        List<Integer> l = seznam(500);
        assertEquals(0, CombatEntryCap.trim(l, 0));
        assertEquals(500, l.size());
        assertEquals(0, CombatEntryCap.trim(l, -3));
        assertEquals(500, l.size());
    }

    @Test
    public void podMejoBrezSpremembe() {
        List<Integer> l = seznam(64);
        assertEquals(0, CombatEntryCap.trim(l, 64));
        assertEquals(seznam(64), l);
    }

    @Test
    public void ohraniZadnjeVnose() {
        List<Integer> l = seznam(10);
        assertEquals(7, CombatEntryCap.trim(l, 3));
        assertEquals(Arrays.asList(7, 8, 9), l);
    }

    @Test
    public void poljeTrackerjaNajdeno() {
        org.junit.Assert.assertTrue(CombatEntryCap.available());
        net.minecraft.util.CombatTracker t = new net.minecraft.util.CombatTracker(null);
        assertEquals(0, CombatEntryCap.apply(t, 5));
    }
}
