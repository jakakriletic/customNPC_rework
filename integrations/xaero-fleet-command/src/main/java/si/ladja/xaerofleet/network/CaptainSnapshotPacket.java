package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import si.ladja.xaerofleet.client.ClientCaptainStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CaptainSnapshotPacket implements IMessage {
    public static final int MAX_CAPTAINS = 64;
    private List<CaptainView> captains = Collections.emptyList();

    public CaptainSnapshotPacket() {}
    public CaptainSnapshotPacket(List<CaptainView> captains) {
        this.captains = new ArrayList<>(captains.subList(0,
                Math.min(MAX_CAPTAINS, captains.size())));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(captains.size());
        for (CaptainView captain : captains) {
            writeUuid(buf, captain.captainId);
            writeUuid(buf, captain.shipId);
            buf.writeInt(captain.dimension);
            buf.writeDouble(captain.x);
            buf.writeDouble(captain.y);
            buf.writeDouble(captain.z);
            buf.writeFloat(captain.heading);
            ByteBufUtils.writeUTF8String(buf, captain.faction);
            ByteBufUtils.writeUTF8String(buf, captain.orderState);
            ByteBufUtils.writeUTF8String(buf, captain.label);
            buf.writeBoolean(captain.commandable);
            ByteBufUtils.writeUTF8String(buf, captain.orderEngagement);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readUnsignedByte();
        if (count > MAX_CAPTAINS) throw new IllegalArgumentException("Too many captain markers");
        List<CaptainView> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decoded.add(new CaptainView(readUuid(buf), readUuid(buf), buf.readInt(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(),
                    ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
                    ByteBufUtils.readUTF8String(buf), buf.readBoolean(),
                    ByteBufUtils.readUTF8String(buf)));
        }
        captains = decoded;
    }

    private static void writeUuid(ByteBuf buf, UUID id) {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
    }

    private static UUID readUuid(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    List<CaptainView> captains() {
        return Collections.unmodifiableList(captains);
    }

    public static final class Handler implements IMessageHandler<CaptainSnapshotPacket, IMessage> {
        @Override
        public IMessage onMessage(CaptainSnapshotPacket message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(
                    () -> ClientCaptainStore.replace(message.captains));
            return null;
        }
    }
}
