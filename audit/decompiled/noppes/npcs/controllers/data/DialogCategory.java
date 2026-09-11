/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.api.handler.data.IDialogCategory;
import noppes.npcs.controllers.data.Dialog;

public class DialogCategory
implements IDialogCategory {
    public int id = -1;
    public String title = "";
    public HashMap<Integer, Dialog> dialogs = new HashMap();

    public void readNBT(NBTTagCompound compound) {
        this.id = compound.func_74762_e("Slot");
        this.title = compound.func_74779_i("Title");
        NBTTagList dialogsList = compound.func_150295_c("Dialogs", 10);
        if (dialogsList != null) {
            for (int ii = 0; ii < dialogsList.func_74745_c(); ++ii) {
                Dialog dialog = new Dialog(this);
                NBTTagCompound comp = dialogsList.func_150305_b(ii);
                dialog.readNBT(comp);
                dialog.id = comp.func_74762_e("DialogId");
                this.dialogs.put(dialog.id, dialog);
            }
        }
    }

    public NBTTagCompound writeNBT(NBTTagCompound nbtfactions) {
        nbtfactions.func_74768_a("Slot", this.id);
        nbtfactions.func_74778_a("Title", this.title);
        NBTTagList dialogs = new NBTTagList();
        for (Dialog dialog : this.dialogs.values()) {
            dialogs.func_74742_a((NBTBase)dialog.writeToNBT(new NBTTagCompound()));
        }
        nbtfactions.func_74782_a("Dialogs", (NBTBase)dialogs);
        return nbtfactions;
    }

    @Override
    public List<IDialog> dialogs() {
        return new ArrayList<IDialog>(this.dialogs.values());
    }

    @Override
    public String getName() {
        return this.title;
    }

    @Override
    public IDialog create() {
        return new Dialog(this);
    }
}

