package noppes.npcs.rework.rig.client;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.rig.AnimClip;
import noppes.npcs.rework.rig.AnimLibrary;
import noppes.npcs.rework.rig.Pose;
import noppes.npcs.rework.rig.RigData;
import noppes.npcs.rework.rig.RigModels;
import noppes.npcs.rework.rig.RigNet;
import noppes.npcs.rework.rig.RigPlayer;

/**
 * M16 client: receives {@link RigNet} messages and draws rigged NPCs. Pose per frame:
 * idle loop, blended into the walk loop by walking speed (walk time = distance walked when the clip has a
 * {@code stride}), cross-faded into the action clip or the death clip, a hurt flinch on top, and the head
 * bone turned to where the NPC looks while no action plays. Bones named {@code prop_*} show only while
 * the action clip gives them a scale.
 */
@SideOnly(Side.CLIENT)
public final class RigClient {
    private static final float FADE_IN = 0.12F, FADE_OUT = 0.25F;
    private static final Map<String, GeoMesh> MESHES = new HashMap<String, GeoMesh>();
    private static final Map<String, ResourceLocation> FILE_TEXTURES = new HashMap<String, ResourceLocation>();
    private static int generation = -1;
    /** Frames drawn as a rig (diagnostics). */
    public static volatile int frames;

    private RigClient() {
    }

    public static void install() {
        Listener l = new Listener();
        if (RigNet.channel() != null) { RigNet.channel().register(l); }
        MinecraftForge.EVENT_BUS.register(l);
    }

    public static final class Listener {
        @SubscribeEvent
        public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
            if (!RigNet.CHANNEL.equals(event.getPacket().channel())) { return; }
            ByteBuf payload = event.getPacket().payload();
            if (payload.readableBytes() > 256) { return; }
            byte[] data = new byte[payload.readableBytes()];
            payload.getBytes(payload.readerIndex(), data);
            final Object[] m = RigNet.decode(data);
            if (m == null) { return; }
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                if ((Integer) m[0] == RigNet.RELOAD) { RigModels.reload(); return; }
                if (mc.world == null) { return; }
                Entity e = mc.world.getEntityByID((Integer) m[0]);
                if (e instanceof EntityNPCInterface) {
                    ((EntityNPCInterface) e).rwRigPlayer.accept((String) m[1], (Integer) m[2], (Integer) m[3], (Integer) m[4]);
                }
            });
        }
    }

    /** Whether this NPC is drawn as its rig (switch on, known model with a mesh). */
    public static boolean active(EntityNPCInterface npc) {
        return CustomNpcs.RwAnimCore == 1 && npc.rwRig.active() && mesh(npc.rwRig.model) != null;
    }

    static synchronized GeoMesh mesh(String id) {
        int gen = RigModels.generation();
        if (gen != generation) {
            MESHES.clear();
            FILE_TEXTURES.clear();
            generation = gen;
        }
        if (MESHES.containsKey(id)) { return MESHES.get(id); }
        GeoMesh mesh = null;
        RigModels.Model m = RigModels.get(id);
        if (m != null) {
            try {
                if (m.builtIn()) {
                    mesh = new GeoMesh(new ResourceLocation("customnpcs", "rig/" + id + ".geo.json"));
                } else {
                    try (InputStream in = new FileInputStream(new File(m.folder, id + ".geo.json"))) {
                        mesh = new GeoMesh(GeoMesh.parse(new InputStreamReader(in, StandardCharsets.UTF_8)));
                    }
                }
            } catch (Exception e) {
                LogWriter.error("RWRIG cannot load the model " + id, e);
            }
        }
        MESHES.put(id, mesh);
        return mesh;
    }

    static synchronized ResourceLocation texture(RigData rig) {
        if (!rig.texture.isEmpty()) { return new ResourceLocation(rig.texture); }
        RigModels.Model m = RigModels.get(rig.model);
        if (m == null || m.builtIn()) { return new ResourceLocation("customnpcs", "textures/rig/" + rig.model + ".png"); }
        ResourceLocation loc = FILE_TEXTURES.get(rig.model);
        if (loc == null) {
            loc = new ResourceLocation("customnpcs", "rigfile/" + rig.model);
            try {
                BufferedImage img = ImageIO.read(new File(m.folder, rig.model + ".png"));
                Minecraft.getMinecraft().getTextureManager().loadTexture(loc, new DynamicTexture(img));
            } catch (Exception e) {
                LogWriter.error("RWRIG cannot load the texture of " + rig.model, e);
            }
            FILE_TEXTURES.put(rig.model, loc);
        }
        return loc;
    }

    /**
     * Draws the NPC as its rig at x, y, z (relative to the camera), like RenderLivingBase: rotated to its
     * body yaw, scaled by its size, the death tip-over only when the rig has no death clip.
     */
    public static void render(Render<?> render, EntityNPCInterface npc, double x, double y, double z, float partial) {
        RigData rig = npc.rwRig;
        GeoMesh mesh = mesh(rig.model);
        if (mesh == null) { return; }
        AnimLibrary lib = rig.library();
        RigModels.Model def = rig.modelDef();
        float bodyYaw = interp(npc.prevRenderYawOffset, npc.renderYawOffset, partial);
        float headYaw = interp(npc.prevRotationYawHead, npc.rotationYawHead, partial) - bodyYaw;
        float pitch = npc.prevRotationPitch + (npc.rotationPitch - npc.prevRotationPitch) * partial;
        float swing = npc.limbSwing - npc.limbSwingAmount * (1 - partial);
        float swingAmount = npc.prevLimbSwingAmount + (npc.limbSwingAmount - npc.prevLimbSwingAmount) * partial;
        Pose pose = buildPose(npc, lib, def == null ? null : def.headBone, partial, swing, Math.min(1, swingAmount),
                MathHelper.wrapDegrees(headYaw), pitch);
        float scale = npc.display.getSize() / 5F * rig.scale / 16F;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(180 - bodyYaw, 0, 1, 0);
        if (npc.deathTime > 0 && lib.clipOrNull(rig.death) == null) {
            float f = MathHelper.sqrt(Math.min(1, (npc.deathTime + partial - 1) / 20F * 1.6F));
            GlStateManager.rotate(f * 90, 0, 0, 1);
        }
        // As RenderLivingBase + customnpcentities AnimatedModel: their two flips cancel to this uniform scale.
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableCull();
        boolean hurt = npc.hurtTime > 0 || npc.deathTime > 0 && npc.deathTime <= 20;
        if (hurt) { GlStateManager.color(1, 0.6F, 0.6F, 1); } else { GlStateManager.color(1, 1, 1, 1); }
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture(rig));
        mesh.render(pose);
        frames++;
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private static float interp(float a, float b, float t) {
        float d = MathHelper.wrapDegrees(b - a);
        return a + t * d;
    }

    /** The pose of one frame (also used by the preview tests). */
    public static Pose buildPose(EntityNPCInterface npc, AnimLibrary lib, String headBone, float partial, float limbSwing,
            float limbSwingAmount, float netHeadYaw, float headPitch) {
        RigData rig = npc.rwRig;
        RigPlayer player = npc.rwRigPlayer;
        float time = (npc.ticksExisted + partial) / 20F;
        Pose pose = new Pose();
        AnimClip idle = lib.clipOrNull(rig.idle);
        if (idle != null) { idle.sample(time, pose); }
        AnimClip walk = lib.clipOrNull(rig.walk);
        float walkWeight = MathHelper.clamp(limbSwingAmount * 2.2F, 0, 1);
        if (walk != null && walkWeight > 0) {
            float t = walk.stride > 0 ? limbSwing / 4F / walk.stride * walk.length : time;
            pose.blend(walk.sample(t, new Pose()), walkWeight);
        }
        float actionWeight = 0;
        Pose props = null;
        if (npc.deathTime > 0) {
            AnimClip death = lib.clipOrNull(rig.death);
            if (death != null) {
                float t = Math.min(death.length, (npc.deathTime - 1 + partial) / 20F);
                actionWeight = Math.min(1, (npc.deathTime + partial) / 2F);
                pose.blend(death.sample(t, new Pose()), actionWeight);
            }
        } else if (player.playing()) {
            AnimClip clip = lib.clipOrNull(player.clip());
            if (clip != null) {
                float t = Math.min(clip.length, (player.age() + partial) / 20F);
                float in = player.blend() > 0 ? player.blend() / 20F : FADE_IN;
                actionWeight = MathHelper.clamp(Math.min(1, t / in) * Math.min(1, (clip.length - t) / FADE_OUT), 0, 1);
                if (clip.loop) { actionWeight = Math.min(1, t / in); }
                Pose action = clip.sample(t, new Pose());
                pose.blend(action, actionWeight);
                props = action;
            }
        }
        AnimClip hurt = lib.clipOrNull(rig.hurt);
        if (hurt != null && npc.hurtTime > 0 && npc.deathTime <= 0) {
            float t = Math.min(hurt.length, (npc.maxHurtTime - npc.hurtTime + partial) / 20F);
            float w = (actionWeight > 0.5F ? 0.35F : 0.85F) * MathHelper.sin((float) Math.PI * t / hurt.length);
            if (w > 0) { pose.blend(hurt.sample(t, new Pose()), w); }
        }
        pose.bones.keySet().removeIf(Pose::isProp);
        if (props != null) {
            for (Map.Entry<String, float[]> e : props.bones.entrySet()) {
                if (Pose.isProp(e.getKey())) { pose.bones.put(e.getKey(), e.getValue()); }
            }
        }
        if (headBone != null) {
            float free = 1 - actionWeight;
            float[] head = pose.edit(headBone);
            head[Pose.ROT + 1] += MathHelper.clamp(netHeadYaw, -45, 45) * free;
            head[Pose.ROT] += MathHelper.clamp(headPitch, -30, 30) * free;
        }
        return pose;
    }
}
