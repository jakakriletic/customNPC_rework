/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.GlStateManager$DestFactor
 *  net.minecraft.client.renderer.GlStateManager$SourceFactor
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.block.model.ItemCameraTransforms$TransformType
 *  net.minecraft.client.renderer.texture.TextureMap
 *  net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
 *  net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ITickable
 *  net.minecraftforge.fml.common.ObfuscationReflectionHelper
 */
package noppes.npcs.client.renderer.blocks;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.renderer.blocks.BlockRendererInterface;

public class BlockScriptedRenderer
extends BlockRendererInterface {
    private static Random random = new Random();

    public void render(TileEntity te, double x, double y, double z, float partialTicks, int blockDamage, float alpha) {
        TileScripted tile = (TileScripted)te;
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate((double)(x + 0.5), (double)y, (double)(z + 0.5));
        if (this.overrideModel()) {
            GlStateManager.translate((double)0.0, (double)0.5, (double)0.0);
            this.renderItem(new ItemStack(CustomItems.scripted));
        } else {
            GlStateManager.rotate((float)tile.rotationY, (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.rotate((float)tile.rotationX, (float)1.0f, (float)0.0f, (float)0.0f);
            GlStateManager.rotate((float)tile.rotationZ, (float)0.0f, (float)0.0f, (float)1.0f);
            GlStateManager.scale((float)tile.scaleX, (float)tile.scaleY, (float)tile.scaleZ);
            Block b = tile.blockModel;
            if (b == null || b == Blocks.AIR) {
                GlStateManager.translate((double)0.0, (double)0.5, (double)0.0);
                this.renderItem(tile.itemModel);
            } else if (b == CustomItems.scripted) {
                GlStateManager.translate((double)0.0, (double)0.5, (double)0.0);
                this.renderItem(tile.itemModel);
            } else {
                IBlockState state = b.getStateFromMeta(tile.itemModel.getItemDamage());
                this.renderBlock(tile, b, state);
                if (b.hasTileEntity(state) && !tile.renderTileErrored) {
                    try {
                        TileEntitySpecialRenderer renderer;
                        if (tile.renderTile == null) {
                            TileEntity entity = b.createTileEntity(this.getWorld(), state);
                            entity.setPos(tile.getPos());
                            entity.setWorld(this.getWorld());
                            ObfuscationReflectionHelper.setPrivateValue(TileEntity.class, (Object)entity, (Object)tile.itemModel.getItemDamage(), (int)5);
                            ObfuscationReflectionHelper.setPrivateValue(TileEntity.class, (Object)entity, (Object)b, (int)6);
                            tile.renderTile = entity;
                            if (entity instanceof ITickable) {
                                tile.renderTileUpdate = (ITickable)entity;
                            }
                        }
                        if ((renderer = TileEntityRendererDispatcher.instance.getRenderer(tile.renderTile)) != null) {
                            renderer.render(tile.renderTile, -0.5, 0.0, -0.5, partialTicks, blockDamage, alpha);
                        } else {
                            tile.renderTileErrored = true;
                        }
                    }
                    catch (Exception e) {
                        tile.renderTileErrored = true;
                    }
                }
            }
        }
        GlStateManager.popMatrix();
        if (!tile.text1.text.isEmpty()) {
            this.drawText(tile.text1, x, y, z);
        }
        if (!tile.text2.text.isEmpty()) {
            this.drawText(tile.text2, x, y, z);
        }
        if (!tile.text3.text.isEmpty()) {
            this.drawText(tile.text3, x, y, z);
        }
        if (!tile.text4.text.isEmpty()) {
            this.drawText(tile.text4, x, y, z);
        }
        if (!tile.text5.text.isEmpty()) {
            this.drawText(tile.text5, x, y, z);
        }
        if (!tile.text6.text.isEmpty()) {
            this.drawText(tile.text6, x, y, z);
        }
    }

    private void drawText(TileScripted.TextPlane text1, double x, double y, double z) {
        if (text1.textBlock == null || text1.textHasChanged) {
            text1.textBlock = new TextBlockClient(text1.text, 336, true, Minecraft.getMinecraft().player);
            text1.textHasChanged = false;
        }
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.pushMatrix();
        GlStateManager.translate((double)(x + 0.5), (double)(y + 0.5), (double)(z + 0.5));
        GlStateManager.rotate((float)text1.rotationY, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.rotate((float)text1.rotationX, (float)1.0f, (float)0.0f, (float)0.0f);
        GlStateManager.rotate((float)text1.rotationZ, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.scale((float)text1.scale, (float)text1.scale, (float)1.0f);
        GlStateManager.translate((float)text1.offsetX, (float)text1.offsetY, (float)text1.offsetZ);
        float f1 = 0.6666667f;
        float f3 = 0.0133f * f1;
        GlStateManager.translate((float)0.0f, (float)0.5f, (float)0.01f);
        GlStateManager.scale((float)f3, (float)(-f3), (float)f3);
        GlStateManager.glNormal3f((float)0.0f, (float)0.0f, (float)(-1.0f * f3));
        GlStateManager.depthMask((boolean)false);
        FontRenderer fontrenderer = this.getFontRenderer();
        float lineOffset = 0.0f;
        if (text1.textBlock.lines.size() < 14) {
            lineOffset = (14.0f - (float)text1.textBlock.lines.size()) / 2.0f;
        }
        for (int i = 0; i < text1.textBlock.lines.size(); ++i) {
            String text = text1.textBlock.lines.get(i).getFormattedText();
            fontrenderer.drawString(text, -fontrenderer.getStringWidth(text) / 2, (int)((double)(lineOffset + (float)i) * ((double)fontrenderer.FONT_HEIGHT - 0.3)), 0);
        }
        GlStateManager.depthMask((boolean)true);
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.popMatrix();
    }

    private void renderItem(ItemStack item) {
        Minecraft.getMinecraft().getRenderItem().renderItem(item, ItemCameraTransforms.TransformType.NONE);
    }

    private void renderBlock(TileScripted tile, Block b, IBlockState state) {
        GlStateManager.pushMatrix();
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.blendFunc((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.translate((float)-0.5f, (float)0.0f, (float)0.5f);
        Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlockBrightness(state, 1.0f);
        if (b.getTickRandomly() && random.nextInt(12) == 1) {
            b.randomDisplayTick(state, tile.getWorld(), tile.getPos(), random);
        }
        GlStateManager.popMatrix();
    }

    private boolean overrideModel() {
        ItemStack held = Minecraft.getMinecraft().player.getHeldItemMainhand();
        if (held == null) {
            return false;
        }
        return held.getItem() == CustomItems.wand || held.getItem() == CustomItems.scripter;
    }
}

