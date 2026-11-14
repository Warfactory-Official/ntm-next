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

package com.hbm.client;

import com.hbm.blockentity.machine.PumpBlockEntity;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class PumpSound {

    private PumpSound() {}

    public static void tick(PumpBlockEntity be) {
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
