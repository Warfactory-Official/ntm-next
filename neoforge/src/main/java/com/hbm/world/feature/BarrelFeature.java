// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.world.LocationIsValidSpawn;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BarrelFeature extends PlaceTemplateFeature {

    private static final List<BlockPos> CORNERS =
            List.of(
                    new BlockPos(0, 0, 0), new BlockPos(4, 0, 0),
                    new BlockPos(4, 0, 6), new BlockPos(0, 0, 6));

    public BarrelFeature() {
        super("barrel");
    }

    @Override
    protected List<BlockPos> guardOffsets() {
        return CORNERS;
    }

    @Override
    protected boolean isValidSpawn(WorldGenLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;

        BlockPos check = pos.below();
        BlockState checkState = level.getBlockState(check);
        BlockState belowState = level.getBlockState(check.below());
        return LocationIsValidSpawn.isValidGround(checkState, belowState, false);
    }

    @Override
    protected BlockPos templateOrigin(BlockPos featureOrigin) {
        return featureOrigin.below();
    }
}
