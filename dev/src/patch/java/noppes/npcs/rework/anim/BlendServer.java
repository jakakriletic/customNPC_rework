package noppes.npcs.rework.anim;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

/** P7b (D-098, Q282): server side of {@link IAnimator#blendTo}. The channel exists only with
 * RwAnimHooks>=1 (registered in pre-init on both sides); with 0 nothing is registered or sent.
 * Receivers are the players currently tracking the NPC; a later joiner simply sees the clip. */
public final class BlendServer {
    private static FMLEventChannel channel;

    private BlendServer() { }

    /** Pre-init, only with RwAnimHooks>=1. */
    public static synchronized void init() {
        if (channel == null) { channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(BlendCodec.CHANNEL); }
    }

    static FMLEventChannel channel() { return channel; }

    static boolean enabled() { return channel != null; }

    /** Server thread. @return number of players the blend went to */
    static int send(Entity npc, int token, int ticks) {
        if (channel == null || !(npc.world instanceof WorldServer)) { return 0; }
        byte[] data = BlendCodec.encode(npc.dimension, npc.getEntityId(), npc.getUniqueID().getMostSignificantBits(),
                npc.getUniqueID().getLeastSignificantBits(), token, ticks);
        int sent = 0;
        for (EntityPlayer player : ((WorldServer) npc.world).getEntityTracker().getTrackingPlayers(npc)) {
            if (!(player instanceof EntityPlayerMP)) { continue; }
            channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)), BlendCodec.CHANNEL), (EntityPlayerMP) player);
            sent++;
        }
        return sent;
    }
}
