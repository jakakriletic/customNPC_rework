package noppes.npcs.rework.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import org.junit.After;
import org.junit.Test;

/**
 * M5.13 (S7): dokaz, da je iskanje prejemnikov po {@code world.playerEntities} enako vanilla
 * poizvedbi po chunkih.
 *
 * <p>Vanilla izbor je razdeljen na dva dela: geometrijo (seka skatla kvader) in izbor chunkov ter
 * y-rezov, ki jih poizvedba sploh pregleda. {@link AssociatedPlayers#scan} preveri samo
 * geometrijo (plus nalozenost chunka in {@code addedToChunk}), zato mora biti izbor chunkov in
 * rezov konservativen: nic, kar seka kvader, ne sme biti izven njega. To tukaj preveri
 * {@link #izborChunkovInRezovNeIzpustiNicesar()} na nakljucnih legah, vkljucno z mejami chunkov
 * in legami pod y = 0 in nad y = 255.
 *
 * <p>Model vanille je prepisan iz Forge 14.23.5.2847:
 * {@code World.getEntitiesWithinAABB(Class, AABB, Predicate)} (meje chunkov) in
 * {@code Chunk.getEntitiesOfTypeWithinAABB} (meje y-rezov).
 */
public class AssociatedPlayersTest {
    private static final double MAX_ENTITY_RADIUS = 2.0;
    private static final int SLICES = 16;

    @After
    public void pocisti() {
        AssociatedPlayers.setMode(AssociatedPlayers.ORIGINAL);
        AssociatedPlayers.setTiming(false);
        AssociatedPlayers.reset();
    }

    // --- model vanille ------------------------------------------------------

    /** {@code World.getEntitiesWithinAABB}: chunk (cx, cz) je pregledan. */
    private static boolean chunkPregledan(AxisAlignedBB box, int cx, int cz) {
        int x0 = MathHelper.floor((box.minX - MAX_ENTITY_RADIUS) / 16.0);
        int x1 = MathHelper.ceil((box.maxX + MAX_ENTITY_RADIUS) / 16.0);
        int z0 = MathHelper.floor((box.minZ - MAX_ENTITY_RADIUS) / 16.0);
        int z1 = MathHelper.ceil((box.maxZ + MAX_ENTITY_RADIUS) / 16.0);
        return cx >= x0 && cx < x1 && cz >= z0 && cz < z1;
    }

    /** {@code Chunk.getEntitiesOfTypeWithinAABB}: y-rez je pregledan. */
    private static boolean rezPregledan(AxisAlignedBB box, int rez) {
        int i = MathHelper.clamp(MathHelper.floor((box.minY - MAX_ENTITY_RADIUS) / 16.0), 0, SLICES - 1);
        int j = MathHelper.clamp(MathHelper.floor((box.maxY + MAX_ENTITY_RADIUS) / 16.0), 0, SLICES - 1);
        return rez >= i && rez <= j;
    }

    /** {@code Entity.chunkCoordX/Z}. */
    private static int chunkKoord(double pos) {
        return MathHelper.floor(pos / 16.0);
    }

    /** Rez, v katerem entiteto hrani {@code Chunk.addEntity}. */
    private static int rez(double posY) {
        return MathHelper.clamp(MathHelper.floor(posY / 16.0), 0, SLICES - 1);
    }

    private static AxisAlignedBB skatlaIgralca(double x, double y, double z) {
        return new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
    }

    // --- testi --------------------------------------------------------------

    @Test
    public void izborChunkovInRezovNeIzpustiNicesar() {
        Random r = new Random(5130713L);
        long sekajocih = 0;
        long mimo = 0;
        for (int n = 0; n < 200000; ++n) {
            double npcX = koordinata(r);
            double npcZ = koordinata(r);
            double npcY = r.nextDouble() * 320.0 - 10.0;
            double sirina = 0.4 + r.nextDouble() * 1.2;
            double visina = 0.5 + r.nextDouble() * 2.5;
            AxisAlignedBB npcBox = new AxisAlignedBB(npcX - sirina / 2, npcY, npcZ - sirina / 2,
                    npcX + sirina / 2, npcY + visina, npcZ + sirina / 2);
            // Isti kvader kot Server.sendAssociatedData.
            AxisAlignedBB box = npcBox.grow(AssociatedPlayers.BLINK_RANGE, AssociatedPlayers.BLINK_RANGE,
                    AssociatedPlayers.BLINK_RANGE);

            double igX = npcX + odmik(r);
            double igZ = npcZ + odmik(r);
            double igY = npcY + (r.nextDouble() * 400.0 - 200.0);
            AxisAlignedBB igBox = skatlaIgralca(igX, igY, igZ);

            boolean seka = igBox.intersects(box);
            boolean vanilla = seka
                    && chunkPregledan(box, chunkKoord(igX), chunkKoord(igZ))
                    && rezPregledan(box, rez(igY));
            assertEquals("lega npc=(" + npcX + "," + npcY + "," + npcZ + ") igralec=(" + igX + "," + igY + ","
                    + igZ + ")", seka, vanilla);
            if (seka) {
                ++sekajocih;
            } else {
                ++mimo;
            }
        }
        // Test ne sme biti prazen: potrebna sta oba izida.
        assertTrue("primerov, ki sekajo: " + sekajocih, sekajocih > 1000);
        assertTrue("primerov mimo: " + mimo, mimo > 1000);
    }

    /** Koordinata z nakopicenimi primeri tocno na meji chunka. */
    private static double koordinata(Random r) {
        double osnova = (r.nextInt(640) - 320) * 16.0;
        switch (r.nextInt(4)) {
            case 0:
                return osnova;
            case 1:
                return osnova - 0.001;
            case 2:
                return osnova + 0.001;
            default:
                return osnova + r.nextDouble() * 16.0;
        }
    }

    /** Odmik igralca: polovica primerov tesno okoli roba kvadra 160 blokov. */
    private static double odmik(Random r) {
        double rob = AssociatedPlayers.BLINK_RANGE;
        if (r.nextBoolean()) {
            double znak = r.nextBoolean() ? 1.0 : -1.0;
            return znak * (rob + (r.nextDouble() * 2.0 - 1.0));
        }
        return r.nextDouble() * 400.0 - 200.0;
    }

    @Test
    public void prejemnikJeSamoNespektatorVNalozenemChunkuKiSeka() {
        for (int i = 0; i < 16; ++i) {
            boolean mp = (i & 1) != 0;
            boolean spektator = (i & 2) != 0;
            boolean chunk = (i & 4) != 0;
            boolean seka = (i & 8) != 0;
            boolean want = mp && !spektator && chunk && seka;
            assertEquals("mp=" + mp + " spectator=" + spektator + " chunk=" + chunk + " seka=" + seka,
                    want, AssociatedPlayers.accepts(mp, spektator, chunk, seka));
        }
    }

    @Test
    public void neujemanjeNeStejeVrstnegaReda() {
        assertEquals(0, AssociatedPlayers.mismatch(new int[] {1, 2, 3}, new int[] {3, 1, 2}));
        assertEquals(0, AssociatedPlayers.mismatch(new int[0], new int[0]));
        assertEquals(1, AssociatedPlayers.mismatch(new int[] {1, 2}, new int[] {1}));
        assertEquals(1, AssociatedPlayers.mismatch(new int[] {1}, new int[] {1, 2}));
        assertEquals(2, AssociatedPlayers.mismatch(new int[] {1}, new int[] {2}));
        assertEquals(3, AssociatedPlayers.mismatch(new int[] {1, 2}, new int[] {3}));
    }

    @Test
    public void stevecChunkovUstrezaVanillaObsegu() {
        // NPC sirine 0,6 na sredini chunka: kvader 160 blokov -> 21 x 21 = 441 chunkov
        // (stevilka iz profila JFR in iz raziskave M5.0).
        double x = 8.0;
        double z = 8.0;
        AxisAlignedBB box = new AxisAlignedBB(x - 0.3, 64.0, z - 0.3, x + 0.3, 65.9, z + 0.3)
                .grow(AssociatedPlayers.BLINK_RANGE, AssociatedPlayers.BLINK_RANGE, AssociatedPlayers.BLINK_RANGE);
        long cells = AssociatedPlayers.chunkCells(box.minX, box.maxX, box.minZ, box.maxZ, MAX_ENTITY_RADIUS);
        assertEquals(441L, cells);
        // Stevec se mora ujemati z modelom vanille na vsakem chunku obsega.
        long prestetih = 0;
        for (int cx = -20; cx <= 20; ++cx) {
            for (int cz = -20; cz <= 20; ++cz) {
                if (chunkPregledan(box, cx, cz)) {
                    ++prestetih;
                }
            }
        }
        assertEquals(cells, prestetih);
    }

    @Test
    public void privzetoOriginalInNeveljavniNacini() {
        assertEquals(AssociatedPlayers.ORIGINAL, AssociatedPlayers.mode());
        assertTrue(AssociatedPlayers.isValidMode(AssociatedPlayers.VERIFY));
        assertFalse(AssociatedPlayers.isValidMode(-1));
        assertFalse(AssociatedPlayers.isValidMode(AssociatedPlayers.MODES));
        AssociatedPlayers.setMode(AssociatedPlayers.MODES);
        assertEquals(AssociatedPlayers.ORIGINAL, AssociatedPlayers.mode());
        assertFalse(AssociatedPlayers.timing());
    }

    @Test
    public void tickiSePristejejoNacinuOknaInResetJihPobrise() {
        AssociatedPlayers.reset(100);
        AssociatedPlayers.switchMode(AssociatedPlayers.SCAN, 130);
        assertEquals(30L, AssociatedPlayers.ticksIn(AssociatedPlayers.ORIGINAL, 130));
        assertEquals(20L, AssociatedPlayers.ticksIn(AssociatedPlayers.SCAN, 150));
        AssociatedPlayers.switchMode(AssociatedPlayers.ORIGINAL, 150);
        assertEquals(20L, AssociatedPlayers.ticksIn(AssociatedPlayers.SCAN, 170));
        assertEquals(50L, AssociatedPlayers.ticksIn(AssociatedPlayers.ORIGINAL, 170));
        assertTrue(AssociatedPlayers.perMode(170).contains("0:50:"));
        AssociatedPlayers.reset(170);
        assertEquals(0L, AssociatedPlayers.ticksIn(AssociatedPlayers.SCAN, 170));
        assertEquals("", AssociatedPlayers.perMode(170));
    }
}
