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

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SmolderBlock extends Block {

    public SmolderBlock(BlockBehaviour.Properties props) {
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
