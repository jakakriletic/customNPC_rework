package noppes.npcs.rework.rig;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M16: channel {@value #CHANNEL}, server to client only: "this NPC started clip X (age, fade)", or ended
 * its clip (empty name). Registered with RwAnimCore=1 on both sides; the rig itself travels in the NPC's
 * spawn data, a late joiner gets the running clip there too.
 */
public final class RigNet {
    public static final String CHANNEL = "rwrig";
    /** Longest clip name sent. */
    public static final int MAX_NAME = 64;
    private static FMLEventChannel channel;

    private RigNet() {
    }

    public static synchronized void init() {
        if (channel == null) {
            channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
            RigCreatureTransfer.install();
        }
    }

    public static FMLEventChannel channel() { return channel; }

    public static byte[] encode(int entityId, String clip, int age, int blend, int seq) {
        PacketBuffer b = new PacketBuffer(Unpooled.buffer());
        b.writeInt(entityId);
        b.writeString(clip.length() > MAX_NAME ? clip.substring(0, MAX_NAME) : clip);
        b.writeVarInt(Math.max(0, age));
        b.writeVarInt(Math.max(0, Math.min(200, blend)));
        b.writeVarInt(Math.max(0, seq));
        byte[] out = new byte[b.readableBytes()];
        b.readBytes(out);
        return out;
    }

    /** Decoded message, or null for a malformed one. */
    public static Object[] decode(byte[] data) {
        try {
            PacketBuffer b = new PacketBuffer(Unpooled.wrappedBuffer(data));
            int id = b.readInt();
            String clip = b.readString(MAX_NAME);
            int age = b.readVarInt(), blend = b.readVarInt(), seq = b.readVarInt();
            if (b.readableBytes() != 0 || age < 0 || blend < 0 || blend > 200 || seq < 0) { return null; }
            return new Object[] {id, clip, age, blend, seq};
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Entity id of the "reload the model files" message. */
    public static final int RELOAD = -1;
    /** Camera-only impulse. Old clients ignore this reserved entity id. */
    public static final int CAMERA_SHAKE = -2;

    static void sendShake(EntityNPCInterface npc, AnimEvent event) {
        if (channel == null || !(npc.world instanceof WorldServer)) { return; }
        float radius = event.getFloat("radius", 12);
        float strength = event.getFloat("strength", 2);
        float seconds = event.getFloat("duration", 0.6F);
        if (!(radius > 0 && strength > 0 && seconds > 0)) { return; }
        radius = Math.min(48, radius);
        strength = Math.min(4, strength);
        int ticks = Math.max(1, Math.round(Math.min(2, seconds) * 20));
        double scale = Math.max(0.05, npc.rwRig.scale * npc.display.getSize() / 5.0);
        double[] center = RigEvents.local(npc,
                event.getFloat("forward", 0) * scale, event.getFloat("side", 0) * scale);
        for (EntityPlayer player : npc.world.playerEntities) {
            if (!(player instanceof EntityPlayerMP)) { continue; }
            double distance = Math.sqrt(player.getDistanceSq(center[0], npc.posY, center[1]));
            if (!(distance < radius)) { continue; }
            int amplitude = shakeAmplitude(strength, radius, distance);
            if (amplitude == 0) { continue; }
            byte[] data = encode(CAMERA_SHAKE, Integer.toString(npc.dimension), amplitude, ticks, 0);
            channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)), CHANNEL), (EntityPlayerMP) player);
        }
    }

    static int shakeAmplitude(float strength, float radius, double distance) {
        return distance < radius ? Math.round(strength * 1000 * (1 - (float) (distance / radius))) : 0;
    }

    /** Every player reads the model files again (after /rwrig reload). */
    static void sendReload(net.minecraft.server.MinecraftServer server) {
        if (channel == null) { return; }
        byte[] data = encode(RELOAD, "", 0, 0, 0);
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)), CHANNEL), p);
        }
    }

    static void sendStart(Entity npc, String clip, int age, int blend, int seq) {
        if (channel == null || !(npc.world instanceof WorldServer)) { return; }
        byte[] data = encode(npc.getEntityId(), clip, age, blend, seq);
        for (EntityPlayer p : ((WorldServer) npc.world).getEntityTracker().getTrackingPlayers(npc)) {
            if (p instanceof EntityPlayerMP) {
                channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)), CHANNEL), (EntityPlayerMP) p);
            }
        }
    }
}
