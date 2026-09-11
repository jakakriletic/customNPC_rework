/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.controllers.data;

import java.io.IOException;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import noppes.npcs.CustomItems;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.RecipeCarpentry;

public class RecipesDefault {
    public static void addRecipe(String name, Object ob, boolean isGlobal, Object ... recipe) {
        ItemStack item = ob instanceof Item ? new ItemStack((Item)ob) : (ob instanceof Block ? new ItemStack((Block)ob) : (ItemStack)ob);
        RecipeCarpentry recipeAnvil = new RecipeCarpentry(name);
        recipeAnvil.isGlobal = isGlobal;
        recipeAnvil = RecipeCarpentry.createRecipe(recipeAnvil, item, recipe);
        try {
            RecipeController.instance.saveRecipe(recipeAnvil);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static void loadDefaultRecipes(int i) {
        if (i < 0) {
            RecipesDefault.addRecipe("Npc Wand", CustomItems.wand, true, "XX", " Y", " Y", Character.valueOf('X'), Items.BREAD, Character.valueOf('Y'), Items.STICK);
            RecipesDefault.addRecipe("Mob Cloner", CustomItems.cloner, true, "XX", "XY", " Y", Character.valueOf('X'), Items.BREAD, Character.valueOf('Y'), Items.STICK);
        }
    }
}

