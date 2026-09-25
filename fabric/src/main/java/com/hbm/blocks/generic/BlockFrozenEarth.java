// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockFrozenEarth extends Block {

    private static final int SLOWDOWN_TICKS = 2 * 60 * 20;
    private static final int SLOWDOWN_AMPLIFIER = 2;

    public BlockFrozenEarth(Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (entity instanceof LivingEntity living) {
            living.addEffect(
                    new MobEffectInstance(MobEffects.SLOWNESS, SLOWDOWN_TICKS, SLOWDOWN_AMPLIFIER));
        }
    }
}
