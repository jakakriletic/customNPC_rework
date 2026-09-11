/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.math.ChunkPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraftforge.common.ForgeChunkManager
 *  net.minecraftforge.common.ForgeChunkManager$Ticket
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.ForgeChunkManager;
import noppes.npcs.controllers.ChunkController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;

public class JobChunkLoader
extends JobInterface {
    private List<ChunkPos> chunks = new ArrayList<ChunkPos>();
    private int ticks = 20;
    private long playerLastSeen = 0L;

    public JobChunkLoader(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setLong("ChunkPlayerLastSeen", this.playerLastSeen);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.playerLastSeen = compound.getLong("ChunkPlayerLastSeen");
    }

    @Override
    public boolean aiShouldExecute() {
        --this.ticks;
        if (this.ticks > 0) {
            return false;
        }
        this.ticks = 20;
        List players = this.npc.world.getEntitiesWithinAABB(EntityPlayer.class, this.npc.getEntityBoundingBox().grow(48.0, 48.0, 48.0));
        if (!players.isEmpty()) {
            this.playerLastSeen = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() > this.playerLastSeen + 600000L) {
            ChunkController.instance.deleteNPC(this.npc);
            this.chunks.clear();
            return false;
        }
        ForgeChunkManager.Ticket ticket = ChunkController.instance.getTicket(this.npc);
        if (ticket == null) {
            return false;
        }
        double x = this.npc.posX / 16.0;
        double z = this.npc.posZ / 16.0;
        ArrayList<ChunkPos> list = new ArrayList<ChunkPos>();
        list.add(new ChunkPos(MathHelper.floor((double)x), MathHelper.floor((double)z)));
        list.add(new ChunkPos(MathHelper.ceil((double)x), MathHelper.ceil((double)z)));
        list.add(new ChunkPos(MathHelper.floor((double)x), MathHelper.ceil((double)z)));
        list.add(new ChunkPos(MathHelper.ceil((double)x), MathHelper.floor((double)z)));
        for (ChunkPos chunk : list) {
            if (!this.chunks.contains(chunk)) {
                ForgeChunkManager.forceChunk((ForgeChunkManager.Ticket)ticket, (ChunkPos)chunk);
                continue;
            }
            this.chunks.remove(chunk);
        }
        for (ChunkPos chunk : this.chunks) {
            ForgeChunkManager.unforceChunk((ForgeChunkManager.Ticket)ticket, (ChunkPos)chunk);
        }
        this.chunks = list;
        return false;
    }

    @Override
    public boolean aiContinueExecute() {
        return false;
    }

    @Override
    public void reset() {
        ChunkController.instance.deleteNPC(this.npc);
        this.chunks.clear();
        this.playerLastSeen = 0L;
    }

    @Override
    public void delete() {
    }
}

