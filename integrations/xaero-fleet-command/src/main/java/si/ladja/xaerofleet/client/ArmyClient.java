package si.ladja.xaerofleet.client;

import java.nio.charset.StandardCharsets;
import java.util.*;
import si.ladja.xaerofleet.network.ArmyMarker;

/**
 * Army identity on the server is its case-insensitive name. Since v0.1.13 the map selects single flags (like ships):
 * every visible flag gets a client-side id that follows it between snapshots (nearest flag of the same army, or of
 * an army split off from it, within {@link #TRACK} blocks). Moving some flags of an army splits them off on the
 * server (CNPC M11.11d); moving all of them is an ordinary army move.
 */
public final class ArmyClient {
    /** A flag moves at most a few blocks per one-second snapshot; distinct flags of one army are 48+ apart. */
    public static final double TRACK = 24;
    private static List<ArmyMarker> markers=Collections.emptyList();
    private static SelectionModel selection=new SelectionModel(16);
    private static Map<UUID,ArmyMarker> flags=new LinkedHashMap<>();
    private static long received;
    private ArmyClient(){}
    public static UUID id(String name){return UUID.nameUUIDFromBytes(("cnpc-army:"+name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));}
    public static List<ArmyMarker> markers(){return markers;}
    public static SelectionModel selection(){return selection;}

    /** The id of this visible flag. */
    public static UUID flagId(ArmyMarker m){
        for(Map.Entry<UUID,ArmyMarker> e:flags.entrySet())if(e.getValue()==m)return e.getKey();
        UUID id=UUID.randomUUID();flags.put(id,m);return id;
    }
    public static ArmyMarker flag(UUID id){return flags.get(id);}
    public static Set<UUID> commandable(){
        Set<UUID> out=new LinkedHashSet<>();for(ArmyMarker m:markers)if(m.commandable)out.add(flagId(m));return out;
    }
    /** Every commandable flag of the same army. */
    public static Set<UUID> armyFlags(String name){
        Set<UUID> out=new LinkedHashSet<>();
        for(ArmyMarker m:markers)if(m.commandable && m.name.equalsIgnoreCase(name))out.add(flagId(m));
        return out;
    }
    /** Selected commandable flags. */
    public static List<ArmyMarker> selectedFlags(){
        List<ArmyMarker> out=new ArrayList<>();
        for(UUID id:selection.ids()){ArmyMarker m=flags.get(id);if(m!=null && m.commandable)out.add(m);}
        return out;
    }
    /** Army names of the selected flags, each once (stop and stance act on whole armies). */
    public static List<String> names(){
        List<String> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(ArmyMarker m:selectedFlags())if(seen.add(m.name.toLowerCase(Locale.ROOT)))out.add(m.name);
        return out;
    }
    /** True when every commandable flag of this army is selected. */
    public static boolean wholeArmySelected(String name){
        for(UUID id:armyFlags(name))if(!selection.contains(id))return false;
        return true;
    }
    public static void update(List<ArmyMarker> values){
        markers=Collections.unmodifiableList(new ArrayList<>(values));received=System.currentTimeMillis();
        flags=track(flags,markers);
        selection.retain(commandable());
    }
    /** Greedy nearest matching: same army first, then an army split off from it ({@code name-N}). */
    static Map<UUID,ArmyMarker> track(Map<UUID,ArmyMarker> old,List<ArmyMarker> now){
        List<Object[]> pairs=new ArrayList<>();
        for(Map.Entry<UUID,ArmyMarker> e:old.entrySet())for(ArmyMarker m:now){
            ArmyMarker f=e.getValue();
            boolean same=m.name.equalsIgnoreCase(f.name);
            boolean child=m.name.toLowerCase(Locale.ROOT).startsWith(f.name.toLowerCase(Locale.ROOT)+"-");
            double d=Math.hypot(m.x-f.x,m.z-f.z);
            if(m.dimension==f.dimension && (same || child) && d<=TRACK)pairs.add(new Object[]{e.getKey(),m,same?0:1,d});
        }
        pairs.sort((a,b)->(Integer)a[2]!=(Integer)b[2]?Integer.compare((Integer)a[2],(Integer)b[2]):Double.compare((Double)a[3],(Double)b[3]));
        Map<UUID,ArmyMarker> out=new LinkedHashMap<>();Set<ArmyMarker> used=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Object[] p:pairs){
            UUID id=(UUID)p[0];ArmyMarker m=(ArmyMarker)p[1];
            if(out.containsKey(id) || used.contains(m))continue;
            out.put(id,m);used.add(m);
        }
        for(ArmyMarker m:now)if(!used.contains(m))out.put(UUID.randomUUID(),m);
        return out;
    }
    public static boolean available(){return !markers.isEmpty() && System.currentTimeMillis()-received<5500;}
    public static void clear(){markers=Collections.emptyList();selection=new SelectionModel(16);flags=new LinkedHashMap<>();received=0;}
}
