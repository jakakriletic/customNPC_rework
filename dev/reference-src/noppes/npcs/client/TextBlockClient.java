/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.util.text.Style
 *  net.minecraft.util.text.TextComponentString
 */
package noppes.npcs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.TextBlock;
import noppes.npcs.client.ClientProxy;

public class TextBlockClient
extends TextBlock {
    private Style style = new Style();
    public int color = 0xE0E0E0;
    private String name;
    private ICommandSender sender;

    public TextBlockClient(String name, String text, int lineWidth, int color, Object ... obs) {
        this(text, lineWidth, false, obs);
        this.color = color;
        this.name = name;
    }

    public TextBlockClient(ICommandSender sender, String text, int lineWidth, int color, Object ... obs) {
        this(text, lineWidth, false, obs);
        this.color = color;
        this.sender = sender;
    }

    public String getName() {
        if (this.sender != null) {
            return this.sender.getName();
        }
        return this.name;
    }

    public TextBlockClient(String text, int lineWidth, boolean mcFont, Object ... obs) {
        text = NoppesStringUtils.formatText(text, obs);
        String line = "";
        text = text.replace("\n", " \n ");
        text = text.replace("\r", " \r ");
        String[] words = text.split(" ");
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (String word : words) {
            char c;
            if (word.isEmpty()) continue;
            if (word.length() == 1 && ((c = word.charAt(0)) == '\r' || c == '\n')) {
                this.addLine(line);
                line = "";
                continue;
            }
            String newLine = line.isEmpty() ? word : line + " " + word;
            if ((mcFont ? font.getStringWidth(newLine) : ClientProxy.Font.width(newLine)) > lineWidth) {
                this.addLine(line);
                line = word.trim();
                continue;
            }
            line = newLine;
        }
        if (!line.isEmpty()) {
            this.addLine(line);
        }
    }

    private void addLine(String text) {
        TextComponentString line = new TextComponentString(text);
        line.setStyle(this.style);
        this.lines.add(line);
    }
}

