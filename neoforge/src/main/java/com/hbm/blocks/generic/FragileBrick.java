// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FragileBrick extends Block {

    public static final MapCodec<FragileBrick> CODEC = simpleCodec(FragileBrick::new);

    public FragileBrick(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.destroyBlock(pos, false, null, 512);
        notifyNeighbors(level, pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        if (level.isClientSide()) return;
        level.destroyBlock(pos, false, null, 512);
        notifyNeighbors(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        notifyNeighbors(level, pos);
    }

    private void notifyNeighbors(Level level, BlockPos pos) {
        for (Direction dir : Direction.VALUES) {
            BlockPos npos = pos.relative(dir);
            if (level.getBlockState(npos).is(this)) {
                level.scheduleTick(npos, this, level.getRandom().nextInt(4) + 8);
            }
        }
    }
}
