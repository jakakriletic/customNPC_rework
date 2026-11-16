package noppes.npcs.rework.diag;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.*;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.controllers.ClientCloneController;
import noppes.npcs.client.gui.*;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.data.*;

/** Explicit controller-only job. Real cloner interactions, GUI handlers and CloneSave packets. */
@SideOnly(Side.CLIENT)
public final class ReadableCloneHarness {
    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int delay; final Act action; final Cond until;
        Step(String n,int d,Act a,Cond c) { name=n; delay=d; action=a; until=c; }
    }
    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray(), images = new JsonArray();
    private final Deque<Step> queue = new ArrayDeque<Step>();
    private Step step;
    private int delay;
    private boolean acted, finished;
    private long since;
    private NBTTagCompound localExpected;
    private ReadableCloneHarness(Properties p) { job=p; out=new File(p.getProperty("out")); }
    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(path),StandardCharsets.UTF_8)) { p.load(r); }
        if (!"1".equals(p.getProperty("readableClone"))) { return false; }
        for (String key : new String[]{"out","connect","phase","gate","switch"}) { if (!p.containsKey(key)) { throw new IllegalArgumentException("Missing readableClone job " + key); } }
        ReadableCloneHarness h = new ReadableCloneHarness(p); h.plan(); MinecraftForge.EVENT_BUS.register(h); return true;
    }
    private Minecraft mc() { return Minecraft.getMinecraft(); }
    private void add(String n,int d,Act a,Cond c) { queue.add(new Step(n,d,a,c)); }
    private void act(String n,Act a) { add(n,8,a,() -> true); }
    private void waitFor(String n,Cond c) { add(n,5,() -> {},c); }
    private void cmd(String s) { act(s,() -> mc().player.sendChatMessage("/" + s)); }
    private GuiNPCInterface gui() { return (GuiNPCInterface)mc().currentScreen; }
    private EntityNPCInterface named(String name) {
        for (Entity e : mc().world.loadedEntityList) { if (e instanceof EntityNPCInterface && name.equals(e.getName())) { return (EntityNPCInterface)e; } } return null;
    }
    private EntityNPCInterface at(int x) {
        for (Entity e : mc().world.loadedEntityList) { if (e instanceof EntityNPCInterface && Math.abs(e.posX-(x+0.5))<0.8 && Math.abs(e.posZ-3.5)<0.8) { return (EntityNPCInterface)e; } } return null;
    }
    private void press(int x,int y) throws Exception {
        // Save and Spawn can close the screen during mouseClicked.
        GuiNPCInterface screen = gui();
        screen.drawScreen(x,y,0);
        screen.mouseClicked(x,y,0);
        screen.mouseReleased(x,y,0);
    }
    private void click(int id) throws Exception {
        GuiButton b = gui().getButton(id); if (b == null || !b.enabled || !b.visible) { throw new IllegalStateException("Button " + id); } press(b.x+3,b.y+3);
    }
    private void type(GuiNpcTextField f,String value) throws Exception {
        press(f.x+3,f.y+3); gui().keyTyped((char)0,207);
        for (int i=0;i<256;i++) { gui().keyTyped((char)0,14); }
        for (int i=0;i<value.length();i++) { gui().keyTyped(value.charAt(i),0); }
    }
    private boolean gate() throws IOException { File f = new File(job.getProperty("gate")); return f.isFile() && Files.readAllLines(f.toPath(),StandardCharsets.UTF_8).contains("done"); }
    private void sync() { act("server verification gate",() -> System.out.println("READABLECLIENT SYNC " + job.getProperty("phase"))); waitFor("server verified",() -> gate()); }
    private void check(String key,boolean ok,String detail) {
        JsonObject c = new JsonObject(); c.addProperty("key",key); c.addProperty("ok",ok); c.addProperty("detail",detail); checks.add(c);
        System.out.println("READABLECLIENT CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }
    private void snapshot(String name) throws IOException {
        String file=out.getName().replace(".json","") + "-" + name + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(),file,mc().displayWidth,mc().displayHeight,mc().getFramebuffer()); images.add(file);
    }
    private void save(boolean server,String name) {
        add("cloner on entity " + name,12,() -> mc().playerController.interactWithEntity(mc().player,named("ReadableSeed"),EnumHand.MAIN_HAND),
                () -> mc().currentScreen instanceof GuiNpcMobSpawnerAdd);
        act("choose storage " + name,() -> {
            GuiNpcButton side = (GuiNpcButton)gui().getButton(3);
            if (side.getValue() != (server ? 1 : 0)) { click(3); }
            type(gui().getTextField(0),name);
            if (!server) {
                Field field=GuiNpcMobSpawnerAdd.class.getDeclaredField("compound"); field.setAccessible(true);
                localExpected=((NBTTagCompound)field.get(gui())).copy(); ClientCloneController.Instance.cleanTags(localExpected);
            }
            snapshot(name);
        });
        act("real save " + name,() -> click(0));
        waitFor("saved " + name,() -> mc().currentScreen == null);
        if (!server) {
            act("local disk " + name,() -> {
                File file=new File(new File(ClientCloneController.Instance.getDir(),"1"),name + ".json");
                NBTTagCompound actual=ClientCloneController.Instance.getCloneData(null,name,1);
                check("local-roundtrip",ReadableClone.same(localExpected,actual),"exact cleaned GUI compound, types and presence");
                String bytes=new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
                boolean on="1".equals(job.getProperty("switch"));
                check("local-format",on ? ReadableCloneFiles.hasEnvelope(bytes) : bytes.equals(noppes.npcs.util.NBTJsonUtil.Convert(localExpected)),on ? "readable JSON" : "exact legacy bytes");
                check("local-list",ClientCloneController.Instance.getClones(1).contains(name) && !ClientCloneController.Instance.getClones(1).contains("CLONE-FORMAT"),"real inherited enumeration");
                if ("on".equals(job.getProperty("phase"))) {
                    long start=System.nanoTime(); String encoded=ReadableClone.encode(localExpected); long encodedNs=System.nanoTime()-start;
                    start=System.nanoTime(); NBTTagCompound decoded=ReadableClone.decode(encoded); long decodedNs=System.nanoTime()-start;
                    int legacyBytes=noppes.npcs.util.NBTJsonUtil.Convert(localExpected).getBytes(StandardCharsets.UTF_8).length;
                    check("conversion",ReadableClone.same(localExpected,decoded),"one export="+encodedNs+" ns import="+decodedNs+" ns bytes="+encoded.getBytes(StandardCharsets.UTF_8).length+" legacyBytes="+legacyBytes);
                }
            });
        }
    }
    private void place(int x,boolean server,String clone) {
        // The server rejects block-use packets outside REACH_DISTANCE + 3.
        // Wait for the real teleport response before using the cloner near the target.
        cmd("tp @p " + (x+0.5) + " 4 0.5 0 0");
        add("in reach of clone target " + x,20,() -> {},() ->
                Math.abs(mc().player.posX-(x+0.5))<0.5 && Math.abs(mc().player.posZ-0.5)<0.5
                && mc().world.isBlockLoaded(new BlockPos(x,3,3)));
        add("open clone tab " + x,10,() -> mc().playerController.processRightClickBlock(mc().player,mc().world,new BlockPos(x,3,3),EnumFacing.UP,new Vec3d(x+0.5,4,3.5),EnumHand.MAIN_HAND),
                () -> mc().currentScreen instanceof GuiNpcMobSpawner);
        add("choose clone tab " + x,15,() -> {
            GuiButton b=gui().getTopButton(server ? 5 : 3); press(b.x+3,b.y+3);
        },() -> gui().getScroll(0).getList().contains(clone));
        act("select clone " + clone,() -> {
            GuiCustomScroll s=gui().getScroll(0); int i=s.getList().indexOf(clone);
            press(s.guiLeft+10,s.guiTop+14*i+7);
        });
        act("spawn from tab " + x,() -> click(1));
        waitFor("placed " + x,() -> mc().currentScreen == null && at(x)!=null);
        act("placed rig " + x,() -> {
            check((server ? "server" : "local")+"-placement",at(x)!=null && "knight".equals(at(x).rwRig.model) && at(x).rwRig.clips.contains("rc_probe"),"real clone tab placement with rig and embedded clip");
        });
    }
    private void plan() {
        String phase=job.getProperty("phase");
        waitFor("menu",() -> mc().currentScreen instanceof GuiMainMenu);
        add("connect",10,() -> { mc().gameSettings.pauseOnLostFocus=false; mc().displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc(),"127.0.0.1",Integer.parseInt(job.getProperty("connect")))); },
                () -> mc().world!=null && mc().player!=null && mc().currentScreen==null);
        cmd("gamemode 1"); cmd("tp @p 0.5 4 0.5 0 0"); cmd("replaceitem entity @p slot.hotbar.0 customnpcs:npcmobcloner");
        waitFor("source",() -> named("ReadableSeed")!=null && mc().player.getHeldItemMainhand().getItem().getRegistryName().toString().equals("customnpcs:npcmobcloner"));
        act("switch",() -> check("switch",CustomNpcs.RwReadableClones==Integer.parseInt(job.getProperty("switch")),"startup switch"));
        if (phase.equals("off") || phase.equals("on") || phase.equals("back")) {
            save(false,"RC_"+phase+"Local"); save(true,"RC_"+phase+"Server");
        } else {
            act("restart local read",() -> {
                NBTTagCompound n=ClientCloneController.Instance.getCloneData(null,"RC_onLocal",1);
                check("restart-local-read",n!=null && "knight".equals(n.getCompoundTag("RwRig").getString("Model")) && n.getTagList("Scripts",10).tagCount()>0,"new JVM, new local disk read");
            });
            place(18,false,"RC_onLocal"); place(22,true,"RC_onServer");
        }
        add("script tick settle",30,() -> {},() -> true);
        sync();
        act("finish",() -> finish(true,""));
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase!=TickEvent.Phase.END || finished) { return; }
        try {
            if (step==null) { step=queue.poll(); if (step==null) { return; } acted=false; delay=step.delay; since=System.nanoTime(); System.out.println("READABLECLIENT STEP " + step.name); }
            if (System.nanoTime()-since>180000000000L) { throw new IllegalStateException("Timeout " + step.name + " screen="+(mc().currentScreen==null ? "none" : mc().currentScreen.getClass().getName())); }
            if (delay-->0) { return; }
            if (!acted) { acted=true; step.action.run(); if (finished) { return; } }
            if (step.until.ok()) { step=null; }
        } catch (Exception | LinkageError ex) { ex.printStackTrace(); finish(false,ex.toString()); }
    }
    private void finish(boolean success,String error) {
        finished=true;
        try {
            JsonObject p=new JsonObject(); p.addProperty("success",success); p.addProperty("error",error); p.add("checks",checks); p.add("screenshots",images);
            try (Writer w=Files.newBufferedWriter(out.toPath(),StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(p,w); }
            System.out.println("READABLECLIENT DONE success="+success);
            if (mc().world!=null) { mc().world.sendQuittingDisconnectingPacket(); mc().loadWorld(null); }
            mc().displayGuiScreen(new GuiMainMenu());
            mc().shutdown();
        } catch (Exception ex) { ex.printStackTrace(); mc().shutdown(); }
    }
}
