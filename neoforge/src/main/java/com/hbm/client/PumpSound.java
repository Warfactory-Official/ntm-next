// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.blockentity.machine.BlockEntityMachinePumpBase;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class PumpSound {

    private PumpSound() {}

    public static void tick(BlockEntityMachinePumpBase be) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockPos pos = be.getBlockPos();
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        AudioSystem.playMachineSound(
                mc.level,
                pos,
                x,
                y,
                z,
                ModSounds.STEAM_ENGINE_OPERATE.get(),
                SoundSource.BLOCKS,
                0.5F,
                0.75F);
        AudioSystem.playMachineSound(
                mc.level, pos, x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 0.5F);
    }
}
