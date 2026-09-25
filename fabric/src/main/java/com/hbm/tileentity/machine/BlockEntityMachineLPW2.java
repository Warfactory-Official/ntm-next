// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachineLPW2 extends BlockEntity {

    public BlockEntityMachineLPW2(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LPW2.get(), pos, state);
    }
}
