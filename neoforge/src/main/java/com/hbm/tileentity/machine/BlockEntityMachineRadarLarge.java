// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachineRadarLarge extends BlockEntityMachineRadar {

    public BlockEntityMachineRadarLarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_LARGE.get(), pos, state);
    }

    @Override
    public int getRange() {
        return MachineData.RADAR_LARGE_RANGE.get();
    }

    @Override
    protected void notifyRedstone() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos cell = worldPosition.relative(dir);
            level.updateNeighborsAt(cell, level.getBlockState(cell).getBlock());
        }
    }
}
