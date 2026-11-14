package noppes.npcs.rework.rig.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.LogWriter;
import noppes.npcs.rework.anim.ReloadCodec;
import noppes.npcs.rework.rig.RigCreatureAssets;
import noppes.npcs.rework.rig.RigCreatureTransfer;

@SideOnly(Side.CLIENT)
public final class RigCreatureClient {
    private ReloadCodec.Assembler assembler = new ReloadCodec.Assembler();
    private RigCreatureAssets.Snapshot staged;
    private byte[] stagedHash;
    private int revision;

    static void install() {
        RigCreatureClient client = new RigCreatureClient();
        RigCreatureTransfer.channel().register(client);
        MinecraftForge.EVENT_BUS.register(client);
    }

    @SubscribeEvent public void packet(FMLNetworkEvent.ClientCustomPacketEvent event) {
        if (!RigCreatureTransfer.CHANNEL.equals(event.getPacket().channel())) { return; }
        ByteBuf payload = event.getPacket().payload();
        if (payload.readableBytes() > ReloadCodec.MAX_MESSAGE) { return; }
        byte[] bytes = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), bytes);
        ReloadCodec.Message message = ReloadCodec.decode(bytes);
        if (message != null) { Minecraft.getMinecraft().addScheduledTask(() -> receive(message)); }
    }

    private void receive(ReloadCodec.Message message) {
        if (message.kind == ReloadCodec.PART) {
            byte[] bundle = assembler.accept(message);
            if (bundle != null) { prepare(message.revision, bundle); }
        } else if (message.revision == revision && message.kind == ReloadCodec.ABORT) {
            clear();
        } else if (message.revision == revision && message.kind == ReloadCodec.COMMIT && staged != null
                && Arrays.equals(stagedHash, message.hash)) {
            RigCreatureAssets.publishClient(staged);
            clear();
        }
    }

    private void prepare(int proposed, byte[] bundle) {
        byte[] hash = ReloadCodec.hash(bundle);
        clear();
        try {
            staged = RigCreatureAssets.decode(bundle);
            stagedHash = hash;
            revision = proposed;
        } catch (IOException | RuntimeException failure) {
            LogWriter.error("RWRIG could not prepare creature assets", failure);
        }
        byte[] ack = ReloadCodec.ack(proposed, staged != null, hash);
        RigCreatureTransfer.channel().sendToServer(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(ack)), RigCreatureTransfer.CHANNEL));
    }

    @SubscribeEvent public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            assembler = new ReloadCodec.Assembler();
            clear();
            RigCreatureAssets.publishClient(null);
        });
    }

    private void clear() { staged = null; stagedHash = null; revision = 0; }
}
