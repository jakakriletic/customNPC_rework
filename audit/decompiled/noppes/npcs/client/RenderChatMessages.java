/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.GlStateManager$DestFactor
 *  net.minecraft.client.renderer.GlStateManager$SourceFactor
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.util.text.ITextComponent
 *  org.lwjgl.opengl.GL11
 */
package noppes.npcs.client;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.IChatMessages;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.opengl.GL11;

public class RenderChatMessages
implements IChatMessages {
    private Map<Long, TextBlockClient> messages = new TreeMap<Long, TextBlockClient>();
    private int boxLength = 46;
    private float scale = 0.5f;
    private String lastMessage = "";
    private long lastMessageTime = 0L;

    @Override
    public void addMessage(String message, EntityNPCInterface npc) {
        if (!CustomNpcs.EnableChatBubbles) {
            return;
        }
        long time = System.currentTimeMillis();
        if (message.equals(this.lastMessage) && this.lastMessageTime + 5000L > time) {
            return;
        }
        TreeMap<Long, TextBlockClient> messages = new TreeMap<Long, TextBlockClient>(this.messages);
        messages.put(time, new TextBlockClient(message, this.boxLength * 4, true, new Object[]{Minecraft.func_71410_x().field_71439_g, npc}));
        if (messages.size() > 3) {
            messages.remove(messages.keySet().iterator().next());
        }
        this.messages = messages;
        this.lastMessage = message;
        this.lastMessageTime = time;
    }

    @Override
    public void renderMessages(double par3, double par5, double par7, float textscale, boolean inRange) {
        Map<Long, TextBlockClient> messages = this.getMessages();
        if (messages.isEmpty()) {
            return;
        }
        if (inRange) {
            this.render(par3, par5, par7, textscale, false);
        }
        this.render(par3, par5, par7, textscale, true);
    }

    private void render(double par3, double par5, double par7, float textscale, boolean depth) {
        FontRenderer font = Minecraft.func_71410_x().field_71466_p;
        float var13 = 1.6f;
        float var14 = 0.016666668f * var13;
        GlStateManager.func_179094_E();
        int size = 0;
        for (TextBlockClient block : this.messages.values()) {
            size += block.lines.size();
        }
        Minecraft mc = Minecraft.func_71410_x();
        int textYSize = (int)((float)(size * font.field_78288_b) * this.scale);
        GlStateManager.func_179109_b((float)((float)par3 + 0.0f), (float)((float)par5 + (float)textYSize * textscale * var14), (float)((float)par7));
        GlStateManager.func_179152_a((float)textscale, (float)textscale, (float)textscale);
        GL11.glNormal3f((float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)(-mc.func_175598_ae().field_78735_i), (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)mc.func_175598_ae().field_78732_j, (float)1.0f, (float)0.0f, (float)0.0f);
        GlStateManager.func_179152_a((float)(-var14), (float)(-var14), (float)var14);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179132_a((boolean)true);
        GlStateManager.func_179140_f();
        GlStateManager.func_179147_l();
        if (depth) {
            GlStateManager.func_179126_j();
        } else {
            GlStateManager.func_179097_i();
        }
        int black = depth ? -16777216 : 0x55000000;
        int white = depth ? -1140850689 : 0x44FFFFFF;
        GlStateManager.func_187428_a((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
        GlStateManager.func_179090_x();
        GlStateManager.func_179089_o();
        this.drawRect(-this.boxLength - 2, -2, this.boxLength + 2, textYSize + 1, white, 0.11);
        this.drawRect(-this.boxLength - 1, -3, this.boxLength + 1, -2, black, 0.1);
        this.drawRect(-this.boxLength - 1, textYSize + 2, -1, textYSize + 1, black, 0.1);
        this.drawRect(3, textYSize + 2, this.boxLength + 1, textYSize + 1, black, 0.1);
        this.drawRect(-this.boxLength - 3, -1, -this.boxLength - 2, textYSize, black, 0.1);
        this.drawRect(this.boxLength + 3, -1, this.boxLength + 2, textYSize, black, 0.1);
        this.drawRect(-this.boxLength - 2, -2, -this.boxLength - 1, -1, black, 0.1);
        this.drawRect(this.boxLength + 2, -2, this.boxLength + 1, -1, black, 0.1);
        this.drawRect(-this.boxLength - 2, textYSize + 1, -this.boxLength - 1, textYSize, black, 0.1);
        this.drawRect(this.boxLength + 2, textYSize + 1, this.boxLength + 1, textYSize, black, 0.1);
        this.drawRect(0, textYSize + 1, 3, textYSize + 4, white, 0.11);
        this.drawRect(-1, textYSize + 4, 1, textYSize + 5, white, 0.11);
        this.drawRect(-1, textYSize + 1, 0, textYSize + 4, black, 0.1);
        this.drawRect(3, textYSize + 1, 4, textYSize + 3, black, 0.1);
        this.drawRect(2, textYSize + 3, 3, textYSize + 4, black, 0.1);
        this.drawRect(1, textYSize + 4, 2, textYSize + 5, black, 0.1);
        this.drawRect(-2, textYSize + 4, -1, textYSize + 5, black, 0.1);
        this.drawRect(-2, textYSize + 5, 1, textYSize + 6, black, 0.1);
        GlStateManager.func_179098_w();
        GlStateManager.func_179132_a((boolean)true);
        GlStateManager.func_179152_a((float)this.scale, (float)this.scale, (float)this.scale);
        int index = 0;
        for (TextBlockClient block : this.messages.values()) {
            for (ITextComponent chat : block.lines) {
                String message = chat.func_150254_d();
                font.func_78276_b(message, -font.func_78256_a(message) / 2, index * font.field_78288_b, black);
                ++index;
            }
        }
        GlStateManager.func_179129_p();
        GlStateManager.func_179145_e();
        GlStateManager.func_179084_k();
        GlStateManager.func_179126_j();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179121_F();
    }

    private void drawRect(int par0, int par1, int par2, int par3, int par4, double par5) {
        int j1;
        if (par0 < par2) {
            j1 = par0;
            par0 = par2;
            par2 = j1;
        }
        if (par1 < par3) {
            j1 = par1;
            par1 = par3;
            par3 = j1;
        }
        float f = (float)(par4 >> 24 & 0xFF) / 255.0f;
        float f1 = (float)(par4 >> 16 & 0xFF) / 255.0f;
        float f2 = (float)(par4 >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(par4 & 0xFF) / 255.0f;
        BufferBuilder tessellator = Tessellator.func_178181_a().func_178180_c();
        GlStateManager.func_179131_c((float)f1, (float)f2, (float)f3, (float)f);
        tessellator.func_181668_a(7, DefaultVertexFormats.field_181705_e);
        tessellator.func_181662_b((double)par0, (double)par3, par5).func_181675_d();
        tessellator.func_181662_b((double)par2, (double)par3, par5).func_181675_d();
        tessellator.func_181662_b((double)par2, (double)par1, par5).func_181675_d();
        tessellator.func_181662_b((double)par0, (double)par1, par5).func_181675_d();
        Tessellator.func_178181_a().func_78381_a();
    }

    private Map<Long, TextBlockClient> getMessages() {
        TreeMap<Long, TextBlockClient> messages = new TreeMap<Long, TextBlockClient>();
        long time = System.currentTimeMillis();
        for (Map.Entry<Long, TextBlockClient> entry : this.messages.entrySet()) {
            if (time > entry.getKey() + 10000L) continue;
            messages.put(entry.getKey(), entry.getValue());
        }
        this.messages = messages;
        return this.messages;
    }
}

