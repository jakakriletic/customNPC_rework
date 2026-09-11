/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiConfirmOpenLink
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.script;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.script.GuiScriptList;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiMenuTopButton;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiTextArea;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.ITextChangeListener;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;

public class GuiScriptInterface
extends GuiNPCInterface
implements IGuiData,
ITextChangeListener {
    private int activeTab = 0;
    public IScriptHandler handler;
    public Map<String, List<String>> languages = new HashMap<String, List<String>>();

    public GuiScriptInterface() {
        this.drawDefaultBackground = true;
        this.closeOnEsc = true;
        this.xSize = 420;
        this.setBackground("menubg.png");
    }

    @Override
    public void func_73866_w_() {
        this.xSize = (int)((double)this.field_146294_l * 0.88);
        this.ySize = (int)((double)this.xSize * 0.56);
        if ((double)this.ySize > (double)this.field_146295_m * 0.95) {
            this.ySize = (int)((double)this.field_146295_m * 0.95);
            this.xSize = (int)((double)this.ySize / 0.56);
        }
        this.bgScale = (float)this.xSize / 400.0f;
        super.func_73866_w_();
        this.guiTop += 10;
        int yoffset = (int)((double)this.ySize * 0.02);
        GuiMenuTopButton top = new GuiMenuTopButton(0, this.guiLeft + 4, this.guiTop - 17, "gui.settings");
        this.addTopButton(top);
        for (int i = 0; i < this.handler.getScripts().size(); ++i) {
            ScriptContainer script = this.handler.getScripts().get(i);
            top = new GuiMenuTopButton(i + 1, top, i + 1 + "");
            this.addTopButton(top);
        }
        if (this.handler.getScripts().size() < 16) {
            top = new GuiMenuTopButton(12, top, "+");
            this.addTopButton(top);
        }
        if ((top = this.getTopButton(this.activeTab)) == null) {
            this.activeTab = 0;
            top = this.getTopButton(0);
        }
        top.active = true;
        if (this.activeTab > 0) {
            ScriptContainer container = this.handler.getScripts().get(this.activeTab - 1);
            GuiTextArea ta = new GuiTextArea(2, this.guiLeft + 1 + yoffset, this.guiTop + yoffset, this.xSize - 108 - yoffset, (int)((double)this.ySize * 0.96) - yoffset * 2, container == null ? "" : container.script);
            ta.enableCodeHighlighting();
            ta.setListener(this);
            this.add(ta);
            int left = this.guiLeft + this.xSize - 104;
            this.addButton(new GuiNpcButton(102, left, this.guiTop + yoffset, 60, 20, "gui.clear"));
            this.addButton(new GuiNpcButton(101, left + 61, this.guiTop + yoffset, 60, 20, "gui.paste"));
            this.addButton(new GuiNpcButton(100, left, this.guiTop + 21 + yoffset, 60, 20, "gui.copy"));
            this.addButton(new GuiNpcButton(105, left + 61, this.guiTop + 21 + yoffset, 60, 20, "gui.remove"));
            this.addButton(new GuiNpcButton(107, left, this.guiTop + 66 + yoffset, 80, 20, "script.loadscript"));
            GuiCustomScroll scroll = new GuiCustomScroll(this, 0).setUnselectable();
            scroll.setSize(100, (int)((double)this.ySize * 0.54) - yoffset * 2);
            scroll.guiLeft = left;
            scroll.guiTop = this.guiTop + 88 + yoffset;
            if (container != null) {
                scroll.setList(container.scripts);
            }
            this.addScroll(scroll);
        } else {
            GuiTextArea ta = new GuiTextArea(2, this.guiLeft + 4 + yoffset, this.guiTop + 6 + yoffset, this.xSize - 160 - yoffset, (int)((float)this.ySize * 0.92f) - yoffset * 2, this.getConsoleText());
            ta.enabled = false;
            this.add(ta);
            int left = this.guiLeft + this.xSize - 150;
            this.addButton(new GuiNpcButton(100, left, this.guiTop + 125, 60, 20, "gui.copy"));
            this.addButton(new GuiNpcButton(102, left, this.guiTop + 146, 60, 20, "gui.clear"));
            this.addLabel(new GuiNpcLabel(1, "script.language", left, this.guiTop + 15));
            this.addButton(new GuiNpcButton(103, left + 60, this.guiTop + 10, 80, 20, this.languages.keySet().toArray(new String[this.languages.keySet().size()]), this.getScriptIndex()));
            this.getButton((int)103).field_146124_l = this.languages.size() > 0;
            this.addLabel(new GuiNpcLabel(2, "gui.enabled", left, this.guiTop + 36));
            this.addButton(new GuiNpcButton(104, left + 60, this.guiTop + 31, 50, 20, new String[]{"gui.no", "gui.yes"}, this.handler.getEnabled() ? 1 : 0));
            if (this.player.func_184102_h() != null) {
                this.addButton(new GuiNpcButton(106, left, this.guiTop + 55, 150, 20, "script.openfolder"));
            }
            this.addButton(new GuiNpcButton(109, left, this.guiTop + 78, 80, 20, "gui.website"));
            this.addButton(new GuiNpcButton(112, left + 81, this.guiTop + 78, 80, 20, "gui.forum"));
            this.addButton(new GuiNpcButton(110, left, this.guiTop + 99, 80, 20, "script.apidoc"));
            this.addButton(new GuiNpcButton(111, left + 81, this.guiTop + 99, 80, 20, "script.apisrc"));
        }
        this.xSize = 420;
        this.ySize = 256;
    }

    private String getConsoleText() {
        Map<Long, String> map = this.handler.getConsoleText();
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Long, String> entry : map.entrySet()) {
            builder.insert(0, new Date(entry.getKey()) + entry.getValue() + "\n");
        }
        return builder.toString();
    }

    private int getScriptIndex() {
        int i = 0;
        for (String language : this.languages.keySet()) {
            if (language.equalsIgnoreCase(this.handler.getLanguage())) {
                return i;
            }
            ++i;
        }
        return 0;
    }

    public void func_73878_a(boolean flag, int i) {
        if (flag) {
            if (i == 0) {
                this.openLink("http://www.kodevelopment.nl/minecraft/customnpcs/scripting");
            }
            if (i == 1) {
                this.openLink("http://www.kodevelopment.nl/customnpcs/api/");
            }
            if (i == 2) {
                this.openLink("http://www.kodevelopment.nl/minecraft/customnpcs/scripting");
            }
            if (i == 3) {
                this.openLink("http://www.minecraftforge.net/forum/index.php/board,122.0.html");
            }
            if (i == 10) {
                this.handler.getScripts().remove(this.activeTab - 1);
                this.activeTab = 0;
            }
        }
        this.displayGuiScreen(this);
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        ScriptContainer container;
        if (guibutton.field_146127_k >= 0 && guibutton.field_146127_k < 12) {
            this.setScript();
            this.activeTab = guibutton.field_146127_k;
            this.func_73866_w_();
        }
        if (guibutton.field_146127_k == 12) {
            this.handler.getScripts().add(new ScriptContainer(this.handler));
            this.activeTab = this.handler.getScripts().size();
            this.func_73866_w_();
        }
        if (guibutton.field_146127_k == 109) {
            this.displayGuiScreen((GuiScreen)new GuiConfirmOpenLink((GuiYesNoCallback)this, "http://www.kodevelopment.nl/minecraft/customnpcs/scripting", 0, true));
        }
        if (guibutton.field_146127_k == 110) {
            this.displayGuiScreen((GuiScreen)new GuiConfirmOpenLink((GuiYesNoCallback)this, "http://www.kodevelopment.nl/customnpcs/api/", 1, true));
        }
        if (guibutton.field_146127_k == 111) {
            this.displayGuiScreen((GuiScreen)new GuiConfirmOpenLink((GuiYesNoCallback)this, "https://github.com/Noppes/CustomNPCsAPI", 2, true));
        }
        if (guibutton.field_146127_k == 112) {
            this.displayGuiScreen((GuiScreen)new GuiConfirmOpenLink((GuiYesNoCallback)this, "http://www.minecraftforge.net/forum/index.php/board,122.0.html", 3, true));
        }
        if (guibutton.field_146127_k == 100) {
            NoppesStringUtils.setClipboardContents(((GuiTextArea)this.get(2)).getText());
        }
        if (guibutton.field_146127_k == 101) {
            ((GuiTextArea)this.get(2)).setText(NoppesStringUtils.getClipboardContents());
        }
        if (guibutton.field_146127_k == 102) {
            if (this.activeTab > 0) {
                container = this.handler.getScripts().get(this.activeTab - 1);
                container.script = "";
            } else {
                this.handler.clearConsole();
            }
            this.func_73866_w_();
        }
        if (guibutton.field_146127_k == 103) {
            this.handler.setLanguage(((GuiNpcButton)guibutton).field_146126_j);
        }
        if (guibutton.field_146127_k == 104) {
            this.handler.setEnabled(((GuiNpcButton)guibutton).getValue() == 1);
        }
        if (guibutton.field_146127_k == 105) {
            GuiYesNo guiyesno = new GuiYesNo((GuiYesNoCallback)this, "", I18n.func_74838_a((String)"gui.deleteMessage"), 10);
            this.displayGuiScreen((GuiScreen)guiyesno);
        }
        if (guibutton.field_146127_k == 106) {
            NoppesUtil.openFolder(ScriptController.Instance.dir);
        }
        if (guibutton.field_146127_k == 107) {
            container = this.handler.getScripts().get(this.activeTab - 1);
            if (container == null) {
                container = new ScriptContainer(this.handler);
                this.handler.getScripts().add(container);
            }
            this.setSubGui(new GuiScriptList(this.languages.get(this.handler.getLanguage()), container));
        }
        if (guibutton.field_146127_k == 108 && (container = this.handler.getScripts().get(this.activeTab - 1)) != null) {
            this.setScript();
        }
    }

    private void setScript() {
        if (this.activeTab > 0) {
            ScriptContainer container = this.handler.getScripts().get(this.activeTab - 1);
            if (container == null) {
                container = new ScriptContainer(this.handler);
                this.handler.getScripts().add(container);
            }
            String text = ((GuiTextArea)this.get(2)).getText();
            text = text.replace("\r\n", "\n");
            container.script = text = text.replace("\r", "\n");
        }
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        NBTTagList data = compound.func_150295_c("Languages", 10);
        HashMap<String, List<String>> languages = new HashMap<String, List<String>>();
        for (int i = 0; i < data.func_74745_c(); ++i) {
            NBTTagCompound comp = data.func_150305_b(i);
            ArrayList<String> scripts = new ArrayList<String>();
            NBTTagList list = comp.func_150295_c("Scripts", 8);
            for (int j = 0; j < list.func_74745_c(); ++j) {
                scripts.add(list.func_150307_f(j));
            }
            languages.put(comp.func_74779_i("Language"), scripts);
        }
        this.languages = languages;
        this.func_73866_w_();
    }

    @Override
    public void save() {
        this.setScript();
    }

    @Override
    public void textUpdate(String text) {
        ScriptContainer container = this.handler.getScripts().get(this.activeTab - 1);
        if (container != null) {
            container.script = text;
        }
    }
}

