package si.ladja.xaerofleet.integration;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import si.ladja.xaerofleet.network.ArmyMarker;
import si.ladja.xaerofleet.network.CommandFeedback;

/** Optional static v1 API. Never imports CNPC or falls back to privileged scripts/commands. */
public final class CnpcArmyBridge {
    private static final String SERVICE = "noppes.npcs.rework.army.ArmyCommandService";
    private final Method visible, move, cancel, engagement, flagMove;
    private CnpcArmyBridge(Method visible, Method move, Method cancel, Method engagement, Method flagMove) {
        this.visible=visible; this.move=move; this.cancel=cancel; this.engagement=engagement; this.flagMove=flagMove;
    }
    public static CnpcArmyBridge discover() {
        try { return discover(Class.forName(SERVICE)); }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return unavailable(); }
    }
    /** Also used to test the consumer against a substitute contract without linking CNPC. */
    static CnpcArmyBridge discover(Class<?> type) {
        try {
            Method version = method(type,"getApiVersion",int.class);
            if (!Integer.valueOf(1).equals(version.invoke(null))) return unavailable();
            Method visible=method(type,"armiesVisibleTo",List.class,MinecraftServer.class,UUID.class,int.class);
            Method move=type.getMethod("submitMoveOrder",MinecraftServer.class,UUID.class,String.class,int.class,
                    double.class,double.class,double.class,String.class,double.class,String.class);
            Method cancel=type.getMethod("cancelOrder",MinecraftServer.class,UUID.class,String.class);
            Method engagement=type.getMethod("setOrderEngagement",MinecraftServer.class,UUID.class,String.class,String.class);
            for(Method m: new Method[]{move,cancel,engagement}) {
                if(!Modifier.isStatic(m.getModifiers())) return unavailable();
                method(m.getReturnType(),"isAccepted",boolean.class);
                method(m.getReturnType(),"getMessage",String.class);
            }
            // M11.11d: optional, additive to v1; an older CNPC moves whole armies only.
            Method flag=null;
            try {
                flag=type.getMethod("submitFlagMoveOrder",MinecraftServer.class,UUID.class,String.class,int.class,
                        double.class,double.class,double.class,double.class,double.class,String.class,double.class,String.class);
                if(!Modifier.isStatic(flag.getModifiers()) || flag.getReturnType()!=move.getReturnType())flag=null;
            } catch (NoSuchMethodException old) { flag=null; }
            return new CnpcArmyBridge(visible,move,cancel,engagement,flag);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return unavailable(); }
    }
    private static Method method(Class<?> type,String name,Class<?> result,Class<?>... parameters) throws NoSuchMethodException {
        Method m=type.getMethod(name,parameters);
        if(m.getReturnType()!=result)throw new NoSuchMethodException(name+" return type");
        if(parameters.length>0 || "getApiVersion".equals(name)) {
            if(!Modifier.isStatic(m.getModifiers()))throw new NoSuchMethodException(name+" static");
        }
        return m;
    }
    private static CnpcArmyBridge unavailable(){return new CnpcArmyBridge(null,null,null,null,null);}
    public boolean supportsFlags(){return flagMove!=null;}
    public boolean isAvailable(){return visible!=null;}
    private Object invoke(Method m,Object... args) {
        if(m==null)return null;
        try {return m.invoke(null,args);}
        catch(ReflectiveOperationException | LinkageError | RuntimeException ex){return null;}
    }
    public List<ArmyMarker> markers(MinecraftServer server,UUID player,int dimension) {
        Object result=invoke(visible,server,player,dimension);
        if(!(result instanceof List))return Collections.emptyList();
        try {
            List<ArmyMarker> out=new ArrayList<>();
            for(Object source:(List<?>)result) {
                if(out.size()==256)break;
                ArmyMarker marker=copy(source);
                if(marker.dimension!=dimension)throw new IllegalArgumentException("wrong dimension");
                out.add(marker);
            }
            return Collections.unmodifiableList(out);
        }catch(ReflectiveOperationException | LinkageError | RuntimeException ex){return Collections.emptyList();}
    }
    static ArmyMarker copy(Object s) throws ReflectiveOperationException {
        return new ArmyMarker(str(s,"getArmyName"),str(s,"getOwnerName"),str(s,"getKind"),
                str(s,"getOrderState"),str(s,"getOrderMode"),str(s,"getOrderShape"),str(s,"getOrderEngagement"),
                num(s,"getDimension"),num(s,"getX"),num(s,"getY"),num(s,"getZ"),num(s,"getMemberCount"),
                num(s,"getLoadedCount"),num(s,"getOmittedClusters"),num(s,"getOmittedMembers"),
                (Long)get(s,"getLastSeen",long.class),(Boolean)get(s,"isCommandable",boolean.class));
    }
    private static Object get(Object s,String name,Class<?> result) throws ReflectiveOperationException {
        return method(s.getClass(),name,result).invoke(s);
    }
    private static String str(Object s,String n)throws ReflectiveOperationException{return (String)get(s,n,String.class);}
    private static int num(Object s,String n)throws ReflectiveOperationException{return (Integer)get(s,n,int.class);}
    public CommandFeedback move(MinecraftServer s,UUID p,String name,int d,double x,double y,double z,String shape,double speed,String stance) {
        return reply(invoke(move,s,p,name,d,x,y,z,shape,speed,stance));
    }
    /** One map flag: only its members move (split off by CNPC when they are not the whole army). */
    public CommandFeedback flagMove(MinecraftServer s,UUID p,String name,int d,double fx,double fz,double x,double y,double z,String shape,double speed,String stance) {
        if(flagMove==null)return new CommandFeedback(false,0,1,"CustomNPC Rework too old to move a single flag");
        return reply(invoke(flagMove,s,p,name,d,fx,fz,x,y,z,shape,speed,stance));
    }
    public CommandFeedback cancel(MinecraftServer s,UUID p,String name){return reply(invoke(cancel,s,p,name));}
    public CommandFeedback engagement(MinecraftServer s,UUID p,String name,String stance){return reply(invoke(engagement,s,p,name,stance));}
    static CommandFeedback reply(Object r) {
        try {
            boolean ok=(Boolean)get(r,"isAccepted",boolean.class);
            String text=str(r,"getMessage");
            if(text==null || text.length()>512)throw new IllegalArgumentException("reply bound");
            return new CommandFeedback(ok,ok?1:0,ok?0:1,text);
        }catch(ReflectiveOperationException | LinkageError | RuntimeException ex){
            return new CommandFeedback(false,0,1,"army_integration_unavailable");
        }
    }
}
