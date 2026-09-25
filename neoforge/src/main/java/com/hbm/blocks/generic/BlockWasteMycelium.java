// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.RadiationConfig;
import com.hbm.data.WorldData;
import com.hbm.potion.HbmPotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockWasteMycelium extends WasteEarth {

    public BlockWasteMycelium(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (WorldData.ENABLE_MYCELIUM.get()) {
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    for (int k = -1; k < 2; k++) {
                        BlockPos p = pos.offset(i, j, k);
                        if (level.getBlockState(p.above()).canOcclude()) continue;
                        BlockState g = level.getBlockState(p);
                        if (g.is(Blocks.DIRT)
                                || g.is(Blocks.GRASS_BLOCK)
                                || g.is(Blocks.MYCELIUM)
                                || g.is(ModBlocks.WASTE_EARTH.get())) {
                            level.setBlockAndUpdate(p, defaultBlockState());
                        }
                    }
                }
            }
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 30 * 20, 3));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {

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
