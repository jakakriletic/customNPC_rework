/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelBiped$ArmPose
 *  net.minecraft.client.model.ModelRenderer
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
import net.minecraft.client.model.ModelRenderer;
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

public class ModelBipedAlt
extends ModelBiped {
    private Map<EnumParts, List<ModelScaleRenderer>> map = new HashMap<EnumParts, List<ModelScaleRenderer>>();

    public ModelBipedAlt(float scale) {
        super(scale);
        this.bipedLeftArm = this.createScale(this.bipedLeftArm, EnumParts.ARM_LEFT);
        this.bipedRightArm = this.createScale(this.bipedRightArm, EnumParts.ARM_RIGHT);
        this.bipedLeftLeg = this.createScale(this.bipedLeftLeg, EnumParts.LEG_LEFT);
        this.bipedRightLeg = this.createScale(this.bipedRightLeg, EnumParts.LEG_RIGHT);
        this.bipedHead = this.createScale(this.bipedHead, EnumParts.HEAD);
        this.bipedHeadwear = this.createScale(this.bipedHeadwear, EnumParts.HEAD);
        this.bipedBody = this.createScale(this.bipedBody, EnumParts.BODY);
    }

    private ModelScaleRenderer createScale(ModelRenderer renderer, EnumParts part) {
        int textureX = (Integer)ObfuscationReflectionHelper.getPrivateValue(ModelRenderer.class, (Object)renderer, (int)2);
        int textureY = (Integer)ObfuscationReflectionHelper.getPrivateValue(ModelRenderer.class, (Object)renderer, (int)3);
        ModelScaleRenderer model = new ModelScaleRenderer((ModelBase)this, textureX, textureY, part);
        model.textureHeight = renderer.textureHeight;
        model.textureWidth = renderer.textureWidth;
        model.childModels = renderer.childModels;
        model.cubeList = renderer.cubeList;
        ModelBipedAlt.copyModelAngles((ModelRenderer)renderer, (ModelRenderer)model);
        List<ModelScaleRenderer> list = this.map.get((Object)part);
        if (list == null) {
            list = new ArrayList<ModelScaleRenderer>();
            this.map.put(part, list);
        }
        list.add(model);
        return model;
    }

    public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
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
        super.setRotationAngles(par1, par2, par3, par4, par5, par6, entity);
        if (player.isPlayerSleeping()) {
            if (this.bipedHead.rotateAngleX < 0.0f) {
                this.bipedHead.rotateAngleX = 0.0f;
                this.bipedHeadwear.rotateAngleX = 0.0f;
            }
        } else if (player.currentAnimation == 9) {
            this.bipedHead.rotateAngleX = 0.7f;
            this.bipedHeadwear.rotateAngleX = 0.7f;
        } else if (player.currentAnimation == 3) {
            AniHug.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
        } else if (player.currentAnimation == 7) {
            AniCrawling.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
        } else if (player.currentAnimation == 10) {
            AniWaving.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
        } else if (player.currentAnimation == 5) {
            AniDancing.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
        } else if (player.currentAnimation == 11) {
            AniBow.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
        } else if (player.currentAnimation == 13) {
            AniYes.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
        } else if (player.currentAnimation == 12) {
            AniNo.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
        } else if (player.currentAnimation == 8) {
            AniPoint.setRotationAngles(par1, par2, par3, par4, par5, par6, entity, this);
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
                    this.bipedLeftArm.rotateAngleZ -= MathHelper.cos((float)(par3 * 0.09f)) * 0.05f + 0.05f;
                    this.bipedLeftArm.rotateAngleX -= MathHelper.sin((float)(par3 * 0.067f)) * 0.05f;
                }
            }
            if (!job.rarm.disabled) {
                this.bipedRightArm.rotateAngleX = job.getRotationX(job.rarm, job.rarm2, partialTicks) * pi;
                this.bipedRightArm.rotateAngleY = job.getRotationY(job.rarm, job.rarm2, partialTicks) * pi;
                this.bipedRightArm.rotateAngleZ = job.getRotationZ(job.rarm, job.rarm2, partialTicks) * pi;
                if (player.display.getHasLivingAnimation()) {
                    this.bipedRightArm.rotateAngleZ += MathHelper.cos((float)(par3 * 0.09f)) * 0.05f + 0.05f;
                    this.bipedRightArm.rotateAngleX += MathHelper.sin((float)(par3 * 0.067f)) * 0.05f;
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

