// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.sound;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class MachineSoundInstance extends SimpleSoundInstance {
    public final long machineCore;

    public MachineSoundInstance(
            BlockPos owner,
            SoundEvent sound,
            SoundSource category,
            float volume,
            float pitch,
            RandomSource random,
            double x,
            double y,
            double z) {
        super(sound, category, volume, pitch, random, x, y, z);
        machineCore = owner.asLong();
    }
}
