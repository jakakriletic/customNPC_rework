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
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.rework.army.*;

/** M11.8b controller-only: the Permissions tab of the staff through real mouse/key handlers over a real
 * connection. Rights change only through tab clicks; one forged packet is a negative check. The console
 * deop is setup (the harness waits for the client's own op level to drop), the server text is the proof.
 */
@SideOnly(Side.CLIENT)
public final class ArmyPermHarness {
    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int wait; final Act act; final Cond until;
        Step(String n, int w, Act a, Cond c) { name=n; wait=w; act=a; until=c; }
    }
    private static final String ALLY = "M118B_Ally", SELF = "M118B_Player";
    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray(), images = new JsonArray();
    private final Deque<Step> steps = new ArrayDeque<Step>();
    private Step step;
    private int delay, sent, before;
    private boolean acted, finished;
    private long since;
    private ArmyPermHarness(Properties p) { job=p; out=new File(p.getProperty("out")); }
    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("armyPerm"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("connect") || !p.containsKey("phase")) { throw new IllegalArgumentException("armyPerm job"); }
        ArmyPermHarness h = new ArmyPermHarness(p); h.plan(); MinecraftForge.EVENT_BUS.register(h); return true;
    }
    private void add(String n, int w, Act a, Cond c) { steps.add(new Step(n,w,a,c)); }
    private void act(String n, Act a) { add(n, 10, a, () -> true); }
    private void cmd(String c) { act(c, () -> Minecraft.getMinecraft().player.sendChatMessage("/" + c)); }
    private GuiArmyStaff gui() { return (GuiArmyStaff)Minecraft.getMinecraft().currentScreen; }
    private void air() {
        Minecraft mc = Minecraft.getMinecraft(); mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
    }
    private void openAir() { add("open staff", 15, () -> air(), () -> Minecraft.getMinecraft().currentScreen instanceof GuiArmyStaff); }
    private static void click(GuiScreen screen, int id) throws Exception {
        Field f = GuiScreen.class.getDeclaredField("buttonList"); f.setAccessible(true);
        for (GuiButton b : (List<GuiButton>)f.get(screen)) {
            if (b.id != id || !b.visible) { continue; }
            if (!b.enabled) { throw new IllegalStateException("Disabled button " + id); }
            int x=b.x+3, y=b.y+3; screen.drawScreen(x,y,0);
            Method m=GuiScreen.class.getDeclaredMethod("mouseClicked", int.class,int.class,int.class); m.setAccessible(true); m.invoke(screen,x,y,0);
            m=GuiScreen.class.getDeclaredMethod("mouseReleased", int.class,int.class,int.class); m.setAccessible(true); m.invoke(screen,x,y,0); return;
        }
        throw new IllegalStateException("Missing or hidden button " + id);
    }
    private void click(int id) throws Exception { click(Minecraft.getMinecraft().currentScreen, id); }
    private void type(String value) throws Exception {
        Field f=GuiArmyStaff.class.getDeclaredField("name"); f.setAccessible(true); GuiTextField box=(GuiTextField)f.get(gui());
        Method m=GuiScreen.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class); m.setAccessible(true); m.invoke(gui(),box.x+3,box.y+3,0);
        m=GuiScreen.class.getDeclaredMethod("keyTyped",char.class,int.class); m.setAccessible(true);
        m.invoke(gui(),(char)0,207); // End, then backspace: normal text field input.
        for(int i=0;i<40;i++) { m.invoke(gui(),(char)0,14); }
        for(char c:value.toCharArray()) { m.invoke(gui(),c,0); }
        updateRows();
    }
    private void updateRows() throws Exception { Method m=GuiArmyStaff.class.getDeclaredMethod("updateRows"); m.setAccessible(true); m.invoke(gui()); }
    private List<String> players() throws Exception { Field f=GuiArmyStaff.class.getDeclaredField("players"); f.setAccessible(true); return (List<String>)f.get(gui()); }
    private int row(String name) throws Exception {
        List<String> list=players();
        for(int i=0;i<list.size();i++) { if(list.get(i).equalsIgnoreCase(name)) { if(i>=4) { throw new IllegalStateException("not on page 1: "+name); } return 20+i; } }
        throw new IllegalStateException("player not listed: "+name+" in "+list);
    }
    private int armyRow(String name) throws Exception {
        Field f=GuiArmyStaff.class.getDeclaredField("rows"); f.setAccessible(true);
        List<ArmySnapshot.Entry> rows=(List<ArmySnapshot.Entry>)f.get(gui());
        for(int i=0;i<rows.size();i++) { if(rows.get(i).name.equalsIgnoreCase(name) && i<4) { return 20+i; } }
        throw new IllegalStateException("army not on page 1: "+name);
    }
    private boolean batchDone() throws Exception { Field f=GuiArmyStaff.class.getDeclaredField("batch"); f.setAccessible(true); return !f.getBoolean(gui()); }
    private String reply() { return ArmyClient.staffReply()==null ? "" : ArmyClient.staffReply().text; }
    private void await(String n, Act a) {
        add(n, 15, () -> { before=ArmyClient.staffReply()==null?-1:ArmyClient.staffReply().id; a.run(); },
                () -> ArmyClient.staffReply()!=null && ArmyClient.staffReply().id!=before);
    }
    /** One click, the server answer, then the check on its ok flag and text. */
    private void button(String key, int id, boolean ok, String... contains) { clicked(key, () -> click(id), ok, contains); }
    private void clicked(String key, Act a, boolean ok, String... contains) {
        await(key, a);
        act(key+" check", () -> {
            boolean good=ArmyClient.staffReply().ok==ok; for(String c:contains) { good &= reply().contains(c); }
            check(key, good, reply().replace('\n','|'));
        });
    }
    private void selectPlayer(String key, String name, String... contains) { clicked(key, () -> click(row(name)), true, contains); }
    private void screen(String name) throws Exception {
        Minecraft mc=Minecraft.getMinecraft(); String file=out.getName().replace(".json", "")+"-"+name+".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(),file,mc.displayWidth,mc.displayHeight,mc.getFramebuffer()); images.add(file);
    }
    private void permissionsTab(String key) {
        clicked(key, () -> click(13), true, "RWARMY players n=");
        act(key+" list", () -> check(key+"-list", players().stream().anyMatch(p -> p.equalsIgnoreCase(ALLY))
                && players().stream().anyMatch(p -> p.equalsIgnoreCase(SELF)), String.valueOf(players())));
    }
    private void plan() {
        Minecraft mc=Minecraft.getMinecraft(); String phase=job.getProperty("phase");
        add("menu",0,()->{},()->mc.currentScreen instanceof GuiMainMenu);
        add("join",10,()->{ mc.gameSettings.pauseOnLostFocus=false;
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,"127.0.0.1",Integer.parseInt(job.getProperty("connect"))));
        },()->mc.world!=null && mc.player!=null && mc.currentScreen==null);
        cmd("gamemode 1"); cmd("tp @p 600 4 0 0 0");
        if("off".equals(phase)) {
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff");
            act("off use",()->air());
            act("off assertions",()->{
                check("off-no-gui",!(mc.currentScreen instanceof GuiArmyStaff),String.valueOf(mc.currentScreen));
                check("off-no-reply",ArmyClient.staffReply()==null && ArmyClient.latest()==null,"no army state or reply");
                check("off-item",mc.player.getHeldItemMainhand().getItem()==ArmyStaff.item,"stable registered staff");
            });
        } else if("verify".equals(phase)) {
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff"); openAir();
            permissionsTab("restart-players");
            selectPlayer("restart-grants",ALLY,"grants count=2","by server view=all command=-","by "+SELF+" view=all command=-");
            act("verify image",()->screen("restart")); act("verify close",()->click(12));
        } else {
            cmd("rwarmy create alpha");
            for(String army:new String[]{"beta","hidden"}) { cmd("rwarmy create "+army+" uuid:0b5a1c2e-1111-4a2b-9c3d-123456789abc"); }
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff");
            openAir();
            permissionsTab("tab-players");
            act("tab hides orders",()->check("tab-layout",!buttonVisible(9) && !buttonVisible(5) && buttonVisible(31) && buttonVisible(12),"orders hidden, rights shown"));
            act("no player yet",()->check("needs-player",!buttonById(33).enabled,"rights disabled until a player is chosen"));
            act("player pages",()->{click(4); check("players-page",buttonById(20).visible
                    && buttonById(20).displayString.toLowerCase(Locale.ROOT).contains("m118b_z3"),"cached players have a second page");click(3);});
            act("find prefix",()->type("M118B_A"));
            button("find-player",2,true,"RWARMY players n=1");
            act("find result",()->check("find-result",players().size()==1 && players().get(0).equalsIgnoreCase(ALLY),String.valueOf(players())));
            act("clear prefix",()->type(""));
            button("all-players",2,true,"RWARMY players n=6");
            act("players image",()->screen("players"));
            selectPlayer("select-ally",ALLY,"grants count=0");
            act("type beta",()->type("beta"));
            button("view-selected",32,true,"granted view beta to "+ALLY+" scope=server","by server view=beta command=-");
            button("command-all",36,true,"granted command all to "+ALLY+" scope=server","view=beta command=all");
            act("grants image",()->screen("grants"));
            button("command-none",34,true,"revoked command all from "+ALLY+" changed=1","view=beta command=-");
            button("view-none",31,true,"revoked view all from "+ALLY+" changed=1","grants count=0");
            button("view-all",33,true,"granted view all to "+ALLY+" scope=server","view=all command=-");
            button("command-selected",35,true,"granted command beta to "+ALLY,"view=all command=beta");
            button("revoke-army",37,true,"revoked all beta from "+ALLY+" changed=1 left=view","view=all command=-");
            act("type missing",()->type("nosuch"));
            button("unknown-army",32,false,"unknown army nosuch");
            act("checkbox list",()->{click(13);click(armyRow("alpha"));click(armyRow("beta"));});
            add("grant checked armies",15,()->click(35),()->batchDone() && reply().contains("command=beta"));
            act("checked result",()->check("checkbox-grants",ArmyClient.staffReply().ok && reply().contains("command=alpha")
                    && reply().contains("command=beta"),reply().replace('\n','|')));
            act("checkbox image",()->screen("checked-armies"));
            add("revoke checked armies",15,()->click(37),()->batchDone() && reply().contains("grants count=1"));
            act("checked revoke result",()->check("checkbox-revoke",ArmyClient.staffReply().ok && reply().contains("view=all command=-"),reply().replace('\n','|')));
            act("back to players",()->click(13));
            selectPlayer("select-self",SELF,"grants count=0");
            act("type alpha",()->type("alpha"));
            button("self-refused",32,false,"cannot grant to yourself");
            act("close for forged",()->click(12));
            cmd("replaceitem entity @p slot.hotbar.0 minecraft:stick");
            await("forged-no-staff",()->{ sent=ArmyClient.perm(ArmyStaffCodec.PermOp.GRANT,1,ALLY,"alpha"); });
            act("forged-no-staff check",()->check("forged-no-staff",ArmyClient.staffReply().id==sent && !ArmyClient.staffReply().ok
                    && reply().contains("hold the command staff"),reply()));
            cmd("replaceitem entity @p slot.hotbar.0 customnpcs:army_staff");
            openAir(); permissionsTab("reopen-players");
            selectPlayer("reselect-ally",ALLY,"by server view=all command=-");
            act("prepare stale checkboxes",()->{click(13);click(armyRow("beta"));click(armyRow("alpha"));click(13);});
            act("sync deop",()->System.out.println("ARMYPERM SYNC deop"));
            add("wait deop",10,()->{},()->!mc.player.canUseCommand(2,"rwarmy"));
            button("deop-own-only",30,true,"grants count=0");
            act("type foreign",()->type("beta"));
            button("deop-foreign-hidden",35,false,"unknown army beta");
            act("batch stopped",()->check("batch-stop-on-denial",batchDone() && reply().contains("grants count=0"),
                    "first denied army stops the batch; no owner grant to alpha"));
            selectPlayer("clear-stale-selection",ALLY,"grants count=0");
            act("type own",()->type("alpha"));
            button("owner-command-selected",35,true,"granted command alpha to "+ALLY+" scope=owner","by "+SELF+" view=- command=alpha");
            button("owner-command-all",36,true,"granted command all to "+ALLY+" scope=owner","by "+SELF+" view=- command=all");
            act("owner image",()->screen("owner"));
            button("owner-command-none",34,true,"revoked command all from "+ALLY+" changed=2","grants count=0");
            button("owner-view-all",33,true,"granted view all to "+ALLY+" scope=owner","by "+SELF+" view=all command=-");
            act("non-owner sync",()->System.out.println("ARMYPERM SYNC remove-owner"));
            add("wait removed army",15,()->ArmyClient.request(),()->ArmyClient.latest()!=null && ArmyClient.latest().armies.isEmpty());
            button("non-owner-no-delegation",36,false,"only the owner or an operator can grant");
            act("end main",()->{check("connection-alive",mc.player!=null && mc.getConnection()!=null,"rights changes kept connection"); click(12);});
        }
        act("finish",()->finish(true,""));
    }
    private GuiButton buttonById(int id) throws Exception {
        Field f=GuiScreen.class.getDeclaredField("buttonList"); f.setAccessible(true);
        for(GuiButton b:(List<GuiButton>)f.get(Minecraft.getMinecraft().currentScreen)) { if(b.id==id) { return b; } }
        throw new IllegalStateException("button "+id);
    }
    private boolean buttonVisible(int id) throws Exception { return buttonById(id).visible; }
    private void check(String key, boolean ok, String detail) {
        JsonObject r=new JsonObject();r.addProperty("key",key);r.addProperty("ok",ok);r.addProperty("detail",detail);checks.add(r);
        System.out.println("ARMYPERM CHECK "+key+" ok="+ok+" "+detail);if(!ok){throw new IllegalStateException(key+": "+detail);}
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || finished){return;}
        try {
            if(step==null){step=steps.poll();if(step==null){return;}delay=step.wait;acted=false;since=System.nanoTime();System.out.println("ARMYPERM STEP "+step.name);}
            if(System.nanoTime()-since>120000000000L){throw new IllegalStateException("Timeout "+step.name);}
            if(delay-->0){return;}if(!acted){acted=true;step.act.run();if(finished){return;}}
            if(step.until.ok()){step=null;}
        } catch(Exception | LinkageError ex){ex.printStackTrace();finish(false,ex.toString());}
    }
    private void finish(boolean ok,String error){
        finished=true;Minecraft mc=Minecraft.getMinecraft();
        try {
            if(mc.world!=null){mc.world.sendQuittingDisconnectingPacket();mc.loadWorld(null);}mc.displayGuiScreen(new GuiMainMenu());
            JsonObject p=new JsonObject();p.addProperty("success",ok);p.addProperty("error",error);p.addProperty("phase",job.getProperty("phase"));p.add("checks",checks);p.add("screenshots",images);
            try(Writer w=Files.newBufferedWriter(out.toPath(),StandardCharsets.UTF_8)){new GsonBuilder().setPrettyPrinting().create().toJson(p,w);}
            System.out.println("ARMYPERM DONE success="+ok);
        }catch(Exception ex){ex.printStackTrace();}mc.shutdown();
    }
}
