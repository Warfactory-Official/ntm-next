// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class TickPhase {

    private TickPhase() {}

    public static boolean every(BlockEntity be, long period) {
        Level level = be.getLevel();
        return level != null && every(level, be.getBlockPos(), period);
    }

    public static boolean every(Entity entity, long period) {
        return every(entity.level().getGameTime(), entity.getId(), period);
    }

    public static boolean every(long gameTime, int entityId, long period) {
        return period <= 1 || Math.floorMod(gameTime + entityId, period) == 0L;
    }

    public static boolean every(Level level, BlockPos pos, long period) {
        return every(level.getGameTime(), pos, period);
    }

    public static boolean every(long gameTime, BlockPos pos, long period) {
        return period <= 1 || (gameTime + phase(pos, period)) % period == 0;
    }

    private static long phase(BlockPos pos, long period) {
        int h = pos.hashCode();
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        return Math.floorMod((long) h, period);
    }
}
