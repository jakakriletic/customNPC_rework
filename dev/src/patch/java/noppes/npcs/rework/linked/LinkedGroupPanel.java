package noppes.npcs.rework.linked;

import java.util.Objects;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;

/**
 * M13.2b (Q392): category boxes and undo in Global, Linked NPCs. Appears only after the server answered, so a server
 * at RwLinkedGroups=0 keeps the original GUI. A box changes only the selection here; a transfer reaches every member,
 * so nothing leaves the client before Apply (Q398). The labels follow the order of the bits in {@link LinkedCategories}.
 */
@SideOnly(Side.CLIENT)
public final class LinkedGroupPanel {
    public static final int FIRST_BOX = 100, APPLY = 110, UNDO = 111;
    private static final int LABEL_TITLE = 100, LABEL_GROUP = 101, LABEL_STATUS = 102;
    static final String[] LABELS =
            {"Appearance", "Name and title", "Stats", "AI", "Inventory", "Advanced", "Scripts", "Transform"};

    private GuiNPCInterface gui;
    private boolean answered, allowed, waiting, undo;
    private String group, status = "";
    private int saved, pending, loaded, request;

    public LinkedGroupPanel() {
        this.request = LinkedClient.send(LinkedGuiCodec.Op.GET, 0, "");
        this.waiting = true;
    }

    /** From initGui of the manager; the selection of the list can also change when a group is added or removed. */
    public void init(GuiNPCInterface gui, String selected) {
        this.gui = gui;
        select(selected);
        if (!this.answered) {
            return;
        }
        int x = gui.guiLeft + 6, y = gui.guiTop + 6;
        gui.addLabel(new GuiNpcLabel(LABEL_TITLE, "Passed on to members:", x, y));
        for (int i = 0; i < LABELS.length; i++) {
            gui.addButton(new GuiNpcButton(FIRST_BOX + i, x + i % 2 * 102, y + 12 + i / 2 * 22, 100, 20, ""));
        }
        gui.addButton(new GuiNpcButton(APPLY, x, y + 104, 100, 20, "Apply"));
        gui.addButton(new GuiNpcButton(UNDO, x + 102, y + 104, 100, 20, "Undo last change"));
        gui.addLabel(new GuiNpcLabel(LABEL_GROUP, "", x, y + 130));
        gui.addLabel(new GuiNpcLabel(LABEL_STATUS, "", x, y + 142));
        refresh();
    }

    /** From the list of the manager. */
    public void select(String selected) {
        if (Objects.equals(selected, this.group)) {
            return;
        }
        this.group = selected;
        this.undo = false;
        this.status = "";
        // A name too long for the channel leaves the controls disabled, as a request in flight does.
        this.waiting = true;
        if (selected == null) {
            this.waiting = false;
        } else if (LinkedGuiCodec.validGroup(selected)) {
            this.request = LinkedClient.send(LinkedGuiCodec.Op.GET, 0, selected);
        } else {
            this.status = "Group name too long";
        }
        refresh();
    }

    /** @return true when a button of this panel took the click */
    public boolean button(int id) {
        if (id >= FIRST_BOX && id < FIRST_BOX + LABELS.length) {
            this.pending ^= 1 << (id - FIRST_BOX);
            refresh();
        } else if (id == APPLY) {
            action(LinkedGuiCodec.Op.SET, this.pending);
        } else if (id == UNDO) {
            action(LinkedGuiCodec.Op.UNDO, 0);
        } else {
            return false;
        }
        return true;
    }

    private void action(LinkedGuiCodec.Op op, int mask) {
        this.waiting = true;
        this.request = LinkedClient.send(op, mask, this.group);
        refresh();
    }

    /** @return true when this was the first answer and the manager has to add the controls */
    public boolean reply(LinkedGuiCodec.Response reply) {
        if (reply.id != this.request) {
            return false;
        }
        boolean first = !this.answered;
        this.answered = true;
        this.waiting = false;
        this.allowed = reply.result != LinkedGuiCodec.RESULT_DENIED;
        this.status = reply.text;
        if (reply.result == LinkedGuiCodec.RESULT_OK || reply.result == LinkedGuiCodec.RESULT_NOTHING) {
            this.saved = reply.mask;
            this.pending = reply.mask;
            this.loaded = reply.loaded;
            this.undo = reply.undo;
        } else {
            // Without an answer about this group nothing of the previous one may stay on the screen (Q399).
            this.saved = 0;
            this.pending = 0;
            this.loaded = 0;
            this.undo = false;
        }
        if (!first) {
            refresh();
        }
        return first;
    }

    private void refresh() {
        if (this.gui == null || this.gui.getButton(APPLY) == null) {
            return;
        }
        boolean usable = this.allowed && this.group != null && !this.waiting;
        for (int i = 0; i < LABELS.length; i++) {
            GuiNpcButton box = this.gui.getButton(FIRST_BOX + i);
            box.setDisplayText((LinkedCategories.has(this.pending, 1 << i) ? "[x] " : "[ ] ") + LABELS[i]);
            box.enabled = usable;
        }
        this.gui.getButton(APPLY).enabled = usable && this.pending != this.saved;
        this.gui.getButton(UNDO).enabled = usable && this.undo;
        this.gui.getLabel(LABEL_GROUP).label = this.group == null ? "Select a group" : "Loaded members: " + this.loaded;
        this.gui.getLabel(LABEL_STATUS).label = this.status;
    }
}
