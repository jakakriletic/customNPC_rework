/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.audio.ISound
 *  net.minecraft.client.audio.ISound$AttenuationType
 *  net.minecraft.client.audio.PositionedSoundRecord
 *  net.minecraft.client.audio.SoundHandler
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundCategory
 */
package noppes.npcs.client.controllers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

public class MusicController {
    public static MusicController Instance;
    public PositionedSoundRecord playing;
    public ResourceLocation playingResource;
    public Entity playingEntity;

    public MusicController() {
        Instance = this;
    }

    public void stopMusic() {
        SoundHandler handler = Minecraft.func_71410_x().func_147118_V();
        if (this.playing != null) {
            handler.func_147683_b((ISound)this.playing);
        }
        handler.func_189520_a("", SoundCategory.MUSIC);
        handler.func_189520_a("", SoundCategory.AMBIENT);
        handler.func_189520_a("", SoundCategory.RECORDS);
        this.playingResource = null;
        this.playingEntity = null;
        this.playing = null;
    }

    public void playStreaming(String music, Entity entity) {
        if (this.isPlaying(music)) {
            return;
        }
        this.stopMusic();
        this.playingEntity = entity;
        this.playingResource = new ResourceLocation(music);
        SoundHandler handler = Minecraft.func_71410_x().func_147118_V();
        this.playing = new PositionedSoundRecord(this.playingResource, SoundCategory.RECORDS, 4.0f, 1.0f, false, 0, ISound.AttenuationType.LINEAR, (float)entity.field_70165_t, (float)entity.field_70163_u, (float)entity.field_70161_v);
        handler.func_147682_a((ISound)this.playing);
    }

    public void playMusic(String music, Entity entity) {
        if (this.isPlaying(music)) {
            return;
        }
        this.stopMusic();
        this.playingResource = new ResourceLocation(music);
        this.playingEntity = entity;
        SoundHandler handler = Minecraft.func_71410_x().func_147118_V();
        this.playing = new PositionedSoundRecord(this.playingResource, SoundCategory.MUSIC, 1.0f, 1.0f, false, 0, ISound.AttenuationType.NONE, 0.0f, 0.0f, 0.0f);
        handler.func_147682_a((ISound)this.playing);
    }

    public boolean isPlaying(String music) {
        ResourceLocation resource = new ResourceLocation(music);
        if (this.playingResource == null || !this.playingResource.equals((Object)resource)) {
            return false;
        }
        return Minecraft.func_71410_x().func_147118_V().func_147692_c((ISound)this.playing);
    }

    public void playSound(SoundCategory cat, String music, int x, int y, int z, float volumne, float pitch) {
        PositionedSoundRecord rec = new PositionedSoundRecord(new ResourceLocation(music), cat, volumne, pitch, false, 0, ISound.AttenuationType.LINEAR, (float)x + 0.5f, (float)y, (float)z + 0.5f);
        Minecraft.func_71410_x().func_147118_V().func_147682_a((ISound)rec);
    }
}

