// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class AudioDynamic extends AbstractTickableSoundInstance {

    public float maxVolume = 1F;
    public float range = 10F;
    public int keepAlive;
    public int timeSinceKA;
    public boolean shouldExpire;
    public @Nullable Entity parentEntity;

    protected AudioDynamic(SoundEvent sound, SoundSource category) {
        super(sound, category, RandomSource.create());
        looping = true;
        attenuation = SoundInstance.Attenuation.NONE;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void attachTo(Entity entity) {
        parentEntity = entity;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;

        if (parentEntity != null && player != parentEntity) {
            setPosition(
                    (float) parentEntity.getX(),
                    (float) parentEntity.getY(),
                    (float) parentEntity.getZ());
        }

        if (player != null && player != parentEntity) {
            volume = volumeAtDistance(distanceTo(player));
        } else {
            if (player != null && player == parentEntity) {
                setPosition(
                        (float) parentEntity.getX(),
                        (float) parentEntity.getY() + 10F,
                        (float) parentEntity.getZ());
            }
            volume = maxVolume;
        }

        if (shouldExpire) {
            if (timeSinceKA > keepAlive) {
                stop();
            }
            timeSinceKA++;
        }
    }

    private float distanceTo(LocalPlayer player) {
        double dx = x - player.getX();
        double dy = y - player.getEyeY();
        double dz = z - player.getZ();
        return (float) Mth.length(dx, dy, dz);
    }

    public void start() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();

        if (!manager.isActive(this)) {
            manager.play(this);
        }
    }

    public void stopSound() {
        stop();
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    public void setVolume(float volume) {
        maxVolume = volume;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        shouldExpire = true;
    }

    public void keepAlive() {
        timeSinceKA = 0;
    }

    public float volumeAtDistance(float distance) {
        return (distance / range) * -maxVolume + maxVolume;
    }

    public boolean isPlaying() {
        return Minecraft.getInstance().getSoundManager().isActive(this);
    }

    public float rawVolume() {
        return volume;
    }

    public float rawPitch() {
        return pitch;
    }
}
