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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.SoyuzRenderAnchorBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCellBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class LaunchpadSoyuzAnchorCellBlock extends MultiblockCellBlock
        implements EntityBlock, TickingEntityBlock {
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 1, 3);

    public LaunchpadSoyuzAnchorCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState()
                        .setValue(MultiblockCoreBlock.FACING, Direction.NORTH)
                        .setValue(SEGMENT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MultiblockCoreBlock.FACING, SEGMENT);
    }

    public static BlockPos core(BlockPos anchor, BlockState state) {
        int segment = state.getValue(SEGMENT);
        Direction facing = state.getValue(MultiblockCoreBlock.FACING);
        return anchor.offset(
                -LaunchpadSoyuzBlock.anchorX(segment, facing),
                0,
                -LaunchpadSoyuzBlock.anchorZ(segment, facing));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoyuzRenderAnchorBlockEntity(pos, state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(
                MultiblockCoreBlock.FACING,
                rotation.rotate(state.getValue(MultiblockCoreBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(MultiblockCoreBlock.FACING)));
    }
}
