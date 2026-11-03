package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import si.ladja.xaerofleet.client.MapToast;

public final class CommandResultPacket implements IMessage {

    /**
     * Milliseconds before the same refusal is worth saying again.
     *
     * <p>A refusal repeated eight times tells the player nothing the first one
     * did not, and a wall of red reads as a broken mod rather than as a mod
     * declining. Accepted orders are never suppressed: every one of those is
     * news.</p>
     */
    private static final long SAME_REASON_QUIET_MS = 4000L;

    private static String lastReason = "";
    private static long lastReasonAt;

    public static final byte KIND_MOVE = 0;
    public static final byte KIND_CANCEL = 1;
    public static final byte KIND_ENGAGE = 2;
    public static final byte KIND_PASSIVE = 3;

    private byte kind = KIND_MOVE;
    private CommandFeedback feedback = new CommandFeedback(false, 0, 0, "");

    public CommandResultPacket() {}
    public CommandResultPacket(CommandFeedback feedback) { this(KIND_MOVE, feedback); }
    public CommandResultPacket(byte kind, CommandFeedback feedback) {
        this.kind = kind;
        this.feedback = feedback;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind);
        buf.writeBoolean(feedback.accepted);
        buf.writeByte(Math.min(255, feedback.acceptedCount));
        buf.writeByte(Math.min(255, feedback.rejectedCount));
        ByteBufUtils.writeUTF8String(buf, feedback.reason);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = buf.readByte();
        feedback = new CommandFeedback(buf.readBoolean(), buf.readUnsignedByte(),
                buf.readUnsignedByte(), ByteBufUtils.readUTF8String(buf));
    }

    private static String acceptedLabel(byte kind) {
        switch (kind) {
            case KIND_CANCEL: return "Stopped: ";
            case KIND_ENGAGE: return "Attacking enemies: ";
            case KIND_PASSIVE: return "Passive cruise: ";
            default: return "Under way: ";
        }
    }

    private static String refusedLabel(byte kind) {
        switch (kind) {
            case KIND_CANCEL: return "Stop refused: ";
            case KIND_ENGAGE:
            case KIND_PASSIVE: return "Change refused: ";
            default: return "Order refused: ";
        }
    }

    public static final class Handler implements IMessageHandler<CommandResultPacket, IMessage> {
        @Override
        public IMessage onMessage(CommandResultPacket message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().player == null) return;
                CommandFeedback f = message.feedback;
                if (!f.accepted && f.reason.equals(lastReason)
                        && System.currentTimeMillis() - lastReasonAt < SAME_REASON_QUIET_MS) {
                    // Said that already, and saying it again is just noise.
                    return;
                }
                lastReason = f.accepted ? "" : f.reason;
                lastReasonAt = System.currentTimeMillis();
                String text;
                if (f.accepted) {
                    text = acceptedLabel(message.kind) + f.acceptedCount
                            + (f.rejectedCount > 0 ? ", refused " + f.rejectedCount
                            + " (" + MapToast.reason(f.reason) + ")" : "");
                } else {
                    text = refusedLabel(message.kind) + MapToast.reason(f.reason);
                }
                MapToast.show(text, f.accepted ? (f.rejectedCount > 0 ? MapToast.INFO
                        : MapToast.GOOD) : MapToast.BAD);
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                        (f.accepted ? TextFormatting.GREEN : TextFormatting.RED)
                                + "Fleet: " + text));
                FleetNetwork.CHANNEL.sendToServer(new RequestCaptains());
            });
            return null;
        }
    }
}
