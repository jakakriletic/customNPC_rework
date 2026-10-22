package noppes.npcs.rework.diag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import noppes.npcs.config.ConfigProp;
import org.junit.Before;
import org.junit.Test;

/** M5.Z (D-055): preglasitev configa z vrednostmi stikal, ki jih doloca koda. */
public class ConfigDefaultsTest {
    static class Cfg {
        @ConfigProp
        public static int RwB = 1;
        @ConfigProp
        public static int RwA = 2;
        @ConfigProp
        public static String RwS = "x";
        @ConfigProp
        public static int Other = 5;
        public static int RwNoProp = 7;
    }

    private ConfigDefaults defaults;

    @Before
    public void loadConfig() {
        Cfg.RwA = 2;
        Cfg.RwB = 1;
        Cfg.RwS = "x";
        Cfg.Other = 5;
        Cfg.RwNoProp = 7;
        defaults = ConfigDefaults.capture(Cfg.class);
        // kot ConfigLoader.loadConfig z dev configom: vsa stikala 0
        Cfg.RwA = 0;
        Cfg.RwB = 0;
        Cfg.RwS = "";
        Cfg.Other = 0;
        Cfg.RwNoProp = 0;
    }

    @Test
    public void brezLastnostiSeNicNeSpremeni() {
        assertNull(defaults.apply(null));
        assertNull(defaults.apply("2"));
        assertEquals(0, Cfg.RwA);
        assertEquals("", Cfg.RwS);
    }

    @Test
    public void nicSamoIzpiseVrednostiIzConfiga() {
        assertEquals("RWPRIVZETO 0 spremenjenih=0 stikal=3 RwA=0 RwB=0 RwS=", defaults.apply("0"));
        assertEquals(0, Cfg.RwA);
        assertEquals(0, Cfg.RwB);
    }

    @Test
    public void enaVrnePrivzeteVrednostiSamoStikalRw() {
        assertEquals("RWPRIVZETO 1 spremenjenih=3 stikal=3 RwA=2 RwB=1 RwS=x", defaults.apply("1"));
        assertEquals(2, Cfg.RwA);
        assertEquals(1, Cfg.RwB);
        assertEquals("x", Cfg.RwS);
        // polja brez @ConfigProp in brez predpone Rw ostanejo iz configa
        assertEquals(0, Cfg.Other);
        assertEquals(0, Cfg.RwNoProp);
    }

    @Test
    public void enakaVrednostNiSteta() {
        Cfg.RwB = 1;
        assertEquals("RWPRIVZETO 1 spremenjenih=2 stikal=3 RwA=2 RwB=1 RwS=x", defaults.apply("1"));
    }
}
