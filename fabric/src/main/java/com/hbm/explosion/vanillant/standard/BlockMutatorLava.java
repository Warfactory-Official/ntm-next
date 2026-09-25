// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorLava implements IBlockMutator {

    private final Block lava;

    public BlockMutatorLava(Block lava) {
        this.lava = lava;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos) {

        if (blockState.isSolidRender()) {
            explosion.world.setBlock(pos, lava.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {}
}
