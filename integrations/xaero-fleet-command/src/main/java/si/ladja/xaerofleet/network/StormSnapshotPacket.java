package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.storm.*;

public final class StormSnapshotPacket implements IMessage {
    public int dimension,cold,coldThreshold;
    public long now;
    public boolean canManage;
    public List<StormRegion> regions=Collections.emptyList();
    public StormSnapshotPacket() { }
    public StormSnapshotPacket(int dimension,long now,List<StormRegion> regions,boolean canManage,int cold,int coldThreshold) {
        this.dimension=dimension;this.now=now;this.regions=new ArrayList<>(regions);this.canManage=canManage;this.cold=cold;this.coldThreshold=coldThreshold;
    }
    @Override public void toBytes(ByteBuf b) {
        if(regions.size()>StormRegion.MAX_ACTIVE)throw new IllegalArgumentException("Too many storms");
        b.writeInt(dimension);b.writeLong(now);b.writeBoolean(canManage);b.writeInt(cold);b.writeInt(coldThreshold);b.writeByte(regions.size());
        for(StormRegion r:regions){b.writeInt(r.id);b.writeInt(r.area.minX);b.writeInt(r.area.minZ);b.writeInt(r.area.maxX);b.writeInt(r.area.maxZ);b.writeLong(r.started);b.writeLong(r.expires);b.writeLong(r.seed);}
    }
    @Override public void fromBytes(ByteBuf b) {
        dimension=b.readInt();now=b.readLong();canManage=b.readBoolean();cold=b.readInt();coldThreshold=b.readInt();int count=b.readUnsignedByte();
        if(now<0 || count>StormRegion.MAX_ACTIVE || coldThreshold<20 || coldThreshold>2400 || cold<0 || cold>coldThreshold*2 || b.readableBytes()!=count*44)
            throw new IllegalArgumentException("Invalid storm snapshot");
        List<StormRegion> list=new ArrayList<>(count);
        for(int i=0;i<count;i++)list.add(new StormRegion(b.readInt(),dimension,new GridArea(b.readInt(),b.readInt(),b.readInt(),b.readInt()),b.readLong(),b.readLong(),b.readLong()));
        regions=Collections.unmodifiableList(list);
    }
    public static final class Handler implements IMessageHandler<StormSnapshotPacket,IMessage> {
        @Override public IMessage onMessage(StormSnapshotPacket packet,MessageContext context) {
            si.ladja.xaerofleet.client.StormClient.receive(packet);return null;
        }
    }
}
