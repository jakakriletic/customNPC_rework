package noppes.npcs.rework.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.profiler.Profiler;

import org.junit.After;
import org.junit.Test;

/**
 * M3.6: prioriteta napada proti tavanju, preverjena na <b>pravem</b> vanilla
 * {@link EntityAITasks} (razvrscevalnik se ne ponareja). Taska sta nadomestka z mutex biti
 * originala: {@code EntityAIAttackTarget} 1+2 ({@code AiMutex.PASSIVE + LOOK}),
 * {@code EntityAIWander} 1 ({@code AiMutex.PASSIVE}). Nadomestek tavanja se tako kot
 * original ne umakne ob napadu ({@code shouldContinueExecuting} ne preverja
 * {@code isAttacking()}).
 *
 * <p>{@link #originalWanderingBlocksAttack} pokaze napako (v nacinu 0 napad ne zacne),
 * {@link #fixAttackPreemptsWandering} popravek.
 */
public class AttackPriorityTest {
    /** Prioriteta, ki jo ima napad pri NPC-ju brez projektila (vrednost ni pomembna). */
    private static final int ATTACK_PRIORITY = 12;

    private static final int MOVE = 1;
    private static final int LOOK = 2;
    private static final int JUMP = 4;

    @After
    public void restoreDefault() {
        AttackPriority.setMode(AttackPriority.ORIGINAL);
    }

    /** Nadomestek taska: tece, dokler to zeli; steje zagone in prekinitve. */
    static final class FakeTask extends EntityAIBase {
        boolean wants;
        int starts;
        int resets;
        boolean running;

        FakeTask(int bits) {
            setMutexBits(bits);
        }

        @Override
        public boolean shouldExecute() {
            return wants;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return wants;
        }

        @Override
        public void startExecuting() {
            starts++;
            running = true;
        }

        @Override
        public void resetTask() {
            resets++;
            running = false;
        }
    }

    /**
     * Ponovi vrstni red iz {@code EntityNPCInterface.setResponse} + {@code setMoveType} za
     * NPC brez projektila: napad s {@code taskCount} brez povecanja, nato (M3.6)
     * {@link AttackPriority#nextPriority}, nato tavanje s {@code taskCount++}.
     */
    private static EntityAITasks build(int mode, FakeTask attack, FakeTask wander) {
        EntityAITasks tasks = new EntityAITasks(new Profiler());
        int taskCount = ATTACK_PRIORITY;
        int attackPriority = taskCount;
        tasks.addTask(taskCount, attack);
        taskCount = AttackPriority.nextPriority(mode, taskCount, attackPriority);
        tasks.addTask(taskCount++, wander);
        return tasks;
    }

    /** Razvrscevalnik preveri zacetke vsak 3. klic ({@code tickRate}); 6 je dve rundi. */
    private static void tick(EntityAITasks tasks, int n) {
        for (int i = 0; i < n; i++) {
            tasks.onUpdateTasks();
        }
    }

    private static void wanderThenTarget(EntityAITasks tasks, FakeTask attack, FakeTask wander) {
        wander.wants = true;
        tick(tasks, 3);
        assertTrue("tavanje mora teci pred tarco", wander.running);
        attack.wants = true;   // NPC dobi tarco sredi tavanja
        tick(tasks, 6);
    }

    @Test
    public void defaultIsOriginal() {
        assertEquals(AttackPriority.ORIGINAL, AttackPriority.mode());
    }

    @Test
    public void invalidModeFallsBackToOriginal() {
        assertEquals(AttackPriority.ORIGINAL, AttackPriority.setMode(7));
        assertEquals(AttackPriority.BEFORE_MOVEMENT, AttackPriority.setMode(1));
        assertEquals(AttackPriority.ORIGINAL, AttackPriority.setMode(-1));
    }

    @Test
    public void nextPriorityTable() {
        // brez projektila: taskCount je se enak prioriteti napada
        assertEquals(12, AttackPriority.nextPriority(AttackPriority.ORIGINAL, 12, 12));
        assertEquals(13, AttackPriority.nextPriority(AttackPriority.BEFORE_MOVEMENT, 12, 12));
        // s projektilom: strelski napad je ze povecal taskCount -> brez spremembe v obeh nacinih
        assertEquals(13, AttackPriority.nextPriority(AttackPriority.ORIGINAL, 13, 12));
        assertEquals(13, AttackPriority.nextPriority(AttackPriority.BEFORE_MOVEMENT, 13, 12));
        // neveljaven nacin = original
        assertEquals(12, AttackPriority.nextPriority(5, 12, 12));
    }

    /** Napaka originala: tavajoc NPC s tarco ne zacne napada, dokler tavanje ne konca. */
    @Test
    public void originalWanderingBlocksAttack() {
        FakeTask attack = new FakeTask(MOVE + LOOK);
        FakeTask wander = new FakeTask(MOVE);
        EntityAITasks tasks = build(AttackPriority.ORIGINAL, attack, wander);
        wanderThenTarget(tasks, attack, wander);
        assertFalse("v originalu napad ne sme zaceti", attack.running);
        assertEquals(0, attack.starts);
        assertTrue(wander.running);

        // ko tavanje konca samo (konec poti), napad zacne
        wander.wants = false;
        tick(tasks, 6);
        assertTrue(attack.running);
    }

    /** Popravek: napad prekine tavanje v isti rundi razvrscevalnika. */
    @Test
    public void fixAttackPreemptsWandering() {
        FakeTask attack = new FakeTask(MOVE + LOOK);
        FakeTask wander = new FakeTask(MOVE);
        EntityAITasks tasks = build(AttackPriority.BEFORE_MOVEMENT, attack, wander);
        wanderThenTarget(tasks, attack, wander);
        assertTrue("s popravkom napad zacne", attack.running);
        assertFalse("tavanje je prekinjeno", wander.running);
        assertEquals(1, wander.resets);
    }

    /** V obeh nacinih tavanje med napadom ne zacne (skupni bit MOVE). */
    @Test
    public void wanderingNeverStartsDuringAttack() {
        for (int mode = 0; mode <= 1; mode++) {
            FakeTask attack = new FakeTask(MOVE + LOOK);
            FakeTask wander = new FakeTask(MOVE);
            EntityAITasks tasks = build(mode, attack, wander);
            attack.wants = true;
            tick(tasks, 3);
            assertTrue(attack.running);
            wander.wants = true;
            tick(tasks, 6);
            assertFalse("nacin " + mode, wander.running);
            assertTrue("nacin " + mode, attack.running);
        }
    }

    /**
     * Ovrzba premise iz 02-ZAHTEVE §R1: napad z biti MOVE+LOOK (original) z gibalnim taskom
     * z bitom MOVE ne tece hkrati niti takrat, ko ima gibalni task visjo prioriteto
     * (EntityAIReturn, EntityAIFollow) - taka prekine napad, ne tece vzporedno z njim.
     */
    @Test
    public void attackBitsExcludeHigherPriorityMovementTask() {
        EntityAITasks tasks = new EntityAITasks(new Profiler());
        FakeTask ret = new FakeTask(MOVE);
        FakeTask attack = new FakeTask(MOVE + LOOK);
        tasks.addTask(1, ret);
        tasks.addTask(ATTACK_PRIORITY, attack);
        attack.wants = true;
        tick(tasks, 3);
        assertTrue(attack.running);
        ret.wants = true;
        tick(tasks, 6);
        assertTrue(ret.running);
        assertFalse("gibalni task z visjo prioriteto napad prekine", attack.running);
    }

    /**
     * Kar bi prinesel "popravek" iz 02-ZAHTEVE (+PATHING = vanilla JUMP): edina sprememba je
     * izkljucitev s taskom, ki ima samo JUMP (EntityAIPounceTarget). Z gibalnim taskom je
     * izid enak kot z biti originala.
     */
    @Test
    public void addingJumpBitOnlyAffectsJumpTasks() {
        FakeTask pounce = new FakeTask(JUMP);
        FakeTask attack3 = new FakeTask(MOVE + LOOK);
        FakeTask attack7 = new FakeTask(MOVE + LOOK + JUMP);
        EntityAITasks a = new EntityAITasks(new Profiler());
        a.addTask(1, pounce);
        a.addTask(ATTACK_PRIORITY, attack3);
        EntityAITasks b = new EntityAITasks(new Profiler());
        FakeTask pounceB = new FakeTask(JUMP);
        b.addTask(1, pounceB);
        b.addTask(ATTACK_PRIORITY, attack7);
        attack3.wants = true;
        attack7.wants = true;
        pounce.wants = true;
        pounceB.wants = true;
        tick(a, 6);
        tick(b, 6);
        assertTrue("original: skok in napad hkrati", pounce.running && attack3.running);
        assertTrue(pounceB.running);
        assertFalse("z JUMP bitom napad caka na skok", attack7.running);
    }
}
