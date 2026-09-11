/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.gui.player.companion;

import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiMenuTopIconButton;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.constants.EnumCompanionJobs;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;

public class GuiNpcCompanionStats
extends GuiNPCInterface
implements IGuiData {
    private RoleCompanion role;
    private boolean isEating = false;

    public GuiNpcCompanionStats(EntityNPCInterface npc) {
        super(npc);
        this.role = (RoleCompanion)npc.roleInterface;
        this.closeOnEsc = true;
        this.setBackground("companion.png");
        this.xSize = 171;
        this.ySize = 166;
        NoppesUtilPlayer.sendData(EnumPlayerPacket.RoleGet, new Object[0]);
    }

    @Override
    public void initGui() {
        super.initGui();
        int y = this.guiTop + 10;
        this.addLabel(new GuiNpcLabel(0, NoppesStringUtils.translate("gui.name", ": ", this.npc.display.getName()), this.guiLeft + 4, y));
        this.addLabel(new GuiNpcLabel(1, NoppesStringUtils.translate("companion.owner", ": ", this.role.ownerName), this.guiLeft + 4, y += 12));
        this.addLabel(new GuiNpcLabel(2, NoppesStringUtils.translate("companion.age", ": ", this.role.ticksActive / 18000L + " (", this.role.stage.name, ")"), this.guiLeft + 4, y += 12));
        this.addLabel(new GuiNpcLabel(3, NoppesStringUtils.translate("companion.strength", ": ", this.npc.stats.melee.getStrength()), this.guiLeft + 4, y += 12));
        this.addLabel(new GuiNpcLabel(4, NoppesStringUtils.translate("companion.level", ": ", this.role.getTotalLevel()), this.guiLeft + 4, y += 12));
        this.addLabel(new GuiNpcLabel(5, NoppesStringUtils.translate("job.name", ": ", "gui.none"), this.guiLeft + 4, y += 12));
        GuiNpcCompanionStats.addTopMenu(this.role, this, 1);
    }

    public static void addTopMenu(RoleCompanion role, GuiScreen screen, int active) {
        GuiMenuTopIconButton button;
        Object gui;
        if (screen instanceof GuiNPCInterface) {
            gui = (GuiNPCInterface)screen;
            button = new GuiMenuTopIconButton(1, ((GuiNPCInterface)((Object)gui)).guiLeft + 4, ((GuiNPCInterface)((Object)gui)).guiTop - 27, "menu.stats", new ItemStack(Items.BOOK));
            ((GuiNPCInterface)((Object)gui)).addTopButton(button);
            button = new GuiMenuTopIconButton(2, (GuiButton)button, "companion.talent", new ItemStack(Items.NETHER_STAR));
            ((GuiNPCInterface)((Object)gui)).addTopButton(button);
            if (role.hasInv()) {
                button = new GuiMenuTopIconButton(3, (GuiButton)button, "inv.inventory", new ItemStack((Block)Blocks.CHEST));
                ((GuiNPCInterface)((Object)gui)).addTopButton(button);
            }
            if (role.job != EnumCompanionJobs.NONE) {
                ((GuiNPCInterface)((Object)gui)).addTopButton(new GuiMenuTopIconButton(4, (GuiButton)button, "job.name", new ItemStack(Items.CARROT)));
            }
            ((GuiNPCInterface)((Object)gui)).getTopButton((int)active).active = true;
        }
        if (screen instanceof GuiContainerNPCInterface) {
            gui = (GuiContainerNPCInterface)screen;
            button = new GuiMenuTopIconButton(1, ((GuiContainerNPCInterface)((Object)gui)).guiLeft + 4, ((GuiContainerNPCInterface)((Object)gui)).guiTop - 27, "menu.stats", new ItemStack(Items.BOOK));
            ((GuiContainerNPCInterface)((Object)gui)).addTopButton(button);
            button = new GuiMenuTopIconButton(2, (GuiButton)button, "companion.talent", new ItemStack(Items.NETHER_STAR));
            ((GuiContainerNPCInterface)((Object)gui)).addTopButton(button);
            if (role.hasInv()) {
                button = new GuiMenuTopIconButton(3, (GuiButton)button, "inv.inventory", new ItemStack((Block)Blocks.CHEST));
                ((GuiContainerNPCInterface)((Object)gui)).addTopButton(button);
            }
            if (role.job != EnumCompanionJobs.NONE) {
                ((GuiContainerNPCInterface)((Object)gui)).addTopButton(new GuiMenuTopIconButton(4, (GuiButton)button, "job.name", new ItemStack(Items.CARROT)));
            }
            ((GuiContainerNPCInterface)((Object)gui)).getTopButton((int)active).active = true;
        }
    }

    @Override
    public void actionPerformed(GuiButton guibutton) {
        super.actionPerformed(guibutton);
        int id = guibutton.id;
        if (id == 2) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.CompanionTalent);
        }
        if (id == 3) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.CompanionOpenInv, new Object[0]);
        }
    }

    @Override
    public void drawScreen(int i, int j, float f) {
        super.drawScreen(i, j, f);
        if (this.isEating && !this.role.isEating()) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.RoleGet, new Object[0]);
        }
        this.isEating = this.role.isEating();
        super.drawNpc(34, 150);
        int y = this.drawHealth(this.guiTop + 88);
    }

    private int drawHealth(int y) {
        int x;
        int i;
        this.mc.getTextureManager().bindTexture(ICONS);
        int max = this.role.getTotalArmorValue();
        if (this.role.talents.containsKey((Object)EnumCompanionTalent.ARMOR) || max > 0) {
            for (int i2 = 0; i2 < 10; ++i2) {
                int x2 = this.guiLeft + 66 + i2 * 10;
                if (i2 * 2 + 1 < max) {
                    this.drawTexturedModalRect(x2, y, 34, 9, 9, 9);
                }
                if (i2 * 2 + 1 == max) {
                    this.drawTexturedModalRect(x2, y, 25, 9, 9, 9);
                }
                if (i2 * 2 + 1 <= max) continue;
                this.drawTexturedModalRect(x2, y, 16, 9, 9, 9);
            }
            y += 10;
        }
        max = MathHelper.ceil((float)this.npc.getMaxHealth());
        int k = (int)this.npc.getHealth();
        float scale = 1.0f;
        if (max > 40) {
            scale = (float)max / 40.0f;
            k = (int)((float)k / scale);
            max = 40;
        }
        for (i = 0; i < max; ++i) {
            x = this.guiLeft + 66 + i % 20 * 5;
            int offset = i / 20 * 10;
            this.drawTexturedModalRect(x, y + offset, 52 + i % 2 * 5, 9, i % 2 == 1 ? 4 : 5, 9);
            if (k <= i) continue;
            this.drawTexturedModalRect(x, y + offset, 52 + i % 2 * 5, 0, i % 2 == 1 ? 4 : 5, 9);
        }
        k = this.role.foodstats.getFoodLevel();
        y += 10;
        if (max > 20) {
            y += 10;
        }
        for (i = 0; i < 20; ++i) {
            x = this.guiLeft + 66 + i % 20 * 5;
            this.drawTexturedModalRect(x, y, 16 + i % 2 * 5, 27, i % 2 == 1 ? 4 : 5, 9);
            if (k <= i) continue;
            this.drawTexturedModalRect(x, y, 52 + i % 2 * 5, 27, i % 2 == 1 ? 4 : 5, 9);
        }
        return y;
    }

    @Override
    public void save() {
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.role.readFromNBT(compound);
    }
}

