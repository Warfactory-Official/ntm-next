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

package com.hbm.blocks.fluid;

import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AcidBlock extends ClassicFluidBlock {

    public AcidBlock(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        if (level instanceof ServerLevel server) {
            entity.hurtServer(server, level.damageSources().source(ModDamageTypes.ACID), 10_000F);
        }
    }

    @Override
    protected void flowTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        annihilateNeighbours(level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        annihilateNeighbours(level, pos);
    }

    private void annihilateNeighbours(Level level, BlockPos pos) {
        annihilate(level, pos.east());
        annihilate(level, pos.west());
        annihilate(level, pos.above());
        annihilate(level, pos.below());
        annihilate(level, pos.south());
        annihilate(level, pos.north());
    }

    private void annihilate(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(this)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
}
