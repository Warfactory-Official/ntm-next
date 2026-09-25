// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKDebrisDigamma extends Block {

    public static final MapCodec<RBMKDebrisDigamma> CODEC = simpleCodec(RBMKDebrisDigamma::new);

    private static final int SPREAD_DELAY = 2;

    public RBMKDebrisDigamma(BlockBehaviour.Properties props) {
        super(props);
    }

    private static boolean isCorruptible(Block b) {
        return b == ModBlocks.RBMK_DEBRIS.get()
                || b == ModBlocks.RBMK_DEBRIS_BURNING.get()
                || b == ModBlocks.RBMK_DEBRIS_RADIATING.get()
                || b == ModBlocks.CORIUM.get()
                || b == ModBlocks.BLOCK_CORIUM.get();
    }

    @Override
    protected MapCodec<RBMKDebrisDigamma> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) schedule(server, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.VALUES) {
            BlockPos n = pos.relative(dir);
            if (isCorruptible(level.getBlockState(n).getBlock())) {
                level.setBlock(n, defaultBlockState(), 3);
            }
        }
    }

    private void schedule(ServerLevel level, BlockPos pos) {
        level.scheduleTick(pos, this, SPREAD_DELAY);
    }
}
