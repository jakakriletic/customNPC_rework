/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.client.event.GuiScreenEvent$ActionPerformedEvent$Post
 *  net.minecraftforge.client.event.GuiScreenEvent$ActionPerformedEvent$Pre
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.fml.common.eventhandler.Event
 *  org.lwjgl.input.Mouse
 */
package noppes.npcs.client.gui.util;

import com.google.common.collect.Lists;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiHoverText;
import noppes.npcs.client.gui.util.GuiMenuSideButton;
import noppes.npcs.client.gui.util.GuiMenuTopButton;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcSlider;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGui;
import noppes.npcs.client.gui.util.IKeyListener;
import noppes.npcs.client.gui.util.IMouseListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Mouse;

public abstract class GuiNPCInterface
extends GuiScreen {
    public EntityPlayerSP player;
    public boolean drawDefaultBackground = true;
    public EntityNPCInterface npc;
    private Map<Integer, GuiNpcButton> buttons = new ConcurrentHashMap<Integer, GuiNpcButton>();
    private Map<Integer, GuiMenuTopButton> topbuttons = new ConcurrentHashMap<Integer, GuiMenuTopButton>();
    private Map<Integer, GuiMenuSideButton> sidebuttons = new ConcurrentHashMap<Integer, GuiMenuSideButton>();
    private Map<Integer, GuiNpcTextField> textfields = new ConcurrentHashMap<Integer, GuiNpcTextField>();
    private Map<Integer, GuiNpcLabel> labels = new ConcurrentHashMap<Integer, GuiNpcLabel>();
    private Map<Integer, GuiCustomScroll> scrolls = new ConcurrentHashMap<Integer, GuiCustomScroll>();
    private Map<Integer, GuiNpcSlider> sliders = new ConcurrentHashMap<Integer, GuiNpcSlider>();
    private Map<Integer, GuiScreen> extra = new ConcurrentHashMap<Integer, GuiScreen>();
    private List<IGui> components = new ArrayList<IGui>();
    public String title;
    public ResourceLocation background = null;
    public boolean closeOnEsc = false;
    public int guiLeft;
    public int guiTop;
    public int xSize;
    public int ySize;
    private SubGuiInterface subgui;
    public int mouseX;
    public int mouseY;
    public float bgScale = 1.0f;
    private GuiButton selectedButton;

    public GuiNPCInterface(EntityNPCInterface npc) {
        this.player = Minecraft.func_71410_x().field_71439_g;
        this.npc = npc;
        this.title = "";
        this.xSize = 200;
        this.ySize = 222;
        this.drawDefaultBackground = false;
        this.field_146297_k = Minecraft.func_71410_x();
        this.field_146296_j = this.field_146297_k.func_175599_af();
        this.field_146289_q = this.field_146297_k.field_71466_p;
    }

    public GuiNPCInterface() {
        this(null);
    }

    public void setBackground(String texture) {
        this.background = new ResourceLocation("customnpcs", "textures/gui/" + texture);
    }

    public ResourceLocation getResource(String texture) {
        return new ResourceLocation("customnpcs", "textures/gui/" + texture);
    }

    public void func_146280_a(Minecraft mc, int width, int height) {
        super.func_146280_a(mc, width, height);
        this.initPacket();
    }

    public void initPacket() {
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        GuiNpcTextField.unfocus();
        if (this.subgui != null) {
            this.subgui.func_146280_a(this.field_146297_k, this.field_146294_l, this.field_146295_m);
            this.subgui.func_73866_w_();
        }
        this.guiLeft = (this.field_146294_l - this.xSize) / 2;
        this.guiTop = (this.field_146295_m - this.ySize) / 2;
        this.field_146292_n = Lists.newArrayList();
        this.buttons = new ConcurrentHashMap<Integer, GuiNpcButton>();
        this.topbuttons = new ConcurrentHashMap<Integer, GuiMenuTopButton>();
        this.sidebuttons = new ConcurrentHashMap<Integer, GuiMenuSideButton>();
        this.textfields = new ConcurrentHashMap<Integer, GuiNpcTextField>();
        this.labels = new ConcurrentHashMap<Integer, GuiNpcLabel>();
        this.scrolls = new ConcurrentHashMap<Integer, GuiCustomScroll>();
        this.sliders = new ConcurrentHashMap<Integer, GuiNpcSlider>();
        this.extra = new ConcurrentHashMap<Integer, GuiScreen>();
        this.components = new ArrayList<IGui>();
    }

    public void func_73876_c() {
        if (this.subgui != null) {
            this.subgui.func_73876_c();
        } else {
            for (GuiNpcTextField tf : new ArrayList<GuiNpcTextField>(this.textfields.values())) {
                if (!tf.enabled) continue;
                tf.func_146178_a();
            }
            for (IGui comp : new ArrayList<IGui>(this.components)) {
                comp.updateScreen();
            }
            super.func_73876_c();
        }
    }

    public void addExtra(GuiHoverText gui) {
        gui.func_146280_a(this.field_146297_k, 350, 250);
        this.extra.put(gui.id, gui);
    }

    public void func_73864_a(int i, int j, int k) {
        if (this.subgui != null) {
            this.subgui.func_73864_a(i, j, k);
        } else {
            for (GuiNpcTextField tf : new ArrayList<GuiNpcTextField>(this.textfields.values())) {
                if (!tf.enabled) continue;
                tf.func_146192_a(i, j, k);
            }
            for (IGui comp : new ArrayList<IGui>(this.components)) {
                if (!(comp instanceof IMouseListener)) continue;
                ((IMouseListener)((Object)comp)).mouseClicked(i, j, k);
            }
            this.mouseEvent(i, j, k);
            if (k == 0) {
                for (GuiCustomScroll scroll : new ArrayList<GuiCustomScroll>(this.scrolls.values())) {
                    scroll.func_73864_a(i, j, k);
                }
                for (GuiButton guibutton : this.field_146292_n) {
                    if (!guibutton.func_146116_c(this.field_146297_k, this.mouseX, this.mouseY)) continue;
                    GuiScreenEvent.ActionPerformedEvent.Pre event = new GuiScreenEvent.ActionPerformedEvent.Pre((GuiScreen)this, guibutton, this.field_146292_n);
                    if (MinecraftForge.EVENT_BUS.post((Event)event)) break;
                    this.selectedButton = guibutton = event.getButton();
                    guibutton.func_146113_a(this.field_146297_k.func_147118_V());
                    this.func_146284_a(guibutton);
                    if (!((Object)((Object)this)).equals(this.field_146297_k.field_71462_r)) break;
                    MinecraftForge.EVENT_BUS.post((Event)new GuiScreenEvent.ActionPerformedEvent.Post((GuiScreen)this, event.getButton(), this.field_146292_n));
                    break;
                }
            }
        }
    }

    public void func_146286_b(int mouseX, int mouseY, int state) {
        if (this.selectedButton != null && state == 0) {
            this.selectedButton.func_146118_a(mouseX, mouseY);
            this.selectedButton = null;
        }
    }

    public void mouseEvent(int i, int j, int k) {
    }

    protected void func_146284_a(GuiButton guibutton) {
        if (this.subgui != null) {
            this.subgui.buttonEvent(guibutton);
        } else {
            this.buttonEvent(guibutton);
        }
    }

    public void buttonEvent(GuiButton guibutton) {
    }

    public void func_73869_a(char c, int i) {
        if (this.subgui != null) {
            this.subgui.func_73869_a(c, i);
            return;
        }
        boolean active = false;
        for (IGui gui : this.components) {
            if (!gui.isActive()) continue;
            active = true;
            break;
        }
        boolean bl = active = active || GuiNpcTextField.isActive();
        if (this.closeOnEsc && (i == 1 || !active && this.isInventoryKey(i))) {
            this.close();
            return;
        }
        for (GuiNpcTextField tf : new ArrayList<GuiNpcTextField>(this.textfields.values())) {
            tf.func_146201_a(c, i);
        }
        for (IGui comp : new ArrayList<IGui>(this.components)) {
            if (!(comp instanceof IKeyListener)) continue;
            ((IKeyListener)((Object)comp)).keyTyped(c, i);
        }
    }

    public void func_146281_b() {
        GuiNpcTextField.unfocus();
    }

    public void close() {
        this.displayGuiScreen(null);
        this.field_146297_k.func_71381_h();
        this.save();
    }

    public void addButton(GuiNpcButton button) {
        this.buttons.put(button.field_146127_k, button);
        this.field_146292_n.add(button);
    }

    public void addTopButton(GuiMenuTopButton button) {
        this.topbuttons.put(button.field_146127_k, button);
        this.field_146292_n.add(button);
    }

    public void addSideButton(GuiMenuSideButton button) {
        this.sidebuttons.put(button.field_146127_k, button);
        this.field_146292_n.add(button);
    }

    public GuiNpcButton getButton(int i) {
        return this.buttons.get(i);
    }

    public GuiMenuSideButton getSideButton(int i) {
        return this.sidebuttons.get(i);
    }

    public GuiMenuTopButton getTopButton(int i) {
        return this.topbuttons.get(i);
    }

    public void addTextField(GuiNpcTextField tf) {
        this.textfields.put(tf.field_175208_g, tf);
    }

    public GuiNpcTextField getTextField(int i) {
        return this.textfields.get(i);
    }

    public void add(IGui gui) {
        this.components.add(gui);
    }

    public IGui get(int id) {
        for (IGui comp : this.components) {
            if (comp.getID() != id) continue;
            return comp;
        }
        return null;
    }

    public void addLabel(GuiNpcLabel label) {
        this.labels.put(label.id, label);
    }

    public GuiNpcLabel getLabel(int i) {
        return this.labels.get(i);
    }

    public void addSlider(GuiNpcSlider slider) {
        this.sliders.put(slider.field_146127_k, slider);
        this.field_146292_n.add(slider);
    }

    public GuiNpcSlider getSlider(int i) {
        return this.sliders.get(i);
    }

    public void addScroll(GuiCustomScroll scroll) {
        scroll.func_146280_a(this.field_146297_k, 350, 250);
        this.scrolls.put(scroll.id, scroll);
    }

    public GuiCustomScroll getScroll(int id) {
        return this.scrolls.get(id);
    }

    public abstract void save();

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        int x = mouseX;
        int y = mouseY;
        if (this.subgui != null) {
            y = 0;
            x = 0;
        }
        if (this.drawDefaultBackground && this.subgui == null) {
            this.func_146276_q_();
        }
        if (this.background != null && this.field_146297_k.field_71446_o != null) {
            GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float)this.guiLeft, (float)this.guiTop, (float)0.0f);
            GlStateManager.func_179152_a((float)this.bgScale, (float)this.bgScale, (float)this.bgScale);
            this.field_146297_k.field_71446_o.func_110577_a(this.background);
            if (this.xSize > 256) {
                this.func_73729_b(0, 0, 0, 0, 250, this.ySize);
                this.func_73729_b(250, 0, 256 - (this.xSize - 250), 0, this.xSize - 250, this.ySize);
            } else {
                this.func_73729_b(0, 0, 0, 0, this.xSize, this.ySize);
            }
            GlStateManager.func_179121_F();
        }
        this.func_73732_a(this.field_146289_q, this.title, this.field_146294_l / 2, 8, 0xFFFFFF);
        for (GuiNpcLabel label : new ArrayList<GuiNpcLabel>(this.labels.values())) {
            label.drawLabel(this, this.field_146289_q);
        }
        for (GuiNpcTextField tf : new ArrayList<GuiNpcTextField>(this.textfields.values())) {
            tf.drawTextBox(x, y);
        }
        for (IGui comp : new ArrayList<IGui>(this.components)) {
            comp.drawScreen(x, y);
        }
        for (GuiCustomScroll scroll : new ArrayList<GuiCustomScroll>(this.scrolls.values())) {
            scroll.drawScreen(x, y, partialTicks, !this.hasSubGui() && scroll.isMouseOver(x, y) ? Mouse.getDWheel() : 0);
        }
        for (GuiScreen gui : new ArrayList<GuiScreen>(this.extra.values())) {
            gui.func_73863_a(x, y, partialTicks);
        }
        super.func_73863_a(x, y, partialTicks);
        if (this.subgui != null) {
            this.subgui.func_73863_a(mouseX, mouseY, partialTicks);
        }
    }

    public FontRenderer getFontRenderer() {
        return this.field_146289_q;
    }

    public void elementClicked() {
        if (this.subgui != null) {
            this.subgui.elementClicked();
        }
    }

    public boolean func_73868_f() {
        return false;
    }

    public void doubleClicked() {
    }

    public boolean isInventoryKey(int i) {
        return i == this.field_146297_k.field_71474_y.field_151445_Q.func_151463_i();
    }

    public void func_146276_q_() {
        super.func_146276_q_();
    }

    public void displayGuiScreen(GuiScreen gui) {
        this.field_146297_k.func_147108_a(gui);
    }

    public void setSubGui(SubGuiInterface gui) {
        this.subgui = gui;
        this.subgui.npc = this.npc;
        this.subgui.func_146280_a(this.field_146297_k, this.field_146294_l, this.field_146295_m);
        this.subgui.parent = this;
        this.func_73866_w_();
    }

    public void closeSubGui(SubGuiInterface gui) {
        this.subgui = null;
    }

    public boolean hasSubGui() {
        return this.subgui != null;
    }

    public SubGuiInterface getSubGui() {
        if (this.hasSubGui() && this.subgui.hasSubGui()) {
            return this.subgui.getSubGui();
        }
        return this.subgui;
    }

    public void drawNpc(int x, int y) {
        this.drawNpc((EntityLivingBase)this.npc, x, y, 1.0f, 0);
    }

    public void drawNpc(EntityLivingBase entity, int x, int y, float zoomed, int rotation) {
        EntityNPCInterface npc = null;
        if (entity instanceof EntityNPCInterface) {
            npc = (EntityNPCInterface)entity;
        }
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179142_g();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)(this.guiLeft + x), (float)(this.guiTop + y), (float)50.0f);
        float scale = 1.0f;
        if ((double)entity.field_70131_O > 2.4) {
            scale = 2.0f / entity.field_70131_O;
        }
        GlStateManager.func_179152_a((float)(-30.0f * scale * zoomed), (float)(30.0f * scale * zoomed), (float)(30.0f * scale * zoomed));
        GlStateManager.func_179114_b((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        RenderHelper.func_74519_b();
        float f2 = entity.field_70761_aq;
        float f3 = entity.field_70177_z;
        float f4 = entity.field_70125_A;
        float f7 = entity.field_70759_as;
        float f5 = (float)(this.guiLeft + x) - (float)this.mouseX;
        float f6 = (float)(this.guiTop + y) - 50.0f * scale * zoomed - (float)this.mouseY;
        int orientation = 0;
        if (npc != null) {
            orientation = npc.ais.orientation;
            npc.ais.orientation = rotation;
        }
        GlStateManager.func_179114_b((float)135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)-135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)(-((float)Math.atan(f6 / 40.0f)) * 20.0f), (float)1.0f, (float)0.0f, (float)0.0f);
        entity.field_70761_aq = rotation;
        entity.field_70177_z = (float)Math.atan(f5 / 80.0f) * 40.0f + (float)rotation;
        entity.field_70125_A = -((float)Math.atan(f6 / 40.0f)) * 20.0f;
        entity.field_70759_as = entity.field_70177_z;
        this.field_146297_k.func_175598_ae().field_78735_i = 180.0f;
        this.field_146297_k.func_175598_ae().func_188391_a((Entity)entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, false);
        entity.field_70760_ar = entity.field_70761_aq = f2;
        entity.field_70126_B = entity.field_70177_z = f3;
        entity.field_70127_C = entity.field_70125_A = f4;
        entity.field_70758_at = entity.field_70759_as = f7;
        if (npc != null) {
            npc.ais.orientation = orientation;
        }
        GlStateManager.func_179121_F();
        RenderHelper.func_74518_a();
        GlStateManager.func_179101_C();
        GlStateManager.func_179138_g((int)OpenGlHelper.field_77476_b);
        GlStateManager.func_179090_x();
        GlStateManager.func_179138_g((int)OpenGlHelper.field_77478_a);
    }

    public void openLink(String link) {
        try {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
            oclass.getMethod("browse", URI.class).invoke(object, new URI(link));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

