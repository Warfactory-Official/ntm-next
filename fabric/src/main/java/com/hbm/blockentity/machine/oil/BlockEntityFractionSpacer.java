// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.oil;

import com.hbm.blockentity.FoldedCoreResident;
import com.hbm.blockentity.GraphResident;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFractionSpacer extends BlockEntity
        implements GraphResident, FoldedCoreResident {

    public BlockEntityFractionSpacer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACTION_SPACER.get(), pos, state);
    }
}
