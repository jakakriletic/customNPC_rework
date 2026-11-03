package si.ladja.xaerofleet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import si.ladja.xaerofleet.client.ClientMapEvents;
import si.ladja.xaerofleet.network.FleetNetwork;
import si.ladja.xaerofleet.storm.StormConfig;
import si.ladja.xaerofleet.storm.StormService;

import java.util.Map;

/**
 * Bootstrap for the Xaero Fleet Command milestone.
 *
 * The operational layers deliberately arrive in small, testable slices:
 * command authority and snapshots first, then map interaction, then routing.
 */
@Mod(
        modid = XaeroFleetCommandMod.MOD_ID,
        name = "Xaero Fleet Command",
        version = XaeroFleetCommandMod.VERSION,
        dependencies = "after:davincisvessels;after:xaeroworldmap"
)
public final class XaeroFleetCommandMod {
    public static final String MOD_ID = "xaerofleetcommand";
    public static final String VERSION = "0.6.0-armies";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    /** Whether the server this client is connected to runs the same version of this mod. */
    private static volatile boolean serverSupportsFleet = true;
    private static volatile boolean serverSupportsWinter;
    private static volatile boolean serverSupportsArmies;

    /**
     * Never refuses a connection over this mod.
     *
     * <p>Without a check handler FML demands the same version on both sides,
     * so a client with the map mod could not join a server without it, and the
     * other way round - which is why it kept being taken out of the server
     * mods folders. The map is optional: a client simply notes whether the
     * server can take orders and keeps quiet if it cannot.</p>
     */
    @NetworkCheckHandler
    public boolean acceptRemote(Map<String, String> remoteMods, Side remoteSide) {
        if (remoteSide == Side.SERVER) {
            String remote = remoteMods.get(MOD_ID);
            serverSupportsArmies = VERSION.equals(remote);
            serverSupportsWinter = serverSupportsArmies || "0.5.0-winter".equals(remote);
            serverSupportsFleet = serverSupportsWinter || "0.4.0-milestone".equals(remote);
            if (!serverSupportsFleet) {
                LOG.info("The server does not have Xaero Fleet Command {} ({}); map orders are disabled.",
                        VERSION, remoteMods.get(MOD_ID));
            }
        }
        return true;
    }

    public static boolean serverSupportsFleet() {
        return serverSupportsFleet;
    }

    public static boolean serverSupportsWinter() { return serverSupportsWinter; }
    public static boolean serverSupportsArmies() { return serverSupportsArmies; }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FleetNetwork.init();
        StormConfig.load(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new StormService());
        if (FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new si.ladja.xaerofleet.client.StormClient());
        }
        if (FMLCommonHandler.instance().getSide().isClient()
                && Loader.isModLoaded("xaeroworldmap")) {
            si.ladja.xaerofleet.client.MapMode.useFile(new java.io.File(
                    event.getModConfigurationDirectory(), si.ladja.xaerofleet.client.MapMode.FILE_NAME));
            MinecraftForge.EVENT_BUS.register(new ClientMapEvents());
            MinecraftForge.EVENT_BUS.register(new si.ladja.xaerofleet.client.StormMapLayer());
            MinecraftForge.EVENT_BUS.register(new si.ladja.xaerofleet.client.ArmyMapLayer());
        }
    }

    @Mod.EventHandler public void stopped(net.minecraftforge.fml.common.event.FMLServerStoppedEvent event) { StormService.clear(); }
}
