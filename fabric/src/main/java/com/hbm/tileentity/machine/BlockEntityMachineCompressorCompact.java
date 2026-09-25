// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachineCompressorCompact extends BlockEntityMachineCompressor {

    public BlockEntityMachineCompressorCompact(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR_COMPACT.get(), pos, state);
    }

    @Override
    protected boolean syncMuffled() {
        return false;
    }

    @Override
    public void tickClient() {
        prevFanSpin = fanSpin;

        if (!isOn) return;

        fanSpin += 45;
        if (fanSpin >= 360) {
            prevFanSpin -= 360;
            fanSpin -= 360;
        }
    }
}
