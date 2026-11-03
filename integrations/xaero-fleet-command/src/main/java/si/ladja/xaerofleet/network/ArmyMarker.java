package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/** Bounded copies only: no CustomNPCs types cross this wire. */
public final class ArmyMarker {
    public final String name, owner, kind, state, mode, shape, stance;
    public final int dimension, x, y, z, members, loaded, omittedClusters, omittedMembers;
    public final long lastSeen;
    public final boolean commandable;

    public ArmyMarker(String name, String owner, String kind, String state, String mode,
                      String shape, String stance, int dimension, int x, int y, int z,
                      int members, int loaded, int omittedClusters, int omittedMembers,
                      long lastSeen, boolean commandable) {
        if (name == null || name.isEmpty() || members < 0 || loaded < 0
                || omittedClusters < 0 || omittedMembers < 0 || y < 0 || y > 255
                || Math.abs((long)x) > 29999984 || Math.abs((long)z) > 29999984)
            throw new IllegalArgumentException("Invalid army marker");
        this.name = checked(name); this.owner = checked(owner); this.kind = checked(kind);
        this.state = checked(state); this.mode = checked(mode); this.shape = checked(shape);
        this.stance = checked(stance); this.dimension = dimension; this.x = x; this.y = y; this.z = z;
        this.members = members; this.loaded = loaded; this.omittedClusters = omittedClusters;
        this.omittedMembers = omittedMembers; this.lastSeen = lastSeen; this.commandable = commandable;
    }

    private static String checked(String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length > 256)
            throw new IllegalArgumentException("Army text exceeds bound");
        return value;
    }
    public static void text(ByteBuf b, String value) {
        byte[] bytes = checked(value).getBytes(StandardCharsets.UTF_8);
        b.writeShort(bytes.length); b.writeBytes(bytes);
    }
    public static String text(ByteBuf b) {
        int n = b.readUnsignedShort();
        if (n > 256 || n > b.readableBytes()) throw new IllegalArgumentException("Invalid army text");
        byte[] bytes = new byte[n]; b.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    void write(ByteBuf b) {
        text(b,name); text(b,owner); text(b,kind); text(b,state); text(b,mode); text(b,shape); text(b,stance);
        b.writeInt(dimension).writeInt(x).writeInt(y).writeInt(z).writeInt(members).writeInt(loaded)
                .writeInt(omittedClusters).writeInt(omittedMembers).writeLong(lastSeen).writeBoolean(commandable);
    }
    static ArmyMarker read(ByteBuf b) {
        return new ArmyMarker(text(b),text(b),text(b),text(b),text(b),text(b),text(b),
                b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),
                b.readLong(),b.readBoolean());
    }
}
