/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.IRecipe
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.MathHelper
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.client.gui.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.gui.util.GuiButtonNextPage;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.RecipeCarpentry;

@SideOnly(value=Side.CLIENT)
public class GuiRecipes
extends GuiNPCInterface {
    private static final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/slot.png");
    private int page = 0;
    private boolean npcRecipes = true;
    private GuiNpcLabel label;
    private GuiNpcButton left;
    private GuiNpcButton right;
    private List<IRecipe> recipes = new ArrayList<IRecipe>();

    public GuiRecipes() {
        this.ySize = 182;
        this.xSize = 256;
        this.setBackground("recipes.png");
        this.closeOnEsc = true;
        this.recipes.addAll(RecipeController.instance.anvilRecipes.values());
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addLabel(new GuiNpcLabel(0, "Recipe List", this.guiLeft + 5, this.guiTop + 5));
        this.label = new GuiNpcLabel(1, "", this.guiLeft + 5, this.guiTop + 168);
        this.addLabel(this.label);
        this.left = new GuiButtonNextPage(1, this.guiLeft + 150, this.guiTop + 164, true);
        this.addButton(this.left);
        this.right = new GuiButtonNextPage(2, this.guiLeft + 80, this.guiTop + 164, false);
        this.addButton(this.right);
        this.updateButton();
    }

    private void updateButton() {
        this.right.enabled = this.page > 0;
        this.right.visible = this.right.enabled;
        this.left.enabled = this.page + 1 < MathHelper.ceil((float)((float)this.recipes.size() / 4.0f));
        this.left.visible = this.left.enabled;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button == this.right) {
            --this.page;
        }
        if (button == this.left) {
            ++this.page;
        }
        this.updateButton();
    }

    @Override
    public void drawScreen(int xMouse, int yMouse, float f) {
        ItemStack item;
        int k;
        int j;
        IRecipe irecipe;
        int index;
        int i;
        super.drawScreen(xMouse, yMouse, f);
        this.mc.renderEngine.bindTexture(resource);
        this.label.label = this.page + 1 + "/" + MathHelper.ceil((float)((float)this.recipes.size() / 4.0f));
        this.label.x = this.guiLeft + (256 - Minecraft.getMinecraft().fontRenderer.getStringWidth(this.label.label)) / 2;
        for (i = 0; i < 4 && (index = i + this.page * 4) < this.recipes.size(); ++i) {
            irecipe = this.recipes.get(index);
            if (irecipe.getRecipeOutput().isEmpty()) continue;
            int x = this.guiLeft + 5 + i / 2 * 126;
            int y = this.guiTop + 15 + i % 2 * 76;
            this.drawItem(irecipe.getRecipeOutput(), x + 98, y + 28, xMouse, yMouse);
            if (!(irecipe instanceof RecipeCarpentry)) continue;
            RecipeCarpentry recipe = (RecipeCarpentry)irecipe;
            x += (72 - recipe.recipeWidth * 18) / 2;
            y += (72 - recipe.recipeHeight * 18) / 2;
            for (j = 0; j < recipe.recipeWidth; ++j) {
                for (k = 0; k < recipe.recipeHeight; ++k) {
                    this.mc.renderEngine.bindTexture(resource);
                    GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                    this.drawTexturedModalRect(x + j * 18, y + k * 18, 0, 0, 18, 18);
                    item = recipe.getCraftingItem(j + k * recipe.recipeWidth);
                    if (item.isEmpty()) continue;
                    this.drawItem(item, x + j * 18 + 1, y + k * 18 + 1, xMouse, yMouse);
                }
            }
        }
        for (i = 0; i < 4 && (index = i + this.page * 4) < this.recipes.size(); ++i) {
            RecipeCarpentry recipe;
            irecipe = this.recipes.get(index);
            if (!(irecipe instanceof RecipeCarpentry) || (recipe = (RecipeCarpentry)irecipe).getRecipeOutput().isEmpty()) continue;
            int x = this.guiLeft + 5 + i / 2 * 126;
            int y = this.guiTop + 15 + i % 2 * 76;
            this.drawOverlay(recipe.getRecipeOutput(), x + 98, y + 22, xMouse, yMouse);
            x += (72 - recipe.recipeWidth * 18) / 2;
            y += (72 - recipe.recipeHeight * 18) / 2;
            for (j = 0; j < recipe.recipeWidth; ++j) {
                for (k = 0; k < recipe.recipeHeight; ++k) {
                    item = recipe.getCraftingItem(j + k * recipe.recipeWidth);
                    if (item.isEmpty()) continue;
                    this.drawOverlay(item, x + j * 18 + 1, y + k * 18 + 1, xMouse, yMouse);
                }
            }
        }
    }

    private void drawItem(ItemStack item, int x, int y, int xMouse, int yMouse) {
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.zLevel = 100.0f;
        this.itemRender.renderItemAndEffectIntoGUI(item, x, y);
        this.itemRender.renderItemOverlays(this.fontRenderer, item, x, y);
        this.itemRender.zLevel = 0.0f;
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private void drawOverlay(ItemStack item, int x, int y, int xMouse, int yMouse) {
        if (this.isPointInRegion(x - this.guiLeft, y - this.guiTop, 16, 16, xMouse, yMouse)) {
            this.renderToolTip(item, xMouse, yMouse);
        }
    }

    protected boolean isPointInRegion(int p_146978_1_, int p_146978_2_, int p_146978_3_, int p_146978_4_, int p_146978_5_, int p_146978_6_) {
        int k1 = this.guiLeft;
        int l1 = this.guiTop;
        return (p_146978_5_ -= k1) >= p_146978_1_ - 1 && p_146978_5_ < p_146978_1_ + p_146978_3_ + 1 && (p_146978_6_ -= l1) >= p_146978_2_ - 1 && p_146978_6_ < p_146978_2_ + p_146978_4_ + 1;
    }

    @Override
    public void save() {
    }
}

