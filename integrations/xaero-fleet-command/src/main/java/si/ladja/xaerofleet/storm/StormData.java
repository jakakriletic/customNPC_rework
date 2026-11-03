package si.ladja.xaerofleet.storm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import si.ladja.xaerofleet.XaeroFleetCommandMod;

public final class StormData extends WorldSavedData {
    private static final String KEY="xaerofleet_winter_storms";
    private final List<StormRegion> regions=new ArrayList<>();
    private int nextId=1;
    public StormData(){ super(KEY); }
    public StormData(String name){ super(name); }
    public static StormData get(World world) {
        StormData data=(StormData)world.getPerWorldStorage().getOrLoadData(StormData.class,KEY);
        if(data==null){ data=new StormData(); world.getPerWorldStorage().setData(KEY,data); }
        return data;
    }
    public List<StormRegion> regions(){ return Collections.unmodifiableList(regions); }
    public StormRegion strongest(double x,double z,long now) {
        StormRegion strongest=null; float intensity=0;
        for(StormRegion region:regions){ float candidate=region.intensity(x,z,now); if(candidate>intensity){intensity=candidate; strongest=region;} }
        return strongest;
    }
    public boolean create(int dimension,GridArea area,long now,int duration,long seed) {
        prune(now);
        if(regions.size()>=StormRegion.MAX_ACTIVE || duration<200 || duration>StormRegion.MAX_DURATION || nextId==Integer.MAX_VALUE)return false;
        regions.add(new StormRegion(nextId++,dimension,area,now,now+duration,seed)); markDirty(); return true;
    }
    public int remove(GridArea area) {
        int count=regions.size(); regions.removeIf(region->region.area.overlaps(area));
        int removed=count-regions.size(); if(removed>0)markDirty(); return removed;
    }
    public void prune(long now) {
        boolean changed=regions.removeIf(region->region.expires<=now);
        if(changed)markDirty();
    }
    @Override public void readFromNBT(NBTTagCompound root) {
        regions.clear(); nextId=Math.max(1,root.getInteger("nextId"));
        NBTTagList list=root.getTagList("storms",10);
        for(int i=0;i<Math.min(list.tagCount(),StormRegion.MAX_ACTIVE);i++) {
            NBTTagCompound tag=list.getCompoundTagAt(i);
            try {
                int id=tag.getInteger("id");
                regions.add(new StormRegion(id,tag.getInteger("dimension"),new GridArea(tag.getInteger("x0"),tag.getInteger("z0"),tag.getInteger("x1"),tag.getInteger("z1")),tag.getLong("start"),tag.getLong("end"),tag.getLong("seed")));
                nextId=Math.max(nextId,id==Integer.MAX_VALUE ? Integer.MAX_VALUE : id+1);
            } catch(IllegalArgumentException e){ XaeroFleetCommandMod.LOG.warn("Ignored invalid saved winter storm: {}",e.getMessage()); }
        }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound root) {
        root.setInteger("nextId",nextId); NBTTagList list=new NBTTagList();
        for(StormRegion r:regions){ NBTTagCompound t=new NBTTagCompound();
            t.setInteger("id",r.id);t.setInteger("dimension",r.dimension);
            t.setInteger("x0",r.area.minX);t.setInteger("z0",r.area.minZ);t.setInteger("x1",r.area.maxX);t.setInteger("z1",r.area.maxZ);
            t.setLong("start",r.started);t.setLong("end",r.expires);t.setLong("seed",r.seed);list.appendTag(t);
        }
        root.setTag("storms",list); return root;
    }
}
