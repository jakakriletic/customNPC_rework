package noppes.npcs.rework.far;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M5-V.2: register NPC-jev po strezniskih svetovih (vzorec {@code CollisionWatcher}, M5.16).
 *
 * <p>Seznam se vzdrzuje ob {@code onEntityAdded} / {@code onEntityRemoved} vanilla poslusalca sveta,
 * zacetno stanje pa iz {@code world.loadedEntityList} ob {@code WorldEvent.Load}. Kadenca
 * {@link FarServer} tako ne isce po {@code loadedEntityList} (vse entitete) ampak samo po NPC-jih.
 * Poslusalec samo vodi seznam, igre ne spremeni; zato je prijavljen vedno, tudi z {@code RwFarView=0},
 * da {@code /rwfar 1} deluje med tekom. Klientski svetovi niso spremljani.
 */
public final class FarRegistry implements IWorldEventListener {
    static final Map<World, FarRegistry> WORLDS = new IdentityHashMap<World, FarRegistry>();

    private final List<EntityNPCInterface> npcs = new ArrayList<EntityNPCInterface>();
    private final Map<EntityNPCInterface, Integer> index = new IdentityHashMap<EntityNPCInterface, Integer>();

    private FarRegistry() {
    }

    /** Prijavi se na Forge vodilo; en objekt za dogodke sveta, nato en register na svet. */
    public static Object createEventHandler() {
        return new Events();
    }

    /** Dogodki sveta (javen razred: Forge ASM klicatelj potrebuje dostop). */
    public static final class Events {
        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            World world = event.getWorld();
            if (world == null || world.isRemote || WORLDS.containsKey(world)) {
                return;
            }
            FarRegistry r = new FarRegistry();
            for (int i = 0; i < world.loadedEntityList.size(); ++i) {
                r.onEntityAdded(world.loadedEntityList.get(i));
            }
            WORLDS.put(world, r);
            world.addEventListener(r);
            LogWriter.info("RWFAR svet " + world.provider.getDimension() + " spremljan, NPC-jev " + r.size());
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            if (event.getWorld() != null) {
                WORLDS.remove(event.getWorld());
            }
        }
    }

    @Nullable
    public static FarRegistry of(World world) {
        return WORLDS.get(world);
    }

    /** Pogled na seznam (ne spreminjaj; bere ga samo strezniska nit). */
    public List<EntityNPCInterface> npcs() {
        return this.npcs;
    }

    public int size() {
        return this.npcs.size();
    }

    @Override
    public void onEntityAdded(Entity entity) {
        if (!(entity instanceof EntityNPCInterface)) {
            return;
        }
        EntityNPCInterface npc = (EntityNPCInterface) entity;
        if (this.index.containsKey(npc)) {
            return;
        }
        this.index.put(npc, this.npcs.size());
        this.npcs.add(npc);
    }

    @Override
    public void onEntityRemoved(Entity entity) {
        if (!(entity instanceof EntityNPCInterface)) {
            return;
        }
        Integer at = this.index.remove(entity);
        if (at == null) {
            return;
        }
        int i = at;
        int last = this.npcs.size() - 1;
        EntityNPCInterface moved = this.npcs.remove(last);
        if (i != last) {
            this.npcs.set(i, moved);
            this.index.put(moved, i);
        }
    }

    @Override
    public void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
    }

    @Override
    public void notifyLightSet(BlockPos pos) {
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
    }

    @Override
    public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent sound, SoundCategory category,
            double x, double y, double z, float volume, float pitch) {
    }

    @Override
    public void playRecord(SoundEvent sound, BlockPos pos) {
    }

    @Override
    public void spawnParticle(int particleId, boolean ignoreRange, double x, double y, double z, double xSpeed,
            double ySpeed, double zSpeed, int... parameters) {
    }

    @Override
    public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y,
            double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
    }

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data) {
    }

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data) {
    }

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
    }
}
