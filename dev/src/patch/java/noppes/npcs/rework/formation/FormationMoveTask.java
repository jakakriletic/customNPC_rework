package noppes.npcs.rework.formation;

import net.minecraft.entity.ai.EntityAIBase;

/**
 * AI task clana enote. Sam ne odloca nicesar: vsak tick izvede ukaz, ki ga je planer
 * izracunal na zacetku ticka sveta ({@link Squad#drive(int)}).
 *
 * <p>Prioriteta {@link #PRIORITY} je nad vsemi taski CustomNPCs (ti se stejejo od 0
 * navzgor), maska pa pokrije {@code AiMutex.PASSIVE | AiMutex.LOOK}. Dokler task tece,
 * zato ne tecejo wander, return-home, moving path, follow ali watch-closest: to je
 * natanko tisto, kar se je v skripti tepelo z {@code navigateTo}.
 *
 * <p>CustomNPCs ob vsaki spremembi AI nastavitev pobrise vse taske
 * ({@code EntityNPCInterface.updateTasks}); enota zato task vsak tick preveri in ga po
 * potrebi vrne ({@link Squad#ensureTask(int)}). {@code EntityNPCInterface} se s tem ne
 * spreminja.
 */
public final class FormationMoveTask extends EntityAIBase {
    public static final int PRIORITY = -1;
    /** {@code AiMutex.PASSIVE (1) | AiMutex.LOOK (2)}; v AiMutex nista {@code final}. */
    public static final int MUTEX = 1 | 2;

    private final Squad squad;
    private final int index;

    FormationMoveTask(Squad squad, int index) {
        this.squad = squad;
        this.index = index;
        setMutexBits(MUTEX);
    }

    @Override
    public boolean shouldExecute() {
        return squad.controls(index);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return squad.controls(index);
    }

    @Override
    public void updateTask() {
        squad.drive(index);
    }

    Squad squad() {
        return squad;
    }
}
