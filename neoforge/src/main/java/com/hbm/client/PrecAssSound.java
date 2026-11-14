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

import com.hbm.blockentity.machine.PrecisionAssemblerBlockEntity;
import com.hbm.client.sound.MachineSoundInstance;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class PrecAssSound {

    private PrecAssSound() {}

    public static void tick(PrecisionAssemblerBlockEntity be) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.isProgressing
                        && me != null
                        && me.getEyePosition().distanceToSqr(pos.getX(), pos.getY(), pos.getZ())
                                < 50 * 50,
                0.5F,
                0.75F);
    }

    public static void installOneShots() {
        PrecisionAssemblerBlockEntity.CLIENT_SOUND_START =
                (be, pitch) -> {
                    if (!be.isMuffled())
                        play(
                                be,
                                ModSounds.ASSEMBLER_START.get(),
                                be.getVolume(0.25F),
                                (float) pitch);
                };
        PrecisionAssemblerBlockEntity.CLIENT_SOUND_STRIKE =
                be -> {
                    if (!be.isMuffled())
                        play(be, ModSounds.ASSEMBLER_STRIKE.get(), be.getVolume(0.5F), 1.25F);
                };
        PrecisionAssemblerBlockEntity.CLIENT_SOUND_STOP =
                (be, pitch) -> {
                    if (!be.isMuffled())
                        play(
                                be,
                                ModSounds.ASSEMBLER_STOP.get(),
                                be.getVolume(0.25F),
                                (float) pitch);
                };
    }

    private static void play(
            PrecisionAssemblerBlockEntity be, SoundEvent sound, float volume, float pitch) {
        BlockPos pos = be.getBlockPos();
        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        new MachineSoundInstance(
                                pos,
                                sound,
                                SoundSource.BLOCKS,
                                volume,
                                pitch,
                                SoundInstance.createUnseededRandom(),
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5));
    }
}
