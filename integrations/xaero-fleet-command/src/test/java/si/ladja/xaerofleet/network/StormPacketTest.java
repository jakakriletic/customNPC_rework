package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import org.junit.Test;
import si.ladja.xaerofleet.storm.*;
import static org.junit.Assert.*;

public class StormPacketTest {
    @Test public void controlPacketPreservesReversedNegativeGridSelection() {
        StormControlPacket p=new StormControlPacket(StormControlPacket.CREATE,0,new GridArea(-1,3,-4,0),12000);
        ByteBuf bytes=Unpooled.buffer();p.toBytes(bytes);assertEquals(25,bytes.readableBytes());
        StormControlPacket q=new StormControlPacket();q.fromBytes(bytes);
        assertEquals(-4,q.area.minX);assertEquals(3,q.area.maxZ);assertEquals(12000,q.duration);
        bytes.release();
    }
    @Test public void snapshotsPreserveAuthoritativeClockColdPermissionAndRegion() {
        StormRegion s=new StormRegion(4,7,new GridArea(2,3,4,5),100,12100,999);
        StormSnapshotPacket p=new StormSnapshotPacket(7,350,Collections.singletonList(s),true,310,300);
        ByteBuf bytes=Unpooled.buffer();p.toBytes(bytes);assertEquals(66,bytes.readableBytes());
        StormSnapshotPacket q=new StormSnapshotPacket();q.fromBytes(bytes);
        assertTrue(q.canManage);assertEquals(350,q.now);assertEquals(310,q.cold);assertEquals(7,q.regions.get(0).dimension);assertEquals(999,q.regions.get(0).seed);
        bytes.release();
    }
    @Test(expected=IllegalArgumentException.class) public void unknownActionsAreRejected(){new StormControlPacket(3,0,new GridArea(0,0,0,0),1200);}
    @Test(expected=IllegalArgumentException.class) public void durationLimitIsEnforced(){new StormControlPacket(0,0,new GridArea(0,0,0,0),Integer.MAX_VALUE);}
    @Test(expected=IllegalArgumentException.class) public void truncatedRequestsCannotReachWorldLogic(){new StormControlPacket().fromBytes(Unpooled.buffer().writeByte(0));}
    @Test(expected=IllegalArgumentException.class) public void oversizedGridCannotBeDecoded(){
        ByteBuf b=Unpooled.buffer();b.writeByte(0).writeInt(0).writeInt(0).writeInt(0).writeInt(Integer.MAX_VALUE).writeInt(1).writeInt(1200);
        new StormControlPacket().fromBytes(b);
    }
    @Test(expected=IllegalArgumentException.class) public void claimedSnapshotCountCannotAllocateUnboundedLists(){
        ByteBuf b=Unpooled.buffer();b.writeInt(0).writeLong(1).writeBoolean(false).writeInt(0).writeInt(300).writeByte(255);
        new StormSnapshotPacket().fromBytes(b);
    }
}
