/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelBiped$ArmPose
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.entity.Render
 *  net.minecraft.client.renderer.entity.RenderLivingBase
 *  net.minecraft.client.renderer.entity.layers.LayerArmorBase
 *  net.minecraft.client.renderer.entity.layers.LayerBipedArmor
 *  net.minecraft.client.renderer.entity.layers.LayerCustomHead
 *  net.minecraft.client.renderer.entity.layers.LayerHeldItem
 *  net.minecraft.client.renderer.entity.layers.LayerRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.EnumAction
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.fml.common.ObfuscationReflectionHelper
 */
package noppes.npcs.client.renderer;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.NPCRendererHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.layer.LayerArms;
import noppes.npcs.client.layer.LayerBody;
import noppes.npcs.client.layer.LayerEyes;
import noppes.npcs.client.layer.LayerHead;
import noppes.npcs.client.layer.LayerHeadwear;
import noppes.npcs.client.layer.LayerLegs;
import noppes.npcs.client.layer.LayerNpcCloak;
import noppes.npcs.client.layer.LayerPreRender;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class RenderCustomNpc<T extends EntityCustomNpc>
extends RenderNPCInterface<T> {
    private float partialTicks;
    private EntityLivingBase entity;
    private RenderLivingBase renderEntity;
    public ModelBiped npcmodel;

    public RenderCustomNpc(ModelBiped model) {
        super((ModelBase)model, 0.5f);
        this.npcmodel = (ModelBiped)this.field_77045_g;
        this.field_177097_h.add(new LayerEyes(this));
        this.field_177097_h.add(new LayerHeadwear(this));
        this.field_177097_h.add(new LayerHead(this));
        this.field_177097_h.add(new LayerArms(this));
        this.field_177097_h.add(new LayerLegs(this));
        this.field_177097_h.add(new LayerBody(this));
        this.field_177097_h.add(new LayerNpcCloak(this));
        this.func_177094_a((LayerRenderer)new LayerHeldItem((RenderLivingBase)this));
        this.func_177094_a((LayerRenderer)new LayerCustomHead(this.npcmodel.field_78116_c));
        LayerBipedArmor armor = new LayerBipedArmor((RenderLivingBase)this);
        this.func_177094_a((LayerRenderer)armor);
        ObfuscationReflectionHelper.setPrivateValue(LayerArmorBase.class, (Object)armor, (Object)((Object)new ModelBipedAlt(0.5f)), (int)1);
        ObfuscationReflectionHelper.setPrivateValue(LayerArmorBase.class, (Object)armor, (Object)((Object)new ModelBipedAlt(1.0f)), (int)2);
    }

    @Override
    public void doRender(T npc, double d, double d1, double d2, float f, float partialTicks) {
        this.partialTicks = partialTicks;
        this.entity = ((EntityCustomNpc)((Object)npc)).modelData.getEntity((EntityNPCInterface)((Object)npc));
        if (this.entity != null) {
            Render render = this.field_76990_c.func_78713_a((Entity)this.entity);
            if (render instanceof RenderLivingBase) {
                this.renderEntity = (RenderLivingBase)render;
            } else {
                this.renderEntity = null;
                this.entity = null;
            }
        } else {
            this.renderEntity = null;
            List list = this.field_177097_h;
            for (LayerRenderer layer : list) {
                if (!(layer instanceof LayerPreRender)) continue;
                ((LayerPreRender)layer).preRender((EntityCustomNpc)((Object)npc));
            }
        }
        this.npcmodel.field_187076_m = this.getPose(npc, ((EntityNPCInterface)((Object)npc)).func_184614_ca());
        this.npcmodel.field_187075_l = this.getPose(npc, ((EntityNPCInterface)((Object)npc)).func_184592_cb());
        super.doRender(npc, d, d1, d2, f, partialTicks);
    }

    public ModelBiped.ArmPose getPose(T npc, ItemStack item) {
        if (NoppesUtilServer.IsItemStackNull(item)) {
            return ModelBiped.ArmPose.EMPTY;
        }
        if (npc.func_184605_cv() > 0) {
            EnumAction enumaction = item.func_77975_n();
            if (enumaction == EnumAction.BLOCK) {
                return ModelBiped.ArmPose.BLOCK;
            }
            if (enumaction == EnumAction.BOW) {
                return ModelBiped.ArmPose.BOW_AND_ARROW;
            }
        }
        return ModelBiped.ArmPose.ITEM;
    }

    @Override
    protected void renderModel(T npc, float par2, float par3, float par4, float par5, float par6, float par7) {
        if (this.renderEntity != null) {
            ModelBase pixModel;
            boolean flag1;
            boolean flag = !((EntityNPCInterface)((Object)npc)).func_82150_aj();
            boolean bl = flag1 = !flag && !((EntityNPCInterface)((Object)npc)).func_98034_c((EntityPlayer)Minecraft.func_71410_x().field_71439_g);
            if (!flag && !flag1) {
                return;
            }
            if (flag1) {
                GlStateManager.func_179094_E();
                GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)0.15f);
                GlStateManager.func_179132_a((boolean)false);
                GlStateManager.func_179147_l();
                GlStateManager.func_179112_b((int)770, (int)771);
                GlStateManager.func_179092_a((int)516, (float)0.003921569f);
            }
            ModelBase model = this.renderEntity.field_77045_g;
            if (PixelmonHelper.isPixelmon((Entity)this.entity) && (pixModel = (ModelBase)PixelmonHelper.getModel(this.entity)) != null) {
                model = pixModel;
                PixelmonHelper.setupModel(this.entity, pixModel);
            }
            model.field_78095_p = 1.0f;
            model.field_78093_q = this.entity.func_184218_aH() && this.entity.func_184187_bx() != null && this.entity.func_184187_bx().shouldRiderSit();
            model.func_78086_a(this.entity, par2, par3, this.partialTicks);
            model.func_78087_a(par2, par3, par4, par5, par6, par7, (Entity)this.entity);
            model.field_78091_s = this.entity.func_70631_g_();
            NPCRendererHelper.renderModel(this.entity, par2, par3, par4, par5, par6, par7, this.renderEntity, model, this.getEntityTexture(npc));
            if (!((EntityCustomNpc)((Object)npc)).display.getOverlayTexture().isEmpty()) {
                GlStateManager.func_179143_c((int)515);
                if (((EntityCustomNpc)((Object)npc)).textureGlowLocation == null) {
                    ((EntityCustomNpc)((Object)npc)).textureGlowLocation = new ResourceLocation(((EntityCustomNpc)((Object)npc)).display.getOverlayTexture());
                }
                float f1 = 1.0f;
                GlStateManager.func_179147_l();
                GlStateManager.func_179112_b((int)1, (int)1);
                GlStateManager.func_179140_f();
                if (((EntityNPCInterface)((Object)npc)).func_82150_aj()) {
                    GlStateManager.func_179132_a((boolean)false);
                } else {
                    GlStateManager.func_179132_a((boolean)true);
                }
                GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                GlStateManager.func_179094_E();
                GlStateManager.func_179152_a((float)1.001f, (float)1.001f, (float)1.001f);
                NPCRendererHelper.renderModel(this.entity, par2, par3, par4, par5, par6, par7, this.renderEntity, model, ((EntityCustomNpc)((Object)npc)).textureGlowLocation);
                GlStateManager.func_179121_F();
                GlStateManager.func_179145_e();
                GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)f1);
                GlStateManager.func_179143_c((int)515);
                GlStateManager.func_179084_k();
            }
            if (flag1) {
                GlStateManager.func_179084_k();
                GlStateManager.func_179092_a((int)516, (float)0.1f);
                GlStateManager.func_179121_F();
                GlStateManager.func_179132_a((boolean)true);
            }
        } else {
            super.renderModel(npc, par2, par3, par4, par5, par6, par7);
        }
    }

    protected void renderLayers(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn) {
        if (this.entity != null && this.renderEntity != null) {
            NPCRendererHelper.drawLayers(this.entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn, this.renderEntity);
        } else {
            super.func_177093_a(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn);
        }
    }

    @Override
    protected void preRenderCallback(T npc, float f) {
        if (this.renderEntity != null) {
            this.renderColor((EntityNPCInterface)((Object)npc));
            int size = ((EntityCustomNpc)((Object)npc)).display.getSize();
            if (this.entity instanceof EntityNPCInterface) {
                ((EntityNPCInterface)this.entity).display.setSize(5);
            }
            NPCRendererHelper.preRenderCallback(this.entity, f, this.renderEntity);
            ((EntityCustomNpc)((Object)npc)).display.setSize(size);
            GlStateManager.func_179152_a((float)(0.2f * (float)((EntityCustomNpc)((Object)npc)).display.getSize()), (float)(0.2f * (float)((EntityCustomNpc)((Object)npc)).display.getSize()), (float)(0.2f * (float)((EntityCustomNpc)((Object)npc)).display.getSize()));
        } else {
            super.preRenderCallback(npc, f);
        }
    }

    @Override
    protected float handleRotationFloat(T par1EntityLivingBase, float par2) {
        if (this.renderEntity != null) {
            return NPCRendererHelper.handleRotationFloat(this.entity, par2, this.renderEntity);
        }
        return super.handleRotationFloat(par1EntityLivingBase, par2);
    }
}

