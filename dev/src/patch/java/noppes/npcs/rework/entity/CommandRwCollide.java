package noppes.npcs.rework.entity;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.LogWriter;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.wrapper.WrapperNpcAPI;

/**
 * Ukaz {@code /rwcollide [0|1|2|reset|poslusalec 0|1]}: pokaze ali med tekom preklopi nacin
 * {@link CollideSkip} in izpise stevce. Ne zapise v config — trajna nastavitev je
 * {@code RwCollideSkip}.
 *
 * <p>{@code poslusalec 1} registrira poskusnega poslusalca za {@code NpcEvent.CollideEvent} na
 * {@code WrapperNpcAPI.EVENT_BUS} (kot bi ga skripta ali drug mod), {@code poslusalec 0} ga
 * odjavi. Scenarij s tem preveri, da se preskok ob registraciji takoj ustavi in da poslusalec
 * dogodke dobi. Odgovor gre tudi v log z markerjem {@code RWCOLLIDE}.
 */
public class CommandRwCollide extends CommandBase {
    /** Poskusni poslusalec; steje prejete dogodke. */
    public static final class Probe {
        long received;

        @SubscribeEvent
        public void onCollide(NpcEvent.CollideEvent event) {
            ++this.received;
        }
    }

    private static Probe probe;
    private static long probeReceived;

    @Override
    public String getName() {
        return "rwcollide";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwcollide [0|1|2|reset|poslusalec 0|poslusalec 1]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws WrongUsageException {
        if (args.length > 0) {
            if ("reset".equals(args[0])) {
                CollideSkip.reset();
                probeReceived = 0;
                if (probe != null) {
                    probe.received = 0;
                }
            } else if ("poslusalec".equals(args[0])) {
                if (args.length < 2 || !("0".equals(args[1]) || "1".equals(args[1]))) {
                    throw new WrongUsageException(getUsage(sender));
                }
                if ("1".equals(args[1]) && probe == null) {
                    probe = new Probe();
                    WrapperNpcAPI.EVENT_BUS.register(probe);
                } else if ("0".equals(args[1]) && probe != null) {
                    probeReceived += probe.received;
                    WrapperNpcAPI.EVENT_BUS.unregister(probe);
                    probe = null;
                }
            } else {
                int requested;
                try {
                    requested = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(getUsage(sender));
                }
                if (!CollideSkip.isValidMode(requested)) {
                    throw new WrongUsageException(getUsage(sender));
                }
                CollideSkip.setMode(requested);
            }
        }
        int m = CollideSkip.mode();
        long received = probeReceived + (probe != null ? probe.received : 0);
        String msg = "RWCOLLIDE nacin=" + m + " (" + CollideSkip.describe(m) + ") klicev=" + CollideSkip.calls()
                + " preskocenih=" + CollideSkip.skipped() + " brezOpazovalca=" + CollideSkip.unobservedCalls()
                + " dogodkovBrezOpazovalca=" + CollideSkip.unobservedEvents()
                + " poslusalec=" + (probe != null ? 1 : 0) + " poslusalecDogodkov=" + received
                + " opazovalec=" + (CollideSkip.hasCollideListeners() ? 1 : 0);
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
