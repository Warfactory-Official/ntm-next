// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorDigamma implements IBlockMutator {

    private final boolean circuit;
    private final LongOpenHashSet solidCubes = new LongOpenHashSet();

    public BlockMutatorDigamma(boolean circuit) {
        this.circuit = circuit;
    }

    private static void ash(Level world, BlockPos pos, RandomSource rand) {
        world.setBlock(pos, ModBlocks.ASH_DIGAMMA.get().defaultBlockState(), 3);
        BlockPos above = pos.above();
        if (rand.nextInt(5) == 0 && world.getBlockState(above).isAir()) {
            world.setBlock(above, ModBlocks.FIRE_DIGAMMA.get().defaultBlockState(), 3);
        }
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos) {

        if (blockState.isSolidRender()) solidCubes.add(pos.asLong());
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        if (!solidCubes.contains(pos.asLong())) return;
        Level world = explosion.world;
        RandomSource rand = world.getRandom();

        if (circuit) {

            int x = pos.getX(), z = pos.getZ();
            boolean onGrid = x % 3 == 0 && z % 3 == 0;
            boolean nearGrid = (x % 3 == 0 || z % 3 == 0) && rand.nextBoolean();
            if (onGrid || nearGrid) {
                world.setBlock(pos, ModBlocks.RBMK_DEBRIS_DIGAMMA.get().defaultBlockState(), 3);
                return;
            }
        }
        ash(world, pos, rand);
    }
}
