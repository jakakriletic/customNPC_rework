package noppes.npcs.rework.army;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.rework.formation.SquadOrder;

/** M11.3/Q300: durable intent, never entity references or transient Squad ids. */
public final class ArmyOrder {
    public enum Mode { FORMATION, SWARM, FREEFORALL }
    public enum Stance { PASSIVE, ENGAGE }
    public enum State { WAITING, JOINING, MOVING, ARRIVED, BLOCKED, CANCELLED }
    public final int dimension, companySize;
    public final double x, y, z, speed;
    public final SquadOrder.Shape shape;
    public final State state;
    public final Mode mode;
    public final Stance stance;

    public ArmyOrder(int dimension, double x, double y, double z, SquadOrder.Shape shape,
            double speed, int companySize, State state) {
        this(dimension, x, y, z, shape, speed, companySize, state, Mode.FORMATION, Stance.ENGAGE);
    }

    public ArmyOrder(int dimension, double x, double y, double z, SquadOrder.Shape shape,
            double speed, int companySize, State state, Mode mode, Stance stance) {
        if (!valid(x, y, z, shape, speed) || companySize < 1 || companySize > 24 || state == null
                || mode == null || stance == null) {
            throw new IllegalArgumentException("invalid army order");
        }
        this.dimension = dimension;
        this.x = x; this.y = y; this.z = z;
        this.shape = shape; this.speed = speed; this.companySize = companySize; this.state = state;
        this.mode = mode; this.stance = stance;
    }

    public static boolean valid(double x, double y, double z, SquadOrder.Shape shape, double speed) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Double.isFinite(speed)
                && Math.abs(x) <= 29999984 && Math.abs(z) <= 29999984 && y >= 0 && y < 256
                && shape != null && shape != SquadOrder.Shape.MARCH
                && speed >= SquadOrder.MIN_SPEED && speed <= SquadOrder.MAX_SPEED;
    }

    public boolean active() {
        return state == State.WAITING || state == State.JOINING || state == State.MOVING;
    }

    public ArmyOrder withState(State next) {
        return new ArmyOrder(dimension, x, y, z, shape, speed, companySize, next, mode, stance);
    }

    public ArmyOrder withStance(Stance next) {
        return new ArmyOrder(dimension, x, y, z, shape, speed, companySize, state, mode, next);
    }

    NBTTagCompound write() {
        NBTTagCompound c = new NBTTagCompound();
        c.setInteger("Dimension", dimension);
        c.setDouble("X", x); c.setDouble("Y", y); c.setDouble("Z", z);
        c.setString("Shape", shape.name()); c.setDouble("Speed", speed);
        c.setInteger("Company", companySize); c.setString("State", state.name());
        c.setString("Mode", mode.name()); c.setString("Stance", stance.name());
        return c;
    }

    static ArmyOrder read(NBTTagCompound c) {
        if (!c.hasKey("Dimension", 3) || !c.hasKey("X", 6) || !c.hasKey("Y", 6) || !c.hasKey("Z", 6)
                || !c.hasKey("Speed", 6) || !c.hasKey("Company", 3)) {
            return null;
        }
        try {
            return new ArmyOrder(c.getInteger("Dimension"), c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z"),
                    SquadOrder.Shape.valueOf(c.getString("Shape")), c.getDouble("Speed"), c.getInteger("Company"),
                    State.valueOf(c.getString("State")),
                    c.hasKey("Mode") ? Mode.valueOf(c.getString("Mode")) : Mode.FORMATION,
                    c.hasKey("Stance") ? Stance.valueOf(c.getString("Stance")) : Stance.ENGAGE);
        } catch (IllegalArgumentException e) {
            return null; // corrupt intent never becomes a move to the origin
        }
    }
}
