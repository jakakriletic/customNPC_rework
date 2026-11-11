package si.ladja.xaerofleet.network;

import io.netty.buffer.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArmyPacketTest {
    public static ArmyMarker marker(){return new ArmyMarker("North","Owner","main","MOVING","FORMATION","LEGIJA","PASSIVE",0,-12,64,32,8,32,2,5,123,true);}
    @Test public void snapshotRoundTripCopiesAllFields(){
        ByteBuf b=Unpooled.buffer();new ArmySnapshot(Arrays.asList(marker())).toBytes(b);
        ArmySnapshot q=new ArmySnapshot();q.fromBytes(b);ArmyMarker m=q.markers.get(0);
        assertEquals("North",m.name);assertEquals("Owner",m.owner);assertEquals("main",m.kind);
        assertEquals("MOVING",m.state);assertEquals("FORMATION",m.mode);assertEquals("LEGIJA",m.shape);
        assertEquals("PASSIVE",m.stance);assertEquals(-12,m.x);assertEquals(64,m.y);assertEquals(32,m.z);
        assertEquals(8,m.members);assertEquals(32,m.loaded);assertEquals(2,m.omittedClusters);assertEquals(5,m.omittedMembers);
        assertEquals(123,m.lastSeen);assertTrue(m.commandable);assertFalse(b.isReadable());b.release();
    }
    @Test public void eachActionRoundTripsAndDeduplicatesNames(){
        for(int action=0;action<3;action++){
            ArmyAction p=new ArmyAction(action,Arrays.asList("North","North","South"),-1,-32.25,44.5,3,6,true);
            ByteBuf b=Unpooled.buffer();p.toBytes(b);ArmyAction q=new ArmyAction();q.fromBytes(b);
            assertEquals(Arrays.asList("North","South"),q.names);assertEquals(action,q.action);assertEquals(-1,q.dimension);
            assertEquals(-32.25,q.x,0);assertEquals(44.5,q.z,0);assertEquals(6,q.speed,0);assertEquals(3,q.shape);assertTrue(q.engage);b.release();
        }
    }
    @Test public void emptySnapshotRepresentsDisabledOrAbsentProvider(){
        ByteBuf b=Unpooled.buffer();new ArmySnapshot(Collections.emptyList()).toBytes(b);
        ArmySnapshot p=new ArmySnapshot();p.fromBytes(b);assertTrue(p.markers.isEmpty());b.release();
    }
    @Test public void worstCaseSnapshotStaysBelowForgePayloadLimit(){
        char[] chars=new char[256];Arrays.fill(chars,'x');String text=new String(chars);
        ArmyMarker max=new ArmyMarker(text,text,text,text,text,text,text,0,0,64,0,0,0,0,0,-1,false);
        List<ArmyMarker> markers=new ArrayList<>();for(int i=0;i<256;i++)markers.add(max);
        ByteBuf b=Unpooled.buffer();new ArmySnapshot(markers).toBytes(b);assertTrue(b.readableBytes()<1024*1024);b.release();
    }
    @Test public void malformedActionsAreRefusedBeforeHandlers(){
        ArmyAction good=new ArmyAction(0,Arrays.asList("North"),0,0,0,0,1,false);
        ByteBuf b=Unpooled.buffer();good.toBytes(b);byte[] valid=new byte[b.readableBytes()];b.readBytes(valid);b.release();
        for(int n=0;n<valid.length;n++){
            ByteBuf truncated=Unpooled.wrappedBuffer(Arrays.copyOf(valid,n));
            try{new ArmyAction().fromBytes(truncated);fail("accepted length "+n);}catch(IllegalArgumentException expected){}finally{truncated.release();}
        }
        for(int count:new int[]{0,17,255})reject(Unpooled.buffer().writeByte(0).writeByte(count),false);
        // Leave room for the trailing byte; wrappedBuffer(...).copy() has fixed capacity.
        reject(Unpooled.buffer(valid.length+1).writeBytes(valid).writeByte(4),false);
    }
    @Test public void invalidActionValuesAreRejected(){
        for(double value:new double[]{Double.NaN,Double.POSITIVE_INFINITY,30000000}){
            try{new ArmyAction(0,Arrays.asList("North"),0,value,0,0,1,true);fail();}catch(IllegalArgumentException expected){}
        }
        for(double speed:new double[]{Double.NaN,0.49,6.01}){
            try{new ArmyAction(0,Arrays.asList("North"),0,0,0,0,speed,true);fail();}catch(IllegalArgumentException expected){}
        }
        for(int action:new int[]{-1,3,255}){
            try{new ArmyAction(action,Arrays.asList("North"),0,0,0,0,1,true);fail();}catch(IllegalArgumentException expected){}
        }
        for(int shape:new int[]{-1,5,255}){
            try{new ArmyAction(0,Arrays.asList("North"),0,0,0,shape,1,true);fail();}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void flagActionRoundTripsAndRefusesBadInput(){
        ArmyFlagAction p=new ArmyFlagAction(Arrays.asList("North","North"),new double[]{1,200},new double[]{2,100},0,-32.25,44.5,4,3,true);
        ByteBuf b=Unpooled.buffer();p.toBytes(b);ArmyFlagAction q=new ArmyFlagAction();q.fromBytes(b);b.release();
        assertEquals(Arrays.asList("North","North"),q.names);assertEquals(200,q.flagX[1],0);assertEquals(100,q.flagZ[1],0);
        assertEquals(4,q.shape);assertEquals(-32.25,q.x,0);assertTrue(q.engage);
        try{new ArmyFlagAction(Arrays.asList("North"),new double[]{1,2},new double[]{1},0,0,0,0,1,true);fail();}catch(IllegalArgumentException expected){}
        try{new ArmyFlagAction(Arrays.asList("North"),new double[]{Double.NaN},new double[]{1},0,0,0,0,1,true);fail();}catch(IllegalArgumentException expected){}
        try{new ArmyFlagAction(Collections.<String>emptyList(),new double[0],new double[0],0,0,0,0,1,true);fail();}catch(IllegalArgumentException expected){}
        ByteBuf good=Unpooled.buffer();p.toBytes(good);byte[] valid=new byte[good.readableBytes()];good.readBytes(valid);good.release();
        for(int n=0;n<valid.length;n++){ByteBuf cut=Unpooled.wrappedBuffer(Arrays.copyOf(valid,n));
            try{new ArmyFlagAction().fromBytes(cut);fail("accepted length "+n);}catch(IllegalArgumentException expected){}finally{cut.release();}}
    }
    @Test public void freeForAllShapeRoundTripsAsItsOwnWireValue(){
        ArmyAction p=new ArmyAction(0,Arrays.asList("North"),0,1,2,4,3,false);
        ByteBuf b=Unpooled.buffer();p.toBytes(b);ArmyAction q=new ArmyAction();q.fromBytes(b);b.release();
        assertEquals(4,q.shape);assertEquals("freeforall",ArmyAction.SHAPES[q.shape]);
        assertEquals(Arrays.asList("legion","column","ring","swarm","freeforall"),Arrays.asList(ArmyAction.SHAPES));
    }
    @Test public void malformedSnapshotsAreRejected(){
        reject(Unpooled.buffer().writeShort(257),true);
        reject(Unpooled.buffer().writeShort(1).writeShort(65535),true);
        reject(Unpooled.buffer().writeShort(0).writeByte(1),true);
        ByteBuf b=Unpooled.buffer();new ArmySnapshot(Arrays.asList(marker())).toBytes(b);
        byte[] valid=new byte[b.readableBytes()];b.readBytes(valid);b.release();
        for(int n=0;n<valid.length;n++)reject(Unpooled.wrappedBuffer(Arrays.copyOf(valid,n)),true);
    }
    private void reject(ByteBuf b,boolean snapshot){
        try{if(snapshot)new ArmySnapshot().fromBytes(b);else new ArmyAction().fromBytes(b);fail("accepted malformed packet");}
        catch(IllegalArgumentException expected){}finally{b.release();}
    }
    @Test(expected=IllegalArgumentException.class) public void oversizedTextCannotAllocateUnboundedString(){
        ArmyMarker.text(Unpooled.buffer().writeShort(30000));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidCoordinatesCannotReachRenderer(){
        new ArmyMarker("North","","main","NONE","","","",0,0,-1,0,0,0,0,0,-1,false);
    }
}
