package noppes.npcs.rework.linked;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.LogWriter;
import noppes.npcs.controllers.LinkedNpcController;

/**
 * M13.2b (Q395, Q396): server side of the {@value LinkedGuiCodec#CHANNEL} channel, which exists only with
 * RwLinkedGroups=1. Every request is checked at that moment against the same rights as /rwlinked (level 2) and the
 * original permission of the Linked NPCs manager.
 */
public final class LinkedNet {
    private static FMLEventChannel channel;

    private LinkedNet() {
    }

    /** Pre-init, only with RwLinkedGroups=1. */
    public static synchronized void init() {
        if (channel == null) {
            channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(LinkedGuiCodec.CHANNEL);
            channel.register(new LinkedNet());
            LogWriter.info("RWLINKED channel " + LinkedGuiCodec.CHANNEL + " registered");
        }
    }

    static FMLEventChannel channel() {
        return channel;
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        if (!(event.getHandler() instanceof NetHandlerPlayServer)) {
            return;
        }
        final EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
        final MinecraftServer server = player.getServer();
        ByteBuf payload = event.getPacket().payload();
        if (server == null || payload.readableBytes() > LinkedGuiCodec.MAX_BATCH) {
            return;
        }
        byte[] data = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), data);
        final LinkedGuiCodec.Request request = LinkedGuiCodec.request(data);
        final LinkedGuiCodec.Batch batch = request == null ? LinkedGuiCodec.batch(data) : null;
        if (request == null && batch == null) {
            return;
        }
        server.addScheduledTask(() -> {
            if (!player.hasDisconnected()) {
                byte[] reply = request != null ? execute(server, player, request).encode() : LinkedWand.execute(server, player, batch);
                channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(reply)), LinkedGuiCodec.CHANNEL), player);
            }
        });
    }

    private static LinkedGuiCodec.Response execute(MinecraftServer server, EntityPlayerMP player, LinkedGuiCodec.Request r) {
        if (!player.canUseCommand(2, "rwlinked") || !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_LINKED)) {
            return new LinkedGuiCodec.Response(r.id, LinkedGuiCodec.RESULT_DENIED, 0, false, 0, r.group, "No rights to change groups");
        }
        LinkedNpcController linked = LinkedNpcController.Instance;
        if (r.group.isEmpty() && r.op == LinkedGuiCodec.Op.GET) {
            return new LinkedGuiCodec.Response(r.id, LinkedGuiCodec.RESULT_OK, LinkedCategories.DEFAULT, false, 0, "", "");
        }
        LinkedNpcController.LinkedData data = linked.getData(r.group);
        if (data == null) {
            return new LinkedGuiCodec.Response(r.id, LinkedGuiCodec.RESULT_UNKNOWN, 0, false, 0, r.group, "Group no longer exists");
        }
        int result = LinkedGuiCodec.RESULT_OK;
        String text = "";
        if (r.op == LinkedGuiCodec.Op.SET) {
            linked.setCategories(data, r.mask);
            text = "Categories saved";
            LogWriter.info("RWLINKED gui player=" + player.getName() + " categories group=" + data.name + " now=" + LinkedCategories.toText(r.mask));
        } else if (r.op == LinkedGuiCodec.Op.UNDO) {
            boolean done = linked.undo(data);
            result = done ? LinkedGuiCodec.RESULT_OK : LinkedGuiCodec.RESULT_NOTHING;
            text = done ? "Last change undone" : "Nothing to undo";
            LogWriter.info("RWLINKED gui player=" + player.getName() + " undo group=" + data.name + " done=" + (done ? 1 : 0));
        }
        return new LinkedGuiCodec.Response(r.id, result, data.categories, linked.hasPrevious(data),
                CommandRwLinked.loadedMembers(server, data.name), r.group, text);
    }
}
