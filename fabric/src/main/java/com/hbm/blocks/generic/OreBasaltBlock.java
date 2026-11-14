/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.generic;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class OreBasaltBlock extends Block {

    private final Supplier<Block> gas;

    public OreBasaltBlock(Properties props, Supplier<Block> gas) {
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
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (EnchantmentHelper.getItemEnchantmentLevel(
                        enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool)
                == 0) {
            level.setBlockAndUpdate(pos, gas.get().defaultBlockState());
        }
    }
}
