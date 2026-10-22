package noppes.npcs.rework.diag;

import static org.junit.Assert.assertEquals;

import net.minecraft.util.math.AxisAlignedBB;
import org.junit.Test;

/** M5-K K12: presezek obrisa modela cez okvir upodobitve ({@link RenderBoxAudit#excess}). */
public class RenderBoxAuditTest {
    private static final double E = 1e-9;

    @Test
    public void obrisZnotrajNiPresezka() {
        AxisAlignedBB box = new AxisAlignedBB(-0.5, 4.0, -0.5, 0.5, 5.9, 0.5);
        // Humanoid: sirina obrisa 0,94, od nog do 1,875; spodnji rob na nogah odloci (0).
        assertEquals(0.0, RenderBoxAudit.excess(box, 0.0, 4.0, 0.0, 0.94, 0.0, 1.875), E);
        // Obris nad nogami: rezerva do vrha okvira.
        assertEquals(-0.025, RenderBoxAudit.excess(box, 0.0, 4.0, 0.0, 0.94, 0.1, 1.775), E);
    }

    @Test
    public void hitboxOriginalaNeObjameRok() {
        AxisAlignedBB hitbox = new AxisAlignedBB(-0.3, 4.0, -0.3, 0.3, 5.9, 0.3);
        assertEquals(0.17, RenderBoxAudit.excess(hitbox, 0.0, 4.0, 0.0, 0.94, 0.0, 1.875), E);
        // NPC brez hitboxa: okvir je crta.
        AxisAlignedBB line = new AxisAlignedBB(-5e-6, 4.0, -5e-6, 5e-6, 5.9, 5e-6);
        assertEquals(0.47 - 5e-6, RenderBoxAudit.excess(line, 0.0, 4.0, 0.0, 0.94, 0.0, 1.875), E);
    }

    @Test
    public void navpicniPresezekInNesimetricenOkvir() {
        // Prasic: glava do 1,0 nad hitboxom 0,9.
        AxisAlignedBB pig = new AxisAlignedBB(9.55, 0.0, 9.55, 10.45, 0.9, 10.45);
        assertEquals(0.1, RenderBoxAudit.excess(pig, 10.0, 0.0, 10.0, 0.6, 0.0, 1.0), E);
        // Najblizja stranica okvira odloca.
        AxisAlignedBB off = new AxisAlignedBB(-1.0, 0.0, -0.2, 1.0, 2.0, 1.0);
        assertEquals(0.3, RenderBoxAudit.excess(off, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0), E);
        // Obris pod nogami (spodnji rob negativen).
        assertEquals(0.2, RenderBoxAudit.excess(off, 0.0, 0.0, 0.0, 0.2, -0.2, 1.0), E);
    }
}
