package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public final class StormFeedbackPacket implements IMessage {
    public boolean success;public String text="";
    public StormFeedbackPacket() { }
    public StormFeedbackPacket(boolean success,String text) {this.success=success;this.text=text;}
    @Override public void toBytes(ByteBuf b) {b.writeBoolean(success);ByteBufUtils.writeUTF8String(b,text);}
    @Override public void fromBytes(ByteBuf b) {success=b.readBoolean();text=ByteBufUtils.readUTF8String(b);if(text.length()>160)throw new IllegalArgumentException("Storm feedback too long");}
    public static final class Handler implements IMessageHandler<StormFeedbackPacket,IMessage> {
        @Override public IMessage onMessage(StormFeedbackPacket packet,MessageContext context) {
            si.ladja.xaerofleet.client.StormClient.feedback(packet);return null;
        }
    }
}
