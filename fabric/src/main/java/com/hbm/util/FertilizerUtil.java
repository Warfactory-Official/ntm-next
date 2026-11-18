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

package com.hbm.util;

import com.hbm.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class FertilizerUtil {

    private FertilizerUtil() {}

    public static boolean fertilize(
            Level level,
            BlockPos pos,
            @Nullable Player player,
            ItemStack stack,
            boolean force,
            BonemealSource source) {
        BlockState state = level.getBlockState(pos);

        TriState event = Platform.bonemealEvent(player, level, pos, state, source, stack);
        if (event != TriState.DEFAULT) return event == TriState.TRUE;

        if (!(state.getBlock() instanceof BonemealableBlock growable)) return false;
        if (!growable.isValidBonemealTarget(level, pos, state, source)) return false;

        if (level instanceof ServerLevel server) {
            if (force || growable.isBonemealSuccess(level, level.getRandom(), pos, state, source)) {
                growable.performBonemeal(server, level.getRandom(), pos, state, source);
            }
        }

        return true;
    }

    public static void growthParticles(Level level, BlockPos pos) {
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 0);
    }
}
