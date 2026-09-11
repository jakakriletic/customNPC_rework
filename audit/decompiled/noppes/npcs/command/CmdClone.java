/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.command.NumberInvalidException
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 */
package noppes.npcs.command;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;

public class CmdClone
extends CommandNoppesBase {
    public String func_71517_b() {
        return "clone";
    }

    @Override
    public String getDescription() {
        return "Clone operation (server side)";
    }

    @CommandNoppesBase.SubCommand(desc="Add NPC(s) to clone storage", usage="<npc> <tab> [clonedname]", permission=4)
    public void add(MinecraftServer server, ICommandSender sender, String[] args) {
        int tab = 0;
        try {
            tab = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        List<EntityNPCInterface> list = this.getEntities(EntityNPCInterface.class, sender.func_130014_f_(), sender.func_180425_c(), 80);
        for (EntityNPCInterface npc : list) {
            NBTTagCompound compound;
            if (!npc.display.getName().equalsIgnoreCase(args[0])) continue;
            String name = npc.display.getName();
            if (args.length > 2) {
                name = args[2];
            }
            if (!npc.func_184198_c(compound = new NBTTagCompound())) {
                return;
            }
            ServerCloneController.Instance.addClone(compound, name, tab);
        }
    }

    @CommandNoppesBase.SubCommand(desc="List NPC from clone storage", usage="<tab>", permission=2)
    public void list(MinecraftServer server, ICommandSender sender, String[] args) {
        this.sendMessage(sender, "--- Stored NPCs --- (server side)", new Object[0]);
        int tab = 0;
        try {
            tab = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        for (String name : ServerCloneController.Instance.getClones(tab)) {
            this.sendMessage(sender, name, new Object[0]);
        }
        this.sendMessage(sender, "------------------------------------", new Object[0]);
    }

    @CommandNoppesBase.SubCommand(desc="Remove NPC from clone storage", usage="<name> <tab>", permission=4)
    public void del(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String nametodel = args[0];
        int tab = 0;
        try {
            tab = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        boolean deleted = false;
        for (String name : ServerCloneController.Instance.getClones(tab)) {
            if (!nametodel.equalsIgnoreCase(name)) continue;
            ServerCloneController.Instance.removeClone(name, tab);
            deleted = true;
            break;
        }
        if (!ServerCloneController.Instance.removeClone(nametodel, tab)) {
            throw new CommandException("Npc '%s' wasn't found", new Object[]{nametodel});
        }
    }

    @CommandNoppesBase.SubCommand(desc="Spawn cloned NPC", usage="<name> <tab> [[world:]x,y,z]] [newname]", permission=2)
    public void spawn(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String name = args[0].replaceAll("%", " ");
        int tab = 0;
        try {
            tab = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        String newname = null;
        NBTTagCompound compound = ServerCloneController.Instance.getCloneData(sender, name, tab);
        if (compound == null) {
            throw new CommandException("Unknown npc", new Object[0]);
        }
        World world = sender.func_130014_f_();
        BlockPos pos = sender.func_180425_c();
        if (args.length > 2) {
            String[] par;
            String location = args[2];
            if (location.contains(":")) {
                par = location.split(":");
                location = par[1];
                world = this.getWorld(server, par[0]);
                if (world == null) {
                    throw new CommandException("'%s' is an unknown world", new Object[]{par[0]});
                }
            }
            if (location.contains(",")) {
                par = location.split(",");
                if (par.length != 3) {
                    throw new CommandException("Location need be x,y,z", new Object[0]);
                }
                try {
                    pos = CommandBase.func_175757_a((ICommandSender)sender, (String[])par, (int)0, (boolean)false);
                }
                catch (NumberInvalidException e) {
                    throw new CommandException("Location should be in numbers", new Object[0]);
                }
                if (args.length > 3) {
                    newname = args[3];
                }
            } else {
                newname = location;
            }
        }
        if (pos.func_177958_n() == 0 && pos.func_177956_o() == 0 && pos.func_177952_p() == 0) {
            throw new CommandException("Location needed", new Object[0]);
        }
        Entity entity = EntityList.func_75615_a((NBTTagCompound)compound, (World)world);
        entity.func_70107_b((double)pos.func_177958_n() + 0.5, (double)(pos.func_177956_o() + 1), (double)pos.func_177952_p() + 0.5);
        if (entity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            npc.ais.setStartPos(pos);
            if (newname != null && !newname.isEmpty()) {
                npc.display.setName(newname.replaceAll("%", " "));
            }
        }
        world.func_72838_d(entity);
    }

    @CommandNoppesBase.SubCommand(desc="Spawn multiple cloned NPC in a grid", usage="<name> <tab> <lenght> <width> [[world:]x,y,z]] [newname]", permission=2)
    public boolean grid(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int height;
        int width;
        String name = args[0].replaceAll("%", " ");
        int tab = 0;
        try {
            tab = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        try {
            width = Integer.parseInt(args[2]);
            height = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("lenght or width wasnt a number", new Object[0]);
        }
        String newname = null;
        NBTTagCompound compound = ServerCloneController.Instance.getCloneData(sender, name, tab);
        if (compound == null) {
            throw new CommandException("Unknown npc", new Object[0]);
        }
        World world = sender.func_130014_f_();
        BlockPos curpos = sender.func_180425_c();
        if (args.length > 4) {
            String[] par;
            String location = args[4];
            if (location.contains(":")) {
                par = location.split(":");
                location = par[1];
                world = this.getWorld(server, par[0]);
                if (world == null) {
                    throw new CommandException("'%s' is an unknown world", new Object[]{par[0]});
                }
            }
            if (location.contains(",")) {
                par = location.split(",");
                if (par.length != 3) {
                    throw new CommandException("Location need be x,y,z", new Object[0]);
                }
                try {
                    curpos = CommandBase.func_175757_a((ICommandSender)sender, (String[])par, (int)0, (boolean)false);
                }
                catch (NumberInvalidException e) {
                    throw new CommandException("Location should be in numbers", new Object[0]);
                }
                if (args.length > 5) {
                    newname = args[5];
                }
            } else {
                newname = location;
            }
        }
        if (curpos.func_177958_n() == 0 && curpos.func_177956_o() == 0 && curpos.func_177952_p() == 0) {
            throw new CommandException("Location needed", new Object[0]);
        }
        for (int x = 0; x < width; ++x) {
            for (int z = 0; z < height; ++z) {
                BlockPos npcpos = curpos.func_177982_a(x, -2, z);
                for (int y = 0; y < 10; ++y) {
                    BlockPos pos = npcpos.func_177981_b(y);
                    BlockPos pos2 = pos.func_177984_a();
                    IBlockState b = world.func_180495_p(pos);
                    IBlockState b2 = world.func_180495_p(pos2);
                    if (!b.func_191058_s() || b2.func_191058_s()) continue;
                    npcpos = pos;
                    break;
                }
                Entity entity = EntityList.func_75615_a((NBTTagCompound)compound, (World)world);
                entity.func_70107_b((double)npcpos.func_177958_n() + 0.5, (double)(npcpos.func_177956_o() + 1), (double)npcpos.func_177952_p() + 0.5);
                if (entity instanceof EntityNPCInterface) {
                    EntityNPCInterface npc = (EntityNPCInterface)entity;
                    npc.ais.setStartPos(npcpos);
                    if (newname != null && !newname.isEmpty()) {
                        npc.display.setName(newname.replaceAll("%", " "));
                    }
                }
                world.func_72838_d(entity);
            }
        }
        return true;
    }

    public World getWorld(MinecraftServer server, String t) {
        WorldServer[] ws;
        for (WorldServer w : ws = server.field_71305_c) {
            if (w == null || !(w.field_73011_w.getDimension() + "").equalsIgnoreCase(t)) continue;
            return w;
        }
        return null;
    }

    public <T extends Entity> List<T> getEntities(Class<? extends T> cls, World world, BlockPos pos, int range) {
        return world.func_72872_a(cls, new AxisAlignedBB(pos, pos.func_177982_a(1, 1, 1)).func_72314_b((double)range, (double)range, (double)range));
    }
}

