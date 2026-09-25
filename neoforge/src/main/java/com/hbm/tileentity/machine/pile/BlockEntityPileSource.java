// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityPileSource extends BlockEntityPileBase {

    public BlockEntityPileSource(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_SOURCE.get(), pos, state);
    }

    @Override
    public void tickServer() {

        int n = getBlockState().is(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()) ? 1 : 2;

        for (int i = 0; i < 12; i++) this.castRay(n);
    }
}
