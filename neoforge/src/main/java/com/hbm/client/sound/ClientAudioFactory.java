// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.sound;

import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public final class ClientAudioFactory implements AudioSystem.Factory {

    public static final ClientAudioFactory INSTANCE = new ClientAudioFactory();

    private ClientAudioFactory() {}

    @Override
    public void playMachineSound(
            BlockPos owner,
            SoundEvent sound,
            SoundSource category,
            float volume,
            float pitch,
            RandomSource random,
            double x,
            double y,
            double z) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        new MachineSoundInstance(
                                owner, sound, category, volume, pitch, random, x, y, z));
    }

    @Deprecated
    @Override
    public AudioWrapper getLoopedSound(
            @Nullable SoundEvent sound,
            SoundSource category,
            float x,
            float y,
            float z,
            float volume,
            float range,
            float pitch) {
        AudioWrapperClient audio = new AudioWrapperClient(sound, category);
        audio.updatePosition(x, y, z);
        audio.updateVolume(volume);
        audio.updateRange(range);
        audio.updatePitch(pitch);
        return audio;
    }

    @Override
    public AudioWrapper getLoopedSound(
            @Nullable SoundEvent sound,
            SoundSource category,
            float x,
            float y,
            float z,
            float volume,
            float range,
            float pitch,
            int keepAlive) {
        AudioWrapper audio = getLoopedSound(sound, category, x, y, z, volume, range, pitch);
        audio.setKeepAlive(keepAlive);
        return audio;
    }
}
