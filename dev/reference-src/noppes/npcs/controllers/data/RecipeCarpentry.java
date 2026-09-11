/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.Ingredient
 *  net.minecraft.item.crafting.ShapedRecipes
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.NonNullList
 *  net.minecraft.world.World
 *  net.minecraftforge.common.ForgeHooks
 */
package noppes.npcs.controllers.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.handler.data.IRecipe;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.Availability;

public class RecipeCarpentry
extends ShapedRecipes
implements IRecipe {
    public int id = -1;
    public String name = "";
    public Availability availability = new Availability();
    public boolean isGlobal = false;
    public boolean ignoreDamage = false;
    public boolean ignoreNBT = false;
    public boolean savesRecipe = true;

    public RecipeCarpentry(int width, int height, NonNullList<Ingredient> recipe, ItemStack result) {
        super("customnpcs", width, height, recipe, result);
    }

    public RecipeCarpentry(String name) {
        super("customnpcs", 0, 0, NonNullList.create(), ItemStack.EMPTY);
        this.name = name;
    }

    public static RecipeCarpentry read(NBTTagCompound compound) {
        RecipeCarpentry recipe = new RecipeCarpentry(compound.getInteger("Width"), compound.getInteger("Height"), NBTTags.getIngredientList(compound.getTagList("Materials", 10)), new ItemStack(compound.getCompoundTag("Item")));
        recipe.name = compound.getString("Name");
        recipe.id = compound.getInteger("ID");
        recipe.availability.readFromNBT(compound.getCompoundTag("Availability"));
        recipe.ignoreDamage = compound.getBoolean("IgnoreDamage");
        recipe.ignoreNBT = compound.getBoolean("IgnoreNBT");
        recipe.isGlobal = compound.getBoolean("Global");
        return recipe;
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("ID", this.id);
        compound.setInteger("Width", this.recipeWidth);
        compound.setInteger("Height", this.recipeHeight);
        if (this.getRecipeOutput() != null) {
            compound.setTag("Item", (NBTBase)this.getRecipeOutput().writeToNBT(new NBTTagCompound()));
        }
        compound.setTag("Materials", (NBTBase)NBTTags.nbtIngredientList((NonNullList<Ingredient>)this.recipeItems));
        compound.setTag("Availability", (NBTBase)this.availability.writeToNBT(new NBTTagCompound()));
        compound.setString("Name", this.name);
        compound.setBoolean("Global", this.isGlobal);
        compound.setBoolean("IgnoreDamage", this.ignoreDamage);
        compound.setBoolean("IgnoreNBT", this.ignoreNBT);
        return compound;
    }

    public static RecipeCarpentry createRecipe(RecipeCarpentry recipe, ItemStack par1ItemStack, Object ... par2ArrayOfObj) {
        int var9;
        String var3 = "";
        int var4 = 0;
        int var5 = 0;
        int var6 = 0;
        if (par2ArrayOfObj[var4] instanceof String[]) {
            String[] var7;
            String[] var8 = var7 = (String[])par2ArrayOfObj[var4++];
            var9 = var7.length;
            for (int var10 = 0; var10 < var9; ++var10) {
                String var11 = var8[var10];
                ++var6;
                var5 = var11.length();
                var3 = var3 + var11;
            }
        } else {
            while (par2ArrayOfObj[var4] instanceof String) {
                String var13 = (String)par2ArrayOfObj[var4++];
                ++var6;
                var5 = var13.length();
                var3 = var3 + var13;
            }
        }
        HashMap<Character, ItemStack> var14 = new HashMap<Character, ItemStack>();
        while (var4 < par2ArrayOfObj.length) {
            Character var16 = (Character)par2ArrayOfObj[var4];
            ItemStack var17 = ItemStack.EMPTY;
            if (par2ArrayOfObj[var4 + 1] instanceof Item) {
                var17 = new ItemStack((Item)par2ArrayOfObj[var4 + 1]);
            } else if (par2ArrayOfObj[var4 + 1] instanceof Block) {
                var17 = new ItemStack((Block)par2ArrayOfObj[var4 + 1], 1, -1);
            } else if (par2ArrayOfObj[var4 + 1] instanceof ItemStack) {
                var17 = (ItemStack)par2ArrayOfObj[var4 + 1];
            }
            var14.put(var16, var17);
            var4 += 2;
        }
        NonNullList ingredients = NonNullList.create();
        for (var9 = 0; var9 < var5 * var6; ++var9) {
            char var18 = var3.charAt(var9);
            if (var14.containsKey(Character.valueOf(var18))) {
                ingredients.add(var9, (Object)Ingredient.fromStacks((ItemStack[])new ItemStack[]{((ItemStack)var14.get(Character.valueOf(var18))).copy()}));
                continue;
            }
            ingredients.add(var9, (Object)Ingredient.EMPTY);
        }
        RecipeCarpentry newrecipe = new RecipeCarpentry(var5, var6, (NonNullList<Ingredient>)ingredients, par1ItemStack);
        newrecipe.copy(recipe);
        if (var5 == 4 || var6 == 4) {
            newrecipe.isGlobal = false;
        }
        return newrecipe;
    }

    public boolean matches(InventoryCrafting inventoryCrafting, World world) {
        for (int i = 0; i <= 4 - this.recipeWidth; ++i) {
            for (int j = 0; j <= 4 - this.recipeHeight; ++j) {
                if (this.checkMatch(inventoryCrafting, i, j, true)) {
                    return true;
                }
                if (!this.checkMatch(inventoryCrafting, i, j, false)) continue;
                return true;
            }
        }
        return false;
    }

    public ItemStack getCraftingResult(InventoryCrafting var1) {
        if (this.getRecipeOutput().isEmpty()) {
            return ItemStack.EMPTY;
        }
        return this.getRecipeOutput().copy();
    }

    private boolean checkMatch(InventoryCrafting inventoryCrafting, int par2, int par3, boolean par4) {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                ItemStack var10;
                int var7 = i - par2;
                int var8 = j - par3;
                Ingredient ingredient = Ingredient.EMPTY;
                if (var7 >= 0 && var8 >= 0 && var7 < this.recipeWidth && var8 < this.recipeHeight) {
                    ingredient = par4 ? (Ingredient)this.recipeItems.get(this.recipeWidth - var7 - 1 + var8 * this.recipeWidth) : (Ingredient)this.recipeItems.get(var7 + var8 * this.recipeWidth);
                }
                if (!(var10 = inventoryCrafting.getStackInRowAndColumn(i, j)).isEmpty() || ingredient.getMatchingStacks().length == 0) {
                    return false;
                }
                ItemStack var9 = ingredient.getMatchingStacks()[0];
                if (var10.isEmpty() && var9.isEmpty() || NoppesUtilPlayer.compareItems(var9, var10, this.ignoreDamage, this.ignoreNBT)) continue;
                return false;
            }
        }
        return true;
    }

    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventoryCrafting) {
        NonNullList list = NonNullList.withSize((int)inventoryCrafting.getSizeInventory(), (Object)ItemStack.EMPTY);
        for (int i = 0; i < list.size(); ++i) {
            ItemStack itemstack = inventoryCrafting.getStackInSlot(i);
            list.set(i, (Object)ForgeHooks.getContainerItem((ItemStack)itemstack));
        }
        return list;
    }

    public void copy(RecipeCarpentry recipe) {
        this.id = recipe.id;
        this.name = recipe.name;
        this.availability = recipe.availability;
        this.isGlobal = recipe.isGlobal;
        this.ignoreDamage = recipe.ignoreDamage;
        this.ignoreNBT = recipe.ignoreNBT;
    }

    public ItemStack getCraftingItem(int i) {
        if (this.recipeItems == null || i >= this.recipeItems.size()) {
            return ItemStack.EMPTY;
        }
        Ingredient ingredients = (Ingredient)this.recipeItems.get(i);
        if (ingredients.getMatchingStacks().length == 0) {
            return ItemStack.EMPTY;
        }
        return ingredients.getMatchingStacks()[0];
    }

    public boolean isValid() {
        if (this.recipeItems.size() == 0 || this.getRecipeOutput().isEmpty()) {
            return false;
        }
        for (Ingredient ingredient : this.recipeItems) {
            if (ingredient.getMatchingStacks().length <= 0) continue;
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ItemStack getResult() {
        return this.getRecipeOutput();
    }

    @Override
    public boolean isGlobal() {
        return this.isGlobal;
    }

    @Override
    public void setIsGlobal(boolean bo) {
        this.isGlobal = bo;
    }

    @Override
    public boolean getIgnoreNBT() {
        return this.ignoreNBT;
    }

    @Override
    public void setIgnoreNBT(boolean bo) {
        this.ignoreNBT = bo;
    }

    @Override
    public boolean getIgnoreDamage() {
        return this.ignoreDamage;
    }

    @Override
    public void setIgnoreDamage(boolean bo) {
        this.ignoreDamage = bo;
    }

    @Override
    public void save() {
        try {
            RecipeController.instance.saveRecipe(this);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    @Override
    public void delete() {
        RecipeController.instance.delete(this.id);
    }

    @Override
    public int getWidth() {
        return this.recipeWidth;
    }

    @Override
    public int getHeight() {
        return this.recipeHeight;
    }

    @Override
    public ItemStack[] getRecipe() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        for (Ingredient ingredient : this.recipeItems) {
            if (ingredient.getMatchingStacks().length <= 0) continue;
            list.add(ingredient.getMatchingStacks()[0]);
        }
        return list.toArray(new ItemStack[list.size()]);
    }

    @Override
    public void saves(boolean bo) {
        this.savesRecipe = bo;
    }

    @Override
    public boolean saves() {
        return this.savesRecipe;
    }

    @Override
    public int getId() {
        return this.id;
    }
}

