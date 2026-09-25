// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityHeatBoilerIndustrial extends BlockEntityHeatBoilerBase {

    public static final int WATER_CAP = 64_000;

    public BlockEntityHeatBoilerIndustrial(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_BOILER_INDUSTRIAL.get(), pos, state, WATER_CAP);
    }

    @Override
    protected int maxHeat() {
        return MachineData.BOILER_INDUSTRIAL_MAX_HEAT.get();
    }

    @Override
    protected double diffusion() {
        return MachineData.BOILER_INDUSTRIAL_DIFFUSION.get();
    }

    @Override
    protected boolean canExplode() {
        return false;
    }
}
