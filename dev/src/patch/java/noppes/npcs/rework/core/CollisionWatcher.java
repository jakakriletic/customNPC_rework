package noppes.npcs.rework.core;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.LogWriter;

/**
 * M5.16: steje entitete, katerih razred lahko prispeva trkalno skatlo
 * ({@link CollisionClasses#contributes}), po svetovih.
 *
 * <p>Steje se ob {@code onEntityAdded} / {@code onEntityRemoved} vanilla poslusalca sveta, zacetno
 * stanje pa iz {@code world.loadedEntityList} ob {@code WorldEvent.Load}. Svet, ki ga poslusalec ne
 * spremlja, {@link CollisionGate} nikoli ne preskoci, zato je napaka v registraciji varna.
 *
 * <p>Vse ostale metode {@code IWorldEventListener} so prazne: paket ne spremlja blokov ne zvokov.
 */
public final class CollisionWatcher implements IWorldEventListener {
    private final int[] count;

    private CollisionWatcher(int[] count) {
        this.count = count;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world == null || CollisionGate.COUNTS.containsKey(world)) {
            return;
        }
        int[] count = new int[1];
        for (int i = 0; i < world.loadedEntityList.size(); ++i) {
            if (CollisionClasses.contributes(world.loadedEntityList.get(i).getClass())) {
                ++count[0];
            }
        }
        CollisionGate.COUNTS.put(world, count);
        world.addEventListener(new CollisionWatcher(count));
        LogWriter.info("RWMOVE svet " + world.provider.getDimension() + " spremljan, prispevajocih entitet "
                + count[0] + " od " + world.loadedEntityList.size());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world != null) {
            CollisionGate.COUNTS.remove(world);
            CollisionGate.forgetCache();
        }
    }

    @Override
    public void onEntityAdded(Entity entity) {
        if (CollisionClasses.contributes(entity.getClass())) {
            ++this.count[0];
        }
    }

    @Override
    public void onEntityRemoved(Entity entity) {
        if (CollisionClasses.contributes(entity.getClass())) {
            --this.count[0];
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

    /** Prijavi se na Forge vodilo; en sam objekt za dogodke sveta, nato en poslusalec na svet. */
    public static CollisionWatcher createEventHandler() {
        return new CollisionWatcher(new int[1]);
    }

    /** Samo za {@code /rwmove}: koliko svetov je spremljanih in koliko prispevajocih entitet skupaj. */
    public static String summary(World world) {
        return "svetov=" + CollisionGate.watchedWorlds() + " prispevajocih=" + CollisionGate.contributorsAll()
                + " vTemSvetu=" + CollisionGate.contributors(world) + " razredovVPomnilniku="
                + CollisionClasses.cachedClasses();
    }
}
