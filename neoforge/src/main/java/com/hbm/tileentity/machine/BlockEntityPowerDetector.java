// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.PowerDetectorBlock;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityPowerDetector extends BlockEntity
        implements GraphResident, FoldedCoreResident, IEnergyHandlerMK2 {

    private long power;

    public BlockEntityPowerDetector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HE_DETECTOR.get(), pos, state);
    }

    public void tickServer() {
        boolean lit = power > 0;
        if (power > 0) power--;

        BlockState state = getBlockState();
        if (state.getValue(PowerDetectorBlock.POWERED) != lit) {
            level.setBlock(
                    worldPosition,
                    state.setValue(PowerDetectorBlock.POWERED, lit),
                    Block.UPDATE_ALL);
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return 5;
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.HIGH;
    }
}
