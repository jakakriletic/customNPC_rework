/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.EntityLivingBase
 */
package noppes.npcs.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import noppes.npcs.client.gui.GuiNpcSelectionInterface;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNPCTextures
extends GuiNpcSelectionInterface {
    public GuiNPCTextures(EntityNPCInterface npc, GuiScreen parent) {
        super(npc, parent, npc.display.getSkinTexture());
        this.title = "Select Texture";
        this.parent = parent;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        int index = this.npc.display.getSkinTexture().lastIndexOf("/");
        if (index > 0) {
            String asset = this.npc.display.getSkinTexture().substring(index + 1);
            if (this.npc.display.getSkinTexture().equals(this.assets.getAsset(asset))) {
                this.slot.selected = asset;
            }
        }
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        int l = -50;
        int i1 = this.field_146295_m / 2 + 30;
        this.drawNpc((EntityLivingBase)this.npc, l, i1, 2.0f, 0);
        super.func_73863_a(i, j, f);
    }

    @Override
    public void elementClicked() {
        if (this.dataTextures.contains(this.slot.selected) && this.slot.selected != null) {
            this.npc.display.setSkinTexture(this.assets.getAsset(this.slot.selected));
            this.npc.textureLocation = null;
        }
    }

    @Override
    public void save() {
    }

    @Override
    public String[] getExtension() {
        return new String[]{"png"};
    }
}

