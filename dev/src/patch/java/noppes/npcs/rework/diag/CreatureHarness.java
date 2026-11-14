package noppes.npcs.rework.diag;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.rig.RigModels;
import noppes.npcs.rework.rig.client.GuiRigModel;
import noppes.npcs.rework.rig.client.RigClient;

/** Dedicated M15 proof, running in a client directory without the server's files. */
public final class CreatureHarness {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Properties job;
    private final long deadline = System.nanoTime() + 480_000_000_000L;
    private int stage, frames, entered;

    private CreatureHarness(Properties job) { this.job = job; }

    static boolean installIfRequested(String path) throws Exception {
        Properties job = new Properties();
        try (Reader in = Files.newBufferedReader(new File(path).toPath(), StandardCharsets.UTF_8)) { job.load(in); }
        if (!"1".equals(job.getProperty("creatures"))) { return false; }
        MinecraftForge.EVENT_BUS.register(new CreatureHarness(job));
        return true;
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (stage < 0 || event.phase != TickEvent.Phase.END) { return; }
        try {
            if (System.nanoTime() >= deadline) { throw new IllegalStateException("timed out at stage " + stage); }
            if (stage == 0) {
                if (!(mc.currentScreen instanceof GuiMainMenu)) { return; }
                if (new File(mc.mcDataDir, "customnpcs/creatures").exists()) { throw new IllegalStateException("client has local packs"); }
                mc.gameSettings.pauseOnLostFocus = false;
                mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
                stage++;
                return;
            }
            if (mc.world == null || mc.player == null) { return; }
            String mode = job.getProperty("mode");
            EntityNPCInterface troll = npc("Epic Troll");
            EntityNPCInterface beetle = npc("Copper Beetle");
            if (stage == 1) {
                if (troll == null || beetle == null || !"Folder script ran".equals(troll.display.getTitle()) || RigModels.get("epic_troll", true) == null
                        || RigModels.get("copper_beetle", true) == null) { return; }
                if (RigModels.get("copper_beetle", true).geometry == null || RigModels.get("copper_beetle", true).png == null
                        || !troll.rwRig.library(true).has("m15_roar") || !RigClient.active(beetle) || beetle.getMaxHealth() != 37) {
                    throw new IllegalStateException("missing downloaded model, texture or attack");
                }
                frames = RigClient.frames;
                mc.player.rotationYaw = 0;
                mc.player.rotationPitch = 8;
                System.out.println("M15 CLIENT READY " + mode);
                stage++;
            }
            if (stage == 2 && ("first".equals(mode) || "late".equals(mode))) {
                if (troll == null || !"Folder event ran".equals(troll.display.getTitle()) || !troll.rwRig.library(true).has("m15_updated") || RigClient.frames <= frames
                        || !"m15_updated".equals(troll.rwRigPlayer.clip())) { return; }
                System.out.println("M15 CLIENT RELOAD " + mode);
                stage++;
            }
            if (stage == 2 && "restart".equals(mode) && RigClient.frames > frames) {
                if (!troll.rwRig.library(true).has("m15_updated")) { throw new IllegalStateException("restart missed the updated pack"); }
                finish(true);
            }
            if (stage == 3 && beetle != null && mc.player.getHeldItemMainhand().getItem() == noppes.npcs.CustomItems.wand) {
                mc.playerController.interactWithEntity(mc.player, beetle, EnumHand.MAIN_HAND);
                stage++;
            }
            if (stage == 3 && "late".equals(mode)) {
                EntityNPCInterface fresh = npc("Fresh Troll");
                if (fresh != null && RigClient.active(fresh) && "fresh_troll".equals(fresh.rwRig.model)) {
                    System.out.println("M15 CLIENT DEPLOY late");
                    finish(true);
                }
            }
            if (stage == 4 && mc.currentScreen instanceof GuiNpcDisplay) {
                GuiNpcDisplay display = (GuiNpcDisplay)mc.currentScreen;
                click(display, display.getButton(15));
                GuiRigModel picker = (GuiRigModel)display.getSubGui();
                GuiButton prev = picker.getButton(0);
                while (prev.enabled) { click(display, prev); prev = picker.getButton(0); }
                click(display, picker.getButton(10));
                click(display, picker.getButton(3));
                display.save();
                entered = mc.player.ticksExisted;
                stage++;
            }
            if (stage == 5 && beetle != null && !beetle.rwRig.active() && mc.player.ticksExisted > entered + 10) {
                System.out.println("M15 CLIENT GUI first");
                finish(true);
            }
        } catch (Exception | LinkageError failure) {
            failure.printStackTrace();
            finish(false);
        }
    }

    private EntityNPCInterface npc(String name) {
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityNPCInterface && entity.getName().equals(name)) { return (EntityNPCInterface)entity; }
        }
        return null;
    }

    private void click(GuiNPCInterface screen, GuiButton button) {
        if (button == null || !button.visible || !button.enabled) { throw new IllegalStateException("missing GUI button"); }
        screen.drawScreen(button.x + 2, button.y + 2, 0);
        screen.mouseClicked(button.x + 2, button.y + 2, 0);
        screen.mouseReleased(button.x + 2, button.y + 2, 0);
    }

    private void finish(boolean success) {
        stage = -1;
        System.out.println("M15 CLIENT DONE " + job.getProperty("mode") + " " + success);
        if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); }
        mc.shutdown();
    }
}
