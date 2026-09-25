// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKModerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKModerator extends RBMKBase {

    public RBMKModerator(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends BlockEntityRBMKBase> beType() {
        return ModBlockEntities.RBMK_MODERATOR.get();
    }

    @Override
    protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKModerator(pos, state);
    }
}
