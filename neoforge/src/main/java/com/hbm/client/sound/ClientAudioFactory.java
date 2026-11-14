/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.sound;

import com.hbm.sound.AudioSystem;
import com.hbm.sound.SoundHandle;
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
    public SoundHandle getLoopedSound(
            @Nullable SoundEvent sound,
            SoundSource category,
            float x,
            float y,
            float z,
            float volume,
            float range,
            float pitch) {
        AudioWrapperClient audio = new AudioWrapperClient(sound, category);
        audio.moveTo(x, y, z);
        audio.setVolume(volume);
        audio.setRange(range);
        audio.setPitch(pitch);
        return audio;
    }

    @Override
    public SoundHandle getLoopedSound(
            @Nullable SoundEvent sound,
            SoundSource category,
            float x,
            float y,
            float z,
            float volume,
            float range,
            float pitch,
            int keepAlive) {
        SoundHandle audio = getLoopedSound(sound, category, x, y, z, volume, range, pitch);
        audio.setTimeout(keepAlive);
        return audio;
    }
}
