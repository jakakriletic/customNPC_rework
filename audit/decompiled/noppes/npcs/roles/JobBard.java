/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.entity.data.role.IJobBard;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;

public class JobBard
extends JobInterface
implements IJobBard {
    public int minRange = 2;
    public int maxRange = 64;
    public boolean isStreamer = true;
    public boolean hasOffRange = true;
    public String song = "";
    private long ticks = 0L;

    public JobBard(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.func_74778_a("BardSong", this.song);
        nbttagcompound.func_74768_a("BardMinRange", this.minRange);
        nbttagcompound.func_74768_a("BardMaxRange", this.maxRange);
        nbttagcompound.func_74757_a("BardStreamer", this.isStreamer);
        nbttagcompound.func_74757_a("BardHasOff", this.hasOffRange);
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        this.song = nbttagcompound.func_74779_i("BardSong");
        this.minRange = nbttagcompound.func_74762_e("BardMinRange");
        this.maxRange = nbttagcompound.func_74762_e("BardMaxRange");
        this.isStreamer = nbttagcompound.func_74767_n("BardStreamer");
        this.hasOffRange = nbttagcompound.func_74767_n("BardHasOff");
    }

    public void onLivingUpdate() {
        List list;
        if (!this.npc.isRemote() || this.song.isEmpty()) {
            return;
        }
        if (!MusicController.Instance.isPlaying(this.song)) {
            List list2 = this.npc.field_70170_p.func_72872_a(EntityPlayer.class, this.npc.func_174813_aQ().func_72314_b((double)this.minRange, (double)(this.minRange / 2), (double)this.minRange));
            if (!list2.contains(CustomNpcs.proxy.getPlayer())) {
                return;
            }
            if (this.isStreamer) {
                MusicController.Instance.playStreaming(this.song, (Entity)this.npc);
            } else {
                MusicController.Instance.playMusic(this.song, (Entity)this.npc);
            }
        } else if (MusicController.Instance.playingEntity != this.npc) {
            EntityPlayer player = CustomNpcs.proxy.getPlayer();
            if (this.npc.func_70068_e((Entity)player) < MusicController.Instance.playingEntity.func_70068_e((Entity)player)) {
                MusicController.Instance.playingEntity = this.npc;
            }
        } else if (this.hasOffRange && !(list = this.npc.field_70170_p.func_72872_a(EntityPlayer.class, this.npc.func_174813_aQ().func_72314_b((double)this.maxRange, (double)(this.maxRange / 2), (double)this.maxRange))).contains(CustomNpcs.proxy.getPlayer())) {
            MusicController.Instance.stopMusic();
        }
        if (MusicController.Instance.isPlaying(this.song)) {
            Minecraft.func_71410_x().func_181535_r().field_147676_d = 12000;
        }
    }

    @Override
    public void killed() {
        this.delete();
    }

    @Override
    public void delete() {
        if (this.npc.field_70170_p.field_72995_K && this.hasOffRange && MusicController.Instance.isPlaying(this.song)) {
            MusicController.Instance.stopMusic();
        }
    }

    @Override
    public String getSong() {
        return this.song;
    }

    @Override
    public void setSong(String song) {
        this.song = song;
        this.npc.updateClient = true;
    }
}

