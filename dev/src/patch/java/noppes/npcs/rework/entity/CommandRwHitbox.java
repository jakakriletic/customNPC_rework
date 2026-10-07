package noppes.npcs.rework.entity;

import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M3.8 (R6): {@code /rwhitbox <original|solid|smart|status> [predpona-imena] [global 0|1]}.
 * Nastavi nacin hitboxa nalozenim NPC-jem, katerih ime se zacne s predpono (NBT RwHitboxMode,
 * ostane ob shranjevanju in kloniranju). {@code /rwhitbox global 1} med tekom vklopi stikalo
 * RwHitbox (trajno je v configu). Odgovor gre v log z markerjem {@code RWHITBOX}.
 */
public class CommandRwHitbox extends CommandBase {
    @Override
    public String getName() {
        return "rwhitbox";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwhitbox <original|solid|smart|status> [name-prefix] | global <0|1> | where <name-prefix> | probe|nearby <name-a> <name-b> <dx>"
                + " | mount <rider> <carrier> | dismount <rider> | track <name-prefix> <ticks>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws WrongUsageException {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("probe") || action.equals("nearby")) {
            probe(sender, args);
            return;
        }
        if (action.equals("track")) {
            int ticks;
            try {
                ticks = args.length == 3 ? Integer.parseInt(args[2]) : -1;
            } catch (NumberFormatException e) {
                ticks = -1;
            }
            if (ticks <= 0) {
                throw new WrongUsageException(getUsage(sender));
            }
            String line = "RWHITBOX-TRACK-ZACETEK n=" + HitboxTrack.start(sender.getEntityWorld(), args[1], ticks) + " tickov=" + ticks;
            sender.sendMessage(new TextComponentString(line));
            LogWriter.info(line);
            return;
        }
        if (action.equals("mount") || action.equals("dismount")) {
            mount(sender, action, args);
            return;
        }
        if (action.equals("where")) {
            if (args.length != 2) {
                throw new WrongUsageException(getUsage(sender));
            }
            for (Entity entity : sender.getEntityWorld().loadedEntityList) {
                if (entity instanceof EntityNPCInterface && entity.getName().startsWith(args[1])) {
                    String line = String.format(Locale.ROOT, "RWHITBOX-WHERE ime=%s x=%.4f z=%.4f", entity.getName(), entity.posX, entity.posZ);
                    sender.sendMessage(new TextComponentString(line));
                    LogWriter.info(line);
                }
            }
            return;
        }
        if (action.equals("global")) {
            if (args.length != 2 || !(args[1].equals("0") || args[1].equals("1"))) {
                throw new WrongUsageException(getUsage(sender));
            }
            CustomNpcs.RwHitbox = Integer.parseInt(args[1]);
            action = "status";
            args = new String[0];
        }
        int mode = action.equals("status") ? -1 : HitboxWeights.parse(action);
        if (!action.equals("status") && mode < 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        String prefix = args.length > 1 ? args[1] : "";
        if (mode >= 0 && prefix.isEmpty()) {
            throw new WrongUsageException(getUsage(sender));
        }
        int matched = 0;
        int[] counts = new int[3];
        for (Entity entity : sender.getEntityWorld().loadedEntityList) {
            if (!(entity instanceof EntityNPCInterface) || !entity.getName().startsWith(prefix)) {
                continue;
            }
            EntityNPCInterface npc = (EntityNPCInterface) entity;
            matched++;
            if (mode >= 0) {
                npc.setRwHitboxMode(mode);
            }
            counts[npc.getRwHitboxMode()]++;
        }
        String line = "RWHITBOX global=" + CustomNpcs.RwHitbox + " ujemanj=" + matched + " original=" + counts[0]
                + " solid=" + counts[1] + " smart=" + counts[2] + " scit=" + CustomNpcs.RwHitboxShieldWeight + "%";
        sender.sendMessage(new TextComponentString(line));
        LogWriter.info(line);
    }

    /**
     * Diagnostika (scenarij M3.9): {@code mount} posadi jahaca na nosilca (vanilla
     * {@code startRiding}, ista pot kot skriptni {@code setMount}), {@code dismount} ga spusti.
     */
    private void mount(ICommandSender sender, String action, String[] args) throws WrongUsageException {
        int want = action.equals("mount") ? 3 : 2;
        if (args.length != want) {
            throw new WrongUsageException(getUsage(sender));
        }
        EntityNPCInterface npc = find(sender, args[1]);
        if (npc == null) {
            throw new WrongUsageException("/rwhitbox " + action + ": NPC '" + args[1] + "' ni nalozen");
        }
        boolean ok;
        if (action.equals("mount")) {
            EntityNPCInterface carrier = find(sender, args[2]);
            if (carrier == null || carrier == npc) {
                throw new WrongUsageException("/rwhitbox mount: NPC '" + args[2] + "' ni nalozen");
            }
            ok = npc.startRiding(carrier, true) && npc.getRidingEntity() == carrier;
        } else {
            npc.dismountRidingEntity();
            ok = npc.getRidingEntity() == null;
        }
        String line = "RWHITBOX-" + action.toUpperCase(Locale.ROOT) + " ime=" + npc.getName() + " ok=" + ok;
        sender.sendMessage(new TextComponentString(line));
        LogWriter.info(line);
    }

    /**
     * Diagnostika (scenarij M3.8): B postavi {@code dx} blokov vzhodno od A, obema izbrise hitrost,
     * izvede en trk {@code A.applyEntityCollision(B)} (ista pot kot iz vanilla
     * collideWithNearbyEntities) in izpise vodoravni hitrosti. Brez naklucja in gibanja AI-ja.
     */
    private void probe(ICommandSender sender, String[] args) throws WrongUsageException {
        if (args.length != 4) {
            throw new WrongUsageException("/rwhitbox probe <name-a> <name-b> <dx>");
        }
        EntityNPCInterface a = find(sender, args[1]);
        EntityNPCInterface b = find(sender, args[2]);
        if (a == null || b == null || a == b) {
            throw new WrongUsageException("/rwhitbox probe: NPC '" + args[1] + "' ali '" + args[2] + "' ni nalozen");
        }
        double dx;
        try {
            dx = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            throw new WrongUsageException("/rwhitbox probe <name-a> <name-b> <dx>");
        }
        boolean nearby = args[0].equalsIgnoreCase("nearby");
        double bx = b.posX, by = b.posY, bz = b.posZ;
        b.setPosition(a.posX + dx, a.posY, a.posZ);
        a.motionX = a.motionZ = b.motionX = b.motionZ = 0.0;
        if (nearby) {
            // Prava vanilla pot: A.collideWithNearbyEntities -> collideWithEntity(B) -> B.applyEntityCollision(A).
            collideNearby(a);
        } else {
            a.applyEntityCollision(b);
        }
        double va = Math.sqrt(a.motionX * a.motionX + a.motionZ * a.motionZ);
        double vb = Math.sqrt(b.motionX * b.motionX + b.motionZ * b.motionZ);
        a.motionX = a.motionZ = b.motionX = b.motionZ = 0.0;
        b.setPosition(bx, by, bz);
        double[] s = HitboxWeights.shares(RwHitbox.modeOf(a), RwHitbox.mass(a), RwHitbox.modeOf(b), RwHitbox.mass(b));
        String line = String.format(Locale.ROOT,
                "RWHITBOX-PROBE a=%s b=%s nacinA=%s nacinB=%s sirinaA=%.3f sirinaB=%.3f masaA=%.5f masaB=%.5f scitA=%b scitB=%b vA=%.6f vB=%.6f delezA=%.4f delezB=%.4f pot=%s",
                a.getName(), b.getName(), HitboxWeights.name(RwHitbox.modeOf(a)), HitboxWeights.name(RwHitbox.modeOf(b)),
                a.width, b.width, RwHitbox.mass(a), RwHitbox.mass(b), RwHitbox.holdsShield(a), RwHitbox.holdsShield(b), va, vb, s[0], s[1], nearby ? "nearby" : "probe");
        sender.sendMessage(new TextComponentString(line));
        LogWriter.info(line);
    }

    private static final java.lang.reflect.Method COLLIDE_NEARBY = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(
            net.minecraft.entity.EntityLivingBase.class, "collideWithNearbyEntities", "func_85033_bc");

    private static void collideNearby(EntityNPCInterface npc) throws WrongUsageException {
        try {
            COLLIDE_NEARBY.invoke(npc);
        } catch (ReflectiveOperationException e) {
            throw new WrongUsageException("/rwhitbox nearby: " + e);
        }
    }

    private static EntityNPCInterface find(ICommandSender sender, String name) {
        for (Entity entity : sender.getEntityWorld().loadedEntityList) {
            if (entity instanceof EntityNPCInterface && entity.isEntityAlive() && entity.getName().equals(name)) {
                return (EntityNPCInterface) entity;
            }
        }
        return null;
    }
}
