// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
    public static @Nullable AudioWrapper getLoopedSound(
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

    public static @Nullable AudioWrapper getLoopedSound(
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

        default @Nullable AudioWrapper getLoopedSound(
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

        default @Nullable AudioWrapper getLoopedSound(
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
