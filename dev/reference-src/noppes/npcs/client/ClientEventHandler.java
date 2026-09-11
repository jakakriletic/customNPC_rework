/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.client.renderer.BlockRendererDispatcher
 *  net.minecraft.client.renderer.GLAllocation
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderGlobal
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.texture.TextureMap
 *  net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumBlockRenderType
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3i
 *  net.minecraftforge.client.event.RenderLivingEvent$Post
 *  net.minecraftforge.client.event.RenderWorldLastEvent
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  org.lwjgl.opengl.GL11
 */
package noppes.npcs.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.LogWriter;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.client.renderer.MarkRenderer;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.schematics.SchematicWrapper;
import org.lwjgl.opengl.GL11;

public class ClientEventHandler {
    private int displayList = -1;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @SubscribeEvent
    public void onRenderTick(RenderWorldLastEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (TileBuilder.DrawPos == null || TileBuilder.DrawPos.distanceSq((Vec3i)player.getPosition()) > 1000000.0) {
            return;
        }
        TileEntity te = player.world.getTileEntity(TileBuilder.DrawPos);
        if (te == null || !(te instanceof TileBuilder)) {
            return;
        }
        TileBuilder tile = (TileBuilder)te;
        SchematicWrapper schem = tile.getSchematic();
        if (schem == null) {
            return;
        }
        GlStateManager.pushMatrix();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate((double)((double)TileBuilder.DrawPos.getX() - TileEntityRendererDispatcher.staticPlayerX), (double)((double)TileBuilder.DrawPos.getY() - TileEntityRendererDispatcher.staticPlayerY + 0.01), (double)((double)TileBuilder.DrawPos.getZ() - TileEntityRendererDispatcher.staticPlayerZ));
        GlStateManager.translate((float)1.0f, (float)tile.yOffest, (float)1.0f);
        if (tile.rotation % 2 == 0) {
            this.drawSelectionBox(new BlockPos((int)schem.schema.getWidth(), (int)schem.schema.getHeight(), (int)schem.schema.getLength()));
        } else {
            this.drawSelectionBox(new BlockPos((int)schem.schema.getLength(), (int)schem.schema.getHeight(), (int)schem.schema.getWidth()));
        }
        if (TileBuilder.Compiled) {
            GlStateManager.callList((int)this.displayList);
        } else {
            BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
            if (this.displayList >= 0) {
                GLAllocation.deleteDisplayLists((int)this.displayList);
            }
            this.displayList = GLAllocation.generateDisplayLists((int)1);
            GL11.glNewList((int)this.displayList, (int)4864);
            try {
                for (int i = 0; i < schem.size && i < 25000; ++i) {
                    int posX = i % schem.schema.getWidth();
                    int posZ = (i - posX) / schem.schema.getWidth() % schem.schema.getLength();
                    int posY = ((i - posX) / schem.schema.getWidth() - posZ) / schem.schema.getLength();
                    IBlockState state = schem.schema.getBlockState(posX, posY, posZ);
                    if (state.getRenderType() == EnumBlockRenderType.INVISIBLE) continue;
                    BlockPos pos = schem.rotatePos(posX, posY, posZ, tile.rotation);
                    GlStateManager.pushMatrix();
                    GlStateManager.pushAttrib();
                    GlStateManager.enableRescaleNormal();
                    GlStateManager.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
                    Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                    GlStateManager.rotate((float)-90.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                    state = schem.rotationState(state, tile.rotation);
                    try {
                        dispatcher.renderBlockBrightness(state, 1.0f);
                        if (GL11.glGetError() == 0) continue;
                        break;
                    }
                    catch (Exception exception) {
                        continue;
                    }
                    finally {
                        GlStateManager.popAttrib();
                        GlStateManager.disableRescaleNormal();
                        GlStateManager.popMatrix();
                    }
                }
            }
            catch (Exception e) {
                LogWriter.error("Error preview builder block", e);
            }
            finally {
                GL11.glEndList();
                if (GL11.glGetError() == 0) {
                    TileBuilder.Compiled = true;
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.translate((float)-1.0f, (float)0.0f, (float)-1.0f);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void post(RenderLivingEvent.Post event) {
        MarkData data = MarkData.get(event.getEntity());
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        for (MarkData.Mark m : data.marks) {
            if (m.getType() == 0 || !m.availability.isAvailable((EntityPlayer)player)) continue;
            MarkRenderer.render(event.getEntity(), event.getX(), event.getY(), event.getZ(), m);
            break;
        }
    }

    public void drawSelectionBox(BlockPos pos) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        AxisAlignedBB bb = new AxisAlignedBB(BlockPos.ORIGIN, pos);
        RenderGlobal.drawSelectionBoundingBox((AxisAlignedBB)bb, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }
}

