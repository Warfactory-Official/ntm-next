// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.network.BlockEntityFluidCounterValve;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FluidCounterValveBlock extends FluidValveBlock implements ITickingBlock {

    public FluidCounterValveBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFluidCounterValve(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        super.buildLookOverlay(level, pos, info);
        if (level.getBlockEntity(pos) instanceof BlockEntityFluidCounterValve valve) {
            info.line("Counter: " + valve.getCounter());
        }
    }
}
