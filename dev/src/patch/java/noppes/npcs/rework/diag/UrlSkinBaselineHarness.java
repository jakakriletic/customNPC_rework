package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.*;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ImageDownloadAlt;
import noppes.npcs.client.renderer.ImageBufferDownloadAlt;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Explicit controller-only M12.1. Renderer creates the real original downloader. A
 * forwarding ITextureObject observes lazy upload without changing its fields, scheduling,
 * HTTP, decoding or GL calls. No readback, image IO or stack sampling in timed windows.
 * Synthetic client-only sky grid isolates URL/render load from server AI. */
public final class UrlSkinBaselineHarness {
    private static final long SECOND = 1000000000L;
    private static final String[] MODES = {"normal", "slow", "legacy", "hd", "alpha", "rectangle", "oversized",
        "bigbody", "missing", "invalid", "headers", "body", "trickle", "redirect", "refused"};
    private final Properties job;
    private final File out;
    private final int n;
    private final boolean faults, same;
    private final List<EntityCustomNpc> npcs = new ArrayList<>();
    private final Set<Entity> actors = Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
    private final Map<ResourceLocation, Observed> textures = new LinkedHashMap<>();
    private final JsonArray checks = new JsonArray(), rows = new JsonArray(), samples = new JsonArray();
    private final long[] frameTimes = new long[1 << 18], uploadTimes = new long[1 << 18];
    private final int[] uploadCounts = new int[1 << 18];
    private final long[] uploadBytes = new long[1 << 18];
    private final Set<Integer> rendered = new HashSet<>();
    private String phase = "MENU", error = "";
    private long since = System.nanoTime(), introduced, previousFrame, nextSample, lastGpu;
    private long frameUploadNs, frameUploadBytes, gc0, gcMs0, gcEnd, gcMsEnd;
    private int frameUploads, frames, minRendered = Integer.MAX_VALUE, badFrames, peakThreads;
    private int steadyAt, steadyFrames;
    private boolean finished, ok = true, creating;
    private volatile boolean setupDone;
    private volatile String serverError;
    private Field image, uploaded, thread, parser, pixels;

    private UrlSkinBaselineHarness(Properties p) throws Exception {
        job = p; out = new File(p.getProperty("out"));
        faults = "faults".equals(p.getProperty("cell")); same = "same500".equals(p.getProperty("cell"));
        n = faults ? MODES.length : Integer.parseInt(p.getProperty("n"));
        image = field(ImageDownloadAlt.class, "bufferedImage"); uploaded = field(ImageDownloadAlt.class, "textureUploaded");
        thread = field(ImageDownloadAlt.class, "imageThread"); parser = field(ImageDownloadAlt.class, "imageBuffer");
        pixels = field(ImageBufferDownloadAlt.class, "imageData");
    }
    private static Field field(Class<?> c, String s) throws Exception { Field f = c.getDeclaredField(s); f.setAccessible(true); return f; }
    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(r); }
        if (!"1".equals(p.getProperty("urlSkinBaseline"))) { return false; }
        for (String key : new String[]{"out","world","base","fixture","cell","n","restart"}) {
            if (!p.containsKey(key)) { throw new IllegalArgumentException("Missing " + key); }
        }
        MinecraftForge.EVENT_BUS.register(new UrlSkinBaselineHarness(p)); return true;
    }
    private void check(String key, boolean pass, String detail) {
        JsonObject c = new JsonObject(); c.addProperty("key", key); c.addProperty("ok", pass); c.addProperty("detail", detail);
        checks.add(c); ok &= pass; System.out.println("M121 CHECK " + key + " ok=" + pass + " " + detail);
    }
    private void enter(String p) { phase = p; since = System.nanoTime(); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent ev) {
        if (finished || ev.phase != TickEvent.Phase.END) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (serverError != null) { throw new IllegalStateException(serverError); }
            if (System.nanoTime() - since > 180 * SECOND) { throw new IllegalStateException("Timeout " + phase + " " + conditionDetail(mc)); }
            if (phase.equals("MENU") && mc.currentScreen instanceof GuiMainMenu) {
                mc.gameSettings.pauseOnLostFocus = false; mc.gameSettings.enableVsync = false;
                mc.gameSettings.limitFramerate = 260; mc.gameSettings.renderDistanceChunks = 10;
                mc.gameSettings.fovSetting = 70; mc.gameSettings.viewBobbing = false;
                mc.gameSettings.guiScale = 2; mc.gameSettings.hideGUI = false;
                mc.gameSettings.thirdPersonView = 0;
                mc.resize(1280,720);
                // Explicit scene conditions, never saved to the owner's config.
                CustomNpcs.RwFarView = 0; CustomNpcs.RwNpcInstancing = 0;
                CustomNpcs.RwLodStart = 0; CustomNpcs.RwHeadwearSkip = 0;
                enter("JOIN");
                mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"),
                    new WorldSettings(HarnessJob.seedOf("customnpc-rework"), GameType.CREATIVE,false,false,WorldType.FLAT).enableCommands());
            } else if (phase.equals("JOIN") && mc.world != null && mc.player != null && mc.currentScreen == null) {
                mc.getIntegratedServer().addScheduledTask(() -> {
                    try {
                        for (String c : new String[]{"gamerule doMobSpawning false","gamerule doDaylightCycle false",
                                "weather clear 100000","time set 6000","tp " + mc.player.getName() + " 0.0 70.0 -32.0 0 0"}) {
                            if (mc.getIntegratedServer().getCommandManager().executeCommand(mc.getIntegratedServer(),c) < 1) { throw new IllegalStateException(c); }
                        }
                        net.minecraft.entity.player.EntityPlayerMP player=mc.getIntegratedServer().getPlayerList().getPlayerByUsername(mc.player.getName());
                        player.capabilities.isFlying=true; player.sendPlayerAbilities();
                        setupDone = true;
                    } catch (Exception e) { serverError = e.toString(); }
                });
                enter("SCENE");
            // Integer X/Z in /tp are block-centred (+0.5). Wait for the exact
            // server target AND flight ability to arrive before the 30s warmup;
            // a client-only snap can otherwise be undone by a late teleport packet.
            } else if (phase.equals("SCENE") && setupDone && conditions(mc) && mc.player.capabilities.isFlying) {
                mc.player.capabilities.isFlying = true;
                mc.player.sendPlayerAbilities();
                mc.player.setPositionAndRotation(0,70,-32,0,0);
                mc.player.motionX=mc.player.motionY=mc.player.motionZ=0;
                int cols = faults ? 7 : 25, height = (n+cols-1)/cols;
                for (int i=0; i<n; i++) {
                    EntityCustomNpc npc = new EntityCustomNpc(mc.world);
                    npc.display.setName("M121_" + i); npc.display.setShowName(1);
                    npc.setNoAI(true); npc.setNoGravity(true);
                    npc.setPosition((i%cols-(cols-1)*0.5)*1.3, 71.62+(i/cols-(height-1)*0.5)*2.4-0.9,12);
                    npc.rotationYaw = npc.prevRotationYaw = npc.renderYawOffset = npc.prevRenderYawOffset = 180;
                    npc.textureLocation = DefaultPlayerSkin.getDefaultSkinLegacy();
                    mc.world.addEntityToWorld(-1000-i,npc); npcs.add(npc); actors.add(npc);
                }
                enter("WARM");
            } else if (phase.equals("WARM") && System.nanoTime()-since > 30*SECOND) {
                check("warm-visible", rendered.size()==n, "actual living Post="+rendered.size()+" expected="+n);
                check("conditions", conditions(mc), conditionDetail(mc));
                if (!ok) { throw new IllegalStateException("Scene prerequisites"); }
                enter("INTRODUCE");
            }
        } catch (Exception | LinkageError e) { e.printStackTrace(); error=e.toString(); finish(mc); }
    }
    private boolean conditions(Minecraft mc) {
        return mc.displayWidth==1280 && mc.displayHeight==720 && mc.gameSettings.renderDistanceChunks==10
            && mc.gameSettings.fovSetting==70 && !mc.gameSettings.enableVsync && !mc.gameSettings.viewBobbing
            && mc.gameSettings.guiScale==2 && !mc.gameSettings.hideGUI && mc.currentScreen==null
            && Math.abs(mc.player.posX)<0.01 && Math.abs(mc.player.posY-70)<0.01 && Math.abs(mc.player.posZ+32)<0.01
            && mc.player.rotationYaw==0 && mc.player.rotationPitch==0;
    }
    private String conditionDetail(Minecraft mc) {
        return "size="+mc.displayWidth+"x"+mc.displayHeight+" RD="+mc.gameSettings.renderDistanceChunks
            +" FOV="+mc.gameSettings.fovSetting+" vsync="+mc.gameSettings.enableVsync
            +" bobbing="+mc.gameSettings.viewBobbing+" GUI="+mc.gameSettings.guiScale
            +" hideGUI="+mc.gameSettings.hideGUI+" screen="+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getName())
            +" player="+(mc.player==null?"absent":mc.player.posX+","+mc.player.posY+","+mc.player.posZ
                +" yaw="+mc.player.rotationYaw+" pitch="+mc.player.rotationPitch+" flying="+mc.player.capabilities.isFlying);
    }
    @SubscribeEvent public void post(RenderLivingEvent.Post<?> ev) {
        if (!creating && actors.contains(ev.getEntity())) { rendered.add(ev.getEntity().getEntityId()); }
    }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent ev) {
        if (finished || Minecraft.getMinecraft().world==null || npcs.isEmpty()) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (ev.phase==TickEvent.Phase.START) {
                rendered.clear(); frameUploads=0; frameUploadNs=frameUploadBytes=0;
                if (phase.equals("INTRODUCE")) { introduce(mc); }
                return;
            }
            long now=System.nanoTime();
            if (phase.equals("LOAD") || phase.equals("STEADY")) {
                if (frames>=frameTimes.length) { throw new IllegalStateException("Frame buffer full"); }
                frameTimes[frames]=now-previousFrame; previousFrame=now;
                uploadTimes[frames]=frameUploadNs; uploadCounts[frames]=frameUploads; uploadBytes[frames]=frameUploadBytes; frames++;
                minRendered=Math.min(minRendered,rendered.size());
                if (rendered.size()!=n || !conditions(mc)) { badFrames++; }
                boolean all=true; for (Observed t:textures.values()) { all &= t.gpuAt>0; }
                if (all && lastGpu==0 && rendered.size()==n) { lastGpu=now-introduced; }
                if (now>=nextSample) { sample(now); nextSample=now+250000000L; }
                if (phase.equals("LOAD") && now-introduced>=20*SECOND) {
                    if (faults) {
                        for (Observed t:textures.values()) {
                            if (Arrays.asList("headers","body","trickle").contains(t.mode)) {
                                check("unbounded-"+t.mode,t.worker.isAlive() && t.gpuAt==0,
                                    "20s observation before fixture 30s cleanup, alive="+t.worker.isAlive()+" uploaded="+(t.gpuAt>0));
                            }
                        }
                    }
                    steadyAt=frames; enter("STEADY");
                } else if (phase.equals("STEADY") && now-since>=10*SECOND) {
                    steadyFrames=frames-steadyAt; gcEnd=gc(false); gcMsEnd=gc(true); enter("VERIFY");
                }
            } else if (phase.equals("VERIFY")) {
                check("visible", minRendered==n && badFrames==0, "min="+minRendered+" badFrames="+badFrames+" frames="+frames);
                check("dedup", textures.size()==(same?1:n), "unique="+textures.size());
                check("frames", steadyAt>100 && steadyFrames>100, "load="+steadyAt+" steady="+steadyFrames);
                if (!faults) { check("all-gpu", lastGpu>0, "last actual world Post with all uploads ms="+lastGpu/1e6); }
                // Capture outside both timing windows, before any FBO reference rendering.
                String name=out.getName().replace(".json","")+"-world.png";
                ScreenShotHelper.saveScreenshot(out.getParentFile(),name,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                verify(mc);
                enter("CLEANUP");
            } else if (phase.equals("CLEANUP")) {
                boolean alive=false; for (Observed t:textures.values()) { alive |= t.worker.isAlive(); }
                if (!alive) { check("threads-ended",true,"all original downloader threads ended (fixture closes hangs after 30s)"); finish(mc); }
                else if (now-since>15*SECOND) { throw new IllegalStateException("Fixture cleanup failed"); }
            }
        } catch (Exception | LinkageError e) { e.printStackTrace(); error=e.toString(); finish(mc); }
    }
    @SuppressWarnings("unchecked") private void introduce(Minecraft mc) throws Exception {
        Map<ResourceLocation,ITextureObject> map=(Map<ResourceLocation,ITextureObject>) ReflectionHelper.findField(TextureManager.class,"mapTextureObjects","field_110585_a").get(mc.getTextureManager());
        introduced=previousFrame=System.nanoTime(); nextSample=introduced;
        gc0=gc(false); gcMs0=gc(true); creating=true;
        try {
            for (int i=0;i<n;i++) {
                EntityCustomNpc npc=npcs.get(i); String mode=faults?MODES[i]:"normal";
                String base=mode.equals("refused")?job.getProperty("refusedBase"):job.getProperty("base");
                String url=base+"/"+job.getProperty("cell")+"/"+mode+"/"+(same?0:i)+".png";
                npc.display.setSkinUrl(url); npc.textureLocation=null;
                ResourceLocation loc=((RenderCustomNpc)mc.getRenderManager().getEntityRenderObject(npc)).getEntityTexture(npc);
                if (!textures.containsKey(loc)) {
                    ITextureObject raw=map.get(loc);
                    if (raw==null || raw.getClass()!=ImageDownloadAlt.class) { throw new IllegalStateException("Not the original downloader: "+raw); }
                    Observed observed=new Observed(raw,url,mode); textures.put(loc,observed); map.put(loc,observed);
                }
            }
        } finally { creating=false; }
        enter("LOAD");
    }
    private void sample(long now) throws Exception {
        int alive=0, decoded=0, gpu=0; long ready=0, retained=0;
        for (Observed t:textures.values()) {
            if (t.worker.isAlive()) { alive++; }
            BufferedImage b=(BufferedImage)image.get(t.delegate);
            if (b!=null) { decoded++; retained+=4L*b.getWidth()*b.getHeight(); if(t.gpuAt==0) { ready+=4L*b.getWidth()*b.getHeight(); } }
            if (t.gpuAt>0) { gpu++; }
        }
        peakThreads=Math.max(peakThreads,alive);
        JsonObject s=new JsonObject(); s.addProperty("ms",(now-introduced)/1e6); s.addProperty("downloaderThreads",alive);
        s.addProperty("decoded",decoded); s.addProperty("uploaded",gpu); s.addProperty("readyBytes",ready);
        s.addProperty("retainedRasterBytes",retained); s.addProperty("queued",0); s.addProperty("queuedBytes",0);
        s.addProperty("heapBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()); samples.add(s);
    }
    private void verify(Minecraft mc) throws Exception {
        int index=0;
        Framebuffer fb=new Framebuffer(256,256,true);
        try {
            for (Map.Entry<ResourceLocation,Observed> entry:textures.entrySet()) {
                Observed t=entry.getValue(); BufferedImage got=(BufferedImage)image.get(t.delegate);
                boolean success=!Arrays.asList("missing","invalid","headers","body","trickle","redirect","refused").contains(t.mode);
                String state=t.gpuAt>0?"uploaded":t.worker.isAlive()?"downloading":"failed";
                check("state-"+index,success?t.gpuAt>0:(!t.mode.equals("headers")&&!t.mode.equals("body")&&!t.mode.equals("trickle")?state.equals("failed"):true),t.mode+" "+state);
                JsonObject row=new JsonObject(); row.addProperty("url",t.url); row.addProperty("mode",t.mode); row.addProperty("state",state);
                row.addProperty("firstDecodedObservedMs",t.decodedAt==0?-1:t.decodedAt/1e6);
                row.addProperty("gpuBindMs",t.gpuAt==0?-1:t.gpuAt/1e6); row.addProperty("uploadNs",t.uploadNs);
                row.addProperty("threadName",t.worker.getName()); row.addProperty("workerAliveAtVerify",t.worker.isAlive());
                row.addProperty("cacheFileNull",field(ImageDownloadAlt.class,"cacheFile").get(t.delegate)==null);
                if (success && got!=null) {
                    String key=Arrays.asList("normal","legacy","hd","alpha","rectangle","oversized","bigbody").contains(t.mode)?t.mode:"normal";
                    BufferedImage expected=new ImageBufferDownloadAlt().parseUserSkin(ImageIO.read(new File(job.getProperty("fixture"),key+".png")));
                    boolean equal=got.getWidth()==expected.getWidth() && got.getHeight()==expected.getHeight();
                    if (equal) { equal=Arrays.equals(got.getRGB(0,0,got.getWidth(),got.getHeight(),null,0,got.getWidth()), expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth())); }
                    check("pixels-"+index,equal && gpuEqual(mc,t,expected),t.mode+" original ARGB + GPU exact");
                    Object raster=pixels.get(parser.get(t.delegate));
                    row.addProperty("width",got.getWidth()); row.addProperty("height",got.getHeight());
                    row.addProperty("retainedImageBytes",4L*got.getWidth()*got.getHeight());
                    row.addProperty("retainedParserBytes",raster instanceof int[]?4L*((int[])raster).length:0);
                    row.addProperty("parserAliasesImage",raster==((java.awt.image.DataBufferInt)got.getRaster().getDataBuffer()).getData());
                    row.addProperty("gpuLevel0EstimateBytes",4L*got.getWidth()*got.getHeight());
                }
                if (faults && !t.mode.equals("bigbody")) {
                    EntityCustomNpc npc=npcs.get(index);
                    ResourceLocation expected=DefaultPlayerSkin.getDefaultSkinLegacy();
                    if (success && got!=null) { expected=mc.getTextureManager().getDynamicTextureLocation("m121-known-"+index,new DynamicTexture(got)); }
                    JsonObject comparison=NpcBatchSceneCheck.url(mc,fb,npc,success?"url-loaded":"url-failed",expected,out.getPath()+"-"+t.mode,true);
                    row.add("imageComparison",comparison); check("image-"+index,comparison.get("success").getAsBoolean(),t.mode+" local source / Steve FBO reference");
                    if(success && got!=null) { mc.getTextureManager().deleteTexture(expected); }
                }
                rows.add(row); index++;
            }
        } finally { fb.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); }
        if (!faults && !same) { check("thread-fanout",peakThreads>4,"observed peak original downloaders="+peakThreads); }
        check("no-gl-error",GL11.glGetError()==0,"after GPU/reference verification");
    }
    private boolean gpuEqual(Minecraft mc,Observed t,BufferedImage expected) {
        GlStateManager.bindTexture(t.delegate.getGlTextureId()); int w=expected.getWidth(),h=expected.getHeight();
        if (GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH)!=w || GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT)!=h) { return false; }
        ByteBuffer b=BufferUtils.createByteBuffer(w*h*4); GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);
        for(int y=0;y<h;y++) { for(int x=0;x<w;x++) {
            int i=4*(y*w+x),actual=((b.get(i+3)&255)<<24)|((b.get(i)&255)<<16)|((b.get(i+1)&255)<<8)|(b.get(i+2)&255);
            if(actual!=expected.getRGB(x,y)) { return false; }
        } } return true;
    }
    private static long gc(boolean time) { long n=0; for(GarbageCollectorMXBean b:ManagementFactory.getGarbageCollectorMXBeans()) { n+=Math.max(0,time?b.getCollectionTime():b.getCollectionCount()); } return n; }
    private static JsonObject stats(long[] values,int from,int to) {
        long[] a=Arrays.copyOfRange(values,from,to); Arrays.sort(a); JsonObject s=new JsonObject(); s.addProperty("count",a.length);
        if(a.length>0) { s.addProperty("p50Ms",a[(int)Math.ceil(a.length*.50)-1]/1e6); s.addProperty("p95Ms",a[(int)Math.ceil(a.length*.95)-1]/1e6);
            s.addProperty("p99Ms",a[(int)Math.ceil(a.length*.99)-1]/1e6); s.addProperty("maxMs",a[a.length-1]/1e6); }
        int slow=0; for(long v:a) { if(v>50000000) { slow++; } } s.addProperty("over50Ms",slow); return s;
    }
    private void finish(Minecraft mc) {
        if(finished) { return; } finished=true;
        try {
            JsonObject p=new JsonObject(); p.addProperty("success",ok&&error.isEmpty()); p.addProperty("error",error);
            p.addProperty("cell",job.getProperty("cell")); p.addProperty("restart",job.getProperty("restart")); p.addProperty("n",n);
            p.addProperty("javaVersion",System.getProperty("java.version")); p.addProperty("vm",System.getProperty("java.vm.name"));
            p.addProperty("glVendor",GL11.glGetString(GL11.GL_VENDOR)); p.addProperty("glRenderer",GL11.glGetString(GL11.GL_RENDERER));
            p.addProperty("glVersion",GL11.glGetString(GL11.GL_VERSION));
            p.addProperty("peakDownloaderThreads",peakThreads); p.addProperty("lastGpuDisplayMs",lastGpu==0?-1:lastGpu/1e6);
            p.addProperty("gcCount",gcEnd-gc0); p.addProperty("gcMillis",gcMsEnd-gcMs0);
            p.addProperty("textureManagerLiveEntries",((Map<?,?>)ReflectionHelper.findField(TextureManager.class,"mapTextureObjects","field_110585_a").get(mc.getTextureManager())).size());
            p.add("loadingFrames",stats(frameTimes,0,steadyAt)); p.add("steadyFrames",stats(frameTimes,steadyAt,frames));
            JsonArray f=new JsonArray(); for(int i=0;i<frames;i++) { JsonObject r=new JsonObject(); r.addProperty("frameNs",frameTimes[i]); r.addProperty("uploadCount",uploadCounts[i]); r.addProperty("uploadBytes",uploadBytes[i]); r.addProperty("uploadNs",uploadTimes[i]); f.add(r); }
            p.add("frames",f); p.add("samples",samples); p.add("urls",rows); p.add("checks",checks);
            try(Writer w=Files.newBufferedWriter(out.toPath(),StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(p,w); }
            System.out.println("M121 DONE success="+(ok&&error.isEmpty()));
        } catch(Exception e) { e.printStackTrace(); }
        mc.shutdown();
    }
    /** Transparent delegate inserted only after the renderer starts the original object.
     * Two cached Field reads per not-yet-uploaded bind, nanoTime only around a real upload.
     * This is instrumented baseline data, not a claim of uninstrumented frame cost. */
    private final class Observed implements ITextureObject {
        final ITextureObject delegate; final String url,mode; final Thread worker;
        long decodedAt,gpuAt,uploadNs;
        Observed(ITextureObject d,String u,String m) throws Exception { delegate=d; url=u; mode=m; worker=(Thread)thread.get(d); if(worker==null) { throw new IllegalStateException("No original worker"); } }
        public int getGlTextureId() {
            if(gpuAt>0) { return delegate.getGlTextureId(); }
            try {
                BufferedImage b=(BufferedImage)image.get(delegate); long start=0;
                if(b!=null) { start=System.nanoTime(); if(decodedAt==0) { decodedAt=start-introduced; } }
                int id=delegate.getGlTextureId();
                if(uploaded.getBoolean(delegate)) {
                    long end=System.nanoTime(); gpuAt=end-introduced; uploadNs=start==0?0:end-start;
                    frameUploads++; frameUploadNs+=uploadNs;
                    if(b==null) { b=(BufferedImage)image.get(delegate); }
                    if(b!=null) { frameUploadBytes+=4L*b.getWidth()*b.getHeight(); }
                } return id;
            } catch(Exception e) { throw new IllegalStateException(e); }
        }
        public void loadTexture(IResourceManager r) throws IOException { delegate.loadTexture(r); }
        public void setBlurMipmap(boolean b,boolean m) { delegate.setBlurMipmap(b,m); }
        public void restoreLastBlurMipmap() { delegate.restoreLastBlurMipmap(); }
    }
}
