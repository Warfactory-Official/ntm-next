// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineRadar;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class RadarMastCell extends BlockMultiblockCell {

    public RadarMastCell(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos.relative(direction))
                        instanceof BlockEntityMachineRadar radar
                ? radar.getRedPower()
                : 0;
    }

    @Override
    protected int getDirectSignal(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }
}
