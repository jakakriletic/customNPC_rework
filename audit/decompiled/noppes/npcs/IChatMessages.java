/*
 * Decompiled with CFR 0.152.
 */
package noppes.npcs;

import noppes.npcs.entity.EntityNPCInterface;

public interface IChatMessages {
    public void addMessage(String var1, EntityNPCInterface var2);

    public void renderMessages(double var1, double var3, double var5, float var7, boolean var8);
}

