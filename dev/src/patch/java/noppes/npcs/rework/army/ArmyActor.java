package noppes.npcs.rework.army;

import java.util.UUID;

/**
 * M11.1b: who asks. The operator flag comes from the server at the moment of the call (never stored), so a
 * deop applies to the next command. {@link #SYSTEM} is the trusted caller of {@link ArmyApi}.
 */
public final class ArmyActor {
    /** Script API and other trusted server code: operator rights, no player. */
    public static final ArmyActor SYSTEM = new ArmyActor(null, true);
    /** A sender that is neither a player nor allowed operator commands. */
    public static final ArmyActor NOBODY = new ArmyActor(null, false);

    private final UUID player;
    private final boolean operator;

    private ArmyActor(UUID player, boolean operator) {
        this.player = player;
        this.operator = operator;
    }

    public static ArmyActor player(UUID player, boolean operator) {
        return new ArmyActor(player, operator);
    }

    /** Null for the console, a command block or a script. */
    public UUID player() {
        return player;
    }

    public boolean operator() {
        return operator;
    }

    public boolean owns(Army army) {
        return player != null && army.ownerType() == Army.OwnerType.PLAYER && army.owner().equals(player.toString());
    }
}
