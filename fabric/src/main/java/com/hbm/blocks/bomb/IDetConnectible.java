// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

public interface IDetConnectible {

    static boolean isConnectible(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos).getBlock() instanceof IDetConnectible connectible
                && connectible.canConnectToDetCord(level, pos, dir);
    }

    default boolean canConnectToDetCord(BlockGetter level, BlockPos pos, Direction dir) {
        return true;
    }
}
