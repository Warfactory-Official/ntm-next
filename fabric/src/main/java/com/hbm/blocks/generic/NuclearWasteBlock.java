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

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NuclearWasteBlock extends HazardBlock {
    public static final MapCodec<NuclearWasteBlock> CODEC = simpleCodec(NuclearWasteBlock::new);

    public NuclearWasteBlock(BlockBehaviour.Properties props) {
        super(props);
        setDisplayEffect(ExtDisplayEffect.RADFOG);
    }

    @Override
    protected MapCodec<? extends NuclearWasteBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(
            BlockState _,
            Level level,
            BlockPos pos,
            BlockState _,
            boolean _) {
        if (level instanceof ServerLevel server) server.scheduleTick(pos, this, 20);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos off = pos.relative(Direction.getRandom(random));
        if (random.nextInt(2) == 0 && level.getBlockState(off).isAir()) {
            level.setBlockAndUpdate(off, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }
        level.scheduleTick(pos, this, 20);
    }
}
