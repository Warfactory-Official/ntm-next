// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MobData;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityChimneyIndustrial extends BlockEntityChimneyBase {

    public BlockEntityChimneyIndustrial(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIMNEY_INDUSTRIAL.get(), pos, state);
    }

    @Override
    public boolean capturesSoot() {
        return true;
    }

    @Override
    public int plumeHeight() {
        return 22;
    }

    @Override
    public float plumeBaseScale() {
        return 0.75F;
    }

    @Override
    public double getPollutionMod() {
        return MobData.RAMPANT_MODE.get() ? MobData.RAMPANT_SMOKE_STACK_OVERRIDE.get() / 2D : 0.1D;
    }
}
