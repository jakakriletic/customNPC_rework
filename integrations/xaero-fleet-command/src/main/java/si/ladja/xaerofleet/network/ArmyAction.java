package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.integration.ArmyGround;
import si.ladja.xaerofleet.integration.CnpcArmyBridge;

/** No identity or authority in the payload. Every selected army receives an independent result. */
public final class ArmyAction implements IMessage {
    public static final int MOVE=0, CANCEL=1, ENGAGEMENT=2, MAX_ARMIES=16;
    /** Wire value of the shape index; 4 = free-for-all march (CNPC M11.11, protocol 0.6.1-armies). */
    public static final String[] SHAPES={"legion","column","ring","swarm","freeforall"};
    private static final PacketRate RATE=new PacketRate(500);
    public List<String> names=Collections.emptyList();
    public int action,dimension,shape;
    public double x,z,speed;
    public boolean engage;
    public ArmyAction(){}
    public ArmyAction(int action,List<String> names,int dimension,double x,double z,int shape,double speed,boolean engage){
        this.action=action;this.names=Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(names)));
        this.dimension=dimension;this.x=x;this.z=z;this.shape=shape;this.speed=speed;this.engage=engage;validate();
    }
    void validate(){
        if(action<0 || action>2 || names.isEmpty() || names.size()>MAX_ARMIES || shape<0 || shape>4
                || !Double.isFinite(x) || !Double.isFinite(z) || Math.abs(x)>29999984 || Math.abs(z)>29999984
                || !Double.isFinite(speed) || speed<0.5 || speed>6)throw new IllegalArgumentException("Invalid army action");
        for(String n:names)if(n==null || n.isEmpty() || n.length()>64)throw new IllegalArgumentException("Invalid army name");
    }
    public void toBytes(ByteBuf b){
        validate();b.writeByte(action).writeByte(names.size());for(String n:names)ArmyMarker.text(b,n);
        b.writeInt(dimension).writeDouble(x).writeDouble(z).writeByte(shape).writeDouble(speed).writeBoolean(engage);
    }
    public void fromBytes(ByteBuf b){
        try {
            action=b.readUnsignedByte();int count=b.readUnsignedByte();
            if(count<1 || count>MAX_ARMIES)throw new IllegalArgumentException("Army selection bound");
            LinkedHashSet<String> out=new LinkedHashSet<>();for(int i=0;i<count;i++)out.add(ArmyMarker.text(b));
            names=Collections.unmodifiableList(new ArrayList<>(out));dimension=b.readInt();x=b.readDouble();z=b.readDouble();
            shape=b.readUnsignedByte();speed=b.readDouble();engage=b.readBoolean();validate();
            if(b.isReadable())throw new IllegalArgumentException("Army action trailing bytes");
        }catch(IndexOutOfBoundsException ex){throw new IllegalArgumentException("Truncated army action",ex);}
    }
    public static final class Handler implements IMessageHandler<ArmyAction,IMessage> {
        public IMessage onMessage(ArmyAction m,MessageContext context){
            EntityPlayerMP p=context.getServerHandler().player;
            synchronized(RATE){if(!RATE.allow(p))return null;}
            p.getServerWorld().addScheduledTask(()->handle(p,m));return null;
        }
        private static void handle(EntityPlayerMP p,ArmyAction m){
            if(p.getServer()==null || p.getServer().getPlayerList().getPlayerByUUID(p.getUniqueID())!=p)return;
            CnpcArmyBridge bridge=CnpcArmyBridge.discover();
            if(!bridge.isAvailable() || m.dimension!=p.dimension){send(p,new CommandFeedback(false,0,m.names.size(),"army_integration_unavailable"));return;}
            BlockPos ground=m.action==MOVE?ArmyGround.resolve(p.getServerWorld(),m.x,m.z):null;
            if(m.action==MOVE && ground==null){send(p,new CommandFeedback(false,0,m.names.size(),"Army target needs loaded, safe ground"));return;}
            int accepted=0,rejected=0;String reason="";
            for(String name:m.names){
                CommandFeedback f=m.action==MOVE?bridge.move(p.getServer(),p.getUniqueID(),name,p.dimension,
                        m.x,ground.getY(),m.z,SHAPES[m.shape],m.speed,m.engage?"engage":"passive")
                        :m.action==CANCEL?bridge.cancel(p.getServer(),p.getUniqueID(),name)
                        :bridge.engagement(p.getServer(),p.getUniqueID(),name,m.engage?"engage":"passive");
                accepted+=f.acceptedCount;rejected+=f.rejectedCount;reason=f.reason;
            }
            send(p,new CommandFeedback(rejected==0,accepted,rejected,reason));
        }
        private static void send(EntityPlayerMP p,CommandFeedback f){FleetNetwork.CHANNEL.sendTo(new ArmyFeedback(f),p);}
    }
}
