// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.platform.Services;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public final class BlockReadBounds {
    private static final boolean CLIENT = Services.PLATFORM.isPhysicalClient();

    private BlockReadBounds() {}

    public static boolean canRead(BlockGetter view, BlockPos pos) {
        return canRead(view, pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean canRead(BlockGetter view, int x, int y, int z) {
        return !CLIENT || Client.canRead(view, x, y, z);
    }

    private static final class Client {
        private static boolean canRead(BlockGetter view, int blockX, int blockY, int blockZ) {
            if (!(view instanceof RenderSectionRegion region)) return true;
            int x = (blockX >> 4) - region.minSectionX;
            int y = (blockY >> 4) - region.minSectionY;
            int z = (blockZ >> 4) - region.minSectionZ;
            return x >= 0
                    && x < RenderSectionRegion.SIZE
                    && y >= 0
                    && y < RenderSectionRegion.SIZE
                    && z >= 0
                    && z < RenderSectionRegion.SIZE;
        }
    }
}
