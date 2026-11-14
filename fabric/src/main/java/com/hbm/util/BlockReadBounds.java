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

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public final class BlockReadBounds {
    private static final boolean CLIENT =
            FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;

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
