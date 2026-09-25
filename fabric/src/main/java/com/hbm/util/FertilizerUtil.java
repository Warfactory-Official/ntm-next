// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class FertilizerUtil {

    private FertilizerUtil() {}

    public static boolean fertilize(
            Level level, BlockPos pos, @Nullable Player player, ItemStack stack, boolean force) {

        BlockState state = level.getBlockState(pos);

        TriState event = Services.PLATFORM.bonemealEvent(player, level, pos, state, stack);
        if (event != TriState.DEFAULT) return event == TriState.TRUE;

        if (!(state.getBlock() instanceof BonemealableBlock growable)) return false;
        if (!growable.isValidBonemealTarget(level, pos, state)) return false;

        if (level instanceof ServerLevel server) {
            if (force || growable.isBonemealSuccess(level, level.getRandom(), pos, state)) {
                growable.performBonemeal(server, level.getRandom(), pos, state);
            }
        }

        return true;
    }

    public static void growthParticles(Level level, BlockPos pos) {
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 0);
    }
}
