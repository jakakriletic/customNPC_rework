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
        return this.getTag().getString("title");
    }

    @Override
    public void setTitle(String title) {
        this.getTag().setString("title", title);
    }

    @Override
    public String getAuthor() {
        return this.getTag().getString("author");
    }

    @Override
    public void setAuthor(String author) {
        this.getTag().setString("author", author);
    }

    @Override
    public String[] getText() {
        ArrayList<String> list = new ArrayList<String>();
        NBTTagList pages = this.getTag().getTagList("pages", 8);
        for (int i = 0; i < pages.tagCount(); ++i) {
            list.add(pages.getStringTagAt(i));
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public void setText(String[] pages) {
        NBTTagList list = new NBTTagList();
        if (pages != null && pages.length > 0) {
            for (String page : pages) {
                list.appendTag((NBTBase)new NBTTagString(page));
            }
        }
        this.getTag().setTag("pages", (NBTBase)list);
    }

    private NBTTagCompound getTag() {
        NBTTagCompound comp = this.item.getTagCompound();
        if (comp == null) {
            comp = new NBTTagCompound();
            this.item.setTagCompound(comp);
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

