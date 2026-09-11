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
        super("customnpcs", 0, 0, NonNullList.func_191196_a(), ItemStack.field_190927_a);
        this.name = name;
    }

    public static RecipeCarpentry read(NBTTagCompound compound) {
        RecipeCarpentry recipe = new RecipeCarpentry(compound.func_74762_e("Width"), compound.func_74762_e("Height"), NBTTags.getIngredientList(compound.func_150295_c("Materials", 10)), new ItemStack(compound.func_74775_l("Item")));
        recipe.name = compound.func_74779_i("Name");
        recipe.id = compound.func_74762_e("ID");
        recipe.availability.readFromNBT(compound.func_74775_l("Availability"));
        recipe.ignoreDamage = compound.func_74767_n("IgnoreDamage");
        recipe.ignoreNBT = compound.func_74767_n("IgnoreNBT");
        recipe.isGlobal = compound.func_74767_n("Global");
        return recipe;
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74768_a("ID", this.id);
        compound.func_74768_a("Width", this.field_77576_b);
        compound.func_74768_a("Height", this.field_77577_c);
        if (this.func_77571_b() != null) {
            compound.func_74782_a("Item", (NBTBase)this.func_77571_b().func_77955_b(new NBTTagCompound()));
        }
        compound.func_74782_a("Materials", (NBTBase)NBTTags.nbtIngredientList((NonNullList<Ingredient>)this.field_77574_d));
        compound.func_74782_a("Availability", (NBTBase)this.availability.writeToNBT(new NBTTagCompound()));
        compound.func_74778_a("Name", this.name);
        compound.func_74757_a("Global", this.isGlobal);
        compound.func_74757_a("IgnoreDamage", this.ignoreDamage);
        compound.func_74757_a("IgnoreNBT", this.ignoreNBT);
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
            ItemStack var17 = ItemStack.field_190927_a;
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
        NonNullList ingredients = NonNullList.func_191196_a();
        for (var9 = 0; var9 < var5 * var6; ++var9) {
            char var18 = var3.charAt(var9);
            if (var14.containsKey(Character.valueOf(var18))) {
                ingredients.add(var9, (Object)Ingredient.func_193369_a((ItemStack[])new ItemStack[]{((ItemStack)var14.get(Character.valueOf(var18))).func_77946_l()}));
                continue;
            }
            ingredients.add(var9, (Object)Ingredient.field_193370_a);
        }
        RecipeCarpentry newrecipe = new RecipeCarpentry(var5, var6, (NonNullList<Ingredient>)ingredients, par1ItemStack);
        newrecipe.copy(recipe);
        if (var5 == 4 || var6 == 4) {
            newrecipe.isGlobal = false;
        }
        return newrecipe;
    }

    public boolean func_77569_a(InventoryCrafting inventoryCrafting, World world) {
        for (int i = 0; i <= 4 - this.field_77576_b; ++i) {
            for (int j = 0; j <= 4 - this.field_77577_c; ++j) {
                if (this.checkMatch(inventoryCrafting, i, j, true)) {
                    return true;
                }
                if (!this.checkMatch(inventoryCrafting, i, j, false)) continue;
                return true;
            }
        }
        return false;
    }

    public ItemStack func_77572_b(InventoryCrafting var1) {
        if (this.func_77571_b().func_190926_b()) {
            return ItemStack.field_190927_a;
        }
        return this.func_77571_b().func_77946_l();
    }

    private boolean checkMatch(InventoryCrafting inventoryCrafting, int par2, int par3, boolean par4) {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                ItemStack var10;
                int var7 = i - par2;
                int var8 = j - par3;
                Ingredient ingredient = Ingredient.field_193370_a;
                if (var7 >= 0 && var8 >= 0 && var7 < this.field_77576_b && var8 < this.field_77577_c) {
                    ingredient = par4 ? (Ingredient)this.field_77574_d.get(this.field_77576_b - var7 - 1 + var8 * this.field_77576_b) : (Ingredient)this.field_77574_d.get(var7 + var8 * this.field_77576_b);
                }
                if (!(var10 = inventoryCrafting.func_70463_b(i, j)).func_190926_b() || ingredient.func_193365_a().length == 0) {
                    return false;
                }
                ItemStack var9 = ingredient.func_193365_a()[0];
                if (var10.func_190926_b() && var9.func_190926_b() || NoppesUtilPlayer.compareItems(var9, var10, this.ignoreDamage, this.ignoreNBT)) continue;
                return false;
            }
        }
        return true;
    }

    public NonNullList<ItemStack> func_179532_b(InventoryCrafting inventoryCrafting) {
        NonNullList list = NonNullList.func_191197_a((int)inventoryCrafting.func_70302_i_(), (Object)ItemStack.field_190927_a);
        for (int i = 0; i < list.size(); ++i) {
            ItemStack itemstack = inventoryCrafting.func_70301_a(i);
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
        if (this.field_77574_d == null || i >= this.field_77574_d.size()) {
            return ItemStack.field_190927_a;
        }
        Ingredient ingredients = (Ingredient)this.field_77574_d.get(i);
        if (ingredients.func_193365_a().length == 0) {
            return ItemStack.field_190927_a;
        }
        return ingredients.func_193365_a()[0];
    }

    public boolean isValid() {
        if (this.field_77574_d.size() == 0 || this.func_77571_b().func_190926_b()) {
            return false;
        }
        for (Ingredient ingredient : this.field_77574_d) {
            if (ingredient.func_193365_a().length <= 0) continue;
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
        return this.func_77571_b();
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

    public int func_192403_f() {
        return this.field_77576_b;
    }

    public int func_192404_g() {
        return this.field_77577_c;
    }

    @Override
    public ItemStack[] getRecipe() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        for (Ingredient ingredient : this.field_77574_d) {
            if (ingredient.func_193365_a().length <= 0) continue;
            list.add(ingredient.func_193365_a()[0]);
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

