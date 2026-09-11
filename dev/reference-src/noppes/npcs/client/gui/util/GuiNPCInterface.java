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
        this.player = Minecraft.getMinecraft().player;
        this.npc = npc;
        this.title = "";
        this.xSize = 200;
        this.ySize = 222;
        this.drawDefaultBackground = false;
        this.mc = Minecraft.getMinecraft();
        this.itemRender = this.mc.getRenderItem();
        this.fontRenderer = this.mc.fontRenderer;
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

    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        this.initPacket();
    }

    public void initPacket() {
    }

    public void initGui() {
        super.initGui();
        GuiNpcTextField.unfocus();
        if (this.subgui != null) {
            this.subgui.setWorldAndResolution(this.mc, this.width, this.height);
            this.subgui.initGui();
        }
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.buttonList = Lists.newArrayList();
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

    public void updateScreen() {
        if (this.subgui != null) {
            this.subgui.updateScreen();
        } else {
            for (GuiNpcTextField tf : new ArrayList<GuiNpcTextField>(this.textfields.values())) {
                if (!tf.enabled) continue;
                tf.updateCursorCounter();
            }
            for (IGui comp : new ArrayList<IGui>(this.components)) {
                comp.updateScreen();
            }
            super.updateScreen();
        }
    }

    public void addExtra(GuiHoverText gui) {
        gui.setWorldAndResolution(this.mc, 350, 250);
        this.extra.put(gui.id, gui);
    }

    public void mouseClicked(int i, int j, int k) {
        if (this.subgui != null) {
            this.subgui.mouseClicked(i, j, k);
        } else {
            for (GuiNpcTextField tf : new ArrayList<GuiNpcTextField>(this.textfields.values())) {
                if (!tf.enabled) continue;
                tf.mouseClicked(i, j, k);
            }
            for (IGui comp : new ArrayList<IGui>(this.components)) {
                if (!(comp instanceof IMouseListener)) continue;
                ((IMouseListener)((Object)comp)).mouseClicked(i, j, k);
            }
            this.mouseEvent(i, j, k);
            if (k == 0) {
                for (GuiCustomScroll scroll : new ArrayList<GuiCustomScroll>(this.scrolls.values())) {
                    scroll.mouseClicked(i, j, k);
                }
                for (GuiButton guibutton : this.buttonList) {
                    if (!guibutton.mousePressed(this.mc, this.mouseX, this.mouseY)) continue;
                    GuiScreenEvent.ActionPerformedEvent.Pre event = new GuiScreenEvent.ActionPerformedEvent.Pre((GuiScreen)this, guibutton, this.buttonList);
                    if (MinecraftForge.EVENT_BUS.post((Event)event)) break;
                    this.selectedButton = guibutton = event.getButton();
                    guibutton.playPressSound(this.mc.getSoundHandler());
                    this.actionPerformed(guibutton);
                    if (!((Object)((Object)this)).equals(this.mc.currentScreen)) break;
                    MinecraftForge.EVENT_BUS.post((Event)new GuiScreenEvent.ActionPerformedEvent.Post((GuiScreen)this, event.getButton(), this.buttonList));
                    break;
                }
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.selectedButton != null && state == 0) {
            this.selectedButton.mouseReleased(mouseX, mouseY);
            this.selectedButton = null;
        }
    }

    public void mouseEvent(int i, int j, int k) {
    }

    protected void actionPerformed(GuiButton guibutton) {
        if (this.subgui != null) {
            this.subgui.buttonEvent(guibutton);
        } else {
            this.buttonEvent(guibutton);
        }
    }

    public void buttonEvent(GuiButton guibutton) {
    }

    public void keyTyped(char c, int i) {
        if (this.subgui != null) {
            this.subgui.keyTyped(c, i);
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
            tf.textboxKeyTyped(c, i);
        }
        for (IGui comp : new ArrayList<IGui>(this.components)) {
            if (!(comp instanceof IKeyListener)) continue;
            ((IKeyListener)((Object)comp)).keyTyped(c, i);
        }
    }

    public void onGuiClosed() {
        GuiNpcTextField.unfocus();
    }

    public void close() {
        this.displayGuiScreen(null);
        this.mc.setIngameFocus();
        this.save();
    }

    public void addButton(GuiNpcButton button) {
        this.buttons.put(button.id, button);
        this.buttonList.add(button);
    }

    public void addTopButton(GuiMenuTopButton button) {
        this.topbuttons.put(button.id, button);
        this.buttonList.add(button);
    }

    public void addSideButton(GuiMenuSideButton button) {
        this.sidebuttons.put(button.id, button);
        this.buttonList.add(button);
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
        this.textfields.put(tf.id, tf);
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
        this.sliders.put(slider.id, slider);
        this.buttonList.add(slider);
    }

    public GuiNpcSlider getSlider(int i) {
        return this.sliders.get(i);
    }

    public void addScroll(GuiCustomScroll scroll) {
        scroll.setWorldAndResolution(this.mc, 350, 250);
        this.scrolls.put(scroll.id, scroll);
    }

    public GuiCustomScroll getScroll(int id) {
        return this.scrolls.get(id);
    }

    public abstract void save();

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        int x = mouseX;
        int y = mouseY;
        if (this.subgui != null) {
            y = 0;
            x = 0;
        }
        if (this.drawDefaultBackground && this.subgui == null) {
            this.drawDefaultBackground();
        }
        if (this.background != null && this.mc.renderEngine != null) {
            GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)this.guiLeft, (float)this.guiTop, (float)0.0f);
            GlStateManager.scale((float)this.bgScale, (float)this.bgScale, (float)this.bgScale);
            this.mc.renderEngine.bindTexture(this.background);
            if (this.xSize > 256) {
                this.drawTexturedModalRect(0, 0, 0, 0, 250, this.ySize);
                this.drawTexturedModalRect(250, 0, 256 - (this.xSize - 250), 0, this.xSize - 250, this.ySize);
            } else {
                this.drawTexturedModalRect(0, 0, 0, 0, this.xSize, this.ySize);
            }
            GlStateManager.popMatrix();
        }
        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        for (GuiNpcLabel label : new ArrayList<GuiNpcLabel>(this.labels.values())) {
            label.drawLabel(this, this.fontRenderer);
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
            gui.drawScreen(x, y, partialTicks);
        }
        super.drawScreen(x, y, partialTicks);
        if (this.subgui != null) {
            this.subgui.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }

    public void elementClicked() {
        if (this.subgui != null) {
            this.subgui.elementClicked();
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public void doubleClicked() {
    }

    public boolean isInventoryKey(int i) {
        return i == this.mc.gameSettings.keyBindInventory.getKeyCode();
    }

    public void drawDefaultBackground() {
        super.drawDefaultBackground();
    }

    public void displayGuiScreen(GuiScreen gui) {
        this.mc.displayGuiScreen(gui);
    }

    public void setSubGui(SubGuiInterface gui) {
        this.subgui = gui;
        this.subgui.npc = this.npc;
        this.subgui.setWorldAndResolution(this.mc, this.width, this.height);
        this.subgui.parent = this;
        this.initGui();
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
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)(this.guiLeft + x), (float)(this.guiTop + y), (float)50.0f);
        float scale = 1.0f;
        if ((double)entity.height > 2.4) {
            scale = 2.0f / entity.height;
        }
        GlStateManager.scale((float)(-30.0f * scale * zoomed), (float)(30.0f * scale * zoomed), (float)(30.0f * scale * zoomed));
        GlStateManager.rotate((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        RenderHelper.enableStandardItemLighting();
        float f2 = entity.renderYawOffset;
        float f3 = entity.rotationYaw;
        float f4 = entity.rotationPitch;
        float f7 = entity.rotationYawHead;
        float f5 = (float)(this.guiLeft + x) - (float)this.mouseX;
        float f6 = (float)(this.guiTop + y) - 50.0f * scale * zoomed - (float)this.mouseY;
        int orientation = 0;
        if (npc != null) {
            orientation = npc.ais.orientation;
            npc.ais.orientation = rotation;
        }
        GlStateManager.rotate((float)135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.rotate((float)-135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.rotate((float)(-((float)Math.atan(f6 / 40.0f)) * 20.0f), (float)1.0f, (float)0.0f, (float)0.0f);
        entity.renderYawOffset = rotation;
        entity.rotationYaw = (float)Math.atan(f5 / 80.0f) * 40.0f + (float)rotation;
        entity.rotationPitch = -((float)Math.atan(f6 / 40.0f)) * 20.0f;
        entity.rotationYawHead = entity.rotationYaw;
        this.mc.getRenderManager().playerViewY = 180.0f;
        this.mc.getRenderManager().renderEntity((Entity)entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, false);
        entity.prevRenderYawOffset = entity.renderYawOffset = f2;
        entity.prevRotationYaw = entity.rotationYaw = f3;
        entity.prevRotationPitch = entity.rotationPitch = f4;
        entity.prevRotationYawHead = entity.rotationYawHead = f7;
        if (npc != null) {
            npc.ais.orientation = orientation;
        }
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture((int)OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture((int)OpenGlHelper.defaultTexUnit);
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

