package noppes.npcs.rework.anim;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** P7b: receiver of the {@value BlendCodec#CHANNEL} channel. Installed by CustomNpcs through
 * reflection on the client only, with RwAnimHooks>=1. Checks the size before copying, decodes on the
 * network thread and hands the message to the client main thread. */
@SideOnly(Side.CLIENT)
public final class BlendClient {
    private BlendClient() { }

    public static void install() {
        BlendClient c = new BlendClient();
        BlendServer.channel().register(c);
        MinecraftForge.EVENT_BUS.register(c);
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        ByteBuf payload = event.getPacket().payload();
        BlendCodec.Message m = null;
        if (payload.readableBytes() == BlendCodec.SIZE) {
            byte[] data = new byte[BlendCodec.SIZE];
            payload.getBytes(payload.readerIndex(), data);
            m = BlendCodec.decode(data);
        }
        if (m == null) {
            synchronized (AnimBlend.class) { AnimBlend.rejected++; }
            return;
        }
        final BlendCodec.Message message = m;
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> AnimBlend.accept(mc.world, message));
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) { AnimBlend.clear(); }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(AnimBlend::clear);
    }
}
