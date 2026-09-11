/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.gui.player.companion;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.player.companion.GuiNpcCompanionStats;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCCompanion;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;

public class GuiNpcCompanionInv
extends GuiContainerNPCInterface {
    private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/companioninv.png");
    private final ResourceLocation slot = new ResourceLocation("customnpcs", "textures/gui/slot.png");
    private EntityNPCInterface npc;
    private RoleCompanion role;

    public GuiNpcCompanionInv(EntityNPCInterface npc, ContainerNPCCompanion container) {
        super(npc, container);
        this.npc = npc;
        this.role = (RoleCompanion)npc.roleInterface;
        this.closeOnEsc = true;
        this.field_146999_f = 171;
        this.field_147000_g = 166;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        GuiNpcCompanionStats.addTopMenu(this.role, (GuiScreen)this, 3);
    }

    @Override
    public void func_146284_a(GuiButton guibutton) {
        super.func_146284_a(guibutton);
        int id = guibutton.field_146127_k;
        if (id == 1) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.Companion);
        }
        if (id == 2) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.CompanionTalent);
        }
    }

    @Override
    protected void func_146979_b(int par1, int par2) {
    }

    @Override
    protected void func_146976_a(float f, int xMouse, int yMouse) {
        this.func_146270_b(0);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.resource);
        this.func_73729_b(this.field_147003_i, this.field_147009_r, 0, 0, this.field_146999_f, this.field_147000_g);
        this.field_146297_k.field_71446_o.func_110577_a(this.slot);
        if (this.role.getTalentLevel(EnumCompanionTalent.ARMOR) > 0) {
            for (int i = 0; i < 4; ++i) {
                this.func_73729_b(this.field_147003_i + 5, this.field_147009_r + 7 + i * 18, 0, 0, 18, 18);
            }
        }
        if (this.role.getTalentLevel(EnumCompanionTalent.SWORD) > 0) {
            this.func_73729_b(this.field_147003_i + 78, this.field_147009_r + 16, 0, this.npc.inventory.weapons.get(0) == null ? 18 : 0, 18, 18);
        }
        if (this.role.getTalentLevel(EnumCompanionTalent.RANGED) > 0) {
            // empty if block
        }
        if (this.role.talents.containsKey((Object)EnumCompanionTalent.INVENTORY)) {
            int size = (this.role.getTalentLevel(EnumCompanionTalent.INVENTORY) + 1) * 2;
            for (int i = 0; i < size; ++i) {
                this.func_73729_b(this.field_147003_i + 113 + i % 3 * 18, this.field_147009_r + 7 + i / 3 * 18, 0, 0, 18, 18);
            }
        }
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        super.func_73863_a(i, j, f);
        super.drawNpc(52, 70);
    }

    @Override
    public void save() {
    }
}

