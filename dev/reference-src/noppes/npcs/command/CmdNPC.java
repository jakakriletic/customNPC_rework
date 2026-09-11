/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.command.NumberInvalidException
 *  net.minecraft.command.PlayerNotFoundException
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  org.apache.commons.lang3.ArrayUtils
 */
package noppes.npcs.command;

import java.util.Arrays;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;
import org.apache.commons.lang3.ArrayUtils;

public class CmdNPC
extends CommandNoppesBase {
    public EntityNPCInterface selectedNpc;

    public String getName() {
        return "npc";
    }

    @Override
    public String getDescription() {
        return "NPC operation";
    }

    @Override
    public String getUsage() {
        return "<name> <command>";
    }

    @Override
    public boolean runSubCommands() {
        return false;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String npcname = args[0].replace("%", " ");
        String command = args[1];
        args = Arrays.copyOfRange(args, 2, args.length);
        if (command.equalsIgnoreCase("create")) {
            args = (String[])ArrayUtils.add((Object[])args, (int)0, (Object)npcname);
            this.executeSub(server, sender, command, args);
            return;
        }
        List<EntityNPCInterface> list = this.getEntities(EntityNPCInterface.class, sender.getEntityWorld(), sender.getPosition(), 80);
        for (EntityNPCInterface npc : list) {
            String name = npc.display.getName().replace(" ", "_");
            if (!name.equalsIgnoreCase(npcname) || this.selectedNpc != null && !(this.selectedNpc.getDistanceSq(sender.getPosition()) > npc.getDistanceSq(sender.getPosition()))) continue;
            this.selectedNpc = npc;
        }
        if (this.selectedNpc == null) {
            throw new CommandException("Npc '%s' was not found", new Object[]{npcname});
        }
        this.executeSub(server, sender, command, args);
        this.selectedNpc = null;
    }

    @CommandNoppesBase.SubCommand(desc="Set Home (respawn place)", usage="[x] [y] [z]", permission=2)
    public void home(MinecraftServer server, ICommandSender sender, String[] args) {
        BlockPos pos = sender.getPosition();
        if (args.length == 3) {
            try {
                pos = CommandBase.parseBlockPos((ICommandSender)sender, (String[])args, (int)0, (boolean)false);
            }
            catch (NumberInvalidException numberInvalidException) {
                // empty catch block
            }
        }
        this.selectedNpc.ais.setStartPos(pos);
    }

    @CommandNoppesBase.SubCommand(desc="Set NPC visibility", usage="[true/false/semi]", permission=2)
    public void visible(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 1) {
            return;
        }
        boolean bo = args[0].equalsIgnoreCase("true");
        boolean semi = args[0].equalsIgnoreCase("semi");
        int current = this.selectedNpc.display.getVisible();
        if (semi) {
            this.selectedNpc.display.setVisible(2);
        } else if (bo) {
            this.selectedNpc.display.setVisible(0);
        } else {
            this.selectedNpc.display.setVisible(1);
        }
    }

    @CommandNoppesBase.SubCommand(desc="Delete an NPC")
    public void delete(MinecraftServer server, ICommandSender sender, String[] args) {
        this.selectedNpc.delete();
    }

    @CommandNoppesBase.SubCommand(desc="Sets the owner of an follower/companion", usage="[player]", permission=2)
    public void owner(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 1) {
            EntityPlayer player = null;
            if (this.selectedNpc.roleInterface instanceof RoleFollower) {
                player = ((RoleFollower)this.selectedNpc.roleInterface).owner;
            }
            if (this.selectedNpc.roleInterface instanceof RoleCompanion) {
                player = ((RoleCompanion)this.selectedNpc.roleInterface).owner;
            }
            if (player == null) {
                this.sendMessage(sender, "No owner", new Object[0]);
            } else {
                this.sendMessage(sender, "Owner is: " + player.getName(), new Object[0]);
            }
        } else {
            EntityPlayerMP player = null;
            try {
                player = CommandBase.getPlayer((MinecraftServer)server, (ICommandSender)sender, (String)args[0]);
            }
            catch (PlayerNotFoundException playerNotFoundException) {
            }
            catch (CommandException commandException) {
                // empty catch block
            }
            if (this.selectedNpc.roleInterface instanceof RoleFollower) {
                ((RoleFollower)this.selectedNpc.roleInterface).setOwner((EntityPlayer)player);
            }
            if (this.selectedNpc.roleInterface instanceof RoleCompanion) {
                ((RoleCompanion)this.selectedNpc.roleInterface).setOwner((EntityPlayer)player);
            }
        }
    }

    @CommandNoppesBase.SubCommand(desc="Set NPC name", usage="[name]", permission=2)
    public void name(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 1) {
            return;
        }
        String name = args[0];
        for (int i = 1; i < args.length; ++i) {
            name = name + " " + args[i];
        }
        if (!this.selectedNpc.display.getName().equals(name)) {
            this.selectedNpc.display.setName(name);
            this.selectedNpc.updateClient = true;
        }
    }

    @CommandNoppesBase.SubCommand(desc="Resets the npc", usage="[name]", permission=2)
    public void reset(MinecraftServer server, ICommandSender sender, String[] args) {
        this.selectedNpc.reset();
    }

    @CommandNoppesBase.SubCommand(desc="Creates an NPC", usage="[name]")
    public void create(MinecraftServer server, ICommandSender sender, String[] args) {
        World pw = sender.getEntityWorld();
        EntityCustomNpc npc = new EntityCustomNpc(pw);
        if (args.length > 0) {
            npc.display.setName(args[0]);
        }
        BlockPos pos = sender.getPosition();
        npc.setPositionAndRotation(pos.getX(), pos.getY(), pos.getZ(), 0.0f, 0.0f);
        npc.ais.setStartPos(pos);
        pw.spawnEntity((Entity)npc);
        npc.setHealth(npc.getMaxHealth());
    }

    public List getTabCompletions(MinecraftServer server, ICommandSender par1, String[] args, BlockPos pos) {
        if (args.length == 2) {
            return CommandBase.getListOfStringsMatchingLastWord((String[])args, (String[])new String[]{"create", "home", "visible", "delete", "owner", "name"});
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("owner")) {
            return CommandBase.getListOfStringsMatchingLastWord((String[])args, (String[])server.getOnlinePlayerNames());
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    public <T extends Entity> List<T> getEntities(Class<? extends T> cls, World world, BlockPos pos, int range) {
        return world.getEntitiesWithinAABB(cls, new AxisAlignedBB(pos, pos.add(1, 1, 1)).grow((double)range, (double)range, (double)range));
    }
}

