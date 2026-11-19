package noppes.npcs.rework.diag;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.rig.Pose;
import noppes.npcs.rework.rig.RigModels;
import noppes.npcs.rework.rig.client.RigClient;
import org.lwjgl.opengl.GL11;

/** Explicit controller job only. Clients have no local workspace, source code or model files. */
public final class CloneWorkspaceHarness {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Properties job;
    private final long deadline = System.nanoTime() + 600_000_000_000L;
    private int stage, since = -1, frames, generation, checks;
    private boolean connected;

    private CloneWorkspaceHarness(Properties job) { this.job = job; }
    static boolean installIfRequested(String path) throws Exception {
        Properties job = new Properties();
        try (Reader in = Files.newBufferedReader(new File(path).toPath(), StandardCharsets.UTF_8)) { job.load(in); }
        if (!"1".equals(job.getProperty("cloneWorkspace"))) { return false; }
        MinecraftForge.EVENT_BUS.register(new CloneWorkspaceHarness(job));
        return true;
    }
    private void check(boolean ok, String key) {
        if (!ok) { throw new IllegalStateException(key); }
        checks++; System.out.println("WSLIVE CLIENT CHECK mode=" + job.getProperty("mode") + " key=" + key + " ok=1");
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (stage < 0 || event.phase != TickEvent.Phase.END) { return; }
        try {
            if (System.nanoTime() > deadline) { throw new IllegalStateException("timeout stage " + stage); }
            if (stage == 0) {
                if (!(mc.currentScreen instanceof GuiMainMenu)) { return; }
                check(!new File(mc.mcDataDir, "customnpcs/creatures").exists() && !new File(mc.mcDataDir, "customnpcs/rigs").exists(), "no-local-assets-or-workspaces");
                mc.gameSettings.pauseOnLostFocus = false;
                mc.gameSettings.renderDistanceChunks = 8;
                mc.gameSettings.thirdPersonView = 0;
                mc.gameSettings.fovSetting = 70;
                mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
                stage = 1; return;
            }
            if (mc.world == null || mc.player == null) { return; }
            if (!connected) { connected = true; System.out.println("WSLIVE CLIENT CONNECTED " + job.getProperty("mode")); }
            if (Math.abs(mc.player.posX + 5) > 0.2 || Math.abs(mc.player.posZ + 8) > 0.2) { return; }
            EntityNPCInterface old = npc("WS_Rig_v1", -3), plain = npc("WS_Plain_v1", 0), disabled = npc("WS_Disabled", 3), ordinary = npc("WS_Ordinary", 6);
            boolean restart = job.getProperty("mode").startsWith("restart");
            EntityNPCInterface fresh = npc("WS_Rig_v2", restart ? -15 : -6);
            if (old == null || plain == null || disabled == null || ordinary == null || RigModels.get("z_model", true) == null) { return; }
            if (stage == 2 || restart) {
                if (fresh == null || npc("WS_Plain_v2", restart ? -18 : -9) == null || !"workspace_pose_v2".equals(fresh.rwRigPlayer.clip())) { return; }
            }
            EntityNPCInterface target = stage == 2 || restart ? fresh : old;
            double dx = target.posX - mc.player.posX, dz = target.posZ - mc.player.posZ;
            mc.player.rotationYaw = (float)-Math.toDegrees(Math.atan2(dx, dz));
            mc.player.rotationPitch = (float)Math.toDegrees(Math.atan2(mc.player.posY + mc.player.getEyeHeight() - target.posY - 1, Math.sqrt(dx * dx + dz * dz)));
            if (since < 0) { since = mc.player.ticksExisted; frames = RigClient.frames; return; }
            if (mc.player.ticksExisted - since < 40 || RigClient.frames <= frames) { return; }
            check(RigClient.active(old) && RigClient.active(disabled) && !RigClient.active(plain) && RigClient.active(ordinary), "plain-and-active-models-" + stage);
            check("z_model".equals(old.rwRig.model) && "workspace_pose_v1".equals(old.rwRig.idle) && old.rwRig.library(true).has("workspace_pose_v1") && !old.rwRig.library(true).has("workspace_pose_v2"), "old-rig-remains-v1-" + stage);
            check(RigModels.get("a_rig", true) == null && RigModels.get("b_plain", true) == null && RigModels.get("c_disabled", true) == null, "workspace-ids-not-models-" + stage);
            check(old.script.getScripts().isEmpty() && plain.script.getScripts().isEmpty() && disabled.script.getScripts().isEmpty() && ordinary.script.getScripts().isEmpty(), "scripts-stay-server-side-" + stage);
            check(ordinary.rwRig.library(true).has("m15_pose") && !ordinary.rwRig.library(true).has("should_not_publish"), "ordinary-m15-assets-" + stage);
            if (stage == 2 || restart) {
                check(RigClient.active(fresh) && "z_model".equals(fresh.rwRig.model) && "workspace_pose_v2".equals(fresh.rwRig.idle) && fresh.getMaxHealth() == 41, "fresh-model-binding-health");
                check(fresh.rwRig.library(true).has("workspace_pose_v2") && !fresh.rwRig.library(true).has("workspace_pose_v1") && "workspace_pose_v2".equals(fresh.rwRigPlayer.clip()), "fresh-own-clip-playing");
                check(Math.abs(fresh.rwRig.library(true).clipOrNull("workspace_pose_v2").sample(0.5F, new Pose()).get("armL")[Pose.ROT] + 70) < 0.001F, "fresh-own-pose");
                check(fresh.script.getScripts().isEmpty(), "fresh-scripts-stay-server-side");
                if (!restart) { check(RigModels.generation() > generation, "live-reload-committed-on-client"); }
            }
            check(GL11.glGetError() == 0, "no-gl-error-" + stage);
            File png = new File(job.getProperty("shots") + "-" + job.getProperty("mode") + "-" + stage + ".png");
            check(ImageIO.write(ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer()), "png", png), "screenshot-" + stage);
            if (stage == 1 && !restart) {
                generation = RigModels.generation(); stage = 2; since = -1;
                System.out.println("WSLIVE CLIENT READY " + job.getProperty("mode"));
            } else { finish(true); }
        } catch (Exception | LinkageError error) { error.printStackTrace(); finish(false); }
    }
    private EntityNPCInterface npc(String name, int x) {
        for (Entity e : mc.world.loadedEntityList) {
            if (e instanceof EntityNPCInterface && name.equals(e.getName()) && Math.abs(e.posX - x) < 0.1) { return (EntityNPCInterface)e; }
        }
        return null;
    }
    private void finish(boolean ok) {
        stage = -1;
        System.out.println("WSLIVE CLIENT DONE mode=" + job.getProperty("mode") + " checks=" + checks + " ok=" + ok);
        if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); }
        mc.shutdown();
    }
}
