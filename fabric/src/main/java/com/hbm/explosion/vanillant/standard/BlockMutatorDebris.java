// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorDebris implements IBlockMutator {

    protected Supplier<BlockState> target;

    public BlockMutatorDebris(BlockState target) {
        this(() -> target);
    }

    public BlockMutatorDebris(Supplier<BlockState> target) {
        this.target = target;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos) {}

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {

        Level world = explosion.world;
        BlockState targetState = target.get();

        for (Direction dir : Direction.VALUES) {
            BlockState state = world.getBlockState(pos.relative(dir));
            if (state.isSolidRender() && state != targetState) {
                world.setBlock(pos, targetState, 3);
                return;
            }
        }
    }
}
