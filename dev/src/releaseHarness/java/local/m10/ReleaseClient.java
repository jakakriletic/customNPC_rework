package local.m10;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ImageDownloadAlt;
import noppes.npcs.client.gui.player.*;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityNPCInterface;

/** Test-only input driver shared byte-for-byte by O and K. It uses real interaction,
 * GUI mouse handlers, vanilla container clicks and the real CNPC Netty channel.
 * Reflection only observes state or invokes protected vanilla mouse entry points. */
public final class ReleaseClient {
    final Properties job=ReleaseHarness.job;
    final JsonArray checks=new JsonArray(), screenshots=new JsonArray(), textures=new JsonArray();
    final Set<String> acks=new HashSet<>();
    String phase="MENU", afterAck, error="";
    long since=System.nanoTime(); int delay=20, skinFrames, reload;
    boolean done; Set<String> textureKeys;
    public static void install() { MinecraftForge.EVENT_BUS.register(new ReleaseClient()); }
    void enter(String next,int wait) { phase=next; since=System.nanoTime(); delay=wait; System.out.println("M101D CLIENT PHASE "+phase); }
    void check(String key,boolean ok,String detail) {
        JsonObject r=new JsonObject(); r.addProperty("key",key); r.addProperty("ok",ok); r.addProperty("detail",detail);
        checks.add(r); if(!ok) { throw new IllegalStateException(key+": "+detail); }
    }
    void command(String stage,String next) {
        afterAck=stage; acks.remove(stage); Minecraft.getMinecraft().player.sendChatMessage("/m10probe "+stage);
        enter("ACK:"+next,10);
    }
    @SubscribeEvent public void chat(ClientChatReceivedEvent e) {
        String text=e.getMessage().getUnformattedText();
        String prefix="M101D ACK ";
        int i=text.indexOf(prefix); if(i>=0) { acks.add(text.substring(i+prefix.length()).trim()); }
        if(text.contains("M101D FAILED")) { error=text; }
    }
    @SubscribeEvent public void rendered(RenderLivingEvent.Post<?> e) {
        if(e.getEntity().getName().equals("M101D_Skin")) { skinFrames++; }
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||done) { return; }
        Minecraft mc=Minecraft.getMinecraft();
        try {
            if(!error.isEmpty()) { throw new IllegalStateException(error); }
            if(System.nanoTime()-since>180000000000L) { throw new IllegalStateException("Timeout "+phase+" screen="+mc.currentScreen); }
            if(delay-->0) { return; }
            if(phase.startsWith("ACK:")) {
                if(acks.contains(afterAck)) { enter(phase.substring(4),20); } return;
            }
            switch(phase) {
                case "MENU":
                    if(!(mc.currentScreen instanceof GuiMainMenu)) { break; }
                    mc.gameSettings.pauseOnLostFocus=false;
                    if(job.containsKey("connect")) {
                        mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,"127.0.0.1",Integer.parseInt(job.getProperty("connect"))));
                    } else {
                        boolean verify="verify".equals(job.getProperty("pass"));
                        WorldSettings ws=verify?null:new WorldSettings("customnpc-rework".hashCode(),GameType.CREATIVE,false,false,WorldType.FLAT).enableCommands();
                        mc.launchIntegratedServer(job.getProperty("world"),job.getProperty("world"),ws);
                    }
                    enter("JOIN",100); break;
                case "JOIN":
                    if(mc.world==null||mc.player==null||mc.currentScreen!=null) { break; }
                    // No explicit sync request here: this must be the actual login sync.
                    if(!DialogController.instance.categories.containsKey(ReleaseHarness.CAT)||!QuestController.instance.categories.containsKey(ReleaseHarness.CAT)) { break; }
                    check("login-sync-dialogs",DialogController.instance.categories.get(ReleaseHarness.CAT).dialogs.size()==3,"three dialogs");
                    check("login-sync-quests",QuestController.instance.categories.get(ReleaseHarness.CAT).quests.containsKey(ReleaseHarness.QUEST),"quest present");
                    check("login-sync-factions",!FactionController.instance.factions.isEmpty(),"faction categories");
                    check("connection-mode",mc.isIntegratedServerRunning()==!job.containsKey("connect"),job.getProperty("case"));
                    command("setup","SCENE"); break;
                case "SCENE":
                    if(actor("Offer")==null||actor("Skin")==null) { break; }
                    mc.player.inventory.currentItem=8; // empty hand, never the NPC wand
                    if("verify".equals(job.getProperty("pass"))) { command("verify","MAIL"); }
                    else { interact("Locked"); enter("DENIED",40); } break;
                case "DENIED":
                    check("locked-dialog-no-gui",mc.currentScreen==null,"level 0 cannot open level 100 dialog");
                    command("locked","OFFER"); break;
                case "OFFER": interact("Offer"); enter("OFFER_GUI",20); break;
                case "OFFER_GUI":
                    if(!(mc.currentScreen instanceof GuiDialogInteract)) { break; }
                    check("offer-dialog-id",dialog().id==ReleaseHarness.OFFER,"correct dialog");
                    snap("offer"); clickDialog(); command("accepted","PROGRESS"); break;
                case "PROGRESS": interact("Progress"); enter("PROGRESS_GUI",20); break;
                case "PROGRESS_GUI":
                    if(!(mc.currentScreen instanceof GuiDialogInteract)) { break; }
                    check("progress-dialog-id",dialog().id==ReleaseHarness.PROGRESS,"correct dialog");
                    snap("progress"); clickDialog(); command("progress","COMPLETE"); break;
                case "COMPLETE": interact("Offer"); enter("COMPLETE_GUI",20); break;
                case "COMPLETE_GUI":
                    if(!(mc.currentScreen instanceof GuiQuestCompletion)) { break; }
                    snap("complete"); GuiNPCInterface q=(GuiNPCInterface)mc.currentScreen;
                    mouse(q,q.getButton(0).x+2,q.getButton(0).y+2); command("reward","TRADE"); break;
                case "TRADE": interact("Trader"); enter("BUY",20); break;
                case "BUY":
                    if(!(mc.currentScreen instanceof GuiNPCTrader)) { break; }
                    check("trader-window",mc.player.openContainer.inventorySlots.size()==54,"18 trades plus 36 inventory");
                    snap("trader"); slot(0); enter("PLACE_APPLE",20); break;
                case "PLACE_APPLE": inventory(8); command("bought","SELL"); break;
                case "SELL": slot(1); enter("PLACE_EMERALD",20); break;
                case "PLACE_EMERALD": inventory(0); command("sold","BANK"); break;
                case "BANK": closeContainer(); interact("Bank"); enter("DEPOSIT_PICK",30); break;
                case "DEPOSIT_PICK":
                    if(!(mc.currentScreen instanceof GuiNPCBankChest)) { break; }
                    check("bank-window",mc.player.openContainer.inventorySlots.size()==63,"27 bank plus 36 inventory");
                    snap("bank"); inventory(1); enter("DEPOSIT_PUT",20); break;
                case "DEPOSIT_PUT": slot(0); command("deposit","REOPEN_BANK"); break;
                case "REOPEN_BANK": closeContainer(); enter("BANK_AGAIN",20); break;
                case "BANK_AGAIN": interact("Bank"); enter("WITHDRAW_PICK",30); break;
                case "WITHDRAW_PICK":
                    if(!(mc.currentScreen instanceof GuiNPCBankChest)) { break; }
                    slot(0); enter("WITHDRAW_PUT",20); break;
                case "WITHDRAW_PUT": inventory(1); command("withdraw","SKIN"); break;
                case "SKIN":
                    closeContainer(); mc.player.rotationYaw=180; mc.player.rotationPitch=0;
                    enter("SKIN_WAIT",100); break;
                case "SKIN_WAIT":
                    EntityNPCInterface n=actor("Skin");
                    if(n==null||n.textureLocation==null||skinFrames<10) { break; }
                    Object texture=mc.getTextureManager().getTexture(n.textureLocation);
                    if(!(texture instanceof ImageDownloadAlt)) { break; }
                    Thread thread=(Thread)field(texture,"imageThread");
                    if(thread==null||thread.isAlive()) { break; }
                    check("skin-download-failed",field(texture,"bufferedImage")==null,"real downloader finished without an image");
                    check("render-continues",skinFrames>=10,"rendered skin NPC frames="+skinFrames);
                    snap("skin-fallback"); sampleTextures(); enter("RELOAD",20); break;
                case "RELOAD":
                    // The same public operation called by Minecraft's F3+T handler.
                    mc.refreshResources(); reload++; enter("RELOADED",100); break;
                case "RELOADED":
                    Set<String> now=sampleTextures();
                    if(reload==1) { textureKeys=now; enter("RELOAD",20); }
                    else {
                        check("reload-texture-keys-stable",now.equals(textureKeys),"registered texture keys after two resource reloads");
                        check("reload-render-alive",skinFrames>=20,"frames="+skinFrames); snap("reload");
                        command("save","MALFORMED");
                    } break;
                case "MALFORMED":
                    // Truncated packet body reaches the real handler on the real connection.
                    CustomNpcs.Channel.sendToServer(new FMLProxyPacket(new PacketBuffer(Unpooled.buffer().writeByte(1)),"CustomNPCs"));
                    enter("POST_PACKET",40); break;
                case "POST_PACKET": command("alive","FINISH"); break;
                case "MAIL":
                    net.minecraft.util.math.BlockPos box=new net.minecraft.util.math.BlockPos(0,4,1);
                    mc.playerController.processRightClickBlock(mc.player,mc.world,box,EnumFacing.SOUTH,
                            new net.minecraft.util.math.Vec3d(0.5,4.5,2),EnumHand.MAIN_HAND);
                    enter("MAIL_GUI",30); break;
                case "MAIL_GUI":
                    if(!(mc.currentScreen instanceof GuiMailbox)) { break; }
                    PlayerMailData data=(PlayerMailData)field(mc.currentScreen,"data");
                    if(data==null) { break; }
                    check("mail-gui-delivered",data.playermail.size()==1&&"M101D_Offline".equals(data.playermail.get(0).subject),"server MailGet response");
                    snap("offline-mail"); enter("FINISH",20); break;
                case "FINISH": finish(true,""); break;
                default: throw new IllegalStateException(phase);
            }
        } catch(Exception|LinkageError e) { e.printStackTrace(); finish(false,e.toString()); }
    }
    EntityNPCInterface actor(String suffix) {
        Minecraft mc=Minecraft.getMinecraft();
        for(Entity e:mc.world.loadedEntityList) { if(e instanceof EntityNPCInterface&&e.getName().equals("M101D_"+suffix)) { return (EntityNPCInterface)e; } }
        return null;
    }
    void interact(String suffix) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.currentScreen!=null) { throw new IllegalStateException("Unexpected screen "+mc.currentScreen); }
        EntityNPCInterface n=actor(suffix); if(n==null) { throw new IllegalStateException("Missing "+suffix); }
        mc.playerController.interactWithEntity(mc.player,n,EnumHand.MAIN_HAND);
    }
    Dialog dialog() throws Exception { return (Dialog)field(Minecraft.getMinecraft().currentScreen,"dialog"); }
    void clickDialog() throws Exception {
        GuiDialogInteract g=(GuiDialogInteract)Minecraft.getMinecraft().currentScreen;
        mouse(g,g.guiLeft+2,g.guiTop+200); // first and only visible Close option
    }
    static Object field(Object object,String name) throws Exception {
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass()) {
            try { Field f=c.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch(NoSuchFieldException absent) { /* superclass */ }
        } throw new NoSuchFieldException(name);
    }
    static void input(Object object,String name,int x,int y) throws Exception {
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass()) {
            try { Method m=c.getDeclaredMethod(name,int.class,int.class,int.class); m.setAccessible(true); m.invoke(object,x,y,0); return; }
            catch(NoSuchMethodException absent) { /* superclass */ }
        } throw new NoSuchMethodException(name);
    }
    void mouse(GuiScreen gui,int x,int y) throws Exception { gui.drawScreen(x,y,0); input(gui,"mouseClicked",x,y); input(gui,"mouseReleased",x,y); }
    void slot(int id) throws Exception {
        Minecraft mc=Minecraft.getMinecraft(); GuiContainer g=(GuiContainer)mc.currentScreen;
        Slot s=mc.player.openContainer.getSlot(id);
        mouse(g,(Integer)field(g,"guiLeft")+s.xPos+4,(Integer)field(g,"guiTop")+s.yPos+4);
    }
    void inventory(int index) throws Exception {
        Minecraft mc=Minecraft.getMinecraft();
        for(Slot s:mc.player.openContainer.inventorySlots) { if(s.inventory==mc.player.inventory&&s.getSlotIndex()==index) { slot(s.slotNumber); return; } }
        throw new IllegalStateException("Inventory slot missing "+index);
    }
    void closeContainer() { Minecraft mc=Minecraft.getMinecraft(); if(mc.currentScreen!=null) { mc.player.closeScreen(); } }
    Set<String> sampleTextures() throws Exception {
        Minecraft mc=Minecraft.getMinecraft(); Map<?,?> map=(Map<?,?>)field(mc.getTextureManager(),"mapTextureObjects");
        Set<String> keys=new TreeSet<>(); for(Object k:map.keySet()) { keys.add(k.toString()); }
        JsonObject row=new JsonObject(); row.addProperty("reload",reload); row.addProperty("count",keys.size());
        row.addProperty("heapUsed",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        JsonArray names=new JsonArray(); for(String k:keys) { names.add(k); } row.add("keys",names); textures.add(row); return keys;
    }
    void snap(String label) {
        Minecraft mc=Minecraft.getMinecraft(); String name=ReleaseHarness.file("-"+job.getProperty("pass")+"-"+label+".png").getName();
        ScreenShotHelper.saveScreenshot(ReleaseHarness.file("").getParentFile(),name,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        screenshots.add(name);
    }
    void finish(boolean ok,String error) {
        done=true; Minecraft mc=Minecraft.getMinecraft();
        JsonObject proof=new JsonObject(); proof.addProperty("success",ok); proof.addProperty("error",error);
        proof.addProperty("case",job.getProperty("case")); proof.addProperty("pass",job.getProperty("pass"));
        proof.add("assertions",checks); proof.add("screenshots",screenshots); proof.add("textures",textures);
        ReleaseHarness.write(ReleaseHarness.file("-"+job.getProperty("pass")+"-client.json"),proof);
        if(mc.world!=null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
        mc.displayGuiScreen(new GuiMainMenu()); System.out.println("M101D CLIENT DONE success="+ok); mc.shutdown();
    }
}
