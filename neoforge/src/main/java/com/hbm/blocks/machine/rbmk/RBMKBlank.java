// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKBlank;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKBlank extends RBMKBase {

    public RBMKBlank(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends BlockEntityRBMKBase> beType() {
        return ModBlockEntities.RBMK_BLANK.get();
    }

    @Override
    protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKBlank(pos, state);
    }
}
