/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.player;

import java.util.ArrayList;
import micdoodle8.mods.galacticraft.api.client.tabs.InventoryTabFactions;
import micdoodle8.mods.galacticraft.api.client.tabs.TabRegistry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiButtonNextPage;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerFactionData;

public class GuiFaction
extends GuiNPCInterface
implements IGuiData {
    private int xSize = 200;
    private int ySize = 195;
    private int guiLeft;
    private int guiTop;
    private ArrayList<Faction> playerFactions = new ArrayList();
    private int page = 0;
    private int pages = 1;
    private GuiButtonNextPage buttonNextPage;
    private GuiButtonNextPage buttonPreviousPage;
    private ResourceLocation indicator;

    public GuiFaction() {
        this.drawDefaultBackground = false;
        this.title = "";
        NoppesUtilPlayer.sendData(EnumPlayerPacket.FactionsGet, new Object[0]);
        this.indicator = this.getResource("standardbg.png");
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.guiLeft = (this.field_146294_l - this.xSize) / 2;
        this.guiTop = (this.field_146295_m - this.ySize) / 2 + 12;
        TabRegistry.updateTabValues(this.guiLeft, this.guiTop + 8, InventoryTabFactions.class);
        TabRegistry.addTabsToList(this.field_146292_n);
        this.buttonNextPage = new GuiButtonNextPage(1, this.guiLeft + this.xSize - 43, this.guiTop + 180, true);
        this.field_146292_n.add(this.buttonNextPage);
        this.buttonPreviousPage = new GuiButtonNextPage(2, this.guiLeft + 20, this.guiTop + 180, false);
        this.field_146292_n.add(this.buttonPreviousPage);
        this.updateButtons();
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        this.func_146276_q_();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.indicator);
        this.func_73729_b(this.guiLeft, this.guiTop + 8, 0, 0, this.xSize, this.ySize);
        this.func_73729_b(this.guiLeft + 4, this.guiTop + 8, 56, 0, 200, this.ySize);
        if (this.playerFactions.isEmpty()) {
            String noFaction = I18n.func_74838_a((String)"faction.nostanding");
            this.field_146289_q.func_78276_b(noFaction, this.guiLeft + (this.xSize - this.field_146289_q.func_78256_a(noFaction)) / 2, this.guiTop + 80, CustomNpcResourceListener.DefaultTextColor);
        } else {
            this.renderScreen();
        }
        super.func_73863_a(i, j, f);
    }

    private void renderScreen() {
        int size = 5;
        if (this.playerFactions.size() % 5 != 0 && this.page == this.pages) {
            size = this.playerFactions.size() % 5;
        }
        for (int id = 0; id < size; ++id) {
            this.func_73730_a(this.guiLeft + 2, this.guiLeft + this.xSize, this.guiTop + 14 + id * 30, -16777216 + CustomNpcResourceListener.DefaultTextColor);
            Faction faction = this.playerFactions.get((this.page - 1) * 5 + id);
            String name = faction.name;
            String points = " : " + faction.defaultPoints;
            String standing = I18n.func_74838_a((String)"faction.friendly");
            int color = 65280;
            if (faction.defaultPoints < faction.neutralPoints) {
                standing = I18n.func_74838_a((String)"faction.unfriendly");
                color = 0xFF0000;
                points = points + "/" + faction.neutralPoints;
            } else if (faction.defaultPoints < faction.friendlyPoints) {
                standing = I18n.func_74838_a((String)"faction.neutral");
                color = 0xF2FF00;
                points = points + "/" + faction.friendlyPoints;
            } else {
                points = points + "/-";
            }
            this.field_146289_q.func_78276_b(name, this.guiLeft + (this.xSize - this.field_146289_q.func_78256_a(name)) / 2, this.guiTop + 19 + id * 30, faction.color);
            this.field_146289_q.func_78276_b(standing, this.field_146294_l / 2 - this.field_146289_q.func_78256_a(standing) - 1, this.guiTop + 33 + id * 30, color);
            this.field_146289_q.func_78276_b(points, this.field_146294_l / 2, this.guiTop + 33 + id * 30, CustomNpcResourceListener.DefaultTextColor);
        }
        this.func_73730_a(this.guiLeft + 2, this.guiLeft + this.xSize, this.guiTop + 14 + size * 30, -16777216 + CustomNpcResourceListener.DefaultTextColor);
        if (this.pages > 1) {
            String s = this.page + "/" + this.pages;
            this.field_146289_q.func_78276_b(s, this.guiLeft + (this.xSize - this.field_146289_q.func_78256_a(s)) / 2, this.guiTop + 203, CustomNpcResourceListener.DefaultTextColor);
        }
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        if (!(guibutton instanceof GuiButtonNextPage)) {
            return;
        }
        int id = guibutton.field_146127_k;
        if (id == 1) {
            ++this.page;
        }
        if (id == 2) {
            --this.page;
        }
        this.updateButtons();
    }

    private void updateButtons() {
        this.buttonNextPage.setVisible(this.page < this.pages);
        this.buttonPreviousPage.setVisible(this.page > 1);
    }

    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
    }

    @Override
    public void func_73869_a(char c, int i) {
        if (i == 1 || this.isInventoryKey(i)) {
            this.close();
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.playerFactions = new ArrayList();
        NBTTagList list = compound.func_150295_c("FactionList", 10);
        for (int i = 0; i < list.func_74745_c(); ++i) {
            Faction faction = new Faction();
            faction.readNBT(list.func_150305_b(i));
            this.playerFactions.add(faction);
        }
        PlayerFactionData data = new PlayerFactionData();
        data.loadNBTData(compound);
        for (int id : data.factionData.keySet()) {
            int points = data.factionData.get(id);
            for (Faction faction : this.playerFactions) {
                if (faction.id != id) continue;
                faction.defaultPoints = points;
            }
        }
        this.pages = (this.playerFactions.size() - 1) / 5;
        ++this.pages;
        this.page = 1;
        this.updateButtons();
    }
}

