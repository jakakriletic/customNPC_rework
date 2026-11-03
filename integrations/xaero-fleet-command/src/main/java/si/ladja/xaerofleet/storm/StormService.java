package si.ladja.xaerofleet.storm;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import si.ladja.xaerofleet.network.FleetNetwork;
import si.ladja.xaerofleet.network.StormSnapshotPacket;

public final class StormService {
    private static final String COLD_KEY="xaerofleetWinterCold";
    private static final DamageSource COLD=new DamageSource("winterCold").setDamageBypassesArmor();
    private static final Set<UUID> subscribers=new HashSet<>();
    private int ticks;
    public static boolean canManage(EntityPlayerMP player) {
        MinecraftServer server=player.getServer();
        return StormConfig.enabled && (StormConfig.allowAllPlayers || player.canUseCommand(2,"winterstorm")
                || server!=null && server.isSinglePlayer() && player.getName().equals(server.getServerOwner()));
    }
    public static void subscribe(EntityPlayerMP player) { subscribers.add(player.getUniqueID()); send(player); }
    public static void send(EntityPlayerMP player) {
        FleetNetwork.CHANNEL.sendTo(new StormSnapshotPacket(player.dimension,player.world.getTotalWorldTime(),
                StormConfig.enabled ? StormData.get(player.world).regions() : java.util.Collections.emptyList(),
                canManage(player),player.getEntityData().getInteger(COLD_KEY),StormConfig.coldDelayTicks),player);
    }
    public static void changed(MinecraftServer server,int dimension) {
        if(server==null)return;
        for(EntityPlayerMP player:server.getPlayerList().getPlayers())if(player.dimension==dimension && subscribers.contains(player.getUniqueID()))send(player);
    }
    @SubscribeEvent public void worldTick(TickEvent.WorldTickEvent event) {
        if(event.phase==TickEvent.Phase.END && !event.world.isRemote && event.world.getTotalWorldTime()%40==0)
            StormData.get(event.world).prune(event.world.getTotalWorldTime());
    }
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || ++ticks%40!=0)return;
        MinecraftServer server=FMLCommonHandler.instance().getMinecraftServerInstance();
        if(server!=null)for(EntityPlayerMP p:server.getPlayerList().getPlayers())if(subscribers.contains(p.getUniqueID()))send(p);
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) { subscribers.remove(event.player.getUniqueID());event.player.getEntityData().removeTag(COLD_KEY); }
    public static void clear(){ subscribers.clear(); }
    public static int armour(EntityPlayer player) { int n=0; for(ItemStack item:player.getArmorInventoryList())if(!item.isEmpty())n++;return n; }
    public static boolean sheltered(EntityPlayer player) {
        BlockPos head=new BlockPos(player.posX,player.posY+player.getEyeHeight(),player.posZ);
        return !player.world.isBlockLoaded(head) || !player.world.canSeeSky(head);
    }
    private static boolean heat(EntityPlayer player) {
        BlockPos center=new BlockPos(player);
        BlockPos.MutableBlockPos sample=new BlockPos.MutableBlockPos();
        for(int x=-3;x<=3;x++)for(int y=-2;y<=2;y++)for(int z=-3;z<=3;z++) {
            sample.setPos(center.getX()+x,center.getY()+y,center.getZ()+z);
            if(!player.world.isBlockLoaded(sample))continue;
            Block b=player.world.getBlockState(sample).getBlock();
            if(b==Blocks.FIRE || b==Blocks.LAVA || b==Blocks.FLOWING_LAVA || b==Blocks.LIT_FURNACE || b==Blocks.MAGMA)return true;
        }
        return false;
    }
    @SubscribeEvent public void playerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || event.player.world.isRemote)return;
        EntityPlayer player=event.player;
        if(!StormConfig.enabled || player.isDead || player.capabilities.isCreativeMode || player.isSpectator()) {
            player.getEntityData().setInteger(COLD_KEY,0);return;
        }
        long now=player.world.getTotalWorldTime();
        StormRegion storm=StormData.get(player.world).strongest(player.posX,player.posZ,now);
        float severity=storm==null ? 0 : storm.intensity(player.posX,player.posZ,now);
        boolean covered=severity>0 && sheltered(player);
        if(storm!=null && !covered && StormConfig.windPush && now%10==0 && !player.isRiding()
                && !player.isElytraFlying() && Math.hypot(player.motionX,player.motionZ)<0.5) {
            double force=StormConfig.windStrength*severity*(player.isSneaking()?0.15:1);
            player.motionX+=storm.windX(now)*force;player.motionZ+=storm.windZ(now)*force;player.velocityChanged=true;
        }
        if(now%20!=0)return;
        int armour=armour(player); boolean warm=severity>0 && !covered && heat(player);
        int cold=ColdModel.advance(player.getEntityData().getInteger(COLD_KEY),20,severity,armour,covered,warm,StormConfig.coldDelayTicks);
        player.getEntityData().setInteger(COLD_KEY,cold);
        if(severity>0 && !covered && !warm && now%40==0) {
            float damage=ColdModel.damage(cold,StormConfig.coldDelayTicks,armour,StormConfig.coldDamage);
            if(damage>0)player.attackEntityFrom(COLD,damage);
        }
    }
}
