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

    public void func_192841_a(TileEntity te, double x, double y, double z, float partialTicks, int blockDamage, float alpha) {
        TileScripted tile = (TileScripted)te;
        GlStateManager.func_179094_E();
        GlStateManager.func_179084_k();
        RenderHelper.func_74519_b();
        GlStateManager.func_179137_b((double)(x + 0.5), (double)y, (double)(z + 0.5));
        if (this.overrideModel()) {
            GlStateManager.func_179137_b((double)0.0, (double)0.5, (double)0.0);
            this.renderItem(new ItemStack(CustomItems.scripted));
        } else {
            GlStateManager.func_179114_b((float)tile.rotationY, (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.func_179114_b((float)tile.rotationX, (float)1.0f, (float)0.0f, (float)0.0f);
            GlStateManager.func_179114_b((float)tile.rotationZ, (float)0.0f, (float)0.0f, (float)1.0f);
            GlStateManager.func_179152_a((float)tile.scaleX, (float)tile.scaleY, (float)tile.scaleZ);
            Block b = tile.blockModel;
            if (b == null || b == Blocks.field_150350_a) {
                GlStateManager.func_179137_b((double)0.0, (double)0.5, (double)0.0);
                this.renderItem(tile.itemModel);
            } else if (b == CustomItems.scripted) {
                GlStateManager.func_179137_b((double)0.0, (double)0.5, (double)0.0);
                this.renderItem(tile.itemModel);
            } else {
                IBlockState state = b.func_176203_a(tile.itemModel.func_77952_i());
                this.renderBlock(tile, b, state);
                if (b.hasTileEntity(state) && !tile.renderTileErrored) {
                    try {
                        TileEntitySpecialRenderer renderer;
                        if (tile.renderTile == null) {
                            TileEntity entity = b.createTileEntity(this.func_178459_a(), state);
                            entity.func_174878_a(tile.func_174877_v());
                            entity.func_145834_a(this.func_178459_a());
                            ObfuscationReflectionHelper.setPrivateValue(TileEntity.class, (Object)entity, (Object)tile.itemModel.func_77952_i(), (int)5);
                            ObfuscationReflectionHelper.setPrivateValue(TileEntity.class, (Object)entity, (Object)b, (int)6);
                            tile.renderTile = entity;
                            if (entity instanceof ITickable) {
                                tile.renderTileUpdate = (ITickable)entity;
                            }
                        }
                        if ((renderer = TileEntityRendererDispatcher.field_147556_a.func_147547_b(tile.renderTile)) != null) {
                            renderer.func_192841_a(tile.renderTile, -0.5, 0.0, -0.5, partialTicks, blockDamage, alpha);
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
        GlStateManager.func_179121_F();
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
            text1.textBlock = new TextBlockClient(text1.text, 336, true, Minecraft.func_71410_x().field_71439_g);
            text1.textHasChanged = false;
        }
        GlStateManager.func_179084_k();
        GlStateManager.func_179145_e();
        GlStateManager.func_179124_c((float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b((double)(x + 0.5), (double)(y + 0.5), (double)(z + 0.5));
        GlStateManager.func_179114_b((float)text1.rotationY, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)text1.rotationX, (float)1.0f, (float)0.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)text1.rotationZ, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.func_179152_a((float)text1.scale, (float)text1.scale, (float)1.0f);
        GlStateManager.func_179109_b((float)text1.offsetX, (float)text1.offsetY, (float)text1.offsetZ);
        float f1 = 0.6666667f;
        float f3 = 0.0133f * f1;
        GlStateManager.func_179109_b((float)0.0f, (float)0.5f, (float)0.01f);
        GlStateManager.func_179152_a((float)f3, (float)(-f3), (float)f3);
        GlStateManager.func_187432_a((float)0.0f, (float)0.0f, (float)(-1.0f * f3));
        GlStateManager.func_179132_a((boolean)false);
        FontRenderer fontrenderer = this.func_147498_b();
        float lineOffset = 0.0f;
        if (text1.textBlock.lines.size() < 14) {
            lineOffset = (14.0f - (float)text1.textBlock.lines.size()) / 2.0f;
        }
        for (int i = 0; i < text1.textBlock.lines.size(); ++i) {
            String text = text1.textBlock.lines.get(i).func_150254_d();
            fontrenderer.func_78276_b(text, -fontrenderer.func_78256_a(text) / 2, (int)((double)(lineOffset + (float)i) * ((double)fontrenderer.field_78288_b - 0.3)), 0);
        }
        GlStateManager.func_179132_a((boolean)true);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179121_F();
    }

    private void renderItem(ItemStack item) {
        Minecraft.func_71410_x().func_175599_af().func_181564_a(item, ItemCameraTransforms.TransformType.NONE);
    }

    private void renderBlock(TileScripted tile, Block b, IBlockState state) {
        GlStateManager.func_179094_E();
        this.func_147499_a(TextureMap.field_110575_b);
        GlStateManager.func_187401_a((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.func_179147_l();
        GlStateManager.func_179129_p();
        GlStateManager.func_179109_b((float)-0.5f, (float)0.0f, (float)0.5f);
        Minecraft.func_71410_x().func_175602_ab().func_175016_a(state, 1.0f);
        if (b.func_149653_t() && random.nextInt(12) == 1) {
            b.func_180655_c(state, tile.func_145831_w(), tile.func_174877_v(), random);
        }
        GlStateManager.func_179121_F();
    }

    private boolean overrideModel() {
        ItemStack held = Minecraft.func_71410_x().field_71439_g.func_184614_ca();
        if (held == null) {
            return false;
        }
        return held.func_77973_b() == CustomItems.wand || held.func_77973_b() == CustomItems.scripter;
    }
}

