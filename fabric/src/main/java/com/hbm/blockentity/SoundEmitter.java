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

package com.hbm.blockentity;

import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

public interface SoundEmitter {
    BlockEntityTicker<BlockEntity> TICKER = SoundEmitter::tick;

    interface ManualTick {}

    boolean tickSound();

    boolean soundStarted(boolean active);

    static void tick(Level level, BlockPos pos, SoundEmitter sound) {
        boolean active = sound.tickSound();
        boolean started = sound.soundStarted(active);
        if (active && (started || TickPhase.every(level, pos, SharedConstants.TICKS_PER_SECOND))) {
            WorldSounds.machineNoise(level, pos);
        }
    }

    private static void tick(
            Level level, BlockPos pos, BlockState _, BlockEntity be) {
        tick(level, pos, (SoundEmitter) be);
    }
}
