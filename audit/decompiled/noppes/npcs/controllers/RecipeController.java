/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.IRecipe
 *  net.minecraft.item.crafting.Ingredient
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.NonNullList
 *  net.minecraftforge.registries.IForgeRegistry
 */
package noppes.npcs.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.registries.IForgeRegistry;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.Server;
import noppes.npcs.api.handler.IRecipeHandler;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.controllers.data.RecipesDefault;

public class RecipeController
implements IRecipeHandler {
    public HashMap<Integer, RecipeCarpentry> globalRecipes = new HashMap();
    public HashMap<Integer, RecipeCarpentry> anvilRecipes = new HashMap();
    public static RecipeController instance;
    public static final int version = 1;
    public int nextId = 1;
    public static HashMap<Integer, RecipeCarpentry> syncRecipes;
    public static IForgeRegistry<IRecipe> Registry;

    public RecipeController() {
        instance = this;
    }

    public void load() {
        this.loadCategories();
        this.reloadGlobalRecipes();
        EventHooks.onGlobalRecipesLoaded(this);
    }

    public void reloadGlobalRecipes() {
    }

    private void loadCategories() {
        File saveDir = CustomNpcs.getWorldSaveDirectory();
        try {
            File file = new File(saveDir, "recipes.dat");
            if (file.exists()) {
                this.loadCategories(file);
            } else {
                this.globalRecipes.clear();
                this.anvilRecipes.clear();
                this.loadDefaultRecipes(-1);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            try {
                File file = new File(saveDir, "recipes.dat_old");
                if (file.exists()) {
                    this.loadCategories(file);
                }
            }
            catch (Exception ee) {
                e.printStackTrace();
            }
        }
    }

    private void loadDefaultRecipes(int i) {
        if (i == 1) {
            return;
        }
        RecipesDefault.loadDefaultRecipes(i);
        this.saveCategories();
    }

    private void loadCategories(File file) throws Exception {
        NBTTagCompound nbttagcompound1 = CompressedStreamTools.func_74796_a((InputStream)new FileInputStream(file));
        this.nextId = nbttagcompound1.func_74762_e("LastId");
        NBTTagList list = nbttagcompound1.func_150295_c("Data", 10);
        HashMap<Integer, RecipeCarpentry> globalRecipes = new HashMap<Integer, RecipeCarpentry>();
        HashMap<Integer, RecipeCarpentry> anvilRecipes = new HashMap<Integer, RecipeCarpentry>();
        if (list != null) {
            for (int i = 0; i < list.func_74745_c(); ++i) {
                RecipeCarpentry recipe = RecipeCarpentry.read(list.func_150305_b(i));
                if (recipe.isGlobal) {
                    globalRecipes.put(recipe.id, recipe);
                } else {
                    anvilRecipes.put(recipe.id, recipe);
                }
                if (recipe.id <= this.nextId) continue;
                this.nextId = recipe.id;
            }
        }
        this.anvilRecipes = anvilRecipes;
        this.globalRecipes = globalRecipes;
        this.loadDefaultRecipes(nbttagcompound1.func_74762_e("Version"));
    }

    private void saveCategories() {
        try {
            File saveDir = CustomNpcs.getWorldSaveDirectory();
            NBTTagList list = new NBTTagList();
            for (RecipeCarpentry recipe : this.globalRecipes.values()) {
                if (!recipe.savesRecipe) continue;
                list.func_74742_a((NBTBase)recipe.writeNBT());
            }
            for (RecipeCarpentry recipe : this.anvilRecipes.values()) {
                if (!recipe.savesRecipe) continue;
                list.func_74742_a((NBTBase)recipe.writeNBT());
            }
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74782_a("Data", (NBTBase)list);
            nbttagcompound.func_74768_a("LastId", this.nextId);
            nbttagcompound.func_74768_a("Version", 1);
            File file = new File(saveDir, "recipes.dat_new");
            File file1 = new File(saveDir, "recipes.dat_old");
            File file2 = new File(saveDir, "recipes.dat");
            CompressedStreamTools.func_74799_a((NBTTagCompound)nbttagcompound, (OutputStream)new FileOutputStream(file));
            if (file1.exists()) {
                file1.delete();
            }
            file2.renameTo(file1);
            if (file2.exists()) {
                file2.delete();
            }
            file.renameTo(file2);
            if (file.exists()) {
                file.delete();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RecipeCarpentry findMatchingRecipe(InventoryCrafting inventoryCrafting) {
        for (RecipeCarpentry recipe : this.anvilRecipes.values()) {
            if (!recipe.isValid() || !recipe.func_77569_a(inventoryCrafting, null)) continue;
            return recipe;
        }
        return null;
    }

    public RecipeCarpentry getRecipe(int id) {
        if (this.globalRecipes.containsKey(id)) {
            return this.globalRecipes.get(id);
        }
        if (this.anvilRecipes.containsKey(id)) {
            return this.anvilRecipes.get(id);
        }
        return null;
    }

    public RecipeCarpentry saveRecipe(RecipeCarpentry recipe) throws IOException {
        RecipeCarpentry current = this.getRecipe(recipe.id);
        if (current != null && !current.name.equals(recipe.name)) {
            while (this.containsRecipeName(recipe.name)) {
                recipe.name = recipe.name + "_";
            }
        }
        if (recipe.id == -1) {
            recipe.id = this.getUniqueId();
            while (this.containsRecipeName(recipe.name)) {
                recipe.name = recipe.name + "_";
            }
        }
        if (recipe.isGlobal) {
            this.anvilRecipes.remove(recipe.id);
            this.globalRecipes.put(recipe.id, recipe);
            Server.sendToAll(CustomNpcs.Server, EnumPacketClient.SYNC_UPDATE, 6, recipe.writeNBT());
        } else {
            this.globalRecipes.remove(recipe.id);
            this.anvilRecipes.put(recipe.id, recipe);
            Server.sendToAll(CustomNpcs.Server, EnumPacketClient.SYNC_UPDATE, 7, recipe.writeNBT());
        }
        this.saveCategories();
        this.reloadGlobalRecipes();
        return recipe;
    }

    private int getUniqueId() {
        ++this.nextId;
        return this.nextId;
    }

    private boolean containsRecipeName(String name) {
        name = name.toLowerCase();
        for (RecipeCarpentry recipe : this.globalRecipes.values()) {
            if (!recipe.name.toLowerCase().equals(name)) continue;
            return true;
        }
        for (RecipeCarpentry recipe : this.anvilRecipes.values()) {
            if (!recipe.name.toLowerCase().equals(name)) continue;
            return true;
        }
        return false;
    }

    @Override
    public RecipeCarpentry delete(int id) {
        RecipeCarpentry recipe = this.getRecipe(id);
        if (recipe == null) {
            return null;
        }
        this.globalRecipes.remove(recipe.id);
        this.anvilRecipes.remove(recipe.id);
        if (recipe.isGlobal) {
            Server.sendToAll(CustomNpcs.Server, EnumPacketClient.SYNC_REMOVE, 6, id);
        } else {
            Server.sendToAll(CustomNpcs.Server, EnumPacketClient.SYNC_REMOVE, 7, id);
        }
        this.saveCategories();
        this.reloadGlobalRecipes();
        recipe.id = -1;
        return recipe;
    }

    @Override
    public List<noppes.npcs.api.handler.data.IRecipe> getGlobalList() {
        return new ArrayList<noppes.npcs.api.handler.data.IRecipe>(this.globalRecipes.values());
    }

    @Override
    public List<noppes.npcs.api.handler.data.IRecipe> getCarpentryList() {
        return new ArrayList<noppes.npcs.api.handler.data.IRecipe>(this.anvilRecipes.values());
    }

    @Override
    public noppes.npcs.api.handler.data.IRecipe addRecipe(String name, boolean global, ItemStack result, Object ... objects) {
        RecipeCarpentry recipe = new RecipeCarpentry(name);
        recipe.isGlobal = global;
        recipe = RecipeCarpentry.createRecipe(recipe, result, objects);
        try {
            return this.saveRecipe(recipe);
        }
        catch (IOException e) {
            e.printStackTrace();
            return recipe;
        }
    }

    @Override
    public noppes.npcs.api.handler.data.IRecipe addRecipe(String name, boolean global, ItemStack result, int width, int height, ItemStack ... objects) {
        NonNullList list = NonNullList.func_191196_a();
        for (ItemStack item : objects) {
            if (item.func_190926_b()) continue;
            list.add((Object)Ingredient.func_193369_a((ItemStack[])new ItemStack[]{item}));
        }
        RecipeCarpentry recipe = new RecipeCarpentry(width, height, (NonNullList<Ingredient>)list, result);
        recipe.isGlobal = global;
        recipe.name = name;
        try {
            return this.saveRecipe(recipe);
        }
        catch (IOException e) {
            e.printStackTrace();
            return recipe;
        }
    }

    static {
        syncRecipes = new HashMap();
    }
}

