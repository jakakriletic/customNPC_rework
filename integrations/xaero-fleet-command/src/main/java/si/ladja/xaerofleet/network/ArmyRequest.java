package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.integration.CnpcArmyBridge;

/** Snapshot polling, at most once a second; admission happens before scheduling. */
public final class ArmyRequest implements IMessage {
    private static final PacketRate RATE=new PacketRate(1000);
    public void fromBytes(ByteBuf b){if(b.isReadable())throw new IllegalArgumentException("Army request length");}
    public void toBytes(ByteBuf b){}
    public static final class Handler implements IMessageHandler<ArmyRequest,IMessage> {
        public IMessage onMessage(ArmyRequest message,MessageContext context) {
            EntityPlayerMP p=context.getServerHandler().player;
            synchronized(RATE){if(!RATE.allow(p))return null;}
            p.getServerWorld().addScheduledTask(()->{
                if(p.getServer()==null || p.getServer().getPlayerList().getPlayerByUUID(p.getUniqueID())!=p)return;
                FleetNetwork.CHANNEL.sendTo(new ArmySnapshot(CnpcArmyBridge.discover()
                        .markers(p.getServer(),p.getUniqueID(),p.dimension)),p);
            });
            return null;
        }
    }
}
