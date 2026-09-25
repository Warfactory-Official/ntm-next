// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSmolder extends Block {

    public BlockSmolder(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(3.0F);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!level.getBlockState(pos.above()).isAir()) return;

        double y = pos.getY() + 1.1D;
        level.addParticle(
                ParticleTypes.LAVA,
                pos.getX() + 0.25D + rand.nextDouble() * 0.5D,
                y,
                pos.getZ() + 0.25D + rand.nextDouble() * 0.5D,
                0.0D,
                0.0D,
                0.0D);
        level.addParticle(
                ParticleTypes.FLAME,
                pos.getX() + 0.25D + rand.nextDouble() * 0.5D,
                y,
                pos.getZ() + 0.25D + rand.nextDouble() * 0.5D,
                0.0D,
                0.0D,
                0.0D);
    }
}
