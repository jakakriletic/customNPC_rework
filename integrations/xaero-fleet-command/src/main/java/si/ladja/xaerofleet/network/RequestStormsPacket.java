package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.storm.StormService;

/** Also negotiates storm protocol support: old clients are never sent new packets. */
public final class RequestStormsPacket implements IMessage {
    private static final PacketRate RATE=new PacketRate(1000);
    @Override public void toBytes(ByteBuf buffer) { }
    @Override public void fromBytes(ByteBuf buffer) { }
    public static final class Handler implements IMessageHandler<RequestStormsPacket,IMessage> {
        @Override public IMessage onMessage(RequestStormsPacket packet,MessageContext context) {
            EntityPlayerMP player=context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()->{ if(RATE.allow(player))StormService.subscribe(player); });
            return null;
        }
    }
}
