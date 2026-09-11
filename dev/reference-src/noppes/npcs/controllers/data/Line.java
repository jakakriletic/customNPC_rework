/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 */
package noppes.npcs.controllers.data;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class Line {
    public String text = "";
    public String sound = "";
    public boolean hideText = false;

    public Line() {
    }

    public Line(String text) {
        this.text = text;
    }

    public Line copy() {
        Line line = new Line(this.text);
        line.sound = this.sound;
        line.hideText = this.hideText;
        return line;
    }

    public Line formatTarget(EntityLivingBase entity) {
        if (entity == null) {
            return this;
        }
        Line line = this.copy();
        line.text = entity instanceof EntityPlayer ? line.text.replace("@target", ((EntityPlayer)entity).getDisplayNameString()) : line.text.replace("@target", entity.getName());
        return line;
    }
}

