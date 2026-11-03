package si.ladja.xaerofleet.client;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import si.ladja.xaerofleet.XaeroFleetCommandMod;
import si.ladja.xaerofleet.network.*;
import si.ladja.xaerofleet.storm.*;

/** Snow is rendered locally; positions, duration, cold and gust impulses are server-owned. */
public final class StormClient {
    private static final ResourceLocation SNOW=new ResourceLocation("minecraft","textures/environment/snow.png");
    private static List<StormRegion> regions=Collections.emptyList();
    private static World world;
    private static long serverTime,clientTime;
    private static int dimension,cold,threshold=300,ticks;
    private static boolean manager,received;
    private static float strength,previousStrength;
    private static BlizzardWind wind;
    public static List<StormRegion> regions(){return regions;}
    public static boolean canManage(){return received && manager;}
    public static boolean received(){return received;}
    public static long now(){return serverTime+(world==null?0:Math.max(0,world.getTotalWorldTime()-clientTime));}
    public static void request(){if(Minecraft.getMinecraft().player!=null && XaeroFleetCommandMod.serverSupportsWinter())FleetNetwork.CHANNEL.sendToServer(new RequestStormsPacket());}
    public static void receive(StormSnapshotPacket packet) {
        Minecraft.getMinecraft().addScheduledTask(()->{
            Minecraft mc=Minecraft.getMinecraft();
            if(mc.world==null || mc.player==null || mc.player.dimension!=packet.dimension)return;
            if(world!=mc.world){clear();world=mc.world;}
            dimension=packet.dimension;serverTime=packet.now;clientTime=mc.world.getTotalWorldTime();
            regions=packet.regions;manager=packet.canManage;cold=packet.cold;threshold=packet.coldThreshold;received=true;
        });
    }
    public static void feedback(StormFeedbackPacket packet) {
        Minecraft.getMinecraft().addScheduledTask(()->{
            MapToast.show(packet.text,packet.success?MapToast.GOOD:MapToast.BAD);
            if(Minecraft.getMinecraft().player!=null)Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                    (packet.success?"\u00a7b":"\u00a7c")+packet.text));
        });
    }
    private static void clear() {
        regions=Collections.emptyList();received=manager=false;strength=previousStrength=0;cold=ticks=0;
        if(wind!=null){Minecraft.getMinecraft().getSoundHandler().stopSound(wind);wind=null;}
    }
    @SubscribeEvent public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(()->{clear();world=null;});
    }
    private static StormRegion strongest(Entity camera) {
        if(camera==null || world==null || camera.dimension!=dimension)return null;
        StormRegion best=null;float power=0;long now=now();
        for(StormRegion r:regions){float n=r.intensity(camera.posX,camera.posZ,now);if(n>power){power=n;best=r;}}
        return best;
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world!=world){clear();world=mc.world;}
        if(mc.world==null || mc.player==null || mc.isGamePaused())return;
        if(++ticks%100==1)request();
        previousStrength=strength;
        Entity camera=mc.getRenderViewEntity();StormRegion region=strongest(camera);
        float target=region==null?0:region.intensity(camera.posX,camera.posZ,now());
        strength+=(target-strength)*0.12f;
        if(strength>0.02f && (wind==null || wind.isDonePlaying())) {
            wind=new BlizzardWind();mc.getSoundHandler().playSound(wind);
        }
    }
    private static float power(float partial){return previousStrength+(strength-previousStrength)*partial;}
    private static boolean fogUsable(Entity camera) {
        return camera!=null && world!=null && !camera.isInsideOfMaterial(Material.WATER) && !camera.isInsideOfMaterial(Material.LAVA)
                && (Minecraft.getMinecraft().player==null || !Minecraft.getMinecraft().player.isPotionActive(net.minecraft.init.MobEffects.BLINDNESS));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void fogColor(EntityViewRenderEvent.FogColors event) {
        if(!fogUsable(event.getEntity()))return;
        float n=power((float)event.getRenderPartialTicks());if(n<0.005f)return;
        float light=0.42f+world.getSunBrightness((float)event.getRenderPartialTicks())*0.53f;
        event.setRed(event.getRed()*(1-n)+light*0.90f*n);
        event.setGreen(event.getGreen()*(1-n)+light*0.95f*n);
        event.setBlue(event.getBlue()*(1-n)+light*n);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void fog(EntityViewRenderEvent.RenderFogEvent event) {
        if(!fogUsable(event.getEntity()))return;
        float n=power((float)event.getRenderPartialTicks());if(n<0.005f)return;
        Entity camera=event.getEntity();
        boolean shelter=!world.canSeeSky(new BlockPos(camera.posX,camera.posY+camera.getEyeHeight(),camera.posZ));
        float end=shelter?38:18;
        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        GlStateManager.setFogStart(event.getFarPlaneDistance()*0.65f*(1-n)+2*n);
        GlStateManager.setFogEnd(event.getFarPlaneDistance()*(1-n)+Math.min(end,event.getFarPlaneDistance())*n);
    }
    @SubscribeEvent public void snow(RenderWorldLastEvent event) {
        Minecraft mc=Minecraft.getMinecraft();Entity camera=mc.getRenderViewEntity();
        float partial=event.getPartialTicks(),power=power(partial);StormRegion region=strongest(camera);
        if(world==null || camera==null || region==null || power<0.01f || camera.isInsideOfMaterial(Material.WATER) || camera.isInsideOfMaterial(Material.LAVA))return;
        double camX=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*partial;
        double camY=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*partial;
        double camZ=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*partial;
        long now=now();double time=now+partial,wx=region.windX(now),wz=region.windZ(now);
        SnowState state=new SnowState();GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-camX,-camY,-camZ);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(SNOW);
            GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
            GlStateManager.disableCull();GlStateManager.disableLighting();GlStateManager.disableAlpha();GlStateManager.enableDepth();GlStateManager.depthMask(false);
            BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
            float brightness=0.45f+world.getSunBrightness(partial)*0.5f;
            int cx=(int)Math.floor(camX/2)*2,cz=(int)Math.floor(camZ/2)*2;
            BlockPos.MutableBlockPos sample=new BlockPos.MutableBlockPos();
            for(int x=cx-26;x<=cx+26;x+=2)for(int z=cz-26;z<=cz+26;z+=2) {
                double distance=Math.hypot(x+0.5-camX,z+0.5-camZ);if(distance>27)continue;
                float local=region.area.edge(x+0.5,z+0.5);if(local<=0)continue;
                sample.setPos(x,(int)camY,z);if(!world.isBlockLoaded(sample))continue;
                int floor=world.getPrecipitationHeight(sample).getY();
                double bottom=Math.max(floor,camY-12),top=camY+15;if(top<=bottom)continue;
                int hash=x*73428767^z*912931;
                double jitter=((hash>>>8)&255)/255.0,px=x+jitter*1.5,pz=z+((hash>>>16)&255)/170.0;
                double dx=px-camX,dz=pz-camZ,length=Math.max(0.01,Math.hypot(dx,dz));
                double sideX=-dz/length*0.95,sideZ=dx/length*0.95;
                double shiftX=-wx*(top-bottom)*0.75,shiftZ=-wz*(top-bottom)*0.75;
                double scroll=time*(0.032+jitter*0.016)+jitter*8;
                float alpha=power*local*(float)(0.64*(1-distance*distance/(28*28.0)));
                double v0=bottom/5+scroll,v1=top/5+scroll;
                vertex(b,px-sideX,bottom,pz-sideZ,0,v0,brightness,alpha);
                vertex(b,px+sideX,bottom,pz+sideZ,1,v0,brightness,alpha);
                vertex(b,px+sideX+shiftX,top,pz+sideZ+shiftZ,1,v1,brightness,alpha);
                vertex(b,px-sideX+shiftX,top,pz-sideZ+shiftZ,0,v1,brightness,alpha);
            }
            Tessellator.getInstance().draw();
        } finally {GlStateManager.popMatrix();state.restore();}
    }
    private static void vertex(BufferBuilder b,double x,double y,double z,double u,double v,float light,float alpha) {
        b.pos(x,y,z).tex(u,v).color(light*0.94f,light*0.98f,light,alpha).endVertex();
    }
    @SubscribeEvent public void hud(RenderGameOverlayEvent.Post event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL || mc.player==null || mc.gameSettings.hideGUI || cold==0)return;
        int x=8,y=event.getResolution().getScaledHeight()-49;
        float percent=Math.min(1,cold/(float)threshold);
        Gui.drawRect(x,y,x+102,y+5,0x88202B39);Gui.drawRect(x+1,y+1,x+1+(int)(percent*100),y+4,0xDDADDFF5);
        mc.fontRenderer.drawStringWithShadow((cold>=threshold?"Freezing":"Cold")+" | Armour "+StormService.armour(mc.player)+"/4",x,y-10,0xD9F1FF);
    }
    private static final class BlizzardWind extends MovingSound {
        BlizzardWind(){super(SoundEvents.ITEM_ELYTRA_FLYING,SoundCategory.WEATHER);repeat=true;repeatDelay=0;attenuationType=ISound.AttenuationType.NONE;volume=0.01f;}
        @Override public void update() {
            Minecraft mc=Minecraft.getMinecraft();
            if(mc.player==null || world==null || strength<0.005f){donePlaying=true;volume=0;return;}
            StormRegion r=strongest(mc.player);float gust=r==null?1:r.gust(now());
            float shelter=StormService.sheltered(mc.player)?0.20f:1;
            volume=strength*shelter*0.9f;pitch=0.65f+gust*0.18f;
            xPosF=(float)mc.player.posX;yPosF=(float)mc.player.posY;zPosF=(float)mc.player.posZ;
        }
    }
    static final class SnowState {
        final int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),binding;
        final boolean texture,lightmap,blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),
                lighting=GL11.glIsEnabled(GL11.GL_LIGHTING),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int src=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB),
                srcA=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA),dstA=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        SnowState(){
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);lightmap=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);binding=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        void restore(){
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);GlStateManager.bindTexture(binding);
            if(texture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);if(lightmap)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(active);GlStateManager.tryBlendFuncSeparate(src,dst,srcA,dstA);
            if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
            if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();
            if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();GlStateManager.depthMask(depthMask);GlStateManager.color(1,1,1,1);
        }
    }
}
