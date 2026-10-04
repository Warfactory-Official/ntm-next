// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKReflector;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKReflector extends RBMKBase {

    public RBMKReflector(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends BlockEntityRBMKBase> beType() {
        return ModBlockEntities.RBMK_REFLECTOR.get();
    }

    @Override
    protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKReflector(pos, state);
    }
}
