// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class LibraryDungeonFeature extends PlaceTemplateFeature {

    private static final List<BlockPos> CORNERS =
            List.of(
                    new BlockPos(0, 0, 0), new BlockPos(8, 0, 0),
                    new BlockPos(8, 0, 10), new BlockPos(0, 0, 10));

    public LibraryDungeonFeature() {
        super("library_dungeon");
    }

    @Override
    protected List<BlockPos> guardOffsets() {
        return CORNERS;
    }

    @Override
    protected boolean isValidSpawn(WorldGenLevel level, BlockPos pos) {
        if (pos.getY() <= 5) return false;
        BlockState above = level.getBlockState(pos.above(8));
        BlockState below = level.getBlockState(pos.below());
        return isSolid(above) && isSolid(below);
    }
}
