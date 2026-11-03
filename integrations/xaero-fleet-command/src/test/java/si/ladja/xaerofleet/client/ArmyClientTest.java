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
    @Test public void multipleClustersSelectAndCommandTheWholeArmyOnce(){
        ArmyClient.update(Arrays.asList(marker("North",true,"main"),marker("North",true,"straggler")));
        ArmyClient.selection().apply(ArmyClient.id("NORTH"),SelectionModel.Mode.REPLACE);
        assertTrue(ArmyClient.available());assertEquals(Collections.singletonList("North"),ArmyClient.names());
    }
    @Test public void revocationRemovesSelectionAndSavedGroupCannotRestoreIt(){
        ArmyClient.update(Arrays.asList(marker("North",true,"main")));
        ArmyClient.selection().apply(ArmyClient.id("North"),SelectionModel.Mode.REPLACE);ArmyClient.selection().saveGroup(1);
        ArmyClient.update(Arrays.asList(marker("North",false,"main")));
        assertTrue(ArmyClient.names().isEmpty());ArmyClient.selection().recallGroup(1,false,ArmyClient.commandable());assertTrue(ArmyClient.selection().isEmpty());
        ArmyClient.update(Collections.emptyList());assertFalse(ArmyClient.available());
    }
    @Test public void viewOnlyArmiesRemainVisibleWithoutCommandIdentity(){
        ArmyClient.update(Arrays.asList(marker("North",false,"main")));assertTrue(ArmyClient.available());assertTrue(ArmyClient.commandable().isEmpty());
    }
    @Test public void disconnectRemovesPreviousServerGroupsAndMarkers(){
        ArmyClient.update(Arrays.asList(marker("North",true,"main")));ArmyClient.selection().apply(ArmyClient.id("North"),SelectionModel.Mode.REPLACE);
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
