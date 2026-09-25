// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MobData;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityChimneyBrick extends BlockEntityChimneyBase {

    public BlockEntityChimneyBrick(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIMNEY_BRICK.get(), pos, state);
    }

    @Override
    public int plumeHeight() {
        return 12;
    }

    @Override
    public float plumeBaseScale() {
        return 0.5F;
    }

    @Override
    public double getPollutionMod() {
        return MobData.RAMPANT_MODE.get() ? MobData.RAMPANT_SMOKE_STACK_OVERRIDE.get() : 0.25D;
    }
}
