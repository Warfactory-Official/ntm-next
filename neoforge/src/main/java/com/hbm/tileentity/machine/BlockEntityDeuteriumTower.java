// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityDeuteriumTower extends BlockEntityDeuteriumExtractor {

    public static final long MAX_POWER = 100_000L;

    public BlockEntityDeuteriumTower(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEUTERIUM_TOWER.get(), pos, state, 50_000, 5_000);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }
}
