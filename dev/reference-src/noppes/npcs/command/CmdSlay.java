/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.boss.EntityDragon
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.item.EntityXPOrb
 *  net.minecraft.entity.monster.EntityGhast
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.passive.EntityHorse
 *  net.minecraft.entity.passive.EntityTameable
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraftforge.fml.common.registry.EntityEntry
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 */
package noppes.npcs.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.entity.EntityNPCInterface;

public class CmdSlay
extends CommandNoppesBase {
    public Map<String, Class<?>> SlayMap = new LinkedHashMap();

    public CmdSlay() {
        this.SlayMap.clear();
        this.SlayMap.put("all", EntityLivingBase.class);
        this.SlayMap.put("mobs", EntityMob.class);
        this.SlayMap.put("animals", EntityAnimal.class);
        this.SlayMap.put("items", EntityItem.class);
        this.SlayMap.put("xporbs", EntityXPOrb.class);
        this.SlayMap.put("npcs", EntityNPCInterface.class);
        for (EntityEntry ent : ForgeRegistries.ENTITIES.getValues()) {
            String name = ent.getName();
            Class cls = ent.getEntityClass();
            if (EntityNPCInterface.class.isAssignableFrom(cls) || !EntityLivingBase.class.isAssignableFrom(cls)) continue;
            this.SlayMap.put(name.toLowerCase(), cls);
        }
        this.SlayMap.remove("monster");
        this.SlayMap.remove("mob");
    }

    public String getName() {
        return "slay";
    }

    @Override
    public String getDescription() {
        return "Kills given entity within range. Also has all, mobs, animal options. Can have multiple types";
    }

    @Override
    public String getUsage() {
        return "<type>.. [range]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        ArrayList toDelete = new ArrayList();
        boolean deleteNPCs = false;
        for (String delete : args) {
            Class<?> cls = this.SlayMap.get(delete = delete.toLowerCase());
            if (cls != null) {
                toDelete.add(cls);
            }
            if (delete.equals("mobs")) {
                toDelete.add(EntityGhast.class);
                toDelete.add(EntityDragon.class);
            }
            if (!delete.equals("npcs")) continue;
            deleteNPCs = true;
        }
        int count = 0;
        int range = 120;
        try {
            range = Integer.parseInt(args[args.length - 1]);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        AxisAlignedBB box = new AxisAlignedBB(sender.getPosition(), sender.getPosition().add(1, 1, 1)).grow((double)range, (double)range, (double)range);
        List list = sender.getEntityWorld().getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (Entity entity : list) {
            if (entity instanceof EntityPlayer || entity instanceof EntityTameable && ((EntityTameable)entity).isTamed() || entity instanceof EntityNPCInterface && !deleteNPCs || !this.delete(entity, toDelete)) continue;
            ++count;
        }
        if (toDelete.contains(EntityXPOrb.class)) {
            list = sender.getEntityWorld().getEntitiesWithinAABB(EntityXPOrb.class, box);
            for (Entity entity : list) {
                entity.isDead = true;
                ++count;
            }
        }
        if (toDelete.contains(EntityItem.class)) {
            list = sender.getEntityWorld().getEntitiesWithinAABB(EntityItem.class, box);
            for (Entity entity : list) {
                entity.isDead = true;
                ++count;
            }
        }
        sender.sendMessage((ITextComponent)new TextComponentTranslation(count + " entities deleted", new Object[0]));
    }

    private boolean delete(Entity entity, ArrayList<Class<?>> toDelete) {
        for (Class<?> delete : toDelete) {
            if (delete == EntityAnimal.class && entity instanceof EntityHorse || !delete.isAssignableFrom(entity.getClass())) continue;
            entity.isDead = true;
            return true;
        }
        return false;
    }

    public List getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return CommandBase.getListOfStringsMatchingLastWord((String[])args, (String[])this.SlayMap.keySet().toArray(new String[this.SlayMap.size()]));
    }
}

