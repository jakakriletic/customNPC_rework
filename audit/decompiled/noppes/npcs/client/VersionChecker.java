/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.util.text.event.ClickEvent
 *  net.minecraft.util.text.event.ClickEvent$Action
 */
package noppes.npcs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;

public class VersionChecker
extends Thread {
    @Override
    public void run() {
        EntityPlayerSP player;
        String name = "\u00a72CustomNpcs\u00a7f";
        String link = "\u00a79\u00a7nClick here";
        String text = name + " installed. For more info " + link;
        try {
            player = Minecraft.func_71410_x().field_71439_g;
        }
        catch (NoSuchMethodError e) {
            return;
        }
        while ((player = Minecraft.func_71410_x().field_71439_g) == null) {
            try {
                Thread.sleep(2000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        TextComponentTranslation message = new TextComponentTranslation(text, new Object[0]);
        message.func_150256_b().func_150241_a(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://www.kodevelopment.nl/minecraft/customnpcs/"));
        player.func_145747_a((ITextComponent)message);
    }
}

