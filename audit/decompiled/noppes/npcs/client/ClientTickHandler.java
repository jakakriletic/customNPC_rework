/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.ContainerPlayer
 *  net.minecraft.util.EnumHand
 *  net.minecraft.world.World
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickEmpty
 *  net.minecraftforge.fml.common.eventhandler.EventPriority
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.InputEvent$KeyInputEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$ClientTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$Phase
 *  org.lwjgl.input.Keyboard
 */
package noppes.npcs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.Client;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.player.GuiQuestLog;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.constants.EnumPlayerPacket;
import org.lwjgl.input.Keyboard;

public class ClientTickHandler {
    private World prevWorld;
    private boolean otherContainer = false;
    private int buttonPressed = -1;
    private long buttonTime = 0L;
    private final int[] ignoreKeys = new int[]{157, 29, 54, 42, 184, 56, 220, 219};

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.field_71439_g != null && mc.field_71439_g.field_71070_bA instanceof ContainerPlayer) {
            if (this.otherContainer) {
                NoppesUtilPlayer.sendData(EnumPlayerPacket.CheckQuestCompletion, new Object[0]);
                this.otherContainer = false;
            }
        } else {
            this.otherContainer = true;
        }
        ++CustomNpcs.ticks;
        ++RenderNPCInterface.LastTextureTick;
        if (this.prevWorld != mc.field_71441_e) {
            this.prevWorld = mc.field_71441_e;
            MusicController.Instance.stopMusic();
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (CustomNpcs.SceneButtonsEnabled) {
            if (ClientProxy.Scene1.func_151468_f()) {
                Client.sendData(EnumPacketServer.SceneStart, 1);
            }
            if (ClientProxy.Scene2.func_151468_f()) {
                Client.sendData(EnumPacketServer.SceneStart, 2);
            }
            if (ClientProxy.Scene3.func_151468_f()) {
                Client.sendData(EnumPacketServer.SceneStart, 3);
            }
            if (ClientProxy.SceneReset.func_151468_f()) {
                Client.sendData(EnumPacketServer.SceneReset, new Object[0]);
            }
        }
        Minecraft mc = Minecraft.func_71410_x();
        if (ClientProxy.QuestLog.func_151468_f()) {
            if (mc.field_71462_r == null) {
                NoppesUtil.openGUI((EntityPlayer)mc.field_71439_g, new GuiQuestLog((EntityPlayer)mc.field_71439_g));
            } else if (mc.field_71462_r instanceof GuiQuestLog) {
                mc.func_71381_h();
            }
        }
        int key = Keyboard.getEventKey();
        long time = Keyboard.getEventNanoseconds();
        if (Keyboard.getEventKeyState()) {
            if (!this.isIgnoredKey(key)) {
                this.buttonTime = time;
                this.buttonPressed = key;
            }
        } else {
            if (key == this.buttonPressed && time - this.buttonTime < 500000000L && mc.field_71462_r == null) {
                boolean isCtrlPressed = Keyboard.isKeyDown((int)157) || Keyboard.isKeyDown((int)29);
                boolean isShiftPressed = Keyboard.isKeyDown((int)54) || Keyboard.isKeyDown((int)42);
                boolean isAltPressed = Keyboard.isKeyDown((int)184) || Keyboard.isKeyDown((int)56);
                boolean isMetaPressed = Keyboard.isKeyDown((int)220) || Keyboard.isKeyDown((int)219);
                NoppesUtilPlayer.sendData(EnumPlayerPacket.KeyPressed, key, isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed);
            }
            this.buttonPressed = -1;
            this.buttonTime = 0L;
        }
    }

    @SubscribeEvent
    public void invoke(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        NoppesUtilPlayer.sendData(EnumPlayerPacket.LeftClick, new Object[0]);
    }

    private boolean isIgnoredKey(int key) {
        for (int i : this.ignoreKeys) {
            if (i != key) continue;
            return true;
        }
        return false;
    }
}

