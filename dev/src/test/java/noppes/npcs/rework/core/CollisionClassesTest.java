package noppes.npcs.rework.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * M5.16: kateri razredi lahko prispevajo trkalno skatlo. V vanilli 1.12.2 prepisujeta
 * {@code getCollisionBoundingBox()} ali {@code getCollisionBox(Entity)} samo {@code EntityBoat},
 * {@code EntityMinecart} in {@code EntityShulker} (preverjeno z iskanjem po izvorni kodi Forge
 * 14.23.5.2847); vse drugo mora biti brez prispevka, sicer preskok ni enak originalu.
 */
public class CollisionClassesTest {
    @Before
    public void pocisti() {
        CollisionClasses.clearCache();
    }

    @Test
    public void prispevajoSamoColnMinecartInShulker() {
        assertTrue("coln", CollisionClasses.contributes(EntityBoat.class));
        assertTrue("minecart", CollisionClasses.contributes(EntityMinecartEmpty.class));
        assertTrue("shulker", CollisionClasses.contributes(EntityShulker.class));
    }

    @Test
    public void navadneEntiteteNePrispevajo() {
        assertFalse("zombi", CollisionClasses.contributes(EntityZombie.class));
        assertFalse("prasic", CollisionClasses.contributes(EntityPig.class));
        assertFalse("predmet", CollisionClasses.contributes(EntityItem.class));
        assertFalse("igralec", CollisionClasses.contributes(EntityPlayerMP.class));
        assertFalse("NPC", CollisionClasses.contributes(EntityCustomNpc.class));
    }

    /** Skatlo premikajoce se entitete prepisujeta samo coln in minecart, shulker ne. */
    @Test
    public void prepisanaSkatlaPremikajoceSeEntitete() {
        assertTrue("coln", CollisionClasses.overridesGetCollisionBox(EntityBoat.class));
        assertTrue("minecart", CollisionClasses.overridesGetCollisionBox(EntityMinecartEmpty.class));
        assertFalse("shulker", CollisionClasses.overridesGetCollisionBox(EntityShulker.class));
        assertFalse("zombi", CollisionClasses.overridesGetCollisionBox(EntityZombie.class));
        assertFalse("NPC", CollisionClasses.overridesGetCollisionBox(EntityCustomNpc.class));
    }

    /** {@code Entity} sam je izhodisce: nic ne prepisuje, zato brez prispevka. */
    @Test
    public void entitySamNePrispeva() {
        assertFalse(CollisionClasses.contributes(Entity.class));
        assertFalse(CollisionClasses.overridesGetCollisionBox(Entity.class));
    }

    /** Razred, ki ni entiteta (metode ni mogoce najti), se steje konzervativno: prispeva. */
    @Test
    public void neznanRazredSeStejeKonzervativno() {
        assertTrue(CollisionClasses.contributes(String.class));
        assertTrue(CollisionClasses.overridesGetCollisionBox(String.class));
    }

    @Test
    public void predpomnilnikDaIstiIzid() {
        boolean prvi = CollisionClasses.contributes(EntityBoat.class);
        boolean drugi = CollisionClasses.contributes(EntityBoat.class);
        assertTrue(prvi == drugi && prvi);
        assertTrue(CollisionClasses.cachedClasses() > 0);
    }
}
