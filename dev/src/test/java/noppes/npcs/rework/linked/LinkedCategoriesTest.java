package noppes.npcs.rework.linked;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.controllers.LinkedNpcController;
import org.junit.Test;
import static org.junit.Assert.*;

public class LinkedCategoriesTest {
    private static NBTTagCompound group() {
        NBTTagCompound g = new NBTTagCompound();
        g.setString("Name", "Edited");
        g.setString("Title", "Guard");
        g.setString("Texture", "skin.png");
        g.setTag("ModelData", new NBTTagCompound());
        g.setInteger("MaxHealth", 77);
        g.setInteger("AttackStrenght", 9);
        g.setInteger("pDamage", 3);
        g.setInteger("WalkingRange", 30);
        g.setInteger("MinExp", 5);
        g.setInteger("Role", 2);
        g.setInteger("SomeRoleKey", 1);
        g.setBoolean("TransformIsActive", true);
        g.setString("ScriptLanguage", "ECMAScript");
        return g;
    }

    private static NBTTagCompound member() {
        NBTTagCompound c = new NBTTagCompound();
        c.setString("Name", "B");
        c.setString("Title", "Own");
        c.setString("Texture", "own.png");
        c.setInteger("MaxHealth", 20);
        c.setInteger("WalkingRange", 10);
        c.setInteger("MinExp", 1);
        return c;
    }

    @Test public void switchesAreOffAndBudgetIsDocumented() throws Exception {
        // Reflection only: reading the value would initialize CustomNpcs, which needs a bootstrapped game.
        java.lang.reflect.Field groups = CustomNpcs.class.getField("RwLinkedGroups");
        assertSame(int.class, groups.getType());
        assertTrue(groups.getAnnotation(noppes.npcs.config.ConfigProp.class).info().endsWith("Default 0"));
        java.lang.reflect.Field budget = CustomNpcs.class.getField("RwLinkedBudget");
        assertTrue(budget.getAnnotation(noppes.npcs.config.ConfigProp.class).info().endsWith("Default 50"));
    }

    @Test public void q388TextNoLongerClaimsTheEditedNpcsHome() throws Exception {
        String info = CustomNpcs.class.getField("RwLinkedOwnFields")
                .getAnnotation(noppes.npcs.config.ConfigProp.class).info();
        assertFalse(info.contains("takes the home"));
        assertTrue(info.contains("first point of its own path"));
    }

    @Test public void everyKeyHasItsCategory() {
        assertEquals(LinkedCategories.NAME, LinkedCategories.categoryOf("Name"));
        assertEquals(LinkedCategories.NAME, LinkedCategories.categoryOf("Title"));
        assertEquals(LinkedCategories.APPEARANCE, LinkedCategories.categoryOf("Texture"));
        assertEquals(LinkedCategories.APPEARANCE, LinkedCategories.categoryOf("ModelData"));
        assertEquals(LinkedCategories.STATS, LinkedCategories.categoryOf("MaxHealth"));
        assertEquals(LinkedCategories.STATS, LinkedCategories.categoryOf("AttackStrenght"));
        assertEquals(LinkedCategories.STATS, LinkedCategories.categoryOf("pDamage"));
        assertEquals(LinkedCategories.AI, LinkedCategories.categoryOf("WalkingRange"));
        assertEquals(LinkedCategories.AI, LinkedCategories.categoryOf("StartPosNew"));
        assertEquals(LinkedCategories.INVENTORY, LinkedCategories.categoryOf("Weapons"));
        assertEquals(LinkedCategories.SCRIPTS, LinkedCategories.categoryOf("Scripts"));
        assertEquals(LinkedCategories.TRANSFORM, LinkedCategories.categoryOf("TransformHasAI"));
        assertEquals(LinkedCategories.ADVANCED, LinkedCategories.categoryOf("Role"));
    }

    @Test public void roleAndJobKeysFollowAdvanced() {
        assertEquals(LinkedCategories.ADVANCED, LinkedCategories.categoryOf("SomeRoleKey"));
    }

    @Test public void filterPassesOnlySelectedCategories() {
        NBTTagCompound out = LinkedCategories.filter(group(), LinkedCategories.APPEARANCE | LinkedCategories.STATS);
        assertEquals("skin.png", out.getString("Texture"));
        assertEquals(77, out.getInteger("MaxHealth"));
        assertEquals(3, out.getInteger("pDamage"));
        assertFalse(out.hasKey("Name"));
        assertFalse(out.hasKey("WalkingRange"));
        assertFalse(out.hasKey("Role"));
    }

    @Test public void offCategoryKeepsTheMembersValueAfterTheMerge() {
        NBTTagCompound merged = NBTTags.NBTMerge(member(), LinkedCategories.filter(group(), LinkedCategories.STATS));
        assertEquals(77, merged.getInteger("MaxHealth"));
        assertEquals("B", merged.getString("Name"));
        assertEquals("own.png", merged.getString("Texture"));
        assertEquals(10, merged.getInteger("WalkingRange"));
    }

    @Test public void defaultIsTheOriginalTransferWithoutScripts() {
        NBTTagCompound group = group();
        NBTTagCompound out = LinkedCategories.filter(group, LinkedCategories.DEFAULT);
        group.removeTag("ScriptLanguage");
        assertEquals(group, out);
        assertFalse(LinkedCategories.has(LinkedCategories.DEFAULT, LinkedCategories.SCRIPTS));
    }

    @Test public void filterDoesNotChangeTheGroup() {
        NBTTagCompound group = group();
        NBTTagCompound before = group.copy();
        LinkedCategories.filter(group, LinkedCategories.AI);
        assertEquals(before, group);
    }

    @Test public void textRoundTripsEveryMask() {
        for (int mask = 0; mask <= LinkedCategories.ALL; mask++) {
            assertEquals(mask, LinkedCategories.parse(LinkedCategories.toText(mask)));
        }
    }

    @Test public void unknownTextIsRejected() {
        assertEquals(-1, LinkedCategories.parse("stats,wings"));
        assertEquals(-1, LinkedCategories.parse(""));
    }

    @Test public void commandWordsAndPlainNamesReplace() {
        int current = LinkedCategories.STATS;
        assertEquals(LinkedCategories.DEFAULT, LinkedCategories.apply(current, new String[] {"default"}));
        assertEquals(0, LinkedCategories.apply(current, new String[] {"none"}));
        assertEquals(LinkedCategories.ALL, LinkedCategories.apply(current, new String[] {"all"}));
        assertEquals(LinkedCategories.APPEARANCE | LinkedCategories.AI,
                LinkedCategories.apply(current, new String[] {"appearance", "AI"}));
    }

    @Test public void plusAndMinusChangeTheCurrentSelection() {
        assertEquals(LinkedCategories.STATS | LinkedCategories.SCRIPTS,
                LinkedCategories.apply(LinkedCategories.STATS, new String[] {"+scripts"}));
        assertEquals(LinkedCategories.AI,
                LinkedCategories.apply(LinkedCategories.STATS | LinkedCategories.AI, new String[] {"-stats"}));
    }

    @Test public void badCommandArgumentsAreRejected() {
        assertEquals(-1, LinkedCategories.apply(1, new String[] {"wings"}));
        assertEquals(-1, LinkedCategories.apply(1, new String[] {"+wings"}));
        assertEquals(-1, LinkedCategories.apply(1, new String[0]));
    }

    @Test public void defaultGroupFileHasNoNewKey() {
        LinkedNpcController.LinkedData data = new LinkedNpcController.LinkedData();
        assertFalse(data.getNBT().hasKey("RwCategories"));
        assertEquals(2, data.getNBT().getKeySet().size());
    }

    @Test public void categoriesSurviveTheGroupFile() {
        LinkedNpcController.LinkedData data = new LinkedNpcController.LinkedData();
        data.categories = LinkedCategories.STATS | LinkedCategories.SCRIPTS;
        LinkedNpcController.LinkedData read = new LinkedNpcController.LinkedData();
        read.setNBT(data.getNBT());
        assertEquals(data.categories, read.categories);
    }

    @Test public void aDamagedCategoryValueFallsBackToTheDefault() {
        NBTTagCompound file = new LinkedNpcController.LinkedData().getNBT();
        file.setString("RwCategories", "wings");
        LinkedNpcController.LinkedData read = new LinkedNpcController.LinkedData();
        read.setNBT(file);
        assertEquals(LinkedCategories.DEFAULT, read.categories);
    }

    @Test public void budgetLimitsOneTickAndResetsOnTheNext() {
        LinkedBudget budget = new LinkedBudget();
        assertTrue(budget.tryAcquire(5, 2));
        assertTrue(budget.tryAcquire(5, 2));
        assertFalse(budget.tryAcquire(5, 2));
        assertTrue(budget.tryAcquire(6, 2));
        assertEquals(3, budget.applied);
        assertEquals(1, budget.deferred);
        assertEquals(2, budget.maxPerTick);
    }

    @Test public void budgetZeroMeansNoLimit() {
        LinkedBudget budget = new LinkedBudget();
        for (int i = 0; i < 500; i++) {
            assertTrue(budget.tryAcquire(1, 0));
        }
        assertEquals(500, budget.maxPerTick);
        assertEquals(0, budget.deferred);
    }

    @Test public void microsPerTransferIsAverageOfApplied() {
        LinkedBudget budget = new LinkedBudget();
        assertEquals(0, budget.microsPerTransfer());
        budget.tryAcquire(1, 0);
        budget.tryAcquire(1, 0);
        budget.nanos = 6000;
        assertEquals(3, budget.microsPerTransfer());
    }

    @Test public void sameSharedIgnoresOnlyTheOwnFields() {
        NBTTagCompound a = group();
        a.setInteger("MovingPos", 1);
        NBTTagCompound b = group();
        b.setInteger("MovingPos", 3);
        b.setInteger("Orientation", 180);
        assertTrue(LinkedOwnFields.sameShared(a, b));
        b.setInteger("MaxHealth", 78);
        assertFalse(LinkedOwnFields.sameShared(a, b));
        assertEquals(1, a.getInteger("MovingPos"));
    }
}
