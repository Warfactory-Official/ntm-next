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

import com.hbm.blockentity.FissureBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class FissureBlock extends Block implements TickingEntityBlock, CapabilityBlock {

    public static final BooleanProperty CRATER = BooleanProperty.create("crater");

    public static final MapCodec<FissureBlock> CODEC = simpleCodec(FissureBlock::new);

    public FissureBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(CRATER, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRATER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FissureBlockEntity(pos, state);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (!level.getBlockState(above).canBeReplaced()) return;
        Block lava =
                state.getValue(CRATER)
                        ? ModBlocks.RAD_LAVA_BLOCK.get()
                        : ModBlocks.VOLCANIC_LAVA_BLOCK.get();
        level.setBlockAndUpdate(above, lava.defaultBlockState());
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FISSURE)
                .fluidOut()
                .fluidFaces(FissureBlockEntity.class, (be, face) -> face.side() == Direction.DOWN);
    }
}
