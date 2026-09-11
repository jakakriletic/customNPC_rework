/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.ChunkPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.World
 *  net.minecraftforge.common.ForgeChunkManager
 *  net.minecraftforge.common.ForgeChunkManager$LoadingCallback
 *  net.minecraftforge.common.ForgeChunkManager$Ticket
 *  net.minecraftforge.common.ForgeChunkManager$Type
 */
package noppes.npcs.controllers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;

public class ChunkController
implements ForgeChunkManager.LoadingCallback {
    public static ChunkController instance;
    private HashMap<Entity, ForgeChunkManager.Ticket> tickets = new HashMap();

    public ChunkController() {
        instance = this;
    }

    public void clear() {
        this.tickets = new HashMap();
    }

    public ForgeChunkManager.Ticket getTicket(EntityNPCInterface npc) {
        ForgeChunkManager.Ticket ticket = this.tickets.get((Object)npc);
        if (ticket != null) {
            return ticket;
        }
        if (this.size() >= CustomNpcs.ChuckLoaders) {
            return null;
        }
        ticket = ForgeChunkManager.requestTicket((Object)CustomNpcs.instance, (World)npc.field_70170_p, (ForgeChunkManager.Type)ForgeChunkManager.Type.ENTITY);
        if (ticket == null) {
            return null;
        }
        ticket.bindEntity((Entity)npc);
        ticket.setChunkListDepth(6);
        this.tickets.put((Entity)npc, ticket);
        return null;
    }

    public void deleteNPC(EntityNPCInterface npc) {
        ForgeChunkManager.Ticket ticket = this.tickets.get((Object)npc);
        if (ticket != null) {
            this.tickets.remove((Object)npc);
            ForgeChunkManager.releaseTicket((ForgeChunkManager.Ticket)ticket);
        }
    }

    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket ticket : tickets) {
            if (!(ticket.getEntity() instanceof EntityNPCInterface)) continue;
            EntityNPCInterface npc = (EntityNPCInterface)ticket.getEntity();
            if (npc.advanced.job != 8 || tickets.contains((Object)npc)) continue;
            this.tickets.put((Entity)npc, ticket);
            double x = npc.field_70165_t / 16.0;
            double z = npc.field_70161_v / 16.0;
            ForgeChunkManager.forceChunk((ForgeChunkManager.Ticket)ticket, (ChunkPos)new ChunkPos(MathHelper.func_76128_c((double)x), MathHelper.func_76128_c((double)z)));
            ForgeChunkManager.forceChunk((ForgeChunkManager.Ticket)ticket, (ChunkPos)new ChunkPos(MathHelper.func_76143_f((double)x), MathHelper.func_76143_f((double)z)));
            ForgeChunkManager.forceChunk((ForgeChunkManager.Ticket)ticket, (ChunkPos)new ChunkPos(MathHelper.func_76128_c((double)x), MathHelper.func_76143_f((double)z)));
            ForgeChunkManager.forceChunk((ForgeChunkManager.Ticket)ticket, (ChunkPos)new ChunkPos(MathHelper.func_76143_f((double)x), MathHelper.func_76128_c((double)z)));
        }
    }

    public int size() {
        return this.tickets.size();
    }

    public void unload(int toRemove) {
        Iterator<Entity> ite = this.tickets.keySet().iterator();
        int i = 0;
        while (ite.hasNext()) {
            if (i >= toRemove) {
                return;
            }
            Entity entity = ite.next();
            ForgeChunkManager.releaseTicket((ForgeChunkManager.Ticket)this.tickets.get(entity));
            ite.remove();
            ++i;
        }
    }
}

