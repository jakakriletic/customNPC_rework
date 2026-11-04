package noppes.npcs.rework.linked;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.gui.global.GuiNPCManageLinkedNpc;

/**
 * M13.2b: client half of {@link LinkedNet}, installed by CustomNpcs through reflection with RwLinkedGroups=1. A server
 * without the channel never answers, so the panel of the manager never appears (Q397).
 */
@SideOnly(Side.CLIENT)
public final class LinkedClient {
    private static int nextRequest, replies;
    private static LinkedGuiCodec.Response latest;

    private LinkedClient() {
    }

    public static void install() {
        LinkedNet.channel().register(new LinkedClient());
    }

    /** @return the id of the request */
    public static int send(LinkedGuiCodec.Op op, int mask, String group) {
        int id = nextRequest++ & Integer.MAX_VALUE;
        send(new LinkedGuiCodec.Request(id, op, mask, group).encode());
        return id;
    }

    /** Any bytes on the channel; the harness also sends malformed ones. */
    public static void send(byte[] data) {
        LinkedNet.channel().sendToServer(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)), LinkedGuiCodec.CHANNEL));
    }

    public static int replies() {
        return replies;
    }

    public static LinkedGuiCodec.Response latest() {
        return latest;
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        ByteBuf payload = event.getPacket().payload();
        if (payload.readableBytes() > LinkedGuiCodec.MAX_RESPONSE) {
            return;
        }
        byte[] data = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), data);
        final LinkedGuiCodec.Response reply = LinkedGuiCodec.response(data);
        if (reply == null) {
            return;
        }
        Minecraft.getMinecraft().addScheduledTask(() -> {
            latest = reply;
            replies++;
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen instanceof GuiNPCManageLinkedNpc) {
                ((GuiNPCManageLinkedNpc) screen).rwReply(reply);
            }
        });
    }
}
