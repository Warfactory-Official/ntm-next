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

package com.hbm.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class AudioSystem {

    private static volatile Factory factory = Factory.SERVER;

    private AudioSystem() {}

    public static void installClientFactory(Factory clientFactory) {
        factory = clientFactory;
    }

    public static void playMachineSound(
            Level level,
            BlockPos owner,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundSource category,
            float volume,
            float pitch) {
        if (!level.isClientSide()) return;
        factory.playMachineSound(
                owner,
                sound,
                category,
                volume,
                pitch,
                RandomSource.create(level.getRandom().nextLong()),
                x,
                y,
                z);
    }

    @Deprecated
    public static @Nullable SoundHandle getLoopedSound(
            @Nullable SoundEvent sound,
            SoundSource category,
            float x,
            float y,
            float z,
            float volume,
            float range,
            float pitch) {
        return factory.getLoopedSound(sound, category, x, y, z, volume, range, pitch);
    }

    public static @Nullable SoundHandle getLoopedSound(
            @Nullable SoundEvent sound,
            SoundSource category,
            float x,
            float y,
            float z,
            float volume,
            float range,
            float pitch,
            int keepAlive) {
        return factory.getLoopedSound(sound, category, x, y, z, volume, range, pitch, keepAlive);
    }

    public interface Factory {
        Factory SERVER = new Factory() {};

        default void playMachineSound(
                BlockPos owner,
                SoundEvent sound,
                SoundSource category,
                float volume,
                float pitch,
                RandomSource random,
                double x,
                double y,
                double z) {}

        default @Nullable SoundHandle getLoopedSound(
                @Nullable SoundEvent sound,
                SoundSource category,
                float x,
                float y,
                float z,
                float volume,
                float range,
                float pitch) {
            return null;
        }

        default @Nullable SoundHandle getLoopedSound(
                @Nullable SoundEvent sound,
                SoundSource category,
                float x,
                float y,
                float z,
                float volume,
                float range,
                float pitch,
                int keepAlive) {
            return null;
        }
    }
}
