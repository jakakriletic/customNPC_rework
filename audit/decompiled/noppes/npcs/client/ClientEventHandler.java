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
        EntityPlayerSP player = Minecraft.func_71410_x().field_71439_g;
        if (TileBuilder.DrawPos == null || TileBuilder.DrawPos.func_177951_i((Vec3i)player.func_180425_c()) > 1000000.0) {
            return;
        }
        TileEntity te = player.field_70170_p.func_175625_s(TileBuilder.DrawPos);
        if (te == null || !(te instanceof TileBuilder)) {
            return;
        }
        TileBuilder tile = (TileBuilder)te;
        SchematicWrapper schem = tile.getSchematic();
        if (schem == null) {
            return;
        }
        GlStateManager.func_179094_E();
        RenderHelper.func_74519_b();
        GlStateManager.func_179137_b((double)((double)TileBuilder.DrawPos.func_177958_n() - TileEntityRendererDispatcher.field_147554_b), (double)((double)TileBuilder.DrawPos.func_177956_o() - TileEntityRendererDispatcher.field_147555_c + 0.01), (double)((double)TileBuilder.DrawPos.func_177952_p() - TileEntityRendererDispatcher.field_147552_d));
        GlStateManager.func_179109_b((float)1.0f, (float)tile.yOffest, (float)1.0f);
        if (tile.rotation % 2 == 0) {
            this.drawSelectionBox(new BlockPos((int)schem.schema.getWidth(), (int)schem.schema.getHeight(), (int)schem.schema.getLength()));
        } else {
            this.drawSelectionBox(new BlockPos((int)schem.schema.getLength(), (int)schem.schema.getHeight(), (int)schem.schema.getWidth()));
        }
        if (TileBuilder.Compiled) {
            GlStateManager.func_179148_o((int)this.displayList);
        } else {
            BlockRendererDispatcher dispatcher = Minecraft.func_71410_x().func_175602_ab();
            if (this.displayList >= 0) {
                GLAllocation.func_74523_b((int)this.displayList);
            }
            this.displayList = GLAllocation.func_74526_a((int)1);
            GL11.glNewList((int)this.displayList, (int)4864);
            try {
                for (int i = 0; i < schem.size && i < 25000; ++i) {
                    int posX = i % schem.schema.getWidth();
                    int posZ = (i - posX) / schem.schema.getWidth() % schem.schema.getLength();
                    int posY = ((i - posX) / schem.schema.getWidth() - posZ) / schem.schema.getLength();
                    IBlockState state = schem.schema.getBlockState(posX, posY, posZ);
                    if (state.func_185901_i() == EnumBlockRenderType.INVISIBLE) continue;
                    BlockPos pos = schem.rotatePos(posX, posY, posZ, tile.rotation);
                    GlStateManager.func_179094_E();
                    GlStateManager.func_179123_a();
                    GlStateManager.func_179091_B();
                    GlStateManager.func_179109_b((float)pos.func_177958_n(), (float)pos.func_177956_o(), (float)pos.func_177952_p());
                    Minecraft.func_71410_x().func_110434_K().func_110577_a(TextureMap.field_110575_b);
                    GlStateManager.func_179114_b((float)-90.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                    state = schem.rotationState(state, tile.rotation);
                    try {
                        dispatcher.func_175016_a(state, 1.0f);
                        if (GL11.glGetError() == 0) continue;
                        break;
                    }
                    catch (Exception exception) {
                        continue;
                    }
                    finally {
                        GlStateManager.func_179099_b();
                        GlStateManager.func_179101_C();
                        GlStateManager.func_179121_F();
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
        RenderHelper.func_74518_a();
        GlStateManager.func_179109_b((float)-1.0f, (float)0.0f, (float)-1.0f);
        GlStateManager.func_179121_F();
    }

    @SubscribeEvent
    public void post(RenderLivingEvent.Post event) {
        MarkData data = MarkData.get(event.getEntity());
        EntityPlayerSP player = Minecraft.func_71410_x().field_71439_g;
        for (MarkData.Mark m : data.marks) {
            if (m.getType() == 0 || !m.availability.isAvailable((EntityPlayer)player)) continue;
            MarkRenderer.render(event.getEntity(), event.getX(), event.getY(), event.getZ(), m);
            break;
        }
    }

    public void drawSelectionBox(BlockPos pos) {
        GlStateManager.func_179090_x();
        GlStateManager.func_179140_f();
        GlStateManager.func_179129_p();
        GlStateManager.func_179084_k();
        AxisAlignedBB bb = new AxisAlignedBB(BlockPos.field_177992_a, pos);
        RenderGlobal.func_189697_a((AxisAlignedBB)bb, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.func_179098_w();
        GlStateManager.func_179145_e();
        GlStateManager.func_179089_o();
        GlStateManager.func_179084_k();
    }
}

