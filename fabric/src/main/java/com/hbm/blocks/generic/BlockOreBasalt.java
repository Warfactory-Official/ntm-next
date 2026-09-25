// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockOreBasalt extends Block {

    private final Supplier<Block> gas;

    public BlockOreBasalt(Properties props, Supplier<Block> gas) {
        super(props);
        this.gas = gas;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir()) {
            RandomSource rand = level.getRandom();
            if (level instanceof ServerLevel server) {
                if (rand.nextInt(10) == 0)
                    server.setBlockAndUpdate(above, gas.get().defaultBlockState());
            } else {

                for (int i = 0; i < 5; i++) {
                    level.addParticle(
                            ParticleTypes.MYCELIUM,
                            pos.getX() + rand.nextFloat(),
                            pos.getY() + 1.1,
                            pos.getZ() + rand.nextFloat(),
                            0.0,
                            0.0,
                            0.0);
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void spawnAfterBreak(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            ItemStack tool,
            boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        level.setBlockAndUpdate(pos, gas.get().defaultBlockState());
    }
}
