package noppes.npcs.rework.diag;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomItems;
import noppes.npcs.client.gui.mainmenu.GuiNpcAI;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.army.*;

/** M11.8a controller-only: actual mouse/key handlers and item interactions over a real connection.
 * No GUI actionPerformed/save calls, no direct production assignment/move calls. Forged packets
 * are used only for negative authorization checks. Scene/camera commands are setup, not proof.
 */
@SideOnly(Side.CLIENT)
public final class ArmyGuiHarness {
    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int wait; final Act act; final Cond until;
        Step(String n, int w, Act a, Cond c) { name=n; wait=w; act=a; until=c; }
    }
    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray(), images = new JsonArray();
    private final JsonObject data = new JsonObject();
    private final Deque<Step> steps = new ArrayDeque<Step>();
    private Step step;
    private int delay, sent, before, poll, fpsMin;
    private long fpsSum;
    private boolean acted, finished;
    private long since;
    private ArmyGuiHarness(Properties p) { job=p; out=new File(p.getProperty("out")); }
    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("armyGui"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("connect") || !p.containsKey("phase")) { throw new IllegalArgumentException("armyGui job"); }
        ArmyGuiHarness h = new ArmyGuiHarness(p); h.plan(); MinecraftForge.EVENT_BUS.register(h); return true;
    }
    private void add(String n, int w, Act a, Cond c) { steps.add(new Step(n,w,a,c)); }
    private void act(String n, Act a) { add(n, 10, a, () -> true); }
    private void cmd(String c) { act(c, () -> Minecraft.getMinecraft().player.sendChatMessage("/" + c)); }
    private GuiArmyStaff gui() { return (GuiArmyStaff)Minecraft.getMinecraft().currentScreen; }
    private EntityNPCInterface npc(String name) {
        for (Entity e : Minecraft.getMinecraft().world.loadedEntityList) { if (e instanceof EntityNPCInterface && e.getName().equals(name)) { return (EntityNPCInterface)e; } }
        return null;
    }
    private void openNpc(String name) {
        add("open " + name, 15, () -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (npc(name) == null) { throw new IllegalStateException("NPC missing: " + name); }
            before = ArmyClient.staffReply() == null ? -1 : ArmyClient.staffReply().id;
            mc.playerController.interactWithEntity(mc.player, npc(name), EnumHand.MAIN_HAND);
        }, () -> Minecraft.getMinecraft().currentScreen instanceof GuiArmyStaff && ArmyClient.staffReply() != null && ArmyClient.staffReply().id != before);
    }
    private void air() {
        Minecraft mc = Minecraft.getMinecraft(); mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
    }
    private void openAir() { add("open staff", 15, () -> air(), () -> Minecraft.getMinecraft().currentScreen instanceof GuiArmyStaff); }
    private void click(int id) throws Exception { click(Minecraft.getMinecraft().currentScreen, id); }
    private static void click(GuiScreen screen, int id) throws Exception {
        Field f = GuiScreen.class.getDeclaredField("buttonList"); f.setAccessible(true);
        for (GuiButton b : (List<GuiButton>)f.get(screen)) {
            if (b.id != id) { continue; }
            if (!b.enabled || !b.visible) { throw new IllegalStateException("Unavailable button " + id); }
            int x=b.x+3, y=b.y+3; screen.drawScreen(x,y,0);
            Method m=GuiScreen.class.getDeclaredMethod("mouseClicked", int.class,int.class,int.class); m.setAccessible(true); m.invoke(screen,x,y,0);
            m=GuiScreen.class.getDeclaredMethod("mouseReleased", int.class,int.class,int.class); m.setAccessible(true); m.invoke(screen,x,y,0); return;
        }
        throw new IllegalStateException("Missing button " + id);
    }
    private void type(String value) throws Exception {
        Field f=GuiArmyStaff.class.getDeclaredField("name"); f.setAccessible(true); GuiTextField box=(GuiTextField)f.get(gui());
        Method m=GuiScreen.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class); m.setAccessible(true); m.invoke(gui(),box.x+3,box.y+3,0);
        m=GuiScreen.class.getDeclaredMethod("keyTyped",char.class,int.class); m.setAccessible(true);
        m.invoke(gui(),(char)0,207); // End, then backspace: normal text field input.
        for(int i=0;i<40;i++) { m.invoke(gui(),(char)0,14); }
        for(char c:value.toCharArray()) { m.invoke(gui(),c,0); }
    }
    private void button(String key, int id, boolean ok, String contains) {
        add(key, 15, () -> { before=ArmyClient.staffReply()==null?-1:ArmyClient.staffReply().id; click(id); },
                () -> ArmyClient.staffReply()!=null && ArmyClient.staffReply().id!=before);
        act(key+" check", () -> check(key, ArmyClient.staffReply().ok==ok && ArmyClient.staffReply().text.contains(contains), ArmyClient.staffReply().text));
    }
    private void packet(String key, ArmyStaffCodec.Op op, boolean ok, String contains) {
        add(key, 15, () -> { sent=ArmyClient.action(op,-1,"alpha",3,0); }, () -> ArmyClient.staffReply()!=null && ArmyClient.staffReply().id==sent);
        act(key+" check", () -> check(key, ArmyClient.staffReply().ok==ok && ArmyClient.staffReply().text.contains(contains),ArmyClient.staffReply().text));
    }
    private void snapshot(String key, Cond condition) {
        add(key,25, () -> { sent=ArmyClient.request(); poll=25; }, () -> {
            if(ArmyClient.latest()!=null && ArmyClient.latest().requestId>=sent && condition.ok()) { return true; }
            if(--poll<=0) { sent=ArmyClient.request(); poll=25; } return false;
        });
        act(key+" check", () -> check(key,true,names()));
    }
    private String names() {
        StringJoiner j=new StringJoiner(","); if(ArmyClient.latest()!=null) { for(ArmySnapshot.Entry e:ArmyClient.latest().armies) { j.add(e.name+":"+e.loaded+":"+e.command); } } return j.toString();
    }
    private List<?> rows() throws Exception { Field f=GuiArmyStaff.class.getDeclaredField("rows"); f.setAccessible(true); return (List<?>)f.get(gui()); }
    private void screen(String name) throws Exception {
        Minecraft mc=Minecraft.getMinecraft(); String file=out.getName().replace(".json", "")+"-"+name+".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(),file,mc.displayWidth,mc.displayHeight,mc.getFramebuffer()); images.add(file);
    }
    private void top(int id) throws Exception {
        GuiNPCInterface2 gui=(GuiNPCInterface2)Minecraft.getMinecraft().currentScreen;
        Field m=GuiNPCInterface2.class.getDeclaredField("menu"); m.setAccessible(true);
        Field b=GuiNpcMenu.class.getDeclaredField("topButtons"); b.setAccessible(true);
        for(GuiButton button:(GuiButton[])b.get(m.get(gui))) { if(button.id==id) {
            int x=button.x+2,y=button.y+2; gui.drawScreen(x,y,0); gui.mouseClicked(x,y,0); gui.mouseReleased(x,y,0); return;
        }} throw new IllegalStateException("Menu " + id);
    }
    private void plan() {
        Minecraft mc=Minecraft.getMinecraft(); String phase=job.getProperty("phase");
        add("menu",0,()->{},()->mc.currentScreen instanceof GuiMainMenu);
        add("join",10,()->{ mc.gameSettings.pauseOnLostFocus=false;
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,"127.0.0.1",Integer.parseInt(job.getProperty("connect"))));
        },()->mc.world!=null && mc.player!=null && mc.currentScreen==null);
        cmd("gamemode 1"); cmd("gamerule doMobSpawning false"); cmd("tp @p 600 4 0 0 0");
        add("load scene",80,()->{},()->mc.world.isBlockLoaded(new net.minecraft.util.math.BlockPos(608,4,4)));
        if("off".equals(phase)) {
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff");
            add("use off",20,()->air(),()->true);
            act("off assertions",()->{
                check("off-no-gui",mc.currentScreen==null,"inert staff");
                check("off-no-channel",ArmyClient.latest()==null,"no snapshot");
                check("off-item",mc.player.getHeldItemMainhand().getItem()!=ArmyStaff.item && net.minecraft.item.Item.getByNameOrId("customnpcs:army_staff")==null,"Q435: no staff from a dedicated server at 0");
            });
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:npcwand");
            add("wand off",20,()->mc.playerController.interactWithEntity(mc.player,npc("AGUI_A"),EnumHand.MAIN_HAND),()->mc.currentScreen instanceof GuiNPCInterface2);
            act("AI off",()->top(6));
            add("off AI check",20,()->{},()->mc.currentScreen instanceof GuiNpcAI);
            act("off-hidden",()->check("off-hidden",((GuiNpcAI)mc.currentScreen).getButton(80)==null,"no army editor button"));
            act("off close",()->top(0));
        } else if("load".equals(phase)) {
            planLoad(mc);
        } else if("verify".equals(phase)) {
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff"); openNpc("AGUI_A");
            act("persist check",()->{
                check("restart-membership",ArmyClient.staffReply().ok && "alpha".equals(ArmyClient.staffReply().army),ArmyClient.staffReply().text);
                data.addProperty("uuid",npc("AGUI_A").getUniqueID().toString());
            });
            act("choose verify",()->type("alpha")); button("restart-order",11,true,"state=CANCELLED");
            snapshot("restart-count",()->names().contains("alpha:3:true")); act("verify image",()->screen("restart")); act("verify close",()->click(12));
        } else {
            for(String army:new String[]{"alpha","beta","hidden","remote"}) { cmd("rwarmy create "+army+" uuid:0b5a1c2e-1111-4a2b-9c3d-123456789abc"); }
            for(int i=0;i<3;i++) { cmd("noppes clone spawn AGUI_"+(char)('A'+i)+" 1 "+(600+i*3)+",4,3"); }
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff");
            openNpc("AGUI_A");
            act("initial inspect",()->{ check("staff-npc-open",ArmyClient.staffReply().ok && ArmyClient.staffReply().army.isEmpty(),ArmyClient.staffReply().text); data.addProperty("uuid",npc("AGUI_A").getUniqueID().toString()); });
            act("type alpha",()->type("alpha")); button("staff-assign",5,true,"changed=1");
            act("assigned field",()->check("staff-membership","alpha".equals(ArmyClient.staffReply().army),ArmyClient.staffReply().army));
            act("NPC image",()->screen("npc")); act("close NPC",()->click(12));
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:npcwand");
            add("wand",20,()->mc.playerController.interactWithEntity(mc.player,npc("AGUI_A"),EnumHand.MAIN_HAND),()->mc.currentScreen instanceof GuiNPCInterface2);
            act("AI",()->top(6));
            add("wait AI",20,()->{},()->mc.currentScreen instanceof GuiNpcAI && ((GuiNpcAI)mc.currentScreen).getButton(80)!=null
                    && ((GuiNpcAI)mc.currentScreen).getButton(2).enabled);
            act("editor open",()->{ check("editor-button",((GuiNpcAI)mc.currentScreen).getButton(80).enabled,"AI Army button"); before=ArmyClient.staffReply().id; click(80); });
            add("editor inspect",20,()->{},()->mc.currentScreen instanceof GuiArmyStaff && ArmyClient.staffReply().id!=before);
            act("editor prefill",()->{ check("editor-prefill","alpha".equals(ArmyClient.staffReply().army),ArmyClient.staffReply().text); type("beta"); });
            button("editor-assign",5,true,"changed=1");
            act("editor member",()->check("editor-membership","beta".equals(ArmyClient.staffReply().army),ArmyClient.staffReply().army));
            act("back AI",()->click(12)); act("close AI",()->top(0));
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff"); openAir();
            act("all tab",()->click(1));
            add("all wait",45,()->{},()->rows().size()==4);
            act("all assertion",()->{ check("all-tab",rows().size()==4,"empty armies also listed"); screen("all"); click(0); });
            act("near assertion",()->{ check("near-tab",rows().size()==1 && ((ArmySnapshot.Entry)rows().get(0)).name.equals("beta"),"loaded nearby only"); screen("near"); });
            act("mass type",()->type("alpha")); act("mass confirm",()->click(5));
            act("mass dialog",()->{ check("bulk-confirm",mc.currentScreen instanceof GuiYesNo,"explicit confirmation"); before=ArmyClient.staffReply().id; click(0); });
            add("mass response",20,()->{},()->ArmyClient.staffReply().id!=before);
            act("mass check",()->check("bulk-assign",ArmyClient.staffReply().ok && ArmyClient.staffReply().text.contains("changed=3"),ArmyClient.staffReply().text));
            snapshot("bulk-count",()->names().contains("alpha:3:true"));
            act("swarm",()->{ click(6); click(6); click(6); });
            act("arm move",()->click(8));
            act("aim",()->{ check("aim-closes",mc.currentScreen==null,"aim in world"); mc.player.rotationYaw=0; mc.player.rotationPitch=5; });
            add("ray move",20,()->{ before=ArmyClient.staffReply().id; air(); },()->ArmyClient.staffReply().id!=before);
            act("ray check",()->check("ray-move",ArmyClient.staffReply().ok && ArmyClient.staffReply().text.contains("mode=SWARM stance=PASSIVE"),ArmyClient.staffReply().text));
            snapshot("physical-move",()->{ for(ArmySnapshot.Entry e:ArmyClient.latest().armies) { if(e.name.equals("alpha") && !e.clusters.isEmpty() && e.clusters.get(0).z>=10) { return true; } } return false; });
            openAir(); act("select alpha",()->type("alpha")); button("stop",9,true,"cancelled alpha"); button("stopped-state",11,true,"state=CANCELLED");
            button("return-here",10,true,"target=600.5,4.0,0.5");
            snapshot("physical-return",()->{ for(ArmySnapshot.Entry e:ArmyClient.latest().armies) { if(e.name.equals("alpha") && !e.clusters.isEmpty() && e.clusters.get(0).z<=4) { return true; } } return false; });
            button("return-stop",9,true,"cancelled alpha");
            act("arm miss",()->click(8)); act("look up",()->{mc.player.rotationPitch=-90;mc.player.rotationYaw=0;});
            add("ray miss",20,()->{before=ArmyClient.staffReply().id;air();},()->ArmyClient.staffReply().id!=before);
            act("miss check",()->check("ray-miss",!ArmyClient.staffReply().ok && ArmyClient.staffReply().text.contains("no loaded, safe ground"),ArmyClient.staffReply().text));
            openAir(); act("select again",()->type("alpha")); button("miss-preserves-order",11,true,"state=CANCELLED");
            act("sync view",()->System.out.println("ARMYGUI SYNC view"));
            snapshot("view-filter",()->names().equals("alpha:3:false"));
            act("view screen",()->{ click(1); check("view-command-disabled",!buttonById(9).enabled,"view only"); screen("view"); });
            packet("forged-stop-denied",ArmyStaffCodec.Op.STOP,false,"no command permission");
            act("sync revoke",()->System.out.println("ARMYGUI SYNC revoke"));
            snapshot("revoke-filter",()->ArmyClient.latest().armies.isEmpty());
            packet("revoked-status-hidden",ArmyStaffCodec.Op.STATUS,false,"unknown army");
            act("end main",()->{check("connection-alive",mc.player!=null && mc.getConnection()!=null,"rights changes kept connection"); click(12);});
        }
        act("finish",()->finish(true,""));
    }
    /** M11.10: a real player with the staff commands one army of 200; server cost and FPS are data, arrival state too. */
    private void planLoad(Minecraft mc) {
        cmd("gamerule maxEntityCramming 0");
        cmd("rwarmy create host uuid:0b5a1c2e-1111-4a2b-9c3d-123456789abc");
        cmd("noppes clone grid AGUI_L 1 25 8 590,4,8");
        add("load settle",60,()->{},()->true);
        cmd("rwarmy assign host @e[name=AGUI_L]");
        snapshot("load-count",()->names().contains("host:200:true"));
        act("load clusters",()->{ ArmySnapshot.Entry e=entry("host"); check("load-clusters",e.clusters.size()==1 && e.clusters.get(0).size==200 && e.omittedMembers==0,e.clusters.size()+" clusters"); });
        fps("fpsBefore",200);
        cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff"); openAir();
        act("all tab",()->click(1));
        add("all wait",45,()->{},()->rows().size()==1);
        act("all assertion",()->{ check("load-all-tab",((ArmySnapshot.Entry)rows().get(0)).loaded==200,"one row, 200 loaded"); screen("load-all"); });
        act("sync measure",()->System.out.println("ARMYGUI SYNC measure"));
        add("measure settle",60,()->{},()->true);
        act("select host",()->type("host"));
        act("arm move",()->click(8));
        act("aim",()->{ check("load-aim-closes",mc.currentScreen==null,"aim in world"); mc.player.rotationYaw=0; mc.player.rotationPitch=3; });
        add("ray move",20,()->{ before=ArmyClient.staffReply()==null?-1:ArmyClient.staffReply().id; air(); },()->ArmyClient.staffReply()!=null && ArmyClient.staffReply().id!=before);
        act("ray check",()->check("load-ray-move",ArmyClient.staffReply().ok && ArmyClient.staffReply().text.contains("mode=FORMATION"),ArmyClient.staffReply().text));
        fps("fpsMarch",200);
        act("march image",()->screen("load-march"));
        snapshot("load-physical-move",()->{ ArmySnapshot.Entry e=entry("host"); return !e.clusters.isEmpty() && e.clusters.get(0).z>=22; });
        add("load end",10,()->{ sent=-5; poll=0; },()->{
            if(--poll<=0) { sent=ArmyClient.action(ArmyStaffCodec.Op.STATUS,-1,"host",0,0); poll=40; }
            String t=ArmyClient.staffReply()!=null && ArmyClient.staffReply().id==sent ? ArmyClient.staffReply().text : "";
            return t.contains("state=ARRIVED") || t.contains("state=BLOCKED");
        });
        act("end check",()->{ check("load-march-ended",true,ArmyClient.staffReply().text); data.addProperty("endState",ArmyClient.staffReply().text); });
        act("sync dump",()->System.out.println("ARMYGUI SYNC dump"));
        add("dump settle",60,()->{},()->true);
        act("end load",()->check("connection-alive",mc.player!=null && mc.getConnection()!=null,"200 member snapshots kept the connection"));
    }
    private ArmySnapshot.Entry entry(String army) {
        for(ArmySnapshot.Entry e:ArmyClient.latest().armies) { if(e.name.equals(army)) { return e; } }
        throw new IllegalStateException("army "+army);
    }
    private void fps(String key, int ticks) {
        add(key,0,()->{ poll=0; fpsSum=0; fpsMin=Integer.MAX_VALUE; },()->{ int f=Minecraft.getDebugFPS(); fpsSum+=f; fpsMin=Math.min(fpsMin,f); return ++poll>=ticks; });
        act(key+" data",()->{ data.addProperty(key+"Avg",fpsSum/ticks); data.addProperty(key+"Min",fpsMin); });
    }
    private GuiButton buttonById(int id) throws Exception {
        Field f=GuiScreen.class.getDeclaredField("buttonList"); f.setAccessible(true);
        for(GuiButton b:(List<GuiButton>)f.get(Minecraft.getMinecraft().currentScreen)) { if(b.id==id) { return b; } }
        throw new IllegalStateException("button "+id);
    }
    private void check(String key, boolean ok, String detail) {
        JsonObject r=new JsonObject();r.addProperty("key",key);r.addProperty("ok",ok);r.addProperty("detail",detail);checks.add(r);
        System.out.println("ARMYGUI CHECK "+key+" ok="+ok+" "+detail);if(!ok){throw new IllegalStateException(key+": "+detail);}
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || finished){return;}
        try {
            if(step==null){step=steps.poll();if(step==null){return;}delay=step.wait;acted=false;since=System.nanoTime();System.out.println("ARMYGUI STEP "+step.name);}
            if(System.nanoTime()-since>120000000000L){throw new IllegalStateException("Timeout "+step.name);}
            if(delay-->0){return;}if(!acted){acted=true;step.act.run();if(finished){return;}}
            if(step.until.ok()){step=null;}
        } catch(Exception | LinkageError ex){ex.printStackTrace();finish(false,ex.toString());}
    }
    private void finish(boolean ok,String error){
        finished=true;Minecraft mc=Minecraft.getMinecraft();
        try {
            if(mc.world!=null){mc.world.sendQuittingDisconnectingPacket();mc.loadWorld(null);}mc.displayGuiScreen(new GuiMainMenu());
            JsonObject p=new JsonObject();p.addProperty("success",ok);p.addProperty("error",error);p.addProperty("phase",job.getProperty("phase"));p.add("checks",checks);p.add("data",data);p.add("screenshots",images);
            try(Writer w=Files.newBufferedWriter(out.toPath(),StandardCharsets.UTF_8)){new GsonBuilder().setPrettyPrinting().create().toJson(p,w);}
            System.out.println("ARMYGUI DONE success="+ok);
        }catch(Exception ex){ex.printStackTrace();}mc.shutdown();
    }
}
