package local.m10;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.*;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.*;
import noppes.npcs.quests.QuestDialog;
import noppes.npcs.roles.*;

/** Test-only fixture/observer. All commands require the explicit m10job launch property.
 * UI outcomes are observed, never manufactured by this observer. */
@Mod(modid="m10harness", name="M10 release diagnostic", version="1", acceptableRemoteVersions="*", dependencies="required-after:customnpcs")
public final class ReleaseHarness {
    static final int CAT=61000, OFFER=61001, PROGRESS=61002, LOCKED=61003, QUEST=61001, BANK=61001;
    static Properties job;
    static final JsonArray checks = new JsonArray();

    @Mod.EventHandler public void init(FMLInitializationEvent e) throws Exception {
        String path=System.getProperty("m10job");
        if(path==null) { throw new IllegalStateException("Test-only mod requires m10job"); }
        job=new Properties();
        try(Reader r=Files.newBufferedReader(new File(path).toPath(), StandardCharsets.UTF_8)) { job.load(r); }
        boolean original="O".equals(job.getProperty(FMLCommonHandler.instance().getSide().isClient()?"client":"server"));
        boolean patched;
        try { Class.forName("noppes.npcs.rework.core.RwCoreMod",false,getClass().getClassLoader()); patched=true; }
        catch(ClassNotFoundException absent) { patched=false; }
        if(original==patched) { throw new IllegalStateException("Wrong CNPC arm or rework classpath leakage"); }
        System.out.println("M101D ARM original="+original+" cnpc="+CustomNpcs.class.getProtectionDomain().getCodeSource().getLocation());
        if(FMLCommonHandler.instance().getSide().isClient()) {
            Class.forName("local.m10.ReleaseClient").getMethod("install").invoke(null);
        }
    }
    @Mod.EventHandler public void starting(FMLServerStartingEvent e) {
        e.registerServerCommand(new ProbeCommand());
    }
    @Mod.EventHandler public void started(FMLServerStartedEvent e) {
        // Controllers are loaded before the server-started event and before the first login.
        if(!DialogController.instance.categories.containsKey(CAT)) { fixtures(); }
        System.out.println("M101D FIXTURES READY");
    }
    static void fixtures() {
        DialogCategory dc=new DialogCategory(); dc.id=CAT; dc.title="M101D_Dialogs";
        DialogController.instance.saveCategory(dc);
        QuestCategory qc=new QuestCategory(); qc.id=CAT; qc.title="M101D_Quests";
        QuestController.instance.saveCategory(qc);
        Quest q=new Quest(qc); q.id=QUEST; q.title="M101D_Quest"; q.logText="Read progress dialog";
        q.completeText="One diamond reward"; q.completerNpc="M101D_Offer"; q.setType(1);
        ((QuestDialog)q.questInterface).dialogs.put(0,PROGRESS);
        q.rewardItems.setInventorySlotContents(0,new ItemStack(Items.DIAMOND,1));
        QuestController.instance.saveQuest(qc,q);
        for(int id:new int[]{OFFER,PROGRESS,LOCKED}) {
            Dialog d=new Dialog(dc); d.id=id; d.title="M101D_"+id; d.text="Release matrix "+id;
            if(id==OFFER) { d.quest=QUEST; }
            if(id==LOCKED) { d.availability.minPlayerLevel=100; }
            DialogOption close=new DialogOption(); close.optionType=0; close.title="Close"; d.options.put(0,close);
            DialogController.instance.saveDialog(dc,d);
        }
        Bank b=new Bank(); b.id=BANK; b.name="M101D_Bank"; BankController.getInstance().saveBank(b);
    }
    static File file(String suffix) { return new File(job.getProperty("base")+suffix); }
    static void check(String key,boolean ok,String detail) {
        JsonObject r=new JsonObject(); r.addProperty("key",key); r.addProperty("ok",ok); r.addProperty("detail",detail);
        checks.add(r); System.out.println("M101D SERVER CHECK "+key+" ok="+ok+" "+detail);
        write(file("-"+job.getProperty("pass")+"-server.json"),checks);
        if(!ok) { throw new IllegalStateException(key+": "+detail); }
    }
    static void write(File f,JsonElement value) {
        try(Writer w=Files.newBufferedWriter(f.toPath(),StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(value,w); }
        catch(IOException e) { throw new IllegalStateException(e); }
    }
    static int count(EntityPlayerMP p,Item item) {
        int n=0; for(ItemStack s:p.inventory.mainInventory) { if(s.getItem()==item) { n+=s.getCount(); } }
        if(p.inventory.getItemStack().getItem()==item) { n+=p.inventory.getItemStack().getCount(); } return n;
    }
    static EntityNPCInterface actor(MinecraftServer s,String name) {
        for(Entity e:s.getWorld(0).loadedEntityList) { if(e instanceof EntityNPCInterface && name.equals(e.getName())) { return (EntityNPCInterface)e; } }
        return null;
    }
    static void spawn(MinecraftServer s,String name,int dialog,int role,double x) {
        if(actor(s,name)!=null) { return; }
        // P2: load the target chunk first; a spawn into an unloaded chunk is silently refused.
        s.getWorld(0).getChunkFromChunkCoords(net.minecraft.util.math.MathHelper.floor(x)>>4,0);
        EntityCustomNpc n=new EntityCustomNpc(s.getWorld(0)); n.display.setName(name); n.setPosition(x,4,0);
        n.modelData.eyes.color=8339378; // P2: fixed palette colour instead of the random constructor pick
        n.ais.setMovingType(0); n.advanced.setRole(role); n.advanced.attackOtherFactions=false;
        if(dialog>0) { DialogOption o=new DialogOption(); o.dialogId=dialog; n.dialogs.put(0,o); }
        if(role==1) {
            RoleTrader t=(RoleTrader)n.roleInterface;
            t.inventoryCurrency.setInventorySlotContents(0,new ItemStack(Items.EMERALD,2));
            t.inventorySold.setInventorySlotContents(0,new ItemStack(Items.APPLE,3));
            t.inventoryCurrency.setInventorySlotContents(1,new ItemStack(Items.APPLE,3));
            t.inventorySold.setInventorySlotContents(1,new ItemStack(Items.EMERALD,2));
        }
        if(role==3) { ((RoleBank)n.roleInterface).bankId=BANK; }
        if(name.endsWith("Skin")) { n.display.setSkinUrl("http://127.0.0.1:9/m101d-missing.png"); }
        if(!s.getWorld(0).spawnEntity(n)) { throw new IllegalStateException("spawn refused "+name); }
    }
    static void snapshot(PlayerData d,File f) throws IOException {
        CompressedStreamTools.write(d.getNBT(),f);
    }
    static final class ProbeCommand extends CommandBase {
        @Override public String getName() { return "m10probe"; }
        @Override public String getUsage(ICommandSender s) { return "/m10probe <stage>"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public void execute(MinecraftServer s,ICommandSender sender,String[] args) throws CommandException {
            try {
                String stage=args[0];
                if("offline".equals(stage)) {
                    String name=args[1];
                    boolean absent=s.getPlayerList().getPlayerByUsername(name)==null;
                    JsonObject row=new JsonObject(); row.addProperty("key","mail-player-offline"); row.addProperty("ok",absent); row.addProperty("detail",name);
                    JsonArray offline=new JsonArray(); offline.add(row); write(file("-offline-server.json"),offline);
                    if(!absent) { throw new IllegalStateException("mail recipient still online"); }
                    PlayerMail m=new PlayerMail(); m.subject="M101D_Offline"; m.sender="Release matrix";
                    NBTTagList pages=new NBTTagList(); pages.appendTag(new NBTTagString("Queued while disconnected")); m.message.setTag("pages",pages);
                    PlayerDataController.instance.addPlayerMessage(s,name,m);
                    System.out.println("M101D OFFLINE MAIL QUEUED"); return;
                }
                EntityPlayerMP p=getCommandSenderAsPlayer(sender); PlayerData d=PlayerData.get(p);
                switch(stage) {
                    case "resync":
                        // Original-client arm only (P2, Q251): the same login sync the server sent once.
                        if(!"O".equals(job.getProperty("client"))) { throw new IllegalStateException("resync only for the original client"); }
                        SyncController.syncPlayer(p); break;
                    case "setup":
                        p.setGameType(net.minecraft.world.GameType.SURVIVAL); p.setHealth(p.getMaxHealth());
                        p.connection.setPlayerLocation(0,4,3,180,0); p.experienceLevel=0;
                        s.getWorld(0).getGameRules().setOrCreateGameRule("doMobSpawning","false");
                        s.getWorld(0).setWorldTime(1000);
                        if("edit".equals(job.getProperty("pass"))) {
                            p.inventory.clear(); p.inventory.setInventorySlotContents(0,new ItemStack(Items.EMERALD,8));
                            p.inventory.setInventorySlotContents(1,new ItemStack(Items.GOLD_INGOT,5));
                            spawn(s,"M101D_Offer",OFFER,0,0); spawn(s,"M101D_Progress",PROGRESS,0,2);
                            spawn(s,"M101D_Locked",LOCKED,0,-2); spawn(s,"M101D_Trader",0,1,3);
                            spawn(s,"M101D_Bank",0,3,-3);
                            spawn(s,"M101D_Skin",0,0,1);
                            s.getWorld(0).setBlockState(new net.minecraft.util.math.BlockPos(0,4,1),CustomItems.mailbox.getDefaultState());
                        }
                        check("fixture-categories",DialogController.instance.categories.containsKey(CAT)&&QuestController.instance.categories.containsKey(CAT),"loaded");
                        break;
                    case "locked":
                        check("dialog-denied",d.dialogId!=LOCKED&&!d.dialogData.dialogsRead.contains(LOCKED),"level="+p.experienceLevel); break;
                    case "accepted":
                        check("quest-accepted",d.questData.activeQuests.containsKey(QUEST),"via offer interact");
                        check("quest-incomplete",!QuestController.instance.quests.get(QUEST).questInterface.isCompleted(p),"progress unread"); break;
                    case "progress":
                        check("quest-progress",QuestController.instance.quests.get(QUEST).questInterface.isCompleted(p),"dialog read"); break;
                    case "reward":
                        check("quest-finished",d.questData.finishedQuests.containsKey(QUEST)&&!d.questData.activeQuests.containsKey(QUEST),"completion click");
                        check("quest-reward",count(p,Items.DIAMOND)==1,"diamonds="+count(p,Items.DIAMOND)); break;
                    case "bought":
                        check("trader-buy",count(p,Items.EMERALD)==6&&count(p,Items.APPLE)==3,"emerald="+count(p,Items.EMERALD)+" apple="+count(p,Items.APPLE)); break;
                    case "sold":
                        check("trader-sell",count(p,Items.EMERALD)==8&&count(p,Items.APPLE)==0,"emerald="+count(p,Items.EMERALD)+" apple="+count(p,Items.APPLE)); break;
                    case "deposit":
                        check("bank-deposit",count(p,Items.GOLD_INGOT)==0&&bankGold(p)==5,"inventory="+count(p,Items.GOLD_INGOT)+" bank="+bankGold(p)); break;
                    case "withdraw":
                        check("bank-withdraw",count(p,Items.GOLD_INGOT)==5&&bankGold(p)==0,"inventory="+count(p,Items.GOLD_INGOT)+" bank="+bankGold(p)); break;
                    case "save":
                        check("final-items",count(p,Items.EMERALD)==8&&count(p,Items.GOLD_INGOT)==5&&count(p,Items.DIAMOND)==1&&p.inventory.getItemStack().isEmpty(),"cursor empty and balances");
                        d.save(false); snapshot(d,file("-before.dat"));
                        Files.write(file("-player.txt").toPath(),p.getName().getBytes(StandardCharsets.UTF_8)); break;
                    case "verify":
                        NBTTagCompound before=CompressedStreamTools.read(file("-before.dat"));
                        NBTTagCompound after=d.getNBT(); snapshot(d,file("-after.dat"));
                        check("offline-mail-delivered",d.mailData.playermail.size()==1&&"M101D_Offline".equals(d.mailData.playermail.get(0).subject),"count="+d.mailData.playermail.size());
                        // The only intentional change between sessions is the new offline mail.
                        before.removeTag("MailData"); after.removeTag("MailData");
                        check("playerdata-equal",before.equals(after),"all CNPC NBT except separately checked MailData");
                        check("items-relogin",count(p,Items.EMERALD)==8&&count(p,Items.GOLD_INGOT)==5&&count(p,Items.DIAMOND)==1,"inventory persisted");
                        check("bank-relogin",bankGold(p)==0,"no duplication after restart"); break;
                    case "alive": check("post-malformed-alive",p.connection!=null,"normal command accepted"); break;
                    default: throw new IllegalArgumentException(stage);
                }
                p.sendMessage(new TextComponentString("M101D ACK "+stage));
            } catch(Exception|LinkageError e) {
                e.printStackTrace(); sender.sendMessage(new TextComponentString("M101D FAILED "+e));
                throw new CommandException("M101D FAILED "+e);
            }
        }
        private int bankGold(EntityPlayerMP p) {
            BankData b=PlayerDataController.instance.getBankData(p,BANK).getBankOrDefault(BANK);
            int n=0; for(ItemStack st:b.itemSlots.get(0).items) { if(st.getItem()==Items.GOLD_INGOT) { n+=st.getCount(); } } return n;
        }
    }
}
