package si.ladja.xaerofleet.storm;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class StormRulesTest {
    @Test public void negativeCoordinatesUseFloorAndReverseDraggingSelectsTheSameRectangle() {
        assertEquals(-1,GridArea.cell(-0.1));assertEquals(-1,GridArea.cell(-64));assertEquals(-2,GridArea.cell(-64.1));
        GridArea a=new GridArea(2,3,-1,-2);assertEquals(-64,a.left());assertEquals(192,a.right());assertEquals(4,a.width());
        assertTrue(a.contains(-64,-128));assertFalse(a.contains(192,0));assertFalse(a.contains(0,256));
    }
    @Test public void strongestStormFadesAtEdgesAndWithTime() {
        StormRegion s=new StormRegion(1,0,new GridArea(0,0,0,0),1000,2200,7);
        assertEquals(0,s.intensity(32,32,1000),0);assertEquals(1,s.intensity(32,32,1100),0);
        assertEquals(0.5f,s.intensity(32,32,1050),0.001f);assertEquals(0,s.intensity(32,32,2200),0);
        assertEquals(0,s.intensity(-1,32,1200),0);assertEquals(0,s.intensity(64,32,1200),0);
        assertTrue(s.intensity(8,32,1200)<s.intensity(32,32,1200));
    }
    @Test public void fullExposureStartsDamageAfterFifteenSecondsAndArmourProtects() {
        int cold=0;for(int i=0;i<14;i++)cold=ColdModel.advance(cold,20,1,0,false,false,300);
        assertEquals(0,ColdModel.damage(cold,300,0,2),0);
        cold=ColdModel.advance(cold,20,1,0,false,false,300);
        assertEquals(2,ColdModel.damage(cold,300,0,2),0);
        assertEquals(1,ColdModel.damage(cold,300,2,2),0);
        assertEquals(0,ColdModel.damage(600,300,4,2),0);
        assertEquals(0,ColdModel.advance(0,20,1,4,false,false,300));
    }
    @Test public void ShelterHeatAndLeavingTheStormRecoverCold() {
        assertEquals(240,ColdModel.advance(300,20,1,0,true,false,300));
        assertEquals(240,ColdModel.advance(300,20,1,0,false,true,300));
        assertEquals(240,ColdModel.advance(300,20,0,0,false,false,300));
        assertEquals(0,ColdModel.advance(20,20,0,0,false,false,300));
    }
    @Test public void partialArmourDelaysColdAndExposureIsBounded() {
        assertTrue(ColdModel.advance(0,20,1,2,false,false,300)<ColdModel.advance(0,20,1,0,false,false,300));
        int cold=0;for(int i=0;i<10000;i++)cold=ColdModel.advance(cold,20,1,0,false,false,300);
        assertEquals(600,cold);
    }
    @Test public void savedStormsRetainAreasTimesAndSeedsAndExpire() {
        StormData data=new StormData();GridArea a=new GridArea(-2,-3,0,1);
        assertTrue(data.create(0,a,1000,1200,123456));
        StormData restored=new StormData();restored.readFromNBT(data.writeToNBT(new NBTTagCompound()));
        assertEquals(1,restored.regions().size());StormRegion r=restored.regions().get(0);
        assertEquals(-128,r.area.left());assertEquals(2200,r.expires);assertEquals(123456,r.seed);
        assertNotNull(restored.strongest(-64,0,1200));assertNull(restored.strongest(5000,0,1200));
        restored.prune(2200);assertTrue(restored.regions().isEmpty());
    }
    @Test public void removalOnlyTouchesIntersectingAreasAndCapacityIsBounded() {
        StormData data=new StormData();
        for(int i=0;i<32;i++)assertTrue(data.create(0,new GridArea(i*2,0,i*2,0),0,1200,i));
        assertFalse(data.create(0,new GridArea(100,0,100,0),0,1200,0));
        assertEquals(1,data.remove(new GridArea(0,0,0,0)));assertEquals(31,data.regions().size());
        assertTrue(data.create(0,new GridArea(100,0,100,0),0,1200,0));
    }
    @Test(expected=IllegalArgumentException.class) public void oversizedAreaCannotBeCreated(){new GridArea(0,0,16,0);}
    @Test(expected=IllegalArgumentException.class) public void integerOverflowCannotBypassAreaLimits(){new GridArea(Integer.MIN_VALUE,0,Integer.MAX_VALUE,0);}
    @Test(expected=IllegalArgumentException.class) public void nanCoordinatesAreRejected(){GridArea.cell(Double.NaN);}
    @Test(expected=IllegalArgumentException.class) public void worldBorderCannotBeBypassed(){new GridArea(468749,0,468749,0);}
}
