// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.sound;

import com.hbm.sound.AudioWrapper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class AudioWrapperClient extends AudioWrapper {

    protected final @Nullable AudioDynamic sound;

    public AudioWrapperClient(@Nullable SoundEvent source, SoundSource category) {
        sound = source != null ? new AudioDynamic(source, category) : null;
    }

    @Override
    public void setKeepAlive(int keepAlive) {
        if (sound != null) {
            sound.setKeepAlive(keepAlive);
        }
    }

    @Override
    public void keepAlive() {
        if (sound != null) {
            sound.keepAlive();
        }
    }

    @Override
    public void updatePosition(float x, float y, float z) {
        if (sound != null) {
            sound.setPosition(x, y, z);
        }
    }

    @Override
    public void attachTo(Entity entity) {
        if (sound != null) {
            sound.attachTo(entity);
        }
    }

    @Override
    public void updateVolume(float volume) {
        if (sound != null) {
            sound.setVolume(volume);
        }
    }

    @Override
    public void updateRange(float range) {
        if (sound != null) {
            sound.setRange(range);
        }
    }

    @Override
    public void updatePitch(float pitch) {
        if (sound != null) {
            sound.setPitch(pitch);
        }
    }

    @Override
    public float getVolume() {
        return sound != null ? sound.rawVolume() : 1F;
    }

    @Override
    public float getPitch() {
        return sound != null ? sound.rawPitch() : 1F;
    }

    @Override
    public void setDoesRepeat(boolean repeats) {
        if (sound != null) {
            sound.setLooping(repeats);
        }
    }

    @Override
    public boolean isRepeating() {
        return sound != null && sound.isLooping();
    }

    @Override
    public void startSound() {
        if (sound != null) {
            sound.start();
        }
    }

    @Override
    public void stopSound() {
        if (sound != null) {
            sound.stopSound();
            sound.setKeepAlive(0);
        }
    }

    @Override
    public boolean isPlaying() {
        return sound != null && sound.isPlaying();
    }
}
