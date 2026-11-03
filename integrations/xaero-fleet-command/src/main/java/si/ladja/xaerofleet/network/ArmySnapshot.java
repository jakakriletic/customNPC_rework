package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.client.ArmyClient;

public final class ArmySnapshot implements IMessage {
    public static final int MAX_MARKERS=256;
    public List<ArmyMarker> markers=Collections.emptyList();
    public ArmySnapshot(){}
    public ArmySnapshot(List<ArmyMarker> values) {
        if(values.size()>MAX_MARKERS)throw new IllegalArgumentException("Army snapshot bound");
        markers=Collections.unmodifiableList(new ArrayList<>(values));
    }
    public void toBytes(ByteBuf b){b.writeShort(markers.size());for(ArmyMarker m:markers)m.write(b);}
    public void fromBytes(ByteBuf b){
        try {
            int n=b.readUnsignedShort();
            if(n>MAX_MARKERS)throw new IllegalArgumentException("Army snapshot bound");
            List<ArmyMarker> decoded=new ArrayList<>();for(int i=0;i<n;i++)decoded.add(ArmyMarker.read(b));
            if(b.isReadable())throw new IllegalArgumentException("Army snapshot trailing bytes");
            markers=Collections.unmodifiableList(decoded);
        }catch(IndexOutOfBoundsException ex){throw new IllegalArgumentException("Truncated army snapshot",ex);}
    }
    public static final class Handler implements IMessageHandler<ArmySnapshot,IMessage> {
        public IMessage onMessage(ArmySnapshot m,MessageContext c){
            Minecraft.getMinecraft().addScheduledTask(()->ArmyClient.update(m.markers));return null;
        }
    }
}
