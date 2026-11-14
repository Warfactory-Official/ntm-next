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

package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeAssemblyBlockEntity;
import com.hbm.interfaces.TriggerResult;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class NukeAssemblyBlock extends BombRotatableBlock {

    protected NukeAssemblyBlock(Properties props) {
        super(props);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return TriggerResult.NONE;
        if (!(level.getBlockEntity(pos) instanceof NukeAssemblyBlockEntity assembly)) {
            return TriggerResult.NONE;
        }

        int yield = assembly.yield();
        if (yield < 0) return TriggerResult.MISSING_COMPONENT;

        assembly.clearSlots();
        level.removeBlock(pos, false);
        ignite(level, pos, yield, detonator);
        return TriggerResult.DETONATED;
    }

    protected static void playBoom(Level level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    protected abstract void ignite(
            Level level, BlockPos pos, int yield, @Nullable Entity detonator);
}
