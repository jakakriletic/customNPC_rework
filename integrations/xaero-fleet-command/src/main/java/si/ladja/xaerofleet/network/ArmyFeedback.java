package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.client.MapToast;

public final class ArmyFeedback implements IMessage {
    private boolean accepted;
    private int ok,refused;
    private String message="";
    public ArmyFeedback(){}
    public ArmyFeedback(CommandFeedback f){accepted=f.accepted;ok=f.acceptedCount;refused=f.rejectedCount;
        message=f.reason==null?"":f.reason.substring(0,Math.min(100,f.reason.length()));}
    public void toBytes(ByteBuf b){b.writeBoolean(accepted).writeByte(ok).writeByte(refused);ArmyMarker.text(b,message);}
    public void fromBytes(ByteBuf b){accepted=b.readBoolean();ok=b.readUnsignedByte();refused=b.readUnsignedByte();message=ArmyMarker.text(b);
        if(ok+refused>16 || b.isReadable())throw new IllegalArgumentException("Army feedback bound");}
    public static final class Handler implements IMessageHandler<ArmyFeedback,IMessage>{
        public IMessage onMessage(ArmyFeedback m,MessageContext c){Minecraft.getMinecraft().addScheduledTask(()->
            MapToast.show("Armies: "+m.ok+" accepted, "+m.refused+" refused: "+m.message,m.accepted?MapToast.INFO:MapToast.BAD));return null;}
    }
}
