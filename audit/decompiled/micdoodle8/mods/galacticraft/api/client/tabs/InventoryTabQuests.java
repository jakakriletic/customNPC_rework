/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.settings.GameSettings
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 */
package micdoodle8.mods.galacticraft.api.client.tabs;

import java.util.Arrays;
import java.util.List;
import micdoodle8.mods.galacticraft.api.client.tabs.AbstractTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.gui.player.GuiQuestLog;
import noppes.npcs.util.CustomNPCsScheduler;

public class InventoryTabQuests
extends AbstractTab {
    public InventoryTabQuests() {
        super(0, 0, 0, new ItemStack(Items.field_151122_aG));
        this.field_146126_j = NoppesStringUtils.translate("quest.quest") + "(" + GameSettings.func_74298_c((int)ClientProxy.QuestLog.func_151463_i()) + ")";
    }

    @Override
    public void onTabClicked() {
        CustomNPCsScheduler.runTack(() -> {
            Minecraft mc = Minecraft.func_71410_x();
            mc.func_147108_a((GuiScreen)new GuiQuestLog((EntityPlayer)mc.field_71439_g));
        });
    }

    @Override
    public boolean shouldAddToList() {
        return true;
    }

    @Override
    public void func_191745_a(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        boolean hovered;
        if (!this.field_146124_l || !this.field_146125_m) {
            super.func_191745_a(minecraft, mouseX, mouseY, partialTicks);
            return;
        }
        Minecraft mc = Minecraft.func_71410_x();
        boolean bl = hovered = mouseX >= this.field_146128_h && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + this.field_146120_f && mouseY < this.field_146129_i + this.field_146121_g;
        if (hovered) {
            int x = mouseX + mc.field_71466_p.func_78256_a(this.field_146126_j);
            GlStateManager.func_179109_b((float)x, (float)(this.field_146129_i + 2), (float)0.0f);
            this.drawHoveringText(Arrays.asList(this.field_146126_j), 0, 0, mc.field_71466_p);
            GlStateManager.func_179109_b((float)(-x), (float)(-(this.field_146129_i + 2)), (float)0.0f);
        }
        super.func_191745_a(minecraft, mouseX, mouseY, partialTicks);
    }

    protected void drawHoveringText(List list, int x, int y, FontRenderer font) {
        if (list.isEmpty()) {
            return;
        }
        GlStateManager.func_179101_C();
        RenderHelper.func_74518_a();
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        int k = 0;
        for (String s : list) {
            int l = font.func_78256_a(s);
            if (l <= k) continue;
            k = l;
        }
        int j2 = x + 12;
        int k2 = y - 12;
        int i1 = 8;
        if (list.size() > 1) {
            i1 += 2 + (list.size() - 1) * 10;
        }
        if (j2 + k > this.field_146120_f) {
            j2 -= 28 + k;
        }
        if (k2 + i1 + 6 > this.field_146121_g) {
            k2 = this.field_146121_g - i1 - 6;
        }
        this.field_73735_i = 300.0f;
        this.itemRender.field_77023_b = 300.0f;
        int j1 = -267386864;
        this.func_73733_a(j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1);
        this.func_73733_a(j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1);
        this.func_73733_a(j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1);
        this.func_73733_a(j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
        this.func_73733_a(j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1);
        int k1 = 0x505000FF;
        int l1 = (k1 & 0xFEFEFE) >> 1 | k1 & 0xFF000000;
        this.func_73733_a(j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
        this.func_73733_a(j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1);
        this.func_73733_a(j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1);
        this.func_73733_a(j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1);
        for (int i2 = 0; i2 < list.size(); ++i2) {
            String s1 = (String)list.get(i2);
            font.func_175063_a(s1, (float)j2, (float)k2, -1);
            if (i2 == 0) {
                k2 += 2;
            }
            k2 += 10;
        }
        this.field_73735_i = 0.0f;
        this.itemRender.field_77023_b = 0.0f;
        GlStateManager.func_179145_e();
        GlStateManager.func_179126_j();
        RenderHelper.func_74519_b();
        GlStateManager.func_179091_B();
    }
}

