// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockNetherCoal extends BlockOutgas {

    public BlockNetherCoal(BlockBehaviour.Properties props) {
        super(props, () -> ModBlocks.GAS_MONOXIDE.get(), true);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(3.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        for (Direction dir : Direction.VALUES) {
            if (dir == Direction.DOWN || !level.getBlockState(pos.relative(dir)).isAir()) continue;

            double ix = pos.getX() + 0.5F + dir.getStepX() + rand.nextDouble() - 0.5D;
            double iy = pos.getY() + 0.5F + dir.getStepY() + rand.nextDouble() - 0.5D;
            double iz = pos.getZ() + 0.5F + dir.getStepZ() + rand.nextDouble() - 0.5D;

            if (dir.getStepX() != 0)
                ix =
                        pos.getX()
                                + 0.5F
                                + dir.getStepX() * 0.5
                                + rand.nextDouble() * 0.125 * dir.getStepX();
            if (dir.getStepY() != 0)
                iy =
                        pos.getY()
                                + 0.5F
                                + dir.getStepY() * 0.5
                                + rand.nextDouble() * 0.125 * dir.getStepY();
            if (dir.getStepZ() != 0)
                iz =
                        pos.getZ()
                                + 0.5F
                                + dir.getStepZ() * 0.5
                                + rand.nextDouble() * 0.125 * dir.getStepZ();

            level.addParticle(ParticleTypes.FLAME, ix, iy, iz, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.SMOKE, ix, iy, iz, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.SMOKE, ix, iy, iz, 0.0, 0.1, 0.0);
        }
    }
}
