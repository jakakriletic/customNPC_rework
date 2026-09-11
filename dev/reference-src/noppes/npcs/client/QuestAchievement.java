/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.stats.StatBasic
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 */
package noppes.npcs.client;

import net.minecraft.stats.StatBasic;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class QuestAchievement
extends StatBasic {
    public String description;
    public String message;

    public QuestAchievement(String message, String description) {
        super("", (ITextComponent)new TextComponentTranslation(message, new Object[0]));
        this.description = description;
        this.message = message;
    }
}

