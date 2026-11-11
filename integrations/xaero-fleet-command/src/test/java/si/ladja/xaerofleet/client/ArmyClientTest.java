package si.ladja.xaerofleet.client;

import java.io.File;
import java.util.*;
import org.junit.After;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import si.ladja.xaerofleet.network.ArmyMarker;
import static org.junit.Assert.*;

public class ArmyClientTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    @After public void reset(){ArmyClient.clear();MapMode.useFile(null);}
    private ArmyMarker marker(String name,boolean command,String kind){return new ArmyMarker(name,"Owner",kind,"NONE","","","",0,0,64,0,8,16,0,0,100,command);}
    private ArmyMarker at(String name,int x,int z){return new ArmyMarker(name,"Owner","group","NONE","","","",0,x,64,z,8,16,0,0,100,true);}
    @Test public void oneFlagIsSelectedAloneAndBothMakeTheWholeArmy(){
        ArmyClient.update(Arrays.asList(at("North",0,0),at("North",200,100)));
        ArmyMarker first=ArmyClient.markers().get(0),second=ArmyClient.markers().get(1);
        ArmyClient.selection().apply(ArmyClient.flagId(first),SelectionModel.Mode.REPLACE);
        assertTrue(ArmyClient.available());assertEquals(Collections.singletonList("North"),ArmyClient.names());
        assertEquals(1,ArmyClient.selectedFlags().size());assertFalse(ArmyClient.wholeArmySelected("north"));
        ArmyClient.selection().apply(ArmyClient.armyFlags("North"),SelectionModel.Mode.REPLACE);
        assertEquals(2,ArmyClient.selectedFlags().size());assertTrue(ArmyClient.wholeArmySelected("North"));
        assertNotEquals(ArmyClient.flagId(first),ArmyClient.flagId(second));
    }
    @Test public void aSelectedFlagFollowsItsArmyAndItsSplitButNotAFarFlag(){
        ArmyClient.update(Arrays.asList(at("North",0,0),at("North",200,100)));
        UUID id=ArmyClient.flagId(ArmyClient.markers().get(1));ArmyClient.selection().apply(id,SelectionModel.Mode.REPLACE);
        ArmyClient.update(Arrays.asList(at("North",3,0),at("North",210,104)));
        assertTrue(ArmyClient.selection().contains(id));assertEquals(210,ArmyClient.flag(id).x);
        // After the server split the selected flag off, the new army keeps the selection.
        ArmyClient.update(Arrays.asList(at("North",6,0),at("North-2",215,108)));
        assertTrue(ArmyClient.selection().contains(id));assertEquals("North-2",ArmyClient.flag(id).name);
        ArmyClient.update(Arrays.asList(at("North",6,0),at("North-2",400,400)));
        assertFalse(ArmyClient.selection().contains(id));
    }
    @Test public void revocationRemovesSelectionAndSavedGroupCannotRestoreIt(){
        ArmyClient.update(Arrays.asList(marker("North",true,"main")));
        ArmyClient.selection().apply(ArmyClient.armyFlags("North"),SelectionModel.Mode.REPLACE);ArmyClient.selection().saveGroup(1);
        ArmyClient.update(Arrays.asList(marker("North",false,"main")));
        assertTrue(ArmyClient.names().isEmpty());ArmyClient.selection().recallGroup(1,false,ArmyClient.commandable());assertTrue(ArmyClient.selection().isEmpty());
        ArmyClient.update(Collections.emptyList());assertFalse(ArmyClient.available());
    }
    @Test public void viewOnlyArmiesRemainVisibleWithoutCommandIdentity(){
        ArmyClient.update(Arrays.asList(marker("North",false,"main")));assertTrue(ArmyClient.available());assertTrue(ArmyClient.commandable().isEmpty());
    }
    @Test public void disconnectRemovesPreviousServerGroupsAndMarkers(){
        ArmyClient.update(Arrays.asList(marker("North",true,"main")));ArmyClient.selection().apply(ArmyClient.armyFlags("North"),SelectionModel.Mode.REPLACE);
        ArmyClient.selection().saveGroup(1);ArmyClient.clear();assertFalse(ArmyClient.available());assertFalse(ArmyClient.selection().hasGroup(1));
    }
    @Test public void allFourMapModesPersistAndDoNotActivateAnotherLayer() {
        File file=new File(tmp.getRoot(),"mode.properties");
        for(int mode=0;mode<4;mode++){
            MapMode.useFile(file);MapMode.setMode(mode);MapMode.useFile(file);
            assertEquals(mode,MapMode.current());assertEquals(mode==1,MapMode.isShipMap());assertEquals(mode==2,MapMode.isStormMap());assertEquals(mode==3,MapMode.isArmyMap());
        }
    }
    @Test public void fourthTabHitBoxesDoNotOverlapExistingTabs(){
        MapModeSwitch sw=MapModeSwitch.layout(640,50,50,70,40,4);assertEquals(4,sw.count());
        for(int i=0;i<4;i++){int[] h=sw.half(i);assertEquals(i,sw.at((h[0]+h[2])/2.0,(h[1]+h[3])/2.0));}
        assertEquals(MapModeSwitch.NONE,sw.at(sw.right(),10));
    }
}
