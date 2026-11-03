package noppes.npcs.rework.army;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/** M11.1 (Q297, Q299): register NBT round trip, NPC key shape and the switch at 0. */
public class ArmyRegistryTest {
    private static final String P1 = "0b5a1c2e-1111-4a2b-9c3d-123456789abc";
    private static final String P2 = "7f000001-2222-4b3c-8d4e-abcdefabcdef";

    @After
    public void switchOff() {
        ArmyCommandService.setSwitch(0);
    }

    @Test
    public void namesAreShortSafeTokens() {
        for (String ok : new String[] {"a", "North_1", "red-guard", "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"}) {
            assertTrue(ok, Army.validName(ok));
        }
        for (String bad : new String[] {null, "", "two words", "x:y", "č", "@e", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456"}) {
            assertFalse(String.valueOf(bad), Army.validName(bad));
        }
        assertEquals(Army.key("North"), Army.key("nORTH"));
    }

    @Test
    public void ownerSpecsAreCanonical() {
        Object[] p = ArmyRegistry.parseOwnerSpec("uuid:" + P1.toUpperCase());
        assertSame(Army.OwnerType.PLAYER, p[0]);
        assertEquals(UUID.fromString(P1).toString(), p[1]);
        Object[] f = ArmyRegistry.parseOwnerSpec("faction:007");
        assertSame(Army.OwnerType.FACTION, f[0]);
        assertEquals("7", f[1]);
        for (String bad : new String[] {null, "", P1, "uuid:", "uuid:nope", "faction:-1", "faction:x", "team:1",
                ":1"}) {
            assertNull(String.valueOf(bad), ArmyRegistry.parseOwnerSpec(bad));
        }
    }

    @Test
    public void registerSurvivesNbtRoundTripThroughBytes() throws Exception {
        ArmyRegistry reg = new ArmyRegistry();
        Army north = new Army("North", Army.OwnerType.PLAYER, P1, "M111_Owner", 1200L);
        Army guard = new Army("guard", Army.OwnerType.FACTION, "2", "Aggressive", 0L);
        Army east = new Army("east-1", Army.OwnerType.PLAYER, P2, "", Long.MAX_VALUE);
        assertSame(ArmyRegistry.Result.CREATED, reg.create(north));
        assertSame(ArmyRegistry.Result.CREATED, reg.create(guard));
        assertSame(ArmyRegistry.Result.CREATED, reg.create(east));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(reg.write(new NBTTagCompound()), bytes);
        NBTTagCompound loaded = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
        ArmyRegistry back = new ArmyRegistry();
        back.read(loaded);

        assertEquals(ArmyRegistry.VERSION, loaded.getInteger("Version"));
        assertEquals(0, back.skippedOnRead());
        assertEquals(reg.list(), back.list());
        assertEquals(north, back.get("NORTH"));
        assertEquals("uuid:" + P1, back.get("north").ownerSpec());
        assertEquals("faction:2", back.get("guard").ownerSpec());
        assertEquals(reg.write(new NBTTagCompound()), back.write(new NBTTagCompound()));
        // sorted without case, stable on every write
        assertEquals("east-1", back.list().get(0).name());
        assertEquals("North", back.list().get(2).name());
    }

    @Test
    public void emptyOwnerNameIsNotWritten() {
        NBTTagCompound c = new Army("a", Army.OwnerType.FACTION, "0", "", 5L).write();
        assertFalse(c.hasKey("OwnerName"));
        assertEquals(new Army("a", Army.OwnerType.FACTION, "0", null, 5L), Army.read(c));
    }

    @Test
    public void duplicatesAreRefusedWithoutCaseAndRemoveWorks() {
        ArmyRegistry reg = new ArmyRegistry();
        assertSame(ArmyRegistry.Result.CREATED, reg.create(new Army("Alpha", Army.OwnerType.FACTION, "1", "", 0L)));
        assertSame(ArmyRegistry.Result.EXISTS, reg.create(new Army("ALPHA", Army.OwnerType.PLAYER, P1, "", 0L)));
        assertEquals(Army.OwnerType.FACTION, reg.get("alpha").ownerType());
        assertEquals("Alpha", reg.remove("aLpHa").name());
        assertNull(reg.get("alpha"));
        assertNull(reg.remove("alpha"));
        assertEquals(0, reg.size());
    }

    @Test
    public void registerIsBounded() {
        ArmyRegistry reg = new ArmyRegistry();
        for (int i = 0; i < ArmyRegistry.MAX_ARMIES; i++) {
            assertSame(ArmyRegistry.Result.CREATED, reg.create(new Army("a" + i, Army.OwnerType.FACTION, "0", "", 0L)));
        }
        assertSame(ArmyRegistry.Result.FULL, reg.create(new Army("one-more", Army.OwnerType.FACTION, "0", "", 0L)));
    }

    @Test
    public void invalidAndDuplicateEntriesAreSkippedAndCountedNotThrown() {
        NBTTagList list = new NBTTagList();
        list.appendTag(new Army("ok", Army.OwnerType.PLAYER, P1, "", 1L).write());
        list.appendTag(new Army("OK", Army.OwnerType.FACTION, "3", "", 2L).write());
        NBTTagCompound badName = new Army("x", Army.OwnerType.FACTION, "3", "", 0L).write();
        badName.setString("Name", "bad name");
        list.appendTag(badName);
        NBTTagCompound badOwner = new Army("y", Army.OwnerType.PLAYER, P2, "", 0L).write();
        badOwner.setString("Owner", "not-a-uuid");
        list.appendTag(badOwner);
        NBTTagCompound badType = new Army("z", Army.OwnerType.PLAYER, P2, "", 0L).write();
        badType.setString("OwnerType", "team");
        list.appendTag(badType);
        NBTTagCompound missing = new NBTTagCompound();
        missing.setString("Name", "w");
        list.appendTag(missing);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("Armies", list);

        ArmyRegistry reg = new ArmyRegistry();
        reg.create(new Army("stale", Army.OwnerType.FACTION, "0", "", 0L));
        reg.read(root);
        assertEquals(1, reg.size());
        assertEquals(5, reg.skippedOnRead());
        assertEquals(1L, reg.get("ok").created());
        assertNull(reg.get("stale"));

        ArmyRegistry empty = new ArmyRegistry();
        empty.read(new NBTTagCompound());
        assertEquals(0, empty.size());
        assertEquals(0, empty.skippedOnRead());
    }

    @Test
    public void npcWithoutArmyKeepsTheOriginalNbtShape() {
        NBTTagCompound npc = npcData();
        NBTTagCompound before = npc.copy();
        assertEquals("", ArmyRegistry.readNpc(npc));
        ArmyRegistry.writeNpc(npc, ArmyRegistry.readNpc(npc));
        assertEquals(before, npc);
        ArmyRegistry.writeNpc(npc, null);
        assertEquals(before, npc);
    }

    @Test
    public void npcArmyIsWrittenReadAndRemoved() {
        NBTTagCompound npc = npcData();
        ArmyRegistry.writeNpc(npc, "North");
        assertEquals("North", npc.getString(Army.NPC_KEY));
        assertEquals("North", ArmyRegistry.readNpc(npc));
        ArmyRegistry.writeNpc(npc, "");
        assertFalse(npc.hasKey(Army.NPC_KEY));
        assertEquals(npcData(), npc);
    }

    @Test
    public void storedNamesArePreservedVerbatimAndWrongTypesAreIgnored() {
        NBTTagCompound npc = npcData();
        npc.setString(Army.NPC_KEY, "kept even if not a valid name");
        NBTTagCompound before = npc.copy();
        ArmyRegistry.writeNpc(npc, ArmyRegistry.readNpc(npc));
        assertEquals(before, npc);
        npc.setTag(Army.NPC_KEY, new NBTTagInt(7));
        assertEquals("", ArmyRegistry.readNpc(npc));
    }

    @Test
    public void switchDefaultsToZero() throws Exception {
        // Reflection only: reading the value would initialize CustomNpcs, which needs a bootstrapped game.
        java.lang.reflect.Field f = CustomNpcs.class.getField("RwArmies");
        assertSame(int.class, f.getType());
        noppes.npcs.config.ConfigProp prop = f.getAnnotation(noppes.npcs.config.ConfigProp.class);
        assertNotNull(prop);
        assertTrue(prop.info().endsWith("Default 0"));
        ArmyCommandService.setSwitch(0);
        assertFalse(ArmyCommandService.enabled());
    }

    @Test
    public void switchOffChangesNothingAndNeedsNoServer() {
        ArmyCommandService.setSwitch(0);
        assertFalse(ArmyCommandService.enabled());
        assertFalse(ArmyCommandService.create(null, "north", "faction:0").ok);
        assertEquals("RWARMY off (RwArmies=0)", ArmyCommandService.create(null, "north", "faction:0").message);
        assertFalse(ArmyCommandService.remove(null, "north").ok);
        assertFalse(ArmyCommandService.assign(null, new ArrayList<EntityNPCInterface>(), "north").ok);
        assertNull(ArmyCommandService.get(null, "north"));
        assertTrue(ArmyCommandService.list(null).isEmpty());
        assertTrue(ArmyCommandService.members(null, "north").isEmpty());
        assertFalse(ArmyApi.enabled());
        assertFalse(ArmyApi.create("north", "faction:0"));
        assertFalse(ArmyApi.assign(null, "north"));
        assertFalse(ArmyApi.unassign(null));
        assertEquals("", ArmyApi.armyOf(null));
        assertEquals(0, ArmyApi.list().length);
        assertEquals(0, ArmyApi.members("north").length);
        assertEquals("", ArmyApi.owner("north"));
        assertEquals(1, ArmyApi.getApiVersion()); // M11.7a: frozen bridge API v1
    }

    @Test
    public void switchOnValidatesBeforeTouchingTheWorld() {
        ArmyCommandService.setSwitch(1);
        assertTrue(ArmyCommandService.create(null, "bad name", "faction:0").message.contains("invalid name"));
        assertTrue(ArmyCommandService.create(null, "north", "nobody").message.contains("invalid owner"));
        assertTrue(ArmyCommandService.members(null, "north").isEmpty());
    }

    private static NBTTagCompound npcData() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("Name", "army schema fixture");
        data.setInteger("MovementType", 0);
        data.setInteger("RwHitboxMode", 1);
        return data;
    }
}
