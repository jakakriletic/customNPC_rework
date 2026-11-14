package noppes.npcs.client.gui.mainmenu;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManagePlayerData;
import noppes.npcs.client.gui.global.GuiNpcNaturalSpawns;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.army.ArmyCommandService;
import noppes.npcs.rework.army.GuiArmyRegistry;

public class GuiNPCGlobalMainMenu
extends GuiNPCInterface2 {
    public GuiNPCGlobalMainMenu(EntityNPCInterface npc) {
        super(npc, 5);
    }

    @Override
    public void initGui() {
        super.initGui();
        int y = this.guiTop + 10;
        this.addButton(new GuiNpcButton(2, this.guiLeft + 85, y, "global.banks"));
        this.addButton(new GuiNpcButton(3, this.guiLeft + 85, y += 22, "menu.factions"));
        this.addButton(new GuiNpcButton(4, this.guiLeft + 85, y += 22, "dialog.dialogs"));
        this.addButton(new GuiNpcButton(11, this.guiLeft + 85, y += 22, "quest.quests"));
        this.addButton(new GuiNpcButton(12, this.guiLeft + 85, y += 22, "global.transport"));
        this.addButton(new GuiNpcButton(13, this.guiLeft + 85, y += 22, "global.playerdata"));
        this.addButton(new GuiNpcButton(14, this.guiLeft + 85, y += 22, NoppesStringUtils.translate("global.recipes", "(Broken)")));
        this.addButton(new GuiNpcButton(15, this.guiLeft + 85, y += 22, NoppesStringUtils.translate("global.naturalspawn", "(WIP)")));
        this.addButton(new GuiNpcButton(16, this.guiLeft + 85, y += 22, "global.linked"));
        if (CustomNpcs.RwArmies == 1 && this.player.canUseCommand(ArmyCommandService.OPERATOR_LEVEL, ArmyCommandService.COMMAND_NAME)) {
            this.addButton(new GuiNpcButton(17, this.guiLeft + 293, this.guiTop + 10, 115, 20, "Armies"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        int id = guibutton.id;
        if (id == 11) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageQuests);
        }
        if (id == 2) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageBanks);
        }
        if (id == 3) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageFactions);
        }
        if (id == 4) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageDialogs);
        }
        if (id == 12) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageTransport);
        }
        if (id == 13) {
            NoppesUtil.openGUI((EntityPlayer)this.player, new GuiNpcManagePlayerData(this.npc, this));
        }
        if (id == 14) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageRecipes, 4, 0, 0);
        }
        if (id == 15) {
            NoppesUtil.openGUI((EntityPlayer)this.player, new GuiNpcNaturalSpawns(this.npc));
        }
        if (id == 16) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageLinked);
        }
        if (id == 17) {
            this.mc.displayGuiScreen(new GuiArmyRegistry(this));
        }
    }

    @Override
    public void save() {
    }
}
