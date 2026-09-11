/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.player;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.containers.ContainerNPCFollower;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleFollower;

public class GuiNpcFollower
extends GuiContainerNPCInterface
implements IGuiData {
    private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/follower.png");
    private EntityNPCInterface npc;
    private RoleFollower role;

    public GuiNpcFollower(EntityNPCInterface npc, ContainerNPCFollower container) {
        super(npc, container);
        this.npc = npc;
        this.role = (RoleFollower)npc.roleInterface;
        this.closeOnEsc = true;
        NoppesUtilPlayer.sendData(EnumPlayerPacket.RoleGet, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.field_146292_n.clear();
        this.addButton(new GuiNpcButton(4, this.field_147003_i + 100, this.field_147009_r + 110, 50, 20, new String[]{I18n.func_74838_a((String)"follower.waiting"), I18n.func_74838_a((String)"follower.following")}, this.role.isFollowing ? 1 : 0));
        if (!this.role.infiniteDays) {
            this.addButton(new GuiNpcButton(5, this.field_147003_i + 8, this.field_147009_r + 30, 50, 20, I18n.func_74838_a((String)"follower.hire")));
        }
    }

    @Override
    public void func_146284_a(GuiButton guibutton) {
        super.func_146284_a(guibutton);
        int id = guibutton.field_146127_k;
        if (id == 4) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.FollowerState, new Object[0]);
        }
        if (id == 5) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.FollowerExtend, new Object[0]);
        }
    }

    @Override
    protected void func_146979_b(int par1, int par2) {
        this.field_146289_q.func_78276_b(I18n.func_74838_a((String)"follower.health") + ": " + this.npc.func_110143_aJ() + "/" + this.npc.func_110138_aP(), 62, 70, CustomNpcResourceListener.DefaultTextColor);
        if (!this.role.infiniteDays) {
            if (this.role.getDays() <= 1) {
                this.field_146289_q.func_78276_b(I18n.func_74838_a((String)"follower.daysleft") + ": " + I18n.func_74838_a((String)"follower.lastday"), 62, 94, CustomNpcResourceListener.DefaultTextColor);
            } else {
                this.field_146289_q.func_78276_b(I18n.func_74838_a((String)"follower.daysleft") + ": " + (this.role.getDays() - 1), 62, 94, CustomNpcResourceListener.DefaultTextColor);
            }
        }
    }

    @Override
    protected void func_146976_a(float f, int i, int j) {
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.resource);
        int l = this.field_147003_i;
        int i1 = this.field_147009_r;
        this.func_73729_b(l, i1, 0, 0, this.field_146999_f, this.field_147000_g);
        int index = 0;
        if (!this.role.infiniteDays) {
            for (int slot = 0; slot < this.role.inventory.items.size(); ++slot) {
                ItemStack itemstack = (ItemStack)this.role.inventory.items.get(slot);
                if (NoppesUtilServer.IsItemStackNull(itemstack)) continue;
                int days = 1;
                if (this.role.rates.containsKey(slot)) {
                    days = this.role.rates.get(slot);
                }
                int yOffset = index * 20;
                int x = this.field_147003_i + 68;
                int y = this.field_147009_r + yOffset + 4;
                GlStateManager.func_179091_B();
                RenderHelper.func_74520_c();
                this.field_146296_j.func_180450_b(itemstack, x + 11, y);
                this.field_146296_j.func_175030_a(this.field_146289_q, itemstack, x + 11, y);
                RenderHelper.func_74518_a();
                GlStateManager.func_179101_C();
                String daysS = days + " " + (days == 1 ? I18n.func_74838_a((String)"follower.day") : I18n.func_74838_a((String)"follower.days"));
                this.field_146289_q.func_78276_b(" = " + daysS, x + 27, y + 4, CustomNpcResourceListener.DefaultTextColor);
                if (this.func_146978_c(x - this.field_147003_i + 11, y - this.field_147009_r, 16, 16, this.mouseX, this.mouseY)) {
                    this.func_146285_a(itemstack, this.mouseX, this.mouseY);
                }
                ++index;
            }
        }
        this.drawNpc(33, 131);
    }

    @Override
    public void save() {
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.npc.roleInterface.readFromNBT(compound);
        this.func_73866_w_();
    }
}

