package noppes.npcs.rework.linked;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M13.3: multiple selection in the wand list. Becomes active only after the server sent the rows, so a server at
 * RwLinkedGroups=0, or a player without the rights, keeps the original list (Q400, Q401). Selected rows have a [x] prefix
 * and the frame of the list marks the primary row.
 */
@SideOnly(Side.CLIENT)
public final class LinkedWandPanel {
    public static final int EDIT = 0, DELETE = 1, RESET = 2, FREEZE = 3, TELEPORT = 4, RESET_ALL = 5;
    public static final int SAME_NAME = 120, LINK = 121, COPY = 122;
    public static final int FILTER = 0, RADIUS = 1;
    public static final int LABEL_SELECTION = 100, LABEL_STATUS = 101;
    /** The modifier keys; the controller harness puts its own here, because LWJGL keys cannot be pressed from code. */
    public static BooleanSupplier shiftKey = GuiScreen::isShiftKeyDown, ctrlKey = GuiScreen::isCtrlKeyDown;

    private final WandSelection selection = new WandSelection();
    private final List<String> groups = new ArrayList<String>();
    private GuiNPCInterface gui;
    private GuiCustomScroll scroll;
    private boolean active;
    private int listRequest = -1, actionRequest = -1, total;
    private String status = "", radius = "32";

    public boolean active() {
        return this.active;
    }

    /** For the controller harness. */
    public WandSelection selection() {
        return this.selection;
    }

    public boolean pending() {
        return this.listRequest >= 0 || this.actionRequest >= 0;
    }

    public void refresh() {
        this.listRequest = LinkedClient.send(LinkedGuiCodec.BatchOp.LIST, 0, -1, "", new int[0]);
    }

    private void action(LinkedGuiCodec.BatchOp op, int mask, int source, String group, int[] ids) {
        this.actionRequest = LinkedClient.send(op, mask, source, group, ids);
    }

    /** From initGui of the list. */
    public void init(GuiNPCInterface gui, GuiCustomScroll scroll) {
        this.gui = gui;
        this.scroll = scroll;
        if (!this.active) {
            return;
        }
        int x = gui.guiLeft, y = gui.guiTop;
        scroll.guiTop = y + 22;
        // The two text lines stay inside the frame of the original list, which ends at y + 212 (Q409, Q410).
        scroll.setSize(165, 162);
        gui.addTextField(new GuiNpcTextField(FILTER, gui, x + 5, y + 5, 163, 15, this.selection.filter()));
        GuiNpcTextField field = new GuiNpcTextField(RADIUS, gui, x + 228, y + 94, 24, 20, this.radius);
        field.numbersOnly = true;
        field.setMinMaxDefault(0, 9999, 32);
        gui.addTextField(field);
        gui.addButton(new GuiNpcButton(SAME_NAME, x + 170, y + 94, 56, 20, "Same name"));
        gui.addButton(new GuiNpcButton(LINK, x + 170, y + 116, 82, 20, "Link to group"));
        gui.addButton(new GuiNpcButton(COPY, x + 170, y + 138, 82, 20, "Copy from"));
        gui.getButton(RESET_ALL).y = y + 160;
        gui.getButton(FREEZE).y = y + 182;
        gui.addLabel(new GuiNpcLabel(LABEL_SELECTION, "", x + 5, y + 188));
        // Below Freeze (ends at y + 202), so even a long status cannot run behind that button (Q410).
        gui.addLabel(new GuiNpcLabel(LABEL_STATUS, "", x + 5, y + 203));
        fill();
    }

    /** @return true when these were the first rows and the list has to add the controls */
    public boolean rows(LinkedGuiCodec.ListResponse reply) {
        if (reply.id != this.listRequest) {
            return false;
        }
        this.listRequest = -1;
        this.total = reply.total;
        this.groups.clear();
        for (LinkedGuiCodec.Group group : reply.groups) {
            this.groups.add(group.name);
        }
        this.selection.setRows(reply.rows, reply.groups);
        boolean first = !this.active;
        this.active = true;
        if (!first) {
            fill();
        }
        return first;
    }

    public void reply(LinkedGuiCodec.Response reply) {
        if (reply.id == this.listRequest) {
            this.listRequest = -1;
            this.status = reply.text;
        } else if (reply.id == this.actionRequest) {
            this.actionRequest = -1;
            this.status = reply.text;
            if (reply.result == LinkedGuiCodec.RESULT_OK) {
                refresh();
            }
        } else {
            return;
        }
        update();
    }

    /** The rows of the list from the selection; the list is rebuilt, because equal texts would keep stale rows. */
    private void fill() {
        if (this.gui == null || this.gui.getButton(LINK) == null) {
            return;
        }
        List<String> texts = new ArrayList<String>();
        for (int i = 0; i < this.selection.visibleCount(); i++) {
            texts.add(this.selection.text(i));
        }
        this.scroll.clear();
        this.scroll.setUnsortedList(texts);
        update();
    }

    private void update() {
        if (this.gui == null || this.gui.getButton(LINK) == null) {
            return;
        }
        // In place: setting a new list would scroll back to the top on every click.
        List<String> list = this.scroll.getList();
        for (int i = 0; i < list.size() && i < this.selection.visibleCount(); i++) {
            list.set(i, this.selection.text(i));
        }
        this.scroll.selected = this.selection.primaryIndex();
        LinkedGuiCodec.Row main = this.selection.primaryRow();
        int count = this.selection.count();
        this.gui.getButton(EDIT).enabled = main != null;
        this.gui.getButton(TELEPORT).enabled = main != null;
        this.gui.getButton(SAME_NAME).enabled = main != null;
        this.gui.getButton(DELETE).enabled = count > 0;
        this.gui.getButton(RESET).enabled = count > 0;
        this.gui.getButton(LINK).enabled = count > 0 && !this.groups.isEmpty();
        this.gui.getButton(COPY).enabled = count > 1;
        String group = main == null ? null : this.selection.group(main);
        String summary = count + " of " + this.selection.visibleCount()
                + (this.total > LinkedGuiCodec.MAX_ROWS ? " (nearest of " + this.total + ")" : "")
                + (group == null ? "" : "; " + group + " loaded");
        // Three digit counts do not fit with the first word, and the count of the group must stay readable (Q431).
        String full = "Selected " + summary;
        boolean fits = Minecraft.getMinecraft().fontRenderer.getStringWidth(full) <= 163;
        this.gui.getLabel(LABEL_SELECTION).label = fitLine(fits ? full : summary, 163);
        this.gui.getLabel(LABEL_STATUS).label = fitLine(this.status, 247);
    }

    /** Keep arbitrary group names and server messages inside the available column; rows still show group and count. */
    private static String fitLine(String text, int width) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        return font.getStringWidth(text) <= width ? text
                : font.trimStringToWidth(text, width - font.getStringWidth("...")) + "...";
    }

    /** From the list when a row was clicked. */
    public void clicked() {
        int index = this.scroll.selected;
        if (index >= 0 && index < this.selection.visibleCount()) {
            this.selection.click(index, shiftKey.getAsBoolean(), ctrlKey.getAsBoolean());
            update();
        }
    }

    /** From keyTyped of the list. */
    public void typed() {
        GuiNpcTextField field = this.gui.getTextField(FILTER);
        if (field != null && !field.getText().equals(this.selection.filter())) {
            this.selection.setFilter(field.getText());
            fill();
        }
        GuiNpcTextField distance = this.gui.getTextField(RADIUS);
        if (distance != null) {
            this.radius = distance.getText();
        }
    }

    /** From the list for its own buttons and the buttons of this panel, except reset all and freeze. */
    public void button(int id, GuiYesNoCallback confirm) {
        LinkedGuiCodec.Row main = this.selection.primaryRow();
        if (id == EDIT && main != null) {
            Client.sendData(EnumPacketServer.RemoteMainMenu, main.id);
        } else if (id == DELETE && this.selection.count() > 0) {
            this.gui.displayGuiScreen(new GuiYesNo(confirm, "", "Delete " + this.selection.count() + " selected NPCs?", 0));
        } else if (id == RESET && this.selection.count() > 0) {
            int[] ids = this.selection.ids();
            action(LinkedGuiCodec.BatchOp.RESET, 0, -1, "", ids);
            for (int each : ids) {
                Entity entity = this.gui.player.world.getEntityByID(each);
                if (entity instanceof EntityNPCInterface) {
                    ((EntityNPCInterface) entity).reset();
                }
            }
        } else if (id == TELEPORT && main != null) {
            Client.sendData(EnumPacketServer.RemoteTpToNpc, main.id);
            this.gui.close();
        } else if (id == SAME_NAME && main != null) {
            GuiNpcTextField field = this.gui.getTextField(RADIUS);
            field.unFocused();
            this.radius = field.getText();
            this.selection.sameName(field.getInteger());
            update();
        } else if (id == LINK && this.selection.count() > 0) {
            this.gui.setSubGui(new LinkedWandLinkGui(this.groups, this.selection.count()));
        } else if (id == COPY && this.selection.count() > 1) {
            this.gui.setSubGui(new LinkedWandCopyGui(main.name, this.selection.count() - 1));
        }
    }

    /** The answer of the delete dialog. */
    public void confirmed() {
        action(LinkedGuiCodec.BatchOp.DELETE, 0, -1, "", this.selection.ids());
    }

    public void closed(SubGuiInterface sub) {
        if (sub instanceof LinkedWandLinkGui && ((LinkedWandLinkGui) sub).chosen != null) {
            action(LinkedGuiCodec.BatchOp.LINK, 0, -1, ((LinkedWandLinkGui) sub).chosen, this.selection.ids());
        } else if (sub instanceof LinkedWandCopyGui && ((LinkedWandCopyGui) sub).accepted && this.selection.primaryRow() != null) {
            action(LinkedGuiCodec.BatchOp.COPY, ((LinkedWandCopyGui) sub).mask, this.selection.primaryRow().id, "", this.selection.targets());
        }
    }
}
