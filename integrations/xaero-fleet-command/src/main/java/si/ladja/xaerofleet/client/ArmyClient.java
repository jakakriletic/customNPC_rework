package si.ladja.xaerofleet.client;

import java.nio.charset.StandardCharsets;
import java.util.*;
import si.ladja.xaerofleet.network.ArmyMarker;

/** Army identity is its case-insensitive name, never a cluster or a captain UUID. */
public final class ArmyClient {
    private static List<ArmyMarker> markers=Collections.emptyList();
    private static SelectionModel selection=new SelectionModel(16);
    private static long received;
    private ArmyClient(){}
    public static UUID id(String name){return UUID.nameUUIDFromBytes(("cnpc-army:"+name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));}
    public static List<ArmyMarker> markers(){return markers;}
    public static SelectionModel selection(){return selection;}
    public static Set<UUID> commandable(){
        Set<UUID> out=new LinkedHashSet<>();for(ArmyMarker m:markers)if(m.commandable)out.add(id(m.name));return out;
    }
    public static List<String> names(){
        List<String> out=new ArrayList<>();Set<UUID> allowed=commandable();
        for(UUID id:selection.ids())if(allowed.contains(id))for(ArmyMarker m:markers)if(id.equals(id(m.name))){out.add(m.name);break;}
        return out;
    }
    public static void update(List<ArmyMarker> values){
        markers=Collections.unmodifiableList(new ArrayList<>(values));received=System.currentTimeMillis();
        selection.retain(commandable());
    }
    public static boolean available(){return !markers.isEmpty() && System.currentTimeMillis()-received<5500;}
    public static void clear(){markers=Collections.emptyList();selection=new SelectionModel(16);received=0;}
}
