// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.deco;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockEntityObjTester extends BlockEntity {

    public BlockEntityObjTester(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OBJ_TESTER.get(), pos, state);
    }
}
