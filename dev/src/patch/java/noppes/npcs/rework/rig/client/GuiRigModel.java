package noppes.npcs.rework.rig.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.SubGuiInterface;

/** Select a model from the server's synchronized catalogue and the built-in rigs. */
public final class GuiRigModel extends SubGuiInterface {
    private static final int ROWS = 5;
    private final List<String> models = new ArrayList<>();
    private String choice, selected;
    private int page;

    public GuiRigModel(String current, List<String> ids) {
        this.xSize = 240;
        this.ySize = 176;
        this.choice = current;
        models.add("");
        models.addAll(ids);
        page = Math.max(0, models.indexOf(current)) / ROWS;
    }

    public String selected() { return selected; }

    @Override public void initGui() {
        super.initGui();
        addLabel(new GuiNpcLabel(0, "Rig model: " + (choice.isEmpty() ? "Original" : choice), guiLeft + 10, guiTop + 8));
        for (int i = 0; i < ROWS && page * ROWS + i < models.size(); i++) {
            String model = models.get(page * ROWS + i);
            addButton(new GuiNpcButton(10 + i, guiLeft + 10, guiTop + 22 + i * 21, 220, 20,
                    (choice.equals(model) ? "> " : "") + (model.isEmpty() ? "Original" : model)));
        }
        addButton(new GuiNpcButton(0, guiLeft + 10, guiTop + 132, 30, 20, "<", page > 0));
        addButton(new GuiNpcButton(1, guiLeft + 44, guiTop + 132, 30, 20, ">", (page + 1) * ROWS < models.size()));
        addButton(new GuiNpcButton(2, guiLeft + 100, guiTop + 132, 62, 20, "Cancel"));
        addButton(new GuiNpcButton(3, guiLeft + 168, guiTop + 132, 62, 20, "Select"));
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) { page--; }
        else if (button.id == 1) { page++; }
        else if (button.id == 2 || button.id == 3) {
            if (button.id == 3) { selected = choice; }
            close();
            return;
        } else { choice = models.get(page * ROWS + button.id - 10); }
        initGui();
    }
}
