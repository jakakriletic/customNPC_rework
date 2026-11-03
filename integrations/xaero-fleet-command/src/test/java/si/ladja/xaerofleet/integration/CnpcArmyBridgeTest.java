package si.ladja.xaerofleet.integration;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import org.junit.After;
import org.junit.Test;
import si.ladja.xaerofleet.network.*;
import static org.junit.Assert.*;

public class CnpcArmyBridgeTest {
    private static UUID captured;
    private static List<?> values=Collections.emptyList();
    @After public void reset(){values=Collections.emptyList();captured=null;}
    public static class Reply {
        public boolean isAccepted(){return true;}
        public String getMessage(){return "RWARMY accepted";}
    }
    public static class Service {
        public static int getApiVersion(){return 1;}
        public static List<?> armiesVisibleTo(MinecraftServer s,UUID player,int dim){captured=player;return values;}
        public static Reply submitMoveOrder(MinecraftServer s,UUID p,String n,int d,double x,double y,double z,String shape,double speed,String stance){captured=p;return new Reply();}
        public static Reply cancelOrder(MinecraftServer s,UUID p,String n){captured=p;return new Reply();}
        public static Reply setOrderEngagement(MinecraftServer s,UUID p,String n,String stance){captured=p;return new Reply();}
    }
    public static class WrongVersion extends Service { public static int getApiVersion(){return 2;} }
    public static class BrokenService extends Service {
        public static List<?> armiesVisibleTo(MinecraftServer s,UUID p,int dim){throw new LinkageError("incompatible provider");}
    }
    public static class Marker {
        public String getArmyName(){return "North";} public String getOwnerName(){return "Owner";}
        public String getKind(){return "main";} public String getOrderState(){return "MOVING";}
        public String getOrderMode(){return "FORMATION";} public String getOrderShape(){return "LEGIJA";}
        public String getOrderEngagement(){return "PASSIVE";} public boolean isCommandable(){return true;}
        public int getDimension(){return 0;} public int getX(){return -12;} public int getY(){return 64;} public int getZ(){return 32;}
        public int getMemberCount(){return 8;} public int getLoadedCount(){return 32;}
        public int getOmittedClusters(){return 2;} public int getOmittedMembers(){return 5;} public long getLastSeen(){return 123;}
    }
    @Test public void staticContractForwardsAuthenticatedIdentityForEveryOperation(){
        CnpcArmyBridge bridge=CnpcArmyBridge.discover(Service.class);assertTrue(bridge.isAvailable());UUID player=UUID.randomUUID();
        bridge.markers(null,player,0);assertEquals(player,captured);captured=null;
        assertTrue(bridge.move(null,player,"North",0,1,64,2,"swarm",1,"passive").accepted);assertEquals(player,captured);captured=null;
        assertTrue(bridge.cancel(null,player,"North").accepted);assertEquals(player,captured);captured=null;
        assertTrue(bridge.engagement(null,player,"North","engage").accepted);assertEquals(player,captured);
    }
    @Test public void absentAndUnsupportedContractsFailClosed(){
        assertFalse(CnpcArmyBridge.discover(String.class).isAvailable());assertFalse(CnpcArmyBridge.discover(WrongVersion.class).isAvailable());
        assertTrue(CnpcArmyBridge.discover(String.class).markers(null,null,0).isEmpty());
        assertFalse(CnpcArmyBridge.discover(String.class).cancel(null,null,"North").accepted);
    }
    @Test public void linkageFailureAndMalformedMarkersFailClosedWithoutPartialDisclosure(){
        assertTrue(CnpcArmyBridge.discover(BrokenService.class).markers(null,null,0).isEmpty());
        values=Arrays.asList(new Marker(),new Object());assertTrue(CnpcArmyBridge.discover(Service.class).markers(null,null,0).isEmpty());
        assertFalse(CnpcArmyBridge.reply(null).accepted);assertFalse(CnpcArmyBridge.reply(new Object()).accepted);
    }
    @Test public void copiesEveryGetterAndNeverKeepsProviderObjects(){
        values=new ArrayList<>(Arrays.asList(new Marker()));
        List<ArmyMarker> copied=CnpcArmyBridge.discover(Service.class).markers(null,null,0);
        ArmyMarker m=copied.get(0);assertEquals("North",m.name);assertEquals("Owner",m.owner);assertEquals("MOVING",m.state);
        assertEquals("FORMATION",m.mode);assertEquals("LEGIJA",m.shape);assertEquals("PASSIVE",m.stance);assertEquals("main",m.kind);
        assertEquals(0,m.dimension);assertEquals(-12,m.x);assertEquals(64,m.y);assertEquals(32,m.z);assertEquals(8,m.members);
        assertEquals(32,m.loaded);assertEquals(2,m.omittedClusters);assertEquals(5,m.omittedMembers);assertEquals(123,m.lastSeen);assertTrue(m.commandable);
        values=Collections.emptyList();assertEquals(1,copied.size());
        try{copied.clear();fail();}catch(UnsupportedOperationException expected){}
    }
    @Test public void wrongDimensionIsNotDrawnOrPromoted(){
        values=Arrays.asList(new Marker());assertTrue(CnpcArmyBridge.discover(Service.class).markers(null,null,-1).isEmpty());
    }
    @Test public void providerCountIsBounded(){
        values=Collections.nCopies(10000,new Marker());assertEquals(256,CnpcArmyBridge.discover(Service.class).markers(null,null,0).size());
    }
}
