package si.ladja.xaerofleet.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import si.ladja.xaerofleet.XaeroFleetCommandMod;
import si.ladja.xaerofleet.client.xaero.MapViewTransform;
import si.ladja.xaerofleet.network.*;
import xaero.map.gui.GuiMap;

/** Same map gestures as the fleet, with army-only identity and formation settings. */
public final class ArmyMapLayer {
    private MapViewTransform view;
    private int ticks=40,shape=4;
    private double speed=1.0,boxX,boxY;
    private boolean engage=true,boxing,add,tookPress,tookRight,emptyPress;
    private double pressX,pressY;
    private UUID lastClick;
    private long lastClickAt,lastOrder;
    private final Map<UUID,double[]> targets=new HashMap<>();
    private static final String[] SHAPES={"Legion","Column","Ring","Swarm","Free for all"};
    private static final int COLOR=0xFF9BE38A;

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        if(++ticks>=40 && Minecraft.getMinecraft().currentScreen instanceof GuiMap
                && XaeroFleetCommandMod.serverSupportsArmies() && MapViewTransform.isSupported()){
            ticks=0;FleetNetwork.CHANNEL.sendToServer(new ArmyRequest());
        }
        if(!ArmyClient.available() && MapMode.isArmyMap())MapMode.setMode(0);
        if(!MapMode.isArmyMap())boxing=tookPress=tookRight=emptyPress=false;
        targets.keySet().retainAll(ArmyClient.commandable());
    }
    @SubscribeEvent public void open(GuiOpenEvent e){
        view=null;boxing=tookPress=tookRight=emptyPress=false;
        if(!(e.getGui() instanceof GuiMap))targets.clear();
    }
    @SubscribeEvent public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e){
        Minecraft.getMinecraft().addScheduledTask(()->{ArmyClient.clear();targets.clear();view=null;});
    }
    private boolean active(){return MapMode.isArmyMap() && ArmyClient.available() && XaeroFleetCommandMod.serverSupportsArmies();}
    @SubscribeEvent(priority=EventPriority.LOW) public void draw(GuiScreenEvent.DrawScreenEvent.Post e){
        if(!(e.getGui() instanceof GuiMap) || !active())return;
        GuiMap map=(GuiMap)e.getGui();view=MapViewTransform.read(map);if(view==null)return;
        Minecraft mc=Minecraft.getMinecraft();double mx=Mouse.getX()/view.guiScale,my=(mc.displayHeight-Mouse.getY()-1)/view.guiScale;
        ArmyMarker hover=null;
        for(ArmyMarker m:ArmyClient.markers()){
            if(m.dimension!=view.dimension)continue;
            int x=view.screenX(m.x),y=view.screenY(m.z);
            if(x<2 || x>map.width-20 || y<25 || y>map.height-32)continue;
            UUID id=ArmyClient.id(m.name);boolean selected=ArmyClient.selection().contains(id);
            double[] target=targets.get(id);
            if(selected && target!=null)MapDraw.line(x,y,view.screenX(target[0]),view.screenY(target[1]),1,0x709BE38A,0,0);
            int color=selected?0xFFFFD54A:m.commandable?COLOR:0xFF9AA7BB;
            boolean main="main".equals(m.kind),known="last_known".equals(m.kind);
            int size=main?9:6;
            // A pole and rectangular flag; smaller flags distinguish detached clusters.
            MapDraw.rect(x-1,y-7,x+1,y+8,color);MapDraw.frame(x+1,y-7,x+size,y-1,1,color);
            if(!known)MapDraw.rect(x+2,y-6,x+size-1,y-2,color);
            mc.fontRenderer.drawStringWithShadow(known?"?":Integer.toString(m.members),x+size+2,y-6,color);
            if(selected)MapDraw.frame(x-4,y-10,x+size+3,y+10,1,color);
            if(!panel(mx,my) && Math.abs(mx-x)<16 && Math.abs(my-y)<12)hover=m;
        }
        if(boxing){
            if(!Mouse.isButtonDown(0))boxing=false;
            else MapDraw.frame(boxX,boxY,mx,my,1,0xFFFFD54A);
        }
        MapDraw.rect(6,27,174,204,0xE6192532);
        text(mc,"ARMIES "+ArmyClient.selection().size()+"/16",12,33,COLOR);
        text(mc,"Click flag; Shift adds",12,46,0xDDE4E8);
        if(shape==4 && !XaeroFleetCommandMod.serverSupportsFreeForAll())shape=0;
        button(mc,0,"Shape: "+SHAPES[shape]);button(mc,1,"Speed: "+speed);
        button(mc,2,"New order: "+(engage?"Engage":"Passive"));
        button(mc,3,"Apply stance to selection");button(mc,4,"Stop selection [X]");
        text(mc,"Right click: move; Ctrl+drag: box",12,160,0xBCC8D0);
        text(mc,"Ctrl+A all; Ctrl+1..9 save",12,172,0xBCC8D0);
        text(mc,"1..9 recall; Space centre",12,184,0xBCC8D0);
        text(mc,"Line = last requested destination",12,195,0xBCC8D0);
        if(hover!=null){
            List<String> lines=Arrays.asList(hover.name+" / "+hover.owner,hover.kind+": "+hover.members+" (army loaded "+hover.loaded+")",
                    hover.state+" "+hover.mode+" "+hover.shape+" "+hover.stance,
                    "Omitted: "+hover.omittedClusters+" clusters / "+hover.omittedMembers+" members",
                    "Last seen: "+hover.lastSeen+(hover.commandable?" / command":" / view only"));
            net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(lines,(int)mx,(int)my,map.width,map.height,300,mc.fontRenderer);
        }
    }
    private static void text(Minecraft m,String text,int x,int y,int c){m.fontRenderer.drawStringWithShadow(text,x,y,c);}
    private static boolean panel(double x,double y){return x>=6 && x<174 && y>=27 && y<204;}
    private static void button(Minecraft mc,int i,String label){int y=60+i*19;MapDraw.rect(11,y,169,y+16,0x66354F60);text(mc,label,15,y+4,0xE3F2E0);}
    private ArmyMarker hit(double x,double y){
        ArmyMarker found=null;double best=250;
        for(ArmyMarker m:ArmyClient.markers())if(m.dimension==view.dimension && m.commandable){
            double d=Math.pow(view.screenX(m.x)-x,2)+Math.pow(view.screenY(m.z)-y,2);
            if(d<best){found=m;best=d;}
        }return found;
    }
    private void select(Collection<UUID> ids,SelectionModel.Mode mode){
        int dropped=ArmyClient.selection().apply(ids,mode);
        if(dropped>0)MapToast.show("At most 16 armies: "+dropped+" left out",MapToast.INFO);
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public void mouse(GuiScreenEvent.MouseInputEvent.Pre e){
        if(!(e.getGui() instanceof GuiMap) || !active() || view==null)return;
        Minecraft mc=Minecraft.getMinecraft();double x=Mouse.getEventX()/view.guiScale,y=(mc.displayHeight-Mouse.getEventY()-1)/view.guiScale;
        int b=Mouse.getEventButton();boolean down=Mouse.getEventButtonState();
        if(b==0 && !down && tookPress){tookPress=false;e.setCanceled(true);return;}
        if(b==1 && !down && tookRight){tookRight=false;e.setCanceled(true);return;}
        if(b==0 && !down && emptyPress){
            emptyPress=false;
            if(Math.hypot(x-pressX,y-pressY)*view.guiScale<5 && !GuiScreen.isShiftKeyDown())ArmyClient.selection().clear();
        }
        if(y<24)return; // Mode switch owns the full gesture.
        if(boxing){
            if(b==0 && !down){
                boxing=false;List<UUID> ids=new ArrayList<>();
                for(ArmyMarker m:ArmyClient.markers())if(m.commandable && m.dimension==view.dimension
                        && SelectionModel.inBox(view.screenX(m.x),view.screenY(m.z),boxX,boxY,x,y))ids.add(ArmyClient.id(m.name));
                select(new LinkedHashSet<>(ids),add?SelectionModel.Mode.ADD:SelectionModel.Mode.REPLACE);
            }e.setCanceled(true);return;
        }
        if(panel(x,y)){
            if(b==0 && down){tookPress=true;int i=(int)((y-60)/19);
                if(y>=60 && i<5){
                    if(i==0)shape=(shape+1)%(XaeroFleetCommandMod.serverSupportsFreeForAll()?5:4);
                    if(i==1)speed=speed>=6?0.5:speed+0.5;
                    if(i==2)engage=!engage;
                    if(i==3)send(ArmyAction.ENGAGEMENT,0,0);
                    if(i==4)send(ArmyAction.CANCEL,0,0);
                }
            }e.setCanceled(true);return;
        }
        if(b==0 && down){
            if(GuiScreen.isCtrlKeyDown()){boxing=true;emptyPress=false;add=GuiScreen.isShiftKeyDown();boxX=x;boxY=y;e.setCanceled(true);return;}
            ArmyMarker m=hit(x,y);
            if(m!=null){
                UUID id=ArmyClient.id(m.name);long now=System.currentTimeMillis();
                List<UUID> ids=new ArrayList<>();ids.add(id);
                if(id.equals(lastClick) && now-lastClickAt<320)for(ArmyMarker a:ArmyClient.markers())
                    if(a.commandable && a.dimension==view.dimension && a.owner.equals(m.owner))ids.add(ArmyClient.id(a.name));
                select(new LinkedHashSet<>(ids),GuiScreen.isShiftKeyDown()?SelectionModel.Mode.TOGGLE:SelectionModel.Mode.REPLACE);
                lastClick=id;lastClickAt=now;tookPress=true;e.setCanceled(true);
            }else{
                emptyPress=true;pressX=x;pressY=y;
            }
        }
        if(b==1 && down && !ArmyClient.selection().isEmpty()){
            send(ArmyAction.MOVE,view.worldX(x),view.worldZ(y));tookRight=true;e.setCanceled(true);
        }
    }
    private void send(int action,double x,double z){
        if(view==null)return;
        List<String> names=ArmyClient.names();if(names.isEmpty())return;
        long now=System.currentTimeMillis();
        if(now-lastOrder<650){MapToast.show("Wait a moment before the next army order",MapToast.INFO);return;}
        lastOrder=now;
        FleetNetwork.CHANNEL.sendToServer(new ArmyAction(action,names,view.dimension,x,z,shape,speed,engage));
        for(UUID id:ArmyClient.selection().ids()){
            if(action==ArmyAction.MOVE)targets.put(id,new double[]{x,z});
            if(action==ArmyAction.CANCEL)targets.remove(id);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public void key(GuiScreenEvent.KeyboardInputEvent.Pre e){
        if(!(e.getGui() instanceof GuiMap) || !active() || view==null || !Keyboard.getEventKeyState()
                || ((GuiMap)e.getGui()).getFocused()!=null)return;
        SelectionModel selection=ArmyClient.selection();int key=Keyboard.getEventKey();boolean ctrl=GuiScreen.isCtrlKeyDown();
        if(key==Keyboard.KEY_ESCAPE && (!selection.isEmpty() || boxing)){selection.clear();boxing=false;e.setCanceled(true);}
        if(key==Keyboard.KEY_A && ctrl){select(ArmyClient.commandable(),SelectionModel.Mode.REPLACE);e.setCanceled(true);}
        if(key==Keyboard.KEY_X && !selection.isEmpty()){send(ArmyAction.CANCEL,0,0);e.setCanceled(true);}
        if(key==Keyboard.KEY_SPACE && !selection.isEmpty()){
            double x=0,z=0;int n=0;
            for(ArmyMarker m:ArmyClient.markers())if(selection.contains(ArmyClient.id(m.name)) && m.dimension==view.dimension){x+=m.x;z+=m.z;n++;}
            if(n>0)view.centreOn((GuiMap)e.getGui(),x/n,z/n);e.setCanceled(true);
        }
        if(key>=Keyboard.KEY_1 && key<=Keyboard.KEY_9){
            int group=key-Keyboard.KEY_1+1;
            if(ctrl)selection.saveGroup(group);else selection.recallGroup(group,GuiScreen.isShiftKeyDown(),ArmyClient.commandable());
            e.setCanceled(true);
        }
    }
}
