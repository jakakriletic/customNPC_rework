package si.ladja.xaerofleet.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;
import si.ladja.xaerofleet.client.*;
import si.ladja.xaerofleet.network.*;
import si.ladja.xaerofleet.storm.*;

/** Development-only integration harness. Built into a separate QA JAR, never into the release. */
@Mod(modid="winterstormqa",name="Winter QA",version="1",dependencies="required-after:xaerofleetcommand")
public final class WinterQa {
    private static final Logger LOG=LogManager.getLogger("Winter QA");
    private boolean launched,sent,firstShot,coldShot;
    private volatile boolean finish;
    private int clientTicks,serverTicks;
    private String screenshot;
    @Mod.EventHandler public void init(FMLInitializationEvent event) {if(Boolean.getBoolean("xaeroWinterQa"))MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent public void client(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(!launched && mc.currentScreen instanceof GuiMainMenu){
            launched=true;Display.setTitle("Winter Storm QA - Minecraft 1.12.2");
            mc.gameSettings.renderDistanceChunks=8;mc.gameSettings.limitFramerate=60;mc.gameSettings.pauseOnLostFocus=false;
            WorldSettings settings=new WorldSettings(4123,GameType.SURVIVAL,false,false,WorldType.FLAT).enableCommands();
            settings.setGeneratorOptions("3;minecraft:bedrock,3*minecraft:dirt,minecraft:grass;4;decoration");
            String name="Winter_QA_"+System.currentTimeMillis();mc.launchIntegratedServer(name,name,settings);
        }
        if(mc.world==null || mc.player==null)return;
        clientTicks++;
        if(!sent && clientTicks>50 && si.ladja.xaerofleet.XaeroFleetCommandMod.serverSupportsWinter()){
            sent=true;StormClient.request();FleetNetwork.CHANNEL.sendToServer(new StormControlPacket(0,0,new GridArea(0,0,3,3),12000));
            LOG.info("QA_SENT_CREATE_PACKET");
        }
        if(!firstShot && clientTicks>240 && !StormClient.regions().isEmpty()){
            firstShot=true;mc.gameSettings.hideGUI=true;screenshot="winter-blizzard.png";
            LOG.info("QA_CLIENT_STORM_RECEIVED count={}",StormClient.regions().size());
        }
        if(!coldShot && clientTicks>560){coldShot=true;mc.gameSettings.hideGUI=false;screenshot="winter-cold-hud.png";}
        if(Boolean.getBoolean("winterMapQa")) {
            if(clientTicks==280) {
                MapMode.setMode(2);mc.gameSettings.hideGUI=false;
                mc.displayGuiScreen(new xaero.map.gui.GuiMap(null,null,xaero.map.WorldMapSession.getCurrentSession().getMapProcessor(),mc.player));
            }
            if(clientTicks==320)screenshot="winter-map-layer.png";
            if(clientTicks==350){LOG.info("QA_MAP_LAYER_COMPLETE");mc.displayGuiScreen(null);mc.shutdown();}
        }
        if(finish){mc.shutdown();}
        if(clientTicks>1800){LOG.error("QA_TIMEOUT");mc.shutdown();}
    }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || screenshot==null)return;
        Minecraft mc=Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(mc.gameDir,screenshot,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        screenshot=null;
    }
    @SubscribeEvent public void player(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP))return;
        EntityPlayerMP p=(EntityPlayerMP)event.player;WorldServer w=p.getServerWorld();serverTicks++;
        if(serverTicks==20){
            p.setGameType(GameType.SURVIVAL);p.connection.setPlayerLocation(36,5,36,135,8);
            w.setWorldTime(6000);w.getGameRules().setOrCreateGameRule("doDaylightCycle","false");w.getGameRules().setOrCreateGameRule("doMobSpawning","false");
            for(int x=28;x<=44;x++)for(int z=28;z<=44;z++){w.setBlockState(new BlockPos(x,4,z),Blocks.GRASS.getDefaultState(),2);for(int y=5;y<14;y++)w.setBlockToAir(new BlockPos(x,y,z));}
            new WorldGenTrees(false).generate(w,w.rand,new BlockPos(23,5,25));new WorldGenTrees(false).generate(w,w.rand,new BlockPos(22,5,49));new WorldGenTrees(false).generate(w,w.rand,new BlockPos(49,5,22));
            LOG.info("QA_SETUP_COMPLETE owner={} allowed={}",p.getName(),StormService.canManage(p));
        }
        if(serverTicks>20){p.connection.setPlayerLocation(36,5,36,135,8);p.getFoodStats().setFoodLevel(6);}
        if(serverTicks==600){
            LOG.info("QA_COLD_RESULT health={} cold={}",p.getHealth(),p.getEntityData().getInteger("xaerofleetWinterCold"));
            if(p.getHealth()>=20)LOG.error("QA_COLD_FAILED");else LOG.info("QA_COLD_PASS");
            p.setHealth(20);p.setItemStackToSlot(EntityEquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
            p.setItemStackToSlot(EntityEquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));
            p.setItemStackToSlot(EntityEquipmentSlot.LEGS,new ItemStack(Items.LEATHER_LEGGINGS));p.setItemStackToSlot(EntityEquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS));
        }
        if(serverTicks==800){
            LOG.info("QA_ARMOUR_RESULT health={} armour={}",p.getHealth(),StormService.armour(p));
            if(p.getHealth()<20)LOG.error("QA_ARMOUR_FAILED");else LOG.info("QA_ARMOUR_PASS");
            for(EntityEquipmentSlot slot:new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD,EntityEquipmentSlot.CHEST,EntityEquipmentSlot.LEGS,EntityEquipmentSlot.FEET})p.setItemStackToSlot(slot,ItemStack.EMPTY);
            p.setHealth(20);p.getEntityData().setInteger("xaerofleetWinterCold",0);
            for(int x=28;x<=44;x++)for(int z=28;z<=44;z++)w.setBlockState(new BlockPos(x,9,z),Blocks.PLANKS.getDefaultState(),2);
        }
        if(serverTicks==1160){
            LOG.info("QA_SHELTER_RESULT health={} cold={} sheltered={}",p.getHealth(),p.getEntityData().getInteger("xaerofleetWinterCold"),StormService.sheltered(p));
            if(p.getHealth()<20 || p.getEntityData().getInteger("xaerofleetWinterCold")!=0)LOG.error("QA_SHELTER_FAILED");else LOG.info("QA_SHELTER_PASS");
            finish=true;LOG.info("QA_FINISHED");
        }
    }
}
