/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.resources.IResource
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.model.ModelPlaneRenderer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityNpcPony;

public class ModelPony
extends ModelBase {
    private boolean rainboom;
    private float WingRotateAngleX;
    private float WingRotateAngleY;
    private float WingRotateAngleZ;
    private float TailRotateAngleY;
    public ModelRenderer Head;
    public ModelRenderer[] Headpiece;
    public ModelRenderer Helmet;
    public ModelRenderer Body;
    public ModelPlaneRenderer[] Bodypiece;
    public ModelRenderer RightArm;
    public ModelRenderer LeftArm;
    public ModelRenderer RightLeg;
    public ModelRenderer LeftLeg;
    public ModelRenderer unicornarm;
    public ModelPlaneRenderer[] Tail;
    public ModelRenderer[] LeftWing;
    public ModelRenderer[] RightWing;
    public ModelRenderer[] LeftWingExt;
    public ModelRenderer[] RightWingExt;
    public boolean isPegasus;
    public boolean isUnicorn;
    public boolean isFlying;
    public boolean isGlow;
    public boolean isSleeping;
    public boolean isSneak;
    public boolean aimedBow;
    public int heldItemRight;

    public ModelPony(float f) {
        this.init(f, 0.0f);
    }

    public void init(float strech, float f) {
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 0.0f;
        this.Head = new ModelRenderer((ModelBase)this, 0, 0);
        this.Head.func_78790_a(-4.0f, -4.0f, -6.0f, 8, 8, 8, strech);
        this.Head.func_78793_a(f2, f3 + f, f4);
        this.Headpiece = new ModelRenderer[3];
        this.Headpiece[0] = new ModelRenderer((ModelBase)this, 12, 16);
        this.Headpiece[0].func_78790_a(-4.0f, -6.0f, -1.0f, 2, 2, 2, strech);
        this.Headpiece[0].func_78793_a(f2, f3 + f, f4);
        this.Headpiece[1] = new ModelRenderer((ModelBase)this, 12, 16);
        this.Headpiece[1].func_78790_a(2.0f, -6.0f, -1.0f, 2, 2, 2, strech);
        this.Headpiece[1].func_78793_a(f2, f3 + f, f4);
        this.Headpiece[2] = new ModelRenderer((ModelBase)this, 56, 0);
        this.Headpiece[2].func_78790_a(-0.5f, -10.0f, -4.0f, 1, 4, 1, strech);
        this.Headpiece[2].func_78793_a(f2, f3 + f, f4);
        this.Helmet = new ModelRenderer((ModelBase)this, 32, 0);
        this.Helmet.func_78790_a(-4.0f, -4.0f, -6.0f, 8, 8, 8, strech + 0.5f);
        this.Helmet.func_78793_a(f2, f3, f4);
        float f5 = 0.0f;
        float f6 = 0.0f;
        float f7 = 0.0f;
        this.Body = new ModelRenderer((ModelBase)this, 16, 16);
        this.Body.func_78790_a(-4.0f, 4.0f, -2.0f, 8, 8, 4, strech);
        this.Body.func_78793_a(f5, f6 + f, f7);
        this.Bodypiece = new ModelPlaneRenderer[13];
        this.Bodypiece[0] = new ModelPlaneRenderer(this, 24, 0);
        this.Bodypiece[0].addSidePlane(-4.0f, 4.0f, 2.0f, 8, 8, strech);
        this.Bodypiece[0].func_78793_a(f5, f6 + f, f7);
        this.Bodypiece[1] = new ModelPlaneRenderer(this, 24, 0);
        this.Bodypiece[1].addSidePlane(4.0f, 4.0f, 2.0f, 8, 8, strech);
        this.Bodypiece[1].func_78793_a(f5, f6 + f, f7);
        this.Bodypiece[2] = new ModelPlaneRenderer(this, 24, 0);
        this.Bodypiece[2].addTopPlane(-4.0f, 4.0f, 2.0f, 8, 8, strech);
        this.Bodypiece[2].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[3] = new ModelPlaneRenderer(this, 24, 0);
        this.Bodypiece[3].addTopPlane(-4.0f, 12.0f, 2.0f, 8, 8, strech);
        this.Bodypiece[3].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[4] = new ModelPlaneRenderer(this, 0, 20);
        this.Bodypiece[4].addSidePlane(-4.0f, 4.0f, 10.0f, 8, 4, strech);
        this.Bodypiece[4].func_78793_a(f5, f6 + f, f7);
        this.Bodypiece[5] = new ModelPlaneRenderer(this, 0, 20);
        this.Bodypiece[5].addSidePlane(4.0f, 4.0f, 10.0f, 8, 4, strech);
        this.Bodypiece[5].func_78793_a(f5, f6 + f, f7);
        this.Bodypiece[6] = new ModelPlaneRenderer(this, 24, 0);
        this.Bodypiece[6].addTopPlane(-4.0f, 4.0f, 10.0f, 8, 4, strech);
        this.Bodypiece[6].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[7] = new ModelPlaneRenderer(this, 24, 0);
        this.Bodypiece[7].addTopPlane(-4.0f, 12.0f, 10.0f, 8, 4, strech);
        this.Bodypiece[7].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[8] = new ModelPlaneRenderer(this, 24, 0);
        this.Bodypiece[8].addBackPlane(-4.0f, 4.0f, 14.0f, 8, 8, strech);
        this.Bodypiece[8].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[9] = new ModelPlaneRenderer(this, 32, 0);
        this.Bodypiece[9].addTopPlane(-1.0f, 10.0f, 8.0f, 2, 6, strech);
        this.Bodypiece[9].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[10] = new ModelPlaneRenderer(this, 32, 0);
        this.Bodypiece[10].addTopPlane(-1.0f, 12.0f, 8.0f, 2, 6, strech);
        this.Bodypiece[10].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[11] = new ModelPlaneRenderer(this, 32, 0);
        this.Bodypiece[11].field_78809_i = true;
        this.Bodypiece[11].addSidePlane(-1.0f, 10.0f, 8.0f, 2, 6, strech);
        this.Bodypiece[11].func_78793_a(f2, f3 + f, f4);
        this.Bodypiece[12] = new ModelPlaneRenderer(this, 32, 0);
        this.Bodypiece[12].addSidePlane(1.0f, 10.0f, 8.0f, 2, 6, strech);
        this.Bodypiece[12].func_78793_a(f2, f3 + f, f4);
        this.RightArm = new ModelRenderer((ModelBase)this, 40, 16);
        this.RightArm.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.RightArm.func_78793_a(-3.0f, 8.0f + f, 0.0f);
        this.LeftArm = new ModelRenderer((ModelBase)this, 40, 16);
        this.LeftArm.field_78809_i = true;
        this.LeftArm.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.LeftArm.func_78793_a(3.0f, 8.0f + f, 0.0f);
        this.RightLeg = new ModelRenderer((ModelBase)this, 40, 16);
        this.RightLeg.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.RightLeg.func_78793_a(-3.0f, 0.0f + f, 0.0f);
        this.LeftLeg = new ModelRenderer((ModelBase)this, 40, 16);
        this.LeftLeg.field_78809_i = true;
        this.LeftLeg.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.LeftLeg.func_78793_a(3.0f, 0.0f + f, 0.0f);
        this.unicornarm = new ModelRenderer((ModelBase)this, 40, 16);
        this.unicornarm.func_78790_a(-3.0f, -2.0f, -2.0f, 4, 12, 4, strech);
        this.unicornarm.func_78793_a(-5.0f, 2.0f + f, 0.0f);
        float f8 = 0.0f;
        float f9 = 8.0f;
        float f10 = -14.0f;
        float f11 = 0.0f - f8;
        float f12 = 10.0f - f9;
        float f13 = 0.0f;
        this.Tail = new ModelPlaneRenderer[10];
        this.Tail[0] = new ModelPlaneRenderer(this, 32, 0);
        this.Tail[0].addTopPlane(-2.0f + f8, -7.0f + f9, 16.0f + f10, 4, 4, strech);
        this.Tail[0].func_78793_a(f11, f12 + f, f13);
        this.Tail[1] = new ModelPlaneRenderer(this, 32, 0);
        this.Tail[1].addTopPlane(-2.0f + f8, 9.0f + f9, 16.0f + f10, 4, 4, strech);
        this.Tail[1].func_78793_a(f11, f12 + f, f13);
        this.Tail[2] = new ModelPlaneRenderer(this, 32, 0);
        this.Tail[2].addBackPlane(-2.0f + f8, -7.0f + f9, 16.0f + f10, 4, 8, strech);
        this.Tail[2].func_78793_a(f11, f12 + f, f13);
        this.Tail[3] = new ModelPlaneRenderer(this, 32, 0);
        this.Tail[3].addBackPlane(-2.0f + f8, -7.0f + f9, 20.0f + f10, 4, 8, strech);
        this.Tail[3].func_78793_a(f11, f12 + f, f13);
        this.Tail[4] = new ModelPlaneRenderer(this, 32, 0);
        this.Tail[4].addBackPlane(-2.0f + f8, 1.0f + f9, 16.0f + f10, 4, 8, strech);
        this.Tail[4].func_78793_a(f11, f12 + f, f13);
        this.Tail[5] = new ModelPlaneRenderer(this, 32, 0);
        this.Tail[5].addBackPlane(-2.0f + f8, 1.0f + f9, 20.0f + f10, 4, 8, strech);
        this.Tail[5].func_78793_a(f11, f12 + f, f13);
        this.Tail[6] = new ModelPlaneRenderer(this, 36, 0);
        this.Tail[6].field_78809_i = true;
        this.Tail[6].addSidePlane(2.0f + f8, -7.0f + f9, 16.0f + f10, 8, 4, strech);
        this.Tail[6].func_78793_a(f11, f12 + f, f13);
        this.Tail[7] = new ModelPlaneRenderer(this, 36, 0);
        this.Tail[7].addSidePlane(-2.0f + f8, -7.0f + f9, 16.0f + f10, 8, 4, strech);
        this.Tail[7].func_78793_a(f11, f12 + f, f13);
        this.Tail[8] = new ModelPlaneRenderer(this, 36, 0);
        this.Tail[8].field_78809_i = true;
        this.Tail[8].addSidePlane(2.0f + f8, 1.0f + f9, 16.0f + f10, 8, 4, strech);
        this.Tail[8].func_78793_a(f11, f12 + f, f13);
        this.Tail[9] = new ModelPlaneRenderer(this, 36, 0);
        this.Tail[9].addSidePlane(-2.0f + f8, 1.0f + f9, 16.0f + f10, 8, 4, strech);
        this.Tail[9].func_78793_a(f11, f12 + f, f13);
        this.TailRotateAngleY = this.Tail[0].field_78796_g;
        this.TailRotateAngleY = this.Tail[0].field_78796_g;
        float f14 = 0.0f;
        float f15 = 0.0f;
        float f16 = 0.0f;
        this.LeftWing = new ModelRenderer[3];
        this.LeftWing[0] = new ModelRenderer((ModelBase)this, 56, 16);
        this.LeftWing[0].field_78809_i = true;
        this.LeftWing[0].func_78790_a(4.0f, 5.0f, 2.0f, 2, 6, 2, strech);
        this.LeftWing[0].func_78793_a(f14, f15 + f, f16);
        this.LeftWing[1] = new ModelRenderer((ModelBase)this, 56, 16);
        this.LeftWing[1].field_78809_i = true;
        this.LeftWing[1].func_78790_a(4.0f, 5.0f, 4.0f, 2, 8, 2, strech);
        this.LeftWing[1].func_78793_a(f14, f15 + f, f16);
        this.LeftWing[2] = new ModelRenderer((ModelBase)this, 56, 16);
        this.LeftWing[2].field_78809_i = true;
        this.LeftWing[2].func_78790_a(4.0f, 5.0f, 6.0f, 2, 6, 2, strech);
        this.LeftWing[2].func_78793_a(f14, f15 + f, f16);
        this.RightWing = new ModelRenderer[3];
        this.RightWing[0] = new ModelRenderer((ModelBase)this, 56, 16);
        this.RightWing[0].func_78790_a(-6.0f, 5.0f, 2.0f, 2, 6, 2, strech);
        this.RightWing[0].func_78793_a(f14, f15 + f, f16);
        this.RightWing[1] = new ModelRenderer((ModelBase)this, 56, 16);
        this.RightWing[1].func_78790_a(-6.0f, 5.0f, 4.0f, 2, 8, 2, strech);
        this.RightWing[1].func_78793_a(f14, f15 + f, f16);
        this.RightWing[2] = new ModelRenderer((ModelBase)this, 56, 16);
        this.RightWing[2].func_78790_a(-6.0f, 5.0f, 6.0f, 2, 6, 2, strech);
        this.RightWing[2].func_78793_a(f14, f15 + f, f16);
        float f17 = f2 + 4.5f;
        float f18 = f3 + 5.0f;
        float f19 = f4 + 6.0f;
        this.LeftWingExt = new ModelRenderer[7];
        this.LeftWingExt[0] = new ModelRenderer((ModelBase)this, 56, 19);
        this.LeftWingExt[0].field_78809_i = true;
        this.LeftWingExt[0].func_78790_a(0.0f, 0.0f, 0.0f, 1, 8, 2, strech + 0.1f);
        this.LeftWingExt[0].func_78793_a(f17, f18 + f, f19);
        this.LeftWingExt[1] = new ModelRenderer((ModelBase)this, 56, 19);
        this.LeftWingExt[1].field_78809_i = true;
        this.LeftWingExt[1].func_78790_a(0.0f, 8.0f, 0.0f, 1, 6, 2, strech + 0.1f);
        this.LeftWingExt[1].func_78793_a(f17, f18 + f, f19);
        this.LeftWingExt[2] = new ModelRenderer((ModelBase)this, 56, 19);
        this.LeftWingExt[2].field_78809_i = true;
        this.LeftWingExt[2].func_78790_a(0.0f, -1.2f, -0.2f, 1, 8, 2, strech - 0.2f);
        this.LeftWingExt[2].func_78793_a(f17, f18 + f, f19);
        this.LeftWingExt[3] = new ModelRenderer((ModelBase)this, 56, 19);
        this.LeftWingExt[3].field_78809_i = true;
        this.LeftWingExt[3].func_78790_a(0.0f, 1.8f, 1.3f, 1, 8, 2, strech - 0.1f);
        this.LeftWingExt[3].func_78793_a(f17, f18 + f, f19);
        this.LeftWingExt[4] = new ModelRenderer((ModelBase)this, 56, 19);
        this.LeftWingExt[4].field_78809_i = true;
        this.LeftWingExt[4].func_78790_a(0.0f, 5.0f, 2.0f, 1, 8, 2, strech);
        this.LeftWingExt[4].func_78793_a(f17, f18 + f, f19);
        this.LeftWingExt[5] = new ModelRenderer((ModelBase)this, 56, 19);
        this.LeftWingExt[5].field_78809_i = true;
        this.LeftWingExt[5].func_78790_a(0.0f, 0.0f, -0.2f, 1, 6, 2, strech + 0.3f);
        this.LeftWingExt[5].func_78793_a(f17, f18 + f, f19);
        this.LeftWingExt[6] = new ModelRenderer((ModelBase)this, 56, 19);
        this.LeftWingExt[6].field_78809_i = true;
        this.LeftWingExt[6].func_78790_a(0.0f, 0.0f, 0.2f, 1, 3, 2, strech + 0.2f);
        this.LeftWingExt[6].func_78793_a(f17, f18 + f, f19);
        float f20 = f2 - 4.5f;
        float f21 = f3 + 5.0f;
        float f22 = f4 + 6.0f;
        this.RightWingExt = new ModelRenderer[7];
        this.RightWingExt[0] = new ModelRenderer((ModelBase)this, 56, 19);
        this.RightWingExt[0].field_78809_i = true;
        this.RightWingExt[0].func_78790_a(0.0f, 0.0f, 0.0f, 1, 8, 2, strech + 0.1f);
        this.RightWingExt[0].func_78793_a(f20, f21 + f, f22);
        this.RightWingExt[1] = new ModelRenderer((ModelBase)this, 56, 19);
        this.RightWingExt[1].field_78809_i = true;
        this.RightWingExt[1].func_78790_a(0.0f, 8.0f, 0.0f, 1, 6, 2, strech + 0.1f);
        this.RightWingExt[1].func_78793_a(f20, f21 + f, f22);
        this.RightWingExt[2] = new ModelRenderer((ModelBase)this, 56, 19);
        this.RightWingExt[2].field_78809_i = true;
        this.RightWingExt[2].func_78790_a(0.0f, -1.2f, -0.2f, 1, 8, 2, strech - 0.2f);
        this.RightWingExt[2].func_78793_a(f20, f21 + f, f22);
        this.RightWingExt[3] = new ModelRenderer((ModelBase)this, 56, 19);
        this.RightWingExt[3].field_78809_i = true;
        this.RightWingExt[3].func_78790_a(0.0f, 1.8f, 1.3f, 1, 8, 2, strech - 0.1f);
        this.RightWingExt[3].func_78793_a(f20, f21 + f, f22);
        this.RightWingExt[4] = new ModelRenderer((ModelBase)this, 56, 19);
        this.RightWingExt[4].field_78809_i = true;
        this.RightWingExt[4].func_78790_a(0.0f, 5.0f, 2.0f, 1, 8, 2, strech);
        this.RightWingExt[4].func_78793_a(f20, f21 + f, f22);
        this.RightWingExt[5] = new ModelRenderer((ModelBase)this, 56, 19);
        this.RightWingExt[5].field_78809_i = true;
        this.RightWingExt[5].func_78790_a(0.0f, 0.0f, -0.2f, 1, 6, 2, strech + 0.3f);
        this.RightWingExt[5].func_78793_a(f20, f21 + f, f22);
        this.RightWingExt[6] = new ModelRenderer((ModelBase)this, 56, 19);
        this.RightWingExt[6].field_78809_i = true;
        this.RightWingExt[6].func_78790_a(0.0f, 0.0f, 0.2f, 1, 3, 2, strech + 0.2f);
        this.RightWingExt[6].func_78793_a(f20, f21 + f, f22);
        this.WingRotateAngleX = this.LeftWingExt[0].field_78795_f;
        this.WingRotateAngleY = this.LeftWingExt[0].field_78796_g;
        this.WingRotateAngleZ = this.LeftWingExt[0].field_78808_h;
    }

    public void func_78087_a(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        float f11;
        float f10;
        float f9;
        float f8;
        float f7;
        float f6;
        EntityNPCInterface npc = (EntityNPCInterface)entity;
        this.field_78093_q = npc.func_184218_aH();
        if (this.isSneak && (npc.currentAnimation == 7 || npc.currentAnimation == 2)) {
            this.isSneak = false;
        }
        this.rainboom = false;
        if (this.isSleeping) {
            f6 = 1.4f;
            f7 = 0.1f;
        } else {
            f6 = f3 / 57.29578f;
            f7 = f4 / 57.29578f;
        }
        this.Head.field_78796_g = f6;
        this.Head.field_78795_f = f7;
        this.Headpiece[0].field_78796_g = f6;
        this.Headpiece[0].field_78795_f = f7;
        this.Headpiece[1].field_78796_g = f6;
        this.Headpiece[1].field_78795_f = f7;
        this.Headpiece[2].field_78796_g = f6;
        this.Headpiece[2].field_78795_f = f7;
        this.Helmet.field_78796_g = f6;
        this.Helmet.field_78795_f = f7;
        this.Headpiece[2].field_78795_f = f7 + 0.5f;
        if (!this.isFlying || !this.isPegasus) {
            f8 = MathHelper.func_76134_b((float)(f * 0.6662f + 3.141593f)) * 0.6f * f1;
            f9 = MathHelper.func_76134_b((float)(f * 0.6662f)) * 0.6f * f1;
            f10 = MathHelper.func_76134_b((float)(f * 0.6662f)) * 0.3f * f1;
            f11 = MathHelper.func_76134_b((float)(f * 0.6662f + 3.141593f)) * 0.3f * f1;
            this.RightArm.field_78796_g = 0.0f;
            this.unicornarm.field_78796_g = 0.0f;
            this.LeftArm.field_78796_g = 0.0f;
            this.RightLeg.field_78796_g = 0.0f;
            this.LeftLeg.field_78796_g = 0.0f;
        } else {
            if (f1 < 0.9999f) {
                this.rainboom = false;
                f8 = MathHelper.func_76126_a((float)(0.0f - f1 * 0.5f));
                f9 = MathHelper.func_76126_a((float)(0.0f - f1 * 0.5f));
                f10 = MathHelper.func_76126_a((float)(f1 * 0.5f));
                f11 = MathHelper.func_76126_a((float)(f1 * 0.5f));
            } else {
                this.rainboom = true;
                f8 = 4.712f;
                f9 = 4.712f;
                f10 = 1.571f;
                f11 = 1.571f;
            }
            this.RightArm.field_78796_g = 0.2f;
            this.LeftArm.field_78796_g = -0.2f;
            this.RightLeg.field_78796_g = -0.2f;
            this.LeftLeg.field_78796_g = 0.2f;
        }
        if (this.isSleeping) {
            f8 = 4.712f;
            f9 = 4.712f;
            f10 = 1.571f;
            f11 = 1.571f;
        }
        this.RightArm.field_78795_f = f8;
        this.unicornarm.field_78795_f = 0.0f;
        this.LeftArm.field_78795_f = f9;
        this.RightLeg.field_78795_f = f10;
        this.LeftLeg.field_78795_f = f11;
        this.RightArm.field_78808_h = 0.0f;
        this.unicornarm.field_78808_h = 0.0f;
        this.LeftArm.field_78808_h = 0.0f;
        for (int i = 0; i < this.Tail.length; ++i) {
            this.Tail[i].field_78808_h = this.rainboom ? 0.0f : MathHelper.func_76134_b((float)(f * 0.8f)) * 0.2f * f1;
        }
        if (this.heldItemRight != 0 && !this.rainboom && !this.isUnicorn) {
            this.RightArm.field_78795_f = this.RightArm.field_78795_f * 0.5f - 0.3141593f;
        }
        float f12 = 0.0f;
        if (f5 > -9990.0f && !this.isUnicorn) {
            f12 = MathHelper.func_76126_a((float)(MathHelper.func_76129_c((float)f5) * 3.141593f * 2.0f)) * 0.2f;
        }
        this.Body.field_78796_g = (float)((double)f12 * 0.2);
        for (int j = 0; j < this.Bodypiece.length; ++j) {
            this.Bodypiece[j].field_78796_g = (float)((double)f12 * 0.2);
        }
        for (int k = 0; k < this.LeftWing.length; ++k) {
            this.LeftWing[k].field_78796_g = (float)((double)f12 * 0.2);
        }
        for (int l = 0; l < this.RightWing.length; ++l) {
            this.RightWing[l].field_78796_g = (float)((double)f12 * 0.2);
        }
        for (int i1 = 0; i1 < this.Tail.length; ++i1) {
            this.Tail[i1].field_78796_g = f12;
        }
        float f13 = MathHelper.func_76126_a((float)this.Body.field_78796_g) * 5.0f;
        float f14 = MathHelper.func_76134_b((float)this.Body.field_78796_g) * 5.0f;
        float f15 = 4.0f;
        if (this.isSneak && !this.isFlying) {
            f15 = 0.0f;
        }
        if (this.isSleeping) {
            f15 = 2.6f;
        }
        if (this.rainboom) {
            this.RightArm.field_78798_e = f13 + 2.0f;
            this.LeftArm.field_78798_e = 0.0f - f13 + 2.0f;
        } else {
            this.RightArm.field_78798_e = f13 + 1.0f;
            this.LeftArm.field_78798_e = 0.0f - f13 + 1.0f;
        }
        this.RightArm.field_78800_c = 0.0f - f14 - 1.0f + f15;
        this.LeftArm.field_78800_c = f14 + 1.0f - f15;
        this.RightLeg.field_78800_c = 0.0f - f14 - 1.0f + f15;
        this.LeftLeg.field_78800_c = f14 + 1.0f - f15;
        this.RightArm.field_78796_g += this.Body.field_78796_g;
        this.LeftArm.field_78796_g += this.Body.field_78796_g;
        this.LeftArm.field_78795_f += this.Body.field_78796_g;
        this.RightArm.field_78797_d = 8.0f;
        this.LeftArm.field_78797_d = 8.0f;
        this.RightLeg.field_78797_d = 4.0f;
        this.LeftLeg.field_78797_d = 4.0f;
        if (f5 > -9990.0f) {
            float f16 = f5;
            f16 = 1.0f - f5;
            f16 *= f16 * f16;
            f16 = 1.0f - f16;
            float f22 = MathHelper.func_76126_a((float)(f16 * 3.141593f));
            float f28 = MathHelper.func_76126_a((float)(f5 * 3.141593f));
            float f33 = f28 * -(this.Head.field_78795_f - 0.7f) * 0.75f;
            if (this.isUnicorn) {
                this.unicornarm.field_78795_f = (float)((double)this.unicornarm.field_78795_f - ((double)f22 * 1.2 + (double)f33));
                this.unicornarm.field_78796_g += this.Body.field_78796_g * 2.0f;
                this.unicornarm.field_78808_h = f28 * -0.4f;
            } else {
                this.unicornarm.field_78795_f = (float)((double)this.unicornarm.field_78795_f - ((double)f22 * 1.2 + (double)f33));
                this.unicornarm.field_78796_g += this.Body.field_78796_g * 2.0f;
                this.unicornarm.field_78808_h = f28 * -0.4f;
            }
        }
        if (this.isSneak && !this.isFlying) {
            float f50;
            float f48;
            float f46;
            float f17 = 0.4f;
            float f23 = 7.0f;
            float f29 = -4.0f;
            this.Body.field_78795_f = f17;
            this.Body.field_78797_d = f23;
            this.Body.field_78798_e = f29;
            for (int i3 = 0; i3 < this.Bodypiece.length; ++i3) {
                this.Bodypiece[i3].field_78795_f = f17;
                this.Bodypiece[i3].field_78797_d = f23;
                this.Bodypiece[i3].field_78798_e = f29;
            }
            float f34 = 3.5f;
            float f37 = 6.0f;
            for (int i4 = 0; i4 < this.LeftWingExt.length; ++i4) {
                this.LeftWingExt[i4].field_78795_f = (float)((double)f17 + 2.3561947345733643);
                this.LeftWingExt[i4].field_78797_d = f23 + f34;
                this.LeftWingExt[i4].field_78798_e = f29 + f37;
                this.LeftWingExt[i4].field_78795_f = 2.5f;
                this.LeftWingExt[i4].field_78808_h = -6.0f;
            }
            float f40 = 4.5f;
            float f43 = 6.0f;
            for (int i5 = 0; i5 < this.LeftWingExt.length; ++i5) {
                this.RightWingExt[i5].field_78795_f = (float)((double)f17 + 2.3561947345733643);
                this.RightWingExt[i5].field_78797_d = f23 + f40;
                this.RightWingExt[i5].field_78798_e = f29 + f43;
                this.RightWingExt[i5].field_78795_f = 2.5f;
                this.RightWingExt[i5].field_78808_h = 6.0f;
            }
            this.RightLeg.field_78795_f -= 0.0f;
            this.LeftLeg.field_78795_f -= 0.0f;
            this.RightArm.field_78795_f -= 0.4f;
            this.unicornarm.field_78795_f += 0.4f;
            this.LeftArm.field_78795_f -= 0.4f;
            this.RightLeg.field_78798_e = 10.0f;
            this.LeftLeg.field_78798_e = 10.0f;
            this.RightLeg.field_78797_d = 7.0f;
            this.LeftLeg.field_78797_d = 7.0f;
            if (this.isSleeping) {
                f46 = 2.0f;
                f48 = -1.0f;
                f50 = 1.0f;
            } else {
                f46 = 6.0f;
                f48 = -2.0f;
                f50 = 0.0f;
            }
            this.Head.field_78797_d = f46;
            this.Head.field_78798_e = f48;
            this.Head.field_78800_c = f50;
            this.Helmet.field_78797_d = f46;
            this.Helmet.field_78798_e = f48;
            this.Helmet.field_78800_c = f50;
            this.Headpiece[0].field_78797_d = f46;
            this.Headpiece[0].field_78798_e = f48;
            this.Headpiece[0].field_78800_c = f50;
            this.Headpiece[1].field_78797_d = f46;
            this.Headpiece[1].field_78798_e = f48;
            this.Headpiece[1].field_78800_c = f50;
            this.Headpiece[2].field_78797_d = f46;
            this.Headpiece[2].field_78798_e = f48;
            this.Headpiece[2].field_78800_c = f50;
            float f52 = 0.0f;
            float f54 = 8.0f;
            float f56 = -14.0f;
            float f58 = 0.0f - f52;
            float f60 = 9.0f - f54;
            float f62 = -4.0f - f56;
            float f63 = 0.0f;
            for (int i6 = 0; i6 < this.Tail.length; ++i6) {
                this.Tail[i6].field_78800_c = f58;
                this.Tail[i6].field_78797_d = f60;
                this.Tail[i6].field_78798_e = f62;
                this.Tail[i6].field_78795_f = f63;
            }
        } else {
            float f47;
            float f45;
            float f42;
            float f18 = 0.0f;
            float f24 = 0.0f;
            float f30 = 0.0f;
            this.Body.field_78795_f = f18;
            this.Body.field_78797_d = f24;
            this.Body.field_78798_e = f30;
            for (int j3 = 0; j3 < this.Bodypiece.length; ++j3) {
                this.Bodypiece[j3].field_78795_f = f18;
                this.Bodypiece[j3].field_78797_d = f24;
                this.Bodypiece[j3].field_78798_e = f30;
            }
            if (this.isPegasus) {
                if (!this.isFlying) {
                    for (int k3 = 0; k3 < this.LeftWing.length; ++k3) {
                        this.LeftWing[k3].field_78795_f = (float)((double)f18 + 1.5707964897155762);
                        this.LeftWing[k3].field_78797_d = f24 + 13.0f;
                        this.LeftWing[k3].field_78798_e = f30 - 3.0f;
                    }
                    for (int l3 = 0; l3 < this.RightWing.length; ++l3) {
                        this.RightWing[l3].field_78795_f = (float)((double)f18 + 1.5707964897155762);
                        this.RightWing[l3].field_78797_d = f24 + 13.0f;
                        this.RightWing[l3].field_78798_e = f30 - 3.0f;
                    }
                } else {
                    float f35 = 5.5f;
                    float f38 = 3.0f;
                    for (int j4 = 0; j4 < this.LeftWingExt.length; ++j4) {
                        this.LeftWingExt[j4].field_78795_f = (float)((double)f18 + 1.5707964897155762);
                        this.LeftWingExt[j4].field_78797_d = f24 + f35;
                        this.LeftWingExt[j4].field_78798_e = f30 + f38;
                    }
                    float f41 = 6.5f;
                    float f44 = 3.0f;
                    for (int j5 = 0; j5 < this.RightWingExt.length; ++j5) {
                        this.RightWingExt[j5].field_78795_f = (float)((double)f18 + 1.5707964897155762);
                        this.RightWingExt[j5].field_78797_d = f24 + f41;
                        this.RightWingExt[j5].field_78798_e = f30 + f44;
                    }
                }
            }
            this.RightLeg.field_78798_e = 10.0f;
            this.LeftLeg.field_78798_e = 10.0f;
            this.RightLeg.field_78797_d = 8.0f;
            this.LeftLeg.field_78797_d = 8.0f;
            float f36 = MathHelper.func_76134_b((float)(f2 * 0.09f)) * 0.05f + 0.05f;
            float f39 = MathHelper.func_76126_a((float)(f2 * 0.067f)) * 0.05f;
            this.unicornarm.field_78808_h += f36;
            this.unicornarm.field_78795_f += f39;
            if (this.isPegasus && this.isFlying) {
                this.WingRotateAngleY = MathHelper.func_76126_a((float)(f2 * 0.067f * 8.0f)) * 1.0f;
                this.WingRotateAngleZ = MathHelper.func_76126_a((float)(f2 * 0.067f * 8.0f)) * 1.0f;
                for (int k4 = 0; k4 < this.LeftWingExt.length; ++k4) {
                    this.LeftWingExt[k4].field_78795_f = 2.5f;
                    this.LeftWingExt[k4].field_78808_h = -this.WingRotateAngleZ - 4.712f - 0.4f;
                }
                for (int l4 = 0; l4 < this.RightWingExt.length; ++l4) {
                    this.RightWingExt[l4].field_78795_f = 2.5f;
                    this.RightWingExt[l4].field_78808_h = this.WingRotateAngleZ + 4.712f + 0.4f;
                }
            }
            if (this.isSleeping) {
                f42 = 2.0f;
                f45 = 1.0f;
                f47 = 1.0f;
            } else {
                f42 = 0.0f;
                f45 = 0.0f;
                f47 = 0.0f;
            }
            this.Head.field_78797_d = f42;
            this.Head.field_78798_e = f45;
            this.Head.field_78800_c = f47;
            this.Helmet.field_78797_d = f42;
            this.Helmet.field_78798_e = f45;
            this.Helmet.field_78800_c = f47;
            this.Headpiece[0].field_78797_d = f42;
            this.Headpiece[0].field_78798_e = f45;
            this.Headpiece[0].field_78800_c = f47;
            this.Headpiece[1].field_78797_d = f42;
            this.Headpiece[1].field_78798_e = f45;
            this.Headpiece[1].field_78800_c = f47;
            this.Headpiece[2].field_78797_d = f42;
            this.Headpiece[2].field_78798_e = f45;
            this.Headpiece[2].field_78800_c = f47;
            float f49 = 0.0f;
            float f51 = 8.0f;
            float f53 = -14.0f;
            float f55 = 0.0f - f49;
            float f57 = 9.0f - f51;
            float f59 = 0.0f - f53;
            float f61 = 0.5f * f1;
            for (int k5 = 0; k5 < this.Tail.length; ++k5) {
                this.Tail[k5].field_78800_c = f55;
                this.Tail[k5].field_78797_d = f57;
                this.Tail[k5].field_78798_e = f59;
                this.Tail[k5].field_78795_f = this.rainboom ? 1.571f + 0.1f * MathHelper.func_76126_a((float)f) : f61;
            }
            for (int l5 = 0; l5 < this.Tail.length; ++l5) {
                if (this.rainboom) continue;
                this.Tail[l5].field_78795_f += f39;
            }
        }
        this.LeftWingExt[2].field_78795_f -= 0.85f;
        this.LeftWingExt[3].field_78795_f -= 0.75f;
        this.LeftWingExt[4].field_78795_f -= 0.5f;
        this.LeftWingExt[6].field_78795_f -= 0.85f;
        this.RightWingExt[2].field_78795_f -= 0.85f;
        this.RightWingExt[3].field_78795_f -= 0.75f;
        this.RightWingExt[4].field_78795_f -= 0.5f;
        this.RightWingExt[6].field_78795_f -= 0.85f;
        this.Bodypiece[9].field_78795_f += 0.5f;
        this.Bodypiece[10].field_78795_f += 0.5f;
        this.Bodypiece[11].field_78795_f += 0.5f;
        this.Bodypiece[12].field_78795_f += 0.5f;
        if (this.rainboom) {
            for (int j1 = 0; j1 < this.Tail.length; ++j1) {
                this.Tail[j1].field_78797_d += 6.0f;
                this.Tail[j1].field_78798_e += 1.0f;
            }
        }
        if (this.isSleeping) {
            this.RightArm.field_78798_e += 6.0f;
            this.LeftArm.field_78798_e += 6.0f;
            this.RightLeg.field_78798_e -= 8.0f;
            this.LeftLeg.field_78798_e -= 8.0f;
            this.RightArm.field_78797_d += 2.0f;
            this.LeftArm.field_78797_d += 2.0f;
            this.RightLeg.field_78797_d += 2.0f;
            this.LeftLeg.field_78797_d += 2.0f;
        }
        if (this.aimedBow) {
            if (this.isUnicorn) {
                float f20 = 0.0f;
                float f26 = 0.0f;
                this.unicornarm.field_78808_h = 0.0f;
                this.unicornarm.field_78796_g = -(0.1f - f20 * 0.6f) + this.Head.field_78796_g;
                this.unicornarm.field_78795_f = 4.712f + this.Head.field_78795_f;
                this.unicornarm.field_78795_f -= f20 * 1.2f - f26 * 0.4f;
                float f31 = f2;
                this.unicornarm.field_78808_h += MathHelper.func_76134_b((float)(f31 * 0.09f)) * 0.05f + 0.05f;
                this.unicornarm.field_78795_f += MathHelper.func_76126_a((float)(f31 * 0.067f)) * 0.05f;
            } else {
                float f21 = 0.0f;
                float f27 = 0.0f;
                this.RightArm.field_78808_h = 0.0f;
                this.RightArm.field_78796_g = -(0.1f - f21 * 0.6f) + this.Head.field_78796_g;
                this.RightArm.field_78795_f = 4.712f + this.Head.field_78795_f;
                this.RightArm.field_78795_f -= f21 * 1.2f - f27 * 0.4f;
                float f32 = f2;
                this.RightArm.field_78808_h += MathHelper.func_76134_b((float)(f32 * 0.09f)) * 0.05f + 0.05f;
                this.RightArm.field_78795_f += MathHelper.func_76126_a((float)(f32 * 0.067f)) * 0.05f;
                this.RightArm.field_78798_e += 1.0f;
            }
        }
    }

    public void func_78088_a(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        EntityNpcPony pony = (EntityNpcPony)entity;
        if (pony.textureLocation != pony.checked && pony.textureLocation != null) {
            try {
                IResource resource = Minecraft.func_71410_x().func_110442_L().func_110536_a(pony.textureLocation);
                BufferedImage bufferedimage = ImageIO.read(resource.func_110527_b());
                pony.isPegasus = false;
                pony.isUnicorn = false;
                Color color = new Color(bufferedimage.getRGB(0, 0), true);
                Color color1 = new Color(249, 177, 49, 255);
                Color color2 = new Color(136, 202, 240, 255);
                Color color3 = new Color(209, 159, 228, 255);
                Color color4 = new Color(254, 249, 252, 255);
                if (color.equals(color1)) {
                    // empty if block
                }
                if (color.equals(color2)) {
                    pony.isPegasus = true;
                }
                if (color.equals(color3)) {
                    pony.isUnicorn = true;
                }
                if (color.equals(color4)) {
                    pony.isPegasus = true;
                    pony.isUnicorn = true;
                }
                pony.checked = pony.textureLocation;
            }
            catch (IOException resource) {
                // empty catch block
            }
        }
        this.isSleeping = pony.func_70608_bn();
        this.isUnicorn = pony.isUnicorn;
        this.isPegasus = pony.isPegasus;
        this.isSneak = pony.func_70093_af();
        this.heldItemRight = pony.func_184614_ca() == null ? 0 : 1;
        this.func_78087_a(f, f1, f2, f3, f4, f5, entity);
        GlStateManager.func_179094_E();
        if (this.isSleeping) {
            GlStateManager.func_179114_b((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GlStateManager.func_179109_b((float)0.0f, (float)-0.5f, (float)-0.9f);
        }
        float scale = f5;
        this.Head.func_78785_a(scale);
        this.Headpiece[0].func_78785_a(scale);
        this.Headpiece[1].func_78785_a(scale);
        if (this.isUnicorn) {
            this.Headpiece[2].func_78785_a(scale);
        }
        this.Helmet.func_78785_a(scale);
        this.Body.func_78785_a(scale);
        for (int i = 0; i < this.Bodypiece.length; ++i) {
            this.Bodypiece[i].func_78785_a(scale);
        }
        this.LeftArm.func_78785_a(scale);
        this.RightArm.func_78785_a(scale);
        this.LeftLeg.func_78785_a(scale);
        this.RightLeg.func_78785_a(scale);
        for (int j = 0; j < this.Tail.length; ++j) {
            this.Tail[j].func_78785_a(scale);
        }
        if (this.isPegasus) {
            if (this.isFlying || this.isSneak) {
                for (int k = 0; k < this.LeftWingExt.length; ++k) {
                    this.LeftWingExt[k].func_78785_a(scale);
                }
                for (int l = 0; l < this.RightWingExt.length; ++l) {
                    this.RightWingExt[l].func_78785_a(scale);
                }
            } else {
                for (int i1 = 0; i1 < this.LeftWing.length; ++i1) {
                    this.LeftWing[i1].func_78785_a(scale);
                }
                for (int j1 = 0; j1 < this.RightWing.length; ++j1) {
                    this.RightWing[j1].func_78785_a(scale);
                }
            }
        }
        GlStateManager.func_179121_F();
    }
}

