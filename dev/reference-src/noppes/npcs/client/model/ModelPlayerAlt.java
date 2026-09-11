/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelBiped$ArmPose
 *  net.minecraft.client.model.ModelPlayer
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.EnumHandSide
 *  net.minecraft.util.math.MathHelper
 *  net.minecraftforge.fml.common.ObfuscationReflectionHelper
 */
package noppes.npcs.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import noppes.npcs.ModelData;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.client.model.animation.AniBow;
import noppes.npcs.client.model.animation.AniCrawling;
import noppes.npcs.client.model.animation.AniDancing;
import noppes.npcs.client.model.animation.AniHug;
import noppes.npcs.client.model.animation.AniNo;
import noppes.npcs.client.model.animation.AniPoint;
import noppes.npcs.client.model.animation.AniWaving;
import noppes.npcs.client.model.animation.AniYes;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.roles.JobPuppet;

public class ModelPlayerAlt
extends ModelPlayer {
    private ModelRenderer body;
    private ModelRenderer head;
    private Map<EnumParts, List<ModelScaleRenderer>> map = new HashMap<EnumParts, List<ModelScaleRenderer>>();

    public ModelPlayerAlt(float scale, boolean arms) {
        super(scale, arms);
        this.head = new ModelScaleRenderer((ModelBase)this, 24, 0, EnumParts.HEAD);
        this.head.addBox(-3.0f, -6.0f, -1.0f, 6, 6, 1, scale);
        this.body = new ModelScaleRenderer((ModelBase)this, 0, 0, EnumParts.BODY);
        this.body.setTextureSize(64, 32);
        this.body.addBox(-5.0f, 0.0f, -1.0f, 10, 16, 1, scale);
        ObfuscationReflectionHelper.setPrivateValue(ModelPlayer.class, (Object)((Object)this), (Object)this.head, (int)6);
        ObfuscationReflectionHelper.setPrivateValue(ModelPlayer.class, (Object)((Object)this), (Object)this.body, (int)5);
        this.bipedLeftArm = this.createScale(this.bipedLeftArm, EnumParts.ARM_LEFT);
        this.bipedRightArm = this.createScale(this.bipedRightArm, EnumParts.ARM_RIGHT);
        this.bipedLeftArmwear = this.createScale(this.bipedLeftArmwear, EnumParts.ARM_LEFT);
        this.bipedRightArmwear = this.createScale(this.bipedRightArmwear, EnumParts.ARM_RIGHT);
        this.bipedLeftLeg = this.createScale(this.bipedLeftLeg, EnumParts.LEG_LEFT);
        this.bipedRightLeg = this.createScale(this.bipedRightLeg, EnumParts.LEG_RIGHT);
        this.bipedLeftLegwear = this.createScale(this.bipedLeftLegwear, EnumParts.LEG_LEFT);
        this.bipedRightLegwear = this.createScale(this.bipedRightLegwear, EnumParts.LEG_RIGHT);
        this.bipedHead = this.createScale(this.bipedHead, EnumParts.HEAD);
        this.bipedHeadwear = this.createScale(this.bipedHeadwear, EnumParts.HEAD);
        this.bipedBody = this.createScale(this.bipedBody, EnumParts.BODY);
        this.bipedBodyWear = this.createScale(this.bipedBodyWear, EnumParts.BODY);
    }

    private ModelScaleRenderer createScale(ModelRenderer renderer, EnumParts part) {
        int textureX = (Integer)ObfuscationReflectionHelper.getPrivateValue(ModelRenderer.class, (Object)renderer, (int)2);
        int textureY = (Integer)ObfuscationReflectionHelper.getPrivateValue(ModelRenderer.class, (Object)renderer, (int)3);
        ModelScaleRenderer model = new ModelScaleRenderer((ModelBase)this, textureX, textureY, part);
        model.textureHeight = renderer.textureHeight;
        model.textureWidth = renderer.textureWidth;
        if (renderer.childModels != null) {
            model.childModels = new ArrayList(renderer.childModels);
        }
        model.cubeList = new ArrayList(renderer.cubeList);
        ModelPlayerAlt.copyModelAngles((ModelRenderer)renderer, (ModelRenderer)model);
        List<ModelScaleRenderer> list = this.map.get((Object)part);
        if (list == null) {
            list = new ArrayList<ModelScaleRenderer>();
            this.map.put(part, list);
        }
        list.add(model);
        return model;
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
        JobPuppet job;
        EntityCustomNpc player = (EntityCustomNpc)entity;
        ModelData playerdata = player.modelData;
        for (EnumParts part : this.map.keySet()) {
            ModelPartConfig config = playerdata.getPartConfig(part);
            for (ModelScaleRenderer model : this.map.get((Object)part)) {
                model.config = config;
            }
        }
        if (!this.isRiding) {
            boolean bl = this.isRiding = player.currentAnimation == 1;
        }
        if (this.isSneak && (player.currentAnimation == 7 || player.isPlayerSleeping())) {
            this.isSneak = false;
        }
        if (player.currentAnimation == 6) {
            this.rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
        }
        this.isSneak = player.isSneaking();
        this.bipedBody.rotationPointZ = 0.0f;
        this.bipedBody.rotationPointY = 0.0f;
        this.bipedBody.rotationPointX = 0.0f;
        this.bipedBody.rotateAngleZ = 0.0f;
        this.bipedBody.rotateAngleY = 0.0f;
        this.bipedBody.rotateAngleX = 0.0f;
        this.bipedHead.rotateAngleX = 0.0f;
        this.bipedHeadwear.rotateAngleX = 0.0f;
        this.bipedHead.rotateAngleZ = 0.0f;
        this.bipedHeadwear.rotateAngleZ = 0.0f;
        this.bipedHead.rotationPointX = 0.0f;
        this.bipedHeadwear.rotationPointX = 0.0f;
        this.bipedHead.rotationPointY = 0.0f;
        this.bipedHeadwear.rotationPointY = 0.0f;
        this.bipedHead.rotationPointZ = 0.0f;
        this.bipedHeadwear.rotationPointZ = 0.0f;
        this.bipedLeftLeg.rotateAngleX = 0.0f;
        this.bipedLeftLeg.rotateAngleY = 0.0f;
        this.bipedLeftLeg.rotateAngleZ = 0.0f;
        this.bipedRightLeg.rotateAngleX = 0.0f;
        this.bipedRightLeg.rotateAngleY = 0.0f;
        this.bipedRightLeg.rotateAngleZ = 0.0f;
        this.bipedLeftArm.rotationPointX = 0.0f;
        this.bipedLeftArm.rotationPointY = 2.0f;
        this.bipedLeftArm.rotationPointZ = 0.0f;
        this.bipedRightArm.rotationPointX = 0.0f;
        this.bipedRightArm.rotationPointY = 2.0f;
        this.bipedRightArm.rotationPointZ = 0.0f;
        try {
            super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (player.isPlayerSleeping()) {
            if (this.bipedHead.rotateAngleX < 0.0f) {
                this.bipedHead.rotateAngleX = 0.0f;
                this.bipedHeadwear.rotateAngleX = 0.0f;
            }
        } else if (player.currentAnimation == 9) {
            this.bipedHead.rotateAngleX = 0.7f;
            this.bipedHeadwear.rotateAngleX = 0.7f;
        } else if (player.currentAnimation == 3) {
            AniHug.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (player.currentAnimation == 7) {
            AniCrawling.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (player.currentAnimation == 10) {
            AniWaving.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (player.currentAnimation == 5) {
            AniDancing.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (player.currentAnimation == 11) {
            AniBow.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (player.currentAnimation == 13) {
            AniYes.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (player.currentAnimation == 12) {
            AniNo.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (player.currentAnimation == 8) {
            AniPoint.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity, (ModelBiped)this);
        } else if (this.isSneak) {
            this.bipedBody.rotateAngleX = 0.5f / playerdata.getPartConfig((EnumParts)EnumParts.BODY).scaleY;
        }
        if (player.advanced.job == 9 && (job = (JobPuppet)player.jobInterface).isActive()) {
            float pi = (float)Math.PI;
            float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
            if (!job.head.disabled) {
                this.bipedHeadwear.rotateAngleX = this.bipedHead.rotateAngleX = job.getRotationX(job.head, job.head2, partialTicks) * pi;
                this.bipedHeadwear.rotateAngleY = this.bipedHead.rotateAngleY = job.getRotationY(job.head, job.head2, partialTicks) * pi;
                this.bipedHeadwear.rotateAngleZ = this.bipedHead.rotateAngleZ = job.getRotationZ(job.head, job.head2, partialTicks) * pi;
            }
            if (!job.body.disabled) {
                this.bipedBody.rotateAngleX = job.getRotationX(job.body, job.body2, partialTicks) * pi;
                this.bipedBody.rotateAngleY = job.getRotationY(job.body, job.body2, partialTicks) * pi;
                this.bipedBody.rotateAngleZ = job.getRotationZ(job.body, job.body2, partialTicks) * pi;
            }
            if (!job.larm.disabled) {
                this.bipedLeftArm.rotateAngleX = job.getRotationX(job.larm, job.larm2, partialTicks) * pi;
                this.bipedLeftArm.rotateAngleY = job.getRotationY(job.larm, job.larm2, partialTicks) * pi;
                this.bipedLeftArm.rotateAngleZ = job.getRotationZ(job.larm, job.larm2, partialTicks) * pi;
                if (player.display.getHasLivingAnimation()) {
                    this.bipedLeftArm.rotateAngleZ -= MathHelper.cos((float)(ageInTicks * 0.09f)) * 0.05f + 0.05f;
                    this.bipedLeftArm.rotateAngleX -= MathHelper.sin((float)(ageInTicks * 0.067f)) * 0.05f;
                }
            }
            if (!job.rarm.disabled) {
                this.bipedRightArm.rotateAngleX = job.getRotationX(job.rarm, job.rarm2, partialTicks) * pi;
                this.bipedRightArm.rotateAngleY = job.getRotationY(job.rarm, job.rarm2, partialTicks) * pi;
                this.bipedRightArm.rotateAngleZ = job.getRotationZ(job.rarm, job.rarm2, partialTicks) * pi;
                if (player.display.getHasLivingAnimation()) {
                    this.bipedRightArm.rotateAngleZ += MathHelper.cos((float)(ageInTicks * 0.09f)) * 0.05f + 0.05f;
                    this.bipedRightArm.rotateAngleX += MathHelper.sin((float)(ageInTicks * 0.067f)) * 0.05f;
                }
            }
            if (!job.rleg.disabled) {
                this.bipedRightLeg.rotateAngleX = job.getRotationX(job.rleg, job.rleg2, partialTicks) * pi;
                this.bipedRightLeg.rotateAngleY = job.getRotationY(job.rleg, job.rleg2, partialTicks) * pi;
                this.bipedRightLeg.rotateAngleZ = job.getRotationZ(job.rleg, job.rleg2, partialTicks) * pi;
            }
            if (!job.lleg.disabled) {
                this.bipedLeftLeg.rotateAngleX = job.getRotationX(job.lleg, job.lleg2, partialTicks) * pi;
                this.bipedLeftLeg.rotateAngleY = job.getRotationY(job.lleg, job.lleg2, partialTicks) * pi;
                this.bipedLeftLeg.rotateAngleZ = job.getRotationZ(job.lleg, job.lleg2, partialTicks) * pi;
            }
        }
        ModelPlayerAlt.copyModelAngles((ModelRenderer)this.bipedLeftLeg, (ModelRenderer)this.bipedLeftLegwear);
        ModelPlayerAlt.copyModelAngles((ModelRenderer)this.bipedRightLeg, (ModelRenderer)this.bipedRightLegwear);
        ModelPlayerAlt.copyModelAngles((ModelRenderer)this.bipedLeftArm, (ModelRenderer)this.bipedLeftArmwear);
        ModelPlayerAlt.copyModelAngles((ModelRenderer)this.bipedRightArm, (ModelRenderer)this.bipedRightArmwear);
        ModelPlayerAlt.copyModelAngles((ModelRenderer)this.bipedBody, (ModelRenderer)this.bipedBodyWear);
        ModelPlayerAlt.copyModelAngles((ModelRenderer)this.bipedHead, (ModelRenderer)this.bipedHeadwear);
    }

    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        try {
            GlStateManager.pushMatrix();
            if (entityIn.isSneaking()) {
                GlStateManager.translate((float)0.0f, (float)0.2f, (float)0.0f);
            }
            this.bipedHead.render(scale);
            this.bipedBody.render(scale);
            this.bipedRightArm.render(scale);
            this.bipedLeftArm.render(scale);
            this.bipedRightLeg.render(scale);
            this.bipedLeftLeg.render(scale);
            this.bipedHeadwear.render(scale);
            this.bipedLeftLegwear.render(scale);
            this.bipedRightLegwear.render(scale);
            this.bipedLeftArmwear.render(scale);
            this.bipedRightArmwear.render(scale);
            this.bipedBodyWear.render(scale);
            GlStateManager.popMatrix();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    protected EnumHandSide getMainHand(Entity entityIn) {
        if (entityIn instanceof EntityLivingBase && ((EntityLivingBase)entityIn).isSwingInProgress) {
            EntityLivingBase living = (EntityLivingBase)entityIn;
            if (living.swingingHand == EnumHand.MAIN_HAND) {
                return EnumHandSide.RIGHT;
            }
            return EnumHandSide.LEFT;
        }
        return super.getMainHand(entityIn);
    }

    public ModelRenderer getRandomModelBox(Random random) {
        switch (random.nextInt(5)) {
            case 0: {
                return this.bipedHead;
            }
            case 1: {
                return this.bipedBody;
            }
            case 2: {
                return this.bipedLeftArm;
            }
            case 3: {
                return this.bipedRightArm;
            }
            case 4: {
                return this.bipedLeftLeg;
            }
            case 5: {
                return this.bipedRightLeg;
            }
        }
        return this.bipedHead;
    }
}

