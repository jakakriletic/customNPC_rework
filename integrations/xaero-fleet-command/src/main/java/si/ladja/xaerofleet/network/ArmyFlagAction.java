package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import si.ladja.xaerofleet.integration.ArmyGround;
import si.ladja.xaerofleet.integration.CnpcArmyBridge;

/**
 * v0.1.13 (wire 0.6.2-armies): move single map flags. Each entry is an army name and the flag position the player
 * clicked; the server moves only that flag's members (CNPC splits them off when they are not the whole army).
 * No identity or authority in the payload; every flag gets an independent server result.
 */
public final class ArmyFlagAction implements IMessage {
    public static final int MAX_FLAGS = 16;
    private static final PacketRate RATE=new PacketRate(500);
    public List<String> names=Collections.emptyList();
    public double[] flagX=new double[0], flagZ=new double[0];
    public int dimension,shape;
    public double x,z,speed;
    public boolean engage;
    public ArmyFlagAction(){}
    public ArmyFlagAction(List<String> names,double[] flagX,double[] flagZ,int dimension,double x,double z,int shape,double speed,boolean engage){
        this.names=Collections.unmodifiableList(new ArrayList<>(names));this.flagX=flagX.clone();this.flagZ=flagZ.clone();
        this.dimension=dimension;this.x=x;this.z=z;this.shape=shape;this.speed=speed;this.engage=engage;validate();
    }
    void validate(){
        if(names.isEmpty() || names.size()>MAX_FLAGS || flagX.length!=names.size() || flagZ.length!=names.size()
                || shape<0 || shape>=ArmyAction.SHAPES.length || !coord(x) || !coord(z)
                || !Double.isFinite(speed) || speed<0.5 || speed>6)throw new IllegalArgumentException("Invalid army flag action");
        for(int i=0;i<names.size();i++){
            String n=names.get(i);
            if(n==null || n.isEmpty() || n.length()>64 || !coord(flagX[i]) || !coord(flagZ[i]))throw new IllegalArgumentException("Invalid army flag");
        }
    }
    private static boolean coord(double v){return Double.isFinite(v) && Math.abs(v)<=29999984;}
    public void toBytes(ByteBuf b){
        validate();b.writeByte(names.size());
        for(int i=0;i<names.size();i++){ArmyMarker.text(b,names.get(i));b.writeDouble(flagX[i]).writeDouble(flagZ[i]);}
        b.writeInt(dimension).writeDouble(x).writeDouble(z).writeByte(shape).writeDouble(speed).writeBoolean(engage);
    }
    public void fromBytes(ByteBuf b){
        try {
            int count=b.readUnsignedByte();
            if(count<1 || count>MAX_FLAGS)throw new IllegalArgumentException("Army flag bound");
            List<String> n=new ArrayList<>();flagX=new double[count];flagZ=new double[count];
            for(int i=0;i<count;i++){n.add(ArmyMarker.text(b));flagX[i]=b.readDouble();flagZ[i]=b.readDouble();}
            names=Collections.unmodifiableList(n);dimension=b.readInt();x=b.readDouble();z=b.readDouble();
            shape=b.readUnsignedByte();speed=b.readDouble();engage=b.readBoolean();validate();
            if(b.isReadable())throw new IllegalArgumentException("Army flag action trailing bytes");
        }catch(IndexOutOfBoundsException ex){throw new IllegalArgumentException("Truncated army flag action",ex);}
    }
    public static final class Handler implements IMessageHandler<ArmyFlagAction,IMessage> {
        public IMessage onMessage(ArmyFlagAction m,MessageContext context){
            EntityPlayerMP p=context.getServerHandler().player;
            synchronized(RATE){if(!RATE.allow(p))return null;}
            p.getServerWorld().addScheduledTask(()->handle(p,m));return null;
        }
        private static void handle(EntityPlayerMP p,ArmyFlagAction m){
            if(p.getServer()==null || p.getServer().getPlayerList().getPlayerByUUID(p.getUniqueID())!=p)return;
            CnpcArmyBridge bridge=CnpcArmyBridge.discover();
            if(!bridge.isAvailable() || m.dimension!=p.dimension){send(p,new CommandFeedback(false,0,m.names.size(),"army_integration_unavailable"));return;}
            BlockPos ground=ArmyGround.resolve(p.getServerWorld(),m.x,m.z);
            if(ground==null){send(p,new CommandFeedback(false,0,m.names.size(),"Army target needs safe ground within 8 blocks"));return;}
            int accepted=0,rejected=0;String reason="";
            for(int i=0;i<m.names.size();i++){
                CommandFeedback f=bridge.flagMove(p.getServer(),p.getUniqueID(),m.names.get(i),p.dimension,m.flagX[i],m.flagZ[i],
                        m.x,ground.getY(),m.z,ArmyAction.SHAPES[m.shape],m.speed,m.engage?"engage":"passive");
                accepted+=f.acceptedCount;rejected+=f.rejectedCount;reason=f.reason;
            }
            send(p,new CommandFeedback(rejected==0,accepted,rejected,reason));
        }
        private static void send(EntityPlayerMP p,CommandFeedback f){FleetNetwork.CHANNEL.sendTo(new ArmyFeedback(f),p);}
    }
}
