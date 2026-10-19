package local.customnpcs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import noppes.npcs.rework.render.LabelCache;
import org.junit.Test;

/** M5-K K4: kateri napisi gredo v display list (izris je preverjen v klientu, K11). */
public class LabelCacheTest {
    @Test
    public void navadniNapisiInBarvneKode() {
        assertTrue(LabelCache.cacheableText("PERF_Idle"));
        assertTrue(LabelCache.cacheableText("<M2.4 obremenitev: idle>"));
        assertTrue(LabelCache.cacheableText("§cRdeči §lkrepki §oležeči §r"));
        assertTrue(LabelCache.cacheableText("konec s §"));
        assertTrue(LabelCache.cacheableText(""));
    }

    @Test
    public void nakljucniZnakiInCrteGrejoPoOriginalu() {
        assertFalse(LabelCache.cacheableText(null));
        assertFalse(LabelCache.cacheableText("§kskrito"));
        assertFalse(LabelCache.cacheableText("a§Kb"));
        assertFalse(LabelCache.cacheableText("§npodcrtano"));
        assertFalse(LabelCache.cacheableText("x §cr §Mprecrtano"));
    }
}
