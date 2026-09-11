/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.audio.ISound
 *  net.minecraft.client.audio.PositionedSoundRecord
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.multiplayer.WorldClient
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.util.EnumParticleTypes
 *  net.minecraft.util.SoundEvent
 *  net.minecraft.util.Util
 *  net.minecraft.util.Util$EnumOS
 *  org.lwjgl.Sys
 */
package noppes.npcs.client;

import io.netty.buffer.ByteBuf;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import noppes.npcs.CustomNpcs;
import noppes.npcs.Server;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.player.GuiDialogInteract;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.Sys;

public class NoppesUtil {
    private static EntityNPCInterface lastNpc;
    private static HashMap<String, Integer> data;

    public static void requestOpenGUI(EnumGuiType gui) {
        NoppesUtil.requestOpenGUI(gui, 0, 0, 0);
    }

    public static void requestOpenGUI(EnumGuiType gui, int i, int j, int k) {
        Client.sendData(EnumPacketServer.Gui, gui.ordinal(), i, j, k);
    }

    public static void spawnParticle(ByteBuf buffer) throws IOException {
        double posX = buffer.readDouble();
        double posY = buffer.readDouble();
        double posZ = buffer.readDouble();
        float height = buffer.readFloat();
        float width = buffer.readFloat();
        String particle = Server.readString(buffer);
        WorldClient world = Minecraft.getMinecraft().world;
        Random rand = world.rand;
        if (particle.equals("heal")) {
            for (int k = 0; k < 6; ++k) {
                world.spawnParticle(EnumParticleTypes.SPELL_INSTANT, posX + (rand.nextDouble() - 0.5) * (double)width, posY + rand.nextDouble() * (double)height, posZ + (rand.nextDouble() - 0.5) * (double)width, 0.0, 0.0, 0.0, new int[0]);
                world.spawnParticle(EnumParticleTypes.SPELL, posX + (rand.nextDouble() - 0.5) * (double)width, posY + rand.nextDouble() * (double)height, posZ + (rand.nextDouble() - 0.5) * (double)width, 0.0, 0.0, 0.0, new int[0]);
            }
        }
    }

    public static EntityNPCInterface getLastNpc() {
        return lastNpc;
    }

    public static void setLastNpc(EntityNPCInterface npc) {
        lastNpc = npc;
    }

    public static void openGUI(EntityPlayer player, Object guiscreen) {
        CustomNpcs.proxy.openGui(player, guiscreen);
    }

    public static void openFolder(File dir) {
        String s = dir.getAbsolutePath();
        if (Util.getOSType() == Util.EnumOS.OSX) {
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/open", s});
                return;
            }
            catch (IOException iOException) {}
        } else if (Util.getOSType() == Util.EnumOS.WINDOWS) {
            String s1 = String.format("cmd.exe /C start \"Open file\" \"%s\"", s);
            try {
                Runtime.getRuntime().exec(s1);
                return;
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        boolean flag = false;
        try {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
            oclass.getMethod("browse", URI.class).invoke(object, dir.toURI());
        }
        catch (Throwable throwable) {
            flag = true;
        }
        if (flag) {
            Sys.openURL((String)("file://" + s));
        }
    }

    public static void setScrollList(ByteBuf buffer) {
        GuiScreen gui = Minecraft.getMinecraft().currentScreen;
        if (gui instanceof GuiNPCInterface && ((GuiNPCInterface)gui).hasSubGui()) {
            gui = ((GuiNPCInterface)gui).getSubGui();
        }
        if (gui == null || !(gui instanceof IScrollData)) {
            return;
        }
        Vector<String> data = new Vector<String>();
        try {
            int size = buffer.readInt();
            for (int i = 0; i < size; ++i) {
                data.add(Server.readString(buffer));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        ((IScrollData)gui).setData(data, null);
    }

    public static void addScrollData(ByteBuf buffer) {
        try {
            int size = buffer.readInt();
            for (int i = 0; i < size; ++i) {
                int id = buffer.readInt();
                String name = Server.readString(buffer);
                data.put(name, id);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void setScrollData(ByteBuf buffer) {
        GuiScreen gui = Minecraft.getMinecraft().currentScreen;
        if (gui == null) {
            return;
        }
        try {
            int size = buffer.readInt();
            for (int i = 0; i < size; ++i) {
                int id = buffer.readInt();
                String name = Server.readString(buffer);
                data.put(name, id);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (gui instanceof GuiNPCInterface && ((GuiNPCInterface)gui).hasSubGui()) {
            gui = ((GuiNPCInterface)gui).getSubGui();
        }
        if (gui instanceof GuiContainerNPCInterface && ((GuiContainerNPCInterface)gui).hasSubGui()) {
            gui = ((GuiContainerNPCInterface)gui).getSubGui();
        }
        if (gui instanceof IScrollData) {
            ((IScrollData)gui).setData(new Vector<String>(data.keySet()), data);
        }
        data = new HashMap();
    }

    public static void openDialog(Dialog dialog, EntityNPCInterface npc, EntityPlayer player) {
        GuiScreen gui = Minecraft.getMinecraft().currentScreen;
        if (gui == null || !(gui instanceof GuiDialogInteract)) {
            CustomNpcs.proxy.openGui(player, new GuiDialogInteract(npc, dialog));
        } else {
            GuiDialogInteract dia = (GuiDialogInteract)gui;
            dia.appendDialog(dialog);
        }
    }

    public static void clickSound() {
        Minecraft.getMinecraft().getSoundHandler().playSound((ISound)PositionedSoundRecord.getMasterRecord((SoundEvent)SoundEvents.UI_BUTTON_CLICK, (float)1.0f));
    }

    static {
        data = new HashMap();
    }
}

