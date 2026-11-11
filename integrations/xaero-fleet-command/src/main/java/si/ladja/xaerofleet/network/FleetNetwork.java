package si.ladja.xaerofleet.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import si.ladja.xaerofleet.XaeroFleetCommandMod;

public final class FleetNetwork {
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(XaeroFleetCommandMod.MOD_ID);

    private FleetNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(RequestCaptains.Handler.class, RequestCaptains.class, 0, Side.SERVER);
        CHANNEL.registerMessage(CaptainSnapshotPacket.Handler.class, CaptainSnapshotPacket.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(MoveOrderPacket.Handler.class, MoveOrderPacket.class, 2, Side.SERVER);
        CHANNEL.registerMessage(CommandResultPacket.Handler.class, CommandResultPacket.class, 3, Side.CLIENT);
        CHANNEL.registerMessage(CancelOrderPacket.Handler.class, CancelOrderPacket.class, 4, Side.SERVER);
        CHANNEL.registerMessage(EngagementOrderPacket.Handler.class, EngagementOrderPacket.class, 5, Side.SERVER);
        CHANNEL.registerMessage(RequestStormsPacket.Handler.class, RequestStormsPacket.class, 6, Side.SERVER);
        CHANNEL.registerMessage(StormSnapshotPacket.Handler.class, StormSnapshotPacket.class, 7, Side.CLIENT);
        CHANNEL.registerMessage(StormControlPacket.Handler.class, StormControlPacket.class, 8, Side.SERVER);
        CHANNEL.registerMessage(StormFeedbackPacket.Handler.class, StormFeedbackPacket.class, 9, Side.CLIENT);
        CHANNEL.registerMessage(ArmyRequest.Handler.class, ArmyRequest.class, 10, Side.SERVER);
        CHANNEL.registerMessage(ArmySnapshot.Handler.class, ArmySnapshot.class, 11, Side.CLIENT);
        CHANNEL.registerMessage(ArmyAction.Handler.class, ArmyAction.class, 12, Side.SERVER);
        CHANNEL.registerMessage(ArmyFeedback.Handler.class, ArmyFeedback.class, 13, Side.CLIENT);
        // 0.6.2-armies: single-flag moves; sent only to a server that announced this version.
        CHANNEL.registerMessage(ArmyFlagAction.Handler.class, ArmyFlagAction.class, 14, Side.SERVER);
    }
}
