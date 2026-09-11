/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 */
package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.api.item.IItemBook;
import noppes.npcs.api.wrapper.ItemStackWrapper;

public class ItemBookWrapper
extends ItemStackWrapper
implements IItemBook {
    protected ItemBookWrapper(ItemStack item) {
        super(item);
    }

    @Override
    public String getTitle() {
        return this.getTag().func_74779_i("title");
    }

    @Override
    public void setTitle(String title) {
        this.getTag().func_74778_a("title", title);
    }

    @Override
    public String getAuthor() {
        return this.getTag().func_74779_i("author");
    }

    @Override
    public void setAuthor(String author) {
        this.getTag().func_74778_a("author", author);
    }

    @Override
    public String[] getText() {
        ArrayList<String> list = new ArrayList<String>();
        NBTTagList pages = this.getTag().func_150295_c("pages", 8);
        for (int i = 0; i < pages.func_74745_c(); ++i) {
            list.add(pages.func_150307_f(i));
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public void setText(String[] pages) {
        NBTTagList list = new NBTTagList();
        if (pages != null && pages.length > 0) {
            for (String page : pages) {
                list.func_74742_a((NBTBase)new NBTTagString(page));
            }
        }
        this.getTag().func_74782_a("pages", (NBTBase)list);
    }

    private NBTTagCompound getTag() {
        NBTTagCompound comp = this.item.func_77978_p();
        if (comp == null) {
            comp = new NBTTagCompound();
            this.item.func_77982_d(comp);
        }
        return comp;
    }

    @Override
    public boolean isBook() {
        return true;
    }

    @Override
    public int getType() {
        return 1;
    }
}

