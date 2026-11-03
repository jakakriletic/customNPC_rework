package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.storm.*;

public final class StormControlPacket implements IMessage {
    public static final int CREATE=0, REMOVE=1;
    private static final PacketRate RATE=new PacketRate(500);
    public int action, dimension, duration;
    public GridArea area;
    public StormControlPacket() { }
    public StormControlPacket(int action,int dimension,GridArea area,int duration) {
        this.action=action;this.dimension=dimension;this.area=area;this.duration=duration;validate();
    }
    private void validate() {
        if(area==null || action<CREATE || action>REMOVE || action==CREATE && (duration<1200 || duration>StormRegion.MAX_DURATION))
            throw new IllegalArgumentException("Invalid storm request");
    }
    @Override public void toBytes(ByteBuf b) {
        validate();b.writeByte(action);b.writeInt(dimension);b.writeInt(area.minX);b.writeInt(area.minZ);b.writeInt(area.maxX);b.writeInt(area.maxZ);b.writeInt(duration);
    }
    @Override public void fromBytes(ByteBuf b) {
        if(b.readableBytes()!=25)throw new IllegalArgumentException("Invalid storm packet size");
        action=b.readUnsignedByte();dimension=b.readInt();area=new GridArea(b.readInt(),b.readInt(),b.readInt(),b.readInt());duration=b.readInt();validate();
    }
    public static final class Handler implements IMessageHandler<StormControlPacket,IMessage> {
        @Override public IMessage onMessage(StormControlPacket packet,MessageContext context) {
            EntityPlayerMP player=context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()->handle(player,packet));return null;
        }
        private static void reply(EntityPlayerMP p,boolean success,String text) { FleetNetwork.CHANNEL.sendTo(new StormFeedbackPacket(success,text),p); }
        private static void handle(EntityPlayerMP player,StormControlPacket p) {
            if(!RATE.allow(player)){ reply(player,false,"Wait a moment before the next storm action.");return; }
            if(!StormService.canManage(player)){reply(player,false,"Storm control requires OP permission (or the single-player owner).");return;}
            if(player.dimension!=p.dimension || !player.world.provider.hasSkyLight()) { reply(player,false,"Select an area in your current surface dimension.");return; }
            GridArea a=p.area;
            if(!player.world.getWorldBorder().contains(new BlockPos(a.left(),64,a.top()))
                    || !player.world.getWorldBorder().contains(new BlockPos(a.right()-1,64,a.bottom()-1))) {
                reply(player,false,"The selected area crosses the world border.");return;
            }
            StormData data=StormData.get(player.world);
            if(p.action==CREATE) {
                if(!data.create(p.dimension,a,player.world.getTotalWorldTime(),p.duration,player.world.rand.nextLong())) {
                    reply(player,false,"Too many active storms (maximum 32).");return;
                }
                reply(player,true,"Winter storm started: " + a.width()*64 + " x " + a.depth()*64 + " blocks.");
            } else {
                int removed=data.remove(a);
                reply(player,removed>0,removed>0 ? "Winter storms removed: " + removed : "No winter storms intersect the selected area.");
            }
            StormService.subscribe(player);StormService.changed(player.getServer(),player.dimension);
        }
    }
}
