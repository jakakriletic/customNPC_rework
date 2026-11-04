package noppes.npcs.rework.linked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * M13.3: selection of the wand list by entity id, so it survives a refresh of the rows. Rows hidden by the filter leave
 * the selection, so an action never reaches an NPC the player cannot see in the list (Q402). The primary row is the one
 * clicked last: the source of a copy and the target of edit and teleport.
 */
public final class WandSelection {
    private List<LinkedGuiCodec.Row> rows = Collections.emptyList();
    private List<LinkedGuiCodec.Group> groups = Collections.emptyList();
    private final List<LinkedGuiCodec.Row> visible = new ArrayList<LinkedGuiCodec.Row>();
    private final Set<Integer> selected = new LinkedHashSet<Integer>();
    private int primary = -1, anchor = -1;
    private String filter = "";

    public void setRows(List<LinkedGuiCodec.Row> rows, List<LinkedGuiCodec.Group> groups) {
        this.rows = rows;
        this.groups = groups;
        rebuild();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        rebuild();
    }

    public String filter() {
        return this.filter;
    }

    private void rebuild() {
        this.visible.clear();
        String part = this.filter.trim().toLowerCase(Locale.ROOT);
        Set<Integer> ids = new LinkedHashSet<Integer>();
        for (LinkedGuiCodec.Row row : this.rows) {
            if (row.name.toLowerCase(Locale.ROOT).contains(part)) {
                this.visible.add(row);
                ids.add(row.id);
            }
        }
        this.selected.retainAll(ids);
        if (!this.selected.contains(this.primary)) {
            this.primary = last();
        }
        if (!ids.contains(this.anchor)) {
            this.anchor = this.primary;
        }
    }

    private int last() {
        int id = -1;
        for (int each : this.selected) {
            id = each;
        }
        return id;
    }

    /** One click on a visible row: alone it replaces the selection, Ctrl toggles, Shift takes the range from the anchor. */
    public void click(int index, boolean shift, boolean ctrl) {
        int id = this.visible.get(index).id;
        int from = indexOf(this.anchor);
        if (shift && from >= 0) {
            if (!ctrl) {
                this.selected.clear();
            }
            for (int i = Math.min(from, index); i <= Math.max(from, index); i++) {
                this.selected.add(this.visible.get(i).id);
            }
            this.primary = id;
            return;
        }
        this.anchor = id;
        if (ctrl) {
            if (this.selected.remove(id)) {
                if (this.primary == id) {
                    this.primary = last();
                }
            } else {
                this.selected.add(id);
                this.primary = id;
            }
            return;
        }
        this.selected.clear();
        this.selected.add(id);
        this.primary = id;
    }

    /** Selects every visible row with the name of the primary within the radius of the player; 0 = any distance. */
    public void sameName(int radius) {
        LinkedGuiCodec.Row main = primaryRow();
        if (main == null) {
            return;
        }
        this.selected.clear();
        for (LinkedGuiCodec.Row row : this.visible) {
            if (row.name.equals(main.name) && (radius <= 0 || row.distance <= radius)) {
                this.selected.add(row.id);
            }
        }
        this.selected.add(main.id);
    }

    private int indexOf(int id) {
        for (int i = 0; i < this.visible.size(); i++) {
            if (this.visible.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    public int visibleCount() {
        return this.visible.size();
    }

    public int count() {
        return this.selected.size();
    }

    public boolean isSelected(int index) {
        return this.selected.contains(this.visible.get(index).id);
    }

    public LinkedGuiCodec.Row row(int index) {
        return this.visible.get(index);
    }

    public int primaryIndex() {
        return indexOf(this.primary);
    }

    public LinkedGuiCodec.Row primaryRow() {
        int index = primaryIndex();
        return index < 0 ? null : this.visible.get(index);
    }

    public int[] ids() {
        return toArray(-1);
    }

    /** The selection without the primary: what a copy writes to. */
    public int[] targets() {
        return toArray(this.primary);
    }

    private int[] toArray(int skip) {
        int[] out = new int[this.selected.contains(skip) ? this.selected.size() - 1 : this.selected.size()];
        int i = 0;
        for (int id : this.selected) {
            if (id != skip) {
                out[i++] = id;
            }
        }
        return out;
    }

    /** "Group 3" with the loaded members, or null for a row without a group. */
    public String group(LinkedGuiCodec.Row row) {
        if (row.group < 0 || row.group >= this.groups.size()) {
            return null;
        }
        LinkedGuiCodec.Group group = this.groups.get(row.group);
        return group.name + " " + group.loaded;
    }

    public String text(int index) {
        LinkedGuiCodec.Row row = this.visible.get(index);
        String group = group(row);
        return (isSelected(index) ? "[x] " : "") + Math.round(row.distance) + " : " + row.name + (group == null ? "" : " [" + group + "]");
    }
}
