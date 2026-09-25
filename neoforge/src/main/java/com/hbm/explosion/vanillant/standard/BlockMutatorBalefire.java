// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorBalefire implements IBlockMutator {

    protected Supplier<BlockState> target;

    public BlockMutatorBalefire() {
        this(() -> ModBlocks.BALEFIRE.get().defaultBlockState());
    }

    public BlockMutatorBalefire(BlockState target) {
        this(() -> target);
    }

    public BlockMutatorBalefire(Supplier<BlockState> target) {
        this.target = target;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos) {}

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {

        Level world = explosion.world;
        if (world.getBlockState(pos).isAir()
                && world.getBlockState(pos.below()).isSolidRender()
                && world.getRandom().nextInt(3) == 0) {
            world.setBlock(pos, target.get(), 3);
        }
    }
}
