// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.sound;

import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

public final class ClientAudioFactory implements AudioSystem.Factory {

    public static final ClientAudioFactory INSTANCE = new ClientAudioFactory();

    private ClientAudioFactory() {}

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
