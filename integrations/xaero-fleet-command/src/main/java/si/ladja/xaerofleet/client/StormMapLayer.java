package si.ladja.xaerofleet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import si.ladja.xaerofleet.XaeroFleetCommandMod;
import si.ladja.xaerofleet.client.xaero.MapViewTransform;
import si.ladja.xaerofleet.network.*;
import si.ladja.xaerofleet.storm.*;
import xaero.map.gui.GuiMap;

/** Third map layer: a snapped area preview, explicit create/clear actions and storm footprints. */
public final class StormMapLayer {
    private static final int BLUE=0xFF9ADCF7,PANEL=0xE6192532;
    private MapViewTransform view;
    private GridArea selection;
    private int dimension=Integer.MIN_VALUE,anchorX,anchorZ;
    private boolean dragging,pressed,tookPanel;
    private double pressX,pressY;
    private int duration=1;
    private static final int[] DURATIONS={2,10,30};
    private int panelX=6,panelY=27,panelWidth=166;

    @SubscribeEvent public void open(GuiOpenEvent event) {
        dragging=pressed=tookPanel=false;
        if(!(event.getGui() instanceof GuiMap))view=null;
    }
    @SubscribeEvent(priority=EventPriority.LOW) public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(!(event.getGui() instanceof GuiMap) || !MapMode.isStormMap())return;
        GuiMap map=(GuiMap)event.getGui();view=MapViewTransform.read(map);if(view==null)return;
        if(dimension!=view.dimension){selection=null;dimension=view.dimension;dragging=false;}
        Minecraft mc=Minecraft.getMinecraft();
        double mx=Mouse.getX()/view.guiScale,my=(mc.displayHeight-Mouse.getY()-1)/view.guiScale;
        panelX=6;panelY=27;
        // Avoid drawing over Xaero's own lower toolbar or the mode tabs.
        double step=Math.abs(view.screenX(GridArea.CELL)-view.screenX(0));
        if(step>=10) {
            double left=view.worldX(0),right=view.worldX(map.width),top=view.worldZ(24),bottom=view.worldZ(map.height-30);
            int firstX=GridArea.cell(Math.max(-GridArea.WORLD_LIMIT+64,Math.min(GridArea.WORLD_LIMIT-64,left)));
            int firstZ=GridArea.cell(Math.max(-GridArea.WORLD_LIMIT+64,Math.min(GridArea.WORLD_LIMIT-64,top)));
            int nx=Math.min(150,(int)Math.ceil(Math.abs(right-left)/64)+2),nz=Math.min(150,(int)Math.ceil(Math.abs(bottom-top)/64)+2);
            for(int i=0;i<nx;i++){int x=view.screenX((firstX+i)*64.0);MapDraw.line(x,24,x,map.height-30,1,0x2C99D9EE,0,0);}
            for(int i=0;i<nz;i++){int y=view.screenY((firstZ+i)*64.0);MapDraw.line(0,y,map.width,y,1,0x2C99D9EE,0,0);}
        }
        for(StormRegion storm:StormClient.regions()) {
            if(storm.dimension!=dimension || storm.expires<=StormClient.now())continue;
            area(map,storm.area,0x2049ACD1,0xB090DDF7);
            int sx=view.screenX((storm.area.left()+storm.area.right())/2.0),sy=view.screenY((storm.area.top()+storm.area.bottom())/2.0);
            if(sx>0 && sx<map.width-75 && sy>26 && sy<map.height-48) {
                long seconds=Math.max(0,(storm.expires-StormClient.now())/20);
                mc.fontRenderer.drawStringWithShadow("Blizzard "+seconds/60+":"+String.format(java.util.Locale.ROOT,"%02d",seconds%60),sx,sy,0xDDF4FF);
            }
        }
        if(dragging && !Mouse.isButtonDown(0)){dragging=false;pressed=false;}
        if(dragging)selection=dragArea(view.worldX(mx),view.worldZ(my));
        if(selection!=null)area(map,selection,0x3541C8FF,BLUE);
        if(!overPanel(mx,my) && my>=24 && my<map.height-30 && !dragging) {
            try {GridArea hover=new GridArea(GridArea.cell(view.worldX(mx)),GridArea.cell(view.worldZ(my)),GridArea.cell(view.worldX(mx)),GridArea.cell(view.worldZ(my)));
                area(map,hover,0x1AFFFFFF,0x909ADCF7);
            }catch(IllegalArgumentException ignored){ }
        }
        MapDraw.rect(panelX,panelY,panelX+panelWidth,panelY+156,PANEL);
        MapDraw.rect(panelX,panelY,panelX+2,panelY+156,BLUE);
        text(mc,"WINTER STORM",7,7,0xE5F7FF);
        text(mc,"64 x 64 block grid",7,22,0xA7BCC9);
        text(mc,selection==null?"Select an area on the map":selection.width()*64+" x "+selection.depth()*64+" blocks",7,37,0xE5F7FF);
        button(mc,0,"Duration: "+DURATIONS[duration]+" min",0xFF6AAAC6,true,mx,my);
        button(mc,1,"Spawn blizzard",BLUE,selection!=null && allowed(),mx,my);
        button(mc,2,"Clear storms in area",0xFFE2A28B,selection!=null && allowed(),mx,my);
        text(mc,"Click: select one cell",7,116,0xA7BCC9);
        text(mc,"Ctrl+drag: select rectangle",7,127,0xA7BCC9);
        String state=!XaeroFleetCommandMod.serverSupportsWinter()?"Update this mod on the server":!StormClient.received()?"Connecting to storm control...":StormClient.canManage()?"Drag map normally to pan":"Storm control requires OP";
        text(mc,state,7,141,allowed()?0x9BD5BA:0xEDB596);
        GlStateManagerCompat.reset();
    }
    private boolean allowed(){return XaeroFleetCommandMod.serverSupportsWinter() && StormClient.canManage();}
    private void text(Minecraft mc,String text,int x,int y,int color){mc.fontRenderer.drawStringWithShadow(text,panelX+x,panelY+y,color);}
    private void button(Minecraft mc,int index,String label,int color,boolean enabled,double mx,double my) {
        int y=panelY+52+index*19;boolean hover=mx>=panelX+6 && mx<panelX+panelWidth-6 && my>=y && my<y+16;
        MapDraw.rect(panelX+6,y,panelX+panelWidth-6,y+16,enabled?hover?0x884D748A:0x55354F60:0x332C3842);
        MapDraw.frame(panelX+6,y,panelX+panelWidth-6,y+16,1,enabled?color:0x55667885);
        mc.fontRenderer.drawStringWithShadow(label,panelX+11,y+4,enabled?color:0x77838F);
    }
    private boolean overPanel(double x,double y){return x>=panelX && x<panelX+panelWidth && y>=panelY && y<panelY+156;}
    private void area(GuiMap map,GridArea area,int fill,int outline) {
        double x0=Math.max(0,view.screenX(area.left())),x1=Math.min(map.width,view.screenX(area.right()));
        double y0=Math.max(24,view.screenY(area.top())),y1=Math.min(map.height-30,view.screenY(area.bottom()));
        if(x1>x0 && y1>y0){MapDraw.rect(x0,y0,x1,y1,fill);MapDraw.frame(x0,y0,x1,y1,1,outline);}
    }
    private GridArea dragArea(double worldX,double worldZ) {
        try {
            int x=GridArea.cell(worldX),z=GridArea.cell(worldZ);
            x=Math.max(anchorX-15,Math.min(anchorX+15,x));z=Math.max(anchorZ-15,Math.min(anchorZ+15,z));
            return new GridArea(anchorX,anchorZ,x,z);
        }catch(IllegalArgumentException ignored){return selection;}
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public void mouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if(!(event.getGui() instanceof GuiMap) || !MapMode.isStormMap() || view==null)return;
        Minecraft mc=Minecraft.getMinecraft();double x=Mouse.getEventX()/view.guiScale,y=(mc.displayHeight-Mouse.getEventY()-1)/view.guiScale;
        int button=Mouse.getEventButton();boolean down=Mouse.getEventButtonState();
        if(button==0 && !down && tookPanel){tookPanel=false;event.setCanceled(true);return;}
        if(y<24)return; // Existing map-mode switch owns its complete press/release.
        if(overPanel(x,y)) {
            if(button==0 && down){
                tookPanel=true;
                for(int i=0;i<3;i++)if(y>=panelY+52+i*19 && y<panelY+68+i*19){
                    if(i==0)duration=(duration+1)%DURATIONS.length;
                    else if(selection!=null && allowed())FleetNetwork.CHANNEL.sendToServer(new StormControlPacket(i==1?StormControlPacket.CREATE:StormControlPacket.REMOVE,view.dimension,selection,DURATIONS[duration]*1200));
                }
            }
            event.setCanceled(true);return;
        }
        if(dragging) {
            selection=dragArea(view.worldX(x),view.worldZ(y));
            if(button==0 && !down){dragging=false;pressed=false;}
            event.setCanceled(true);return;
        }
        if(button==0 && down && y<event.getGui().height-30) {
            pressX=x;pressY=y;pressed=true;
            if(GuiScreen.isCtrlKeyDown()) {
                try{anchorX=GridArea.cell(view.worldX(x));anchorZ=GridArea.cell(view.worldZ(y));dragging=true;selection=new GridArea(anchorX,anchorZ,anchorX,anchorZ);}
                catch(IllegalArgumentException ignored){pressed=false;}
                event.setCanceled(true);
            }
        }else if(button==0 && !down && pressed) {
            pressed=false;
            if(Math.hypot(x-pressX,y-pressY)*view.guiScale<5) {
                try{int gx=GridArea.cell(view.worldX(x)),gz=GridArea.cell(view.worldZ(y));selection=new GridArea(gx,gz,gx,gz);}
                catch(IllegalArgumentException ignored){ }
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public void key(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if(!(event.getGui() instanceof GuiMap) || !MapMode.isStormMap() || !Keyboard.getEventKeyState() || ((GuiMap)event.getGui()).getFocused()!=null)return;
        if(Keyboard.getEventKey()==Keyboard.KEY_ESCAPE && (selection!=null || dragging)) {
            selection=null;dragging=pressed=false;event.setCanceled(true);
        }
    }
    private static final class GlStateManagerCompat {
        static void reset(){net.minecraft.client.renderer.GlStateManager.enableTexture2D();net.minecraft.client.renderer.GlStateManager.disableBlend();net.minecraft.client.renderer.GlStateManager.color(1,1,1,1);}
    }
}
