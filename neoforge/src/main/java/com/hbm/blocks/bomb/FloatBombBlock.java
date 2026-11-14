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

import com.hbm.explosion.ExplosionChaos;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class FloatBombBlock extends Block implements TriggerHandler {
    private static final int RADIUS = 15;
    private static final int LIFT = 50;

    public FloatBombBlock(Properties props) {
        super(props);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            trigger(level, pos, null);
        }
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {

        level.playSound(
                null,
                pos,
                ModSounds.GUN_SPARK_SHOOT.get(),
                SoundSource.BLOCKS,
                5.0F,
                level.getRandom().nextFloat() * 0.2F + 0.9F);

        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            ExplosionChaos.floater(level, pos, RADIUS, LIFT);
            ExplosionChaos.move(level, pos, RADIUS, 0, LIFT, 0);
        }

        return TriggerResult.DETONATED;
    }
}
