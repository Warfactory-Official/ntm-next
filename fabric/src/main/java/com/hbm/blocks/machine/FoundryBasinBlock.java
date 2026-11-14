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

import com.hbm.blockentity.machine.FoundryBasinBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FoundryBasinBlock extends FoundryCastingBlock implements CapabilityBlock {
    private static final VoxelShape SHAPE = Shapes.box(0D, 0D, 0D, 1D, 0.999D, 1D);
    private static final VoxelShape COLLISION =
            Shapes.or(
                    Shapes.box(0D, 0D, 0D, 1D, 0.125D, 1D),
                    Shapes.box(0D, 0D, 0D, 1D, 1D, 0.125D),
                    Shapes.box(0D, 0D, 0D, 0.125D, 1D, 1D),
                    Shapes.box(0.875D, 0D, 0D, 1D, 1D, 1D),
                    Shapes.box(0D, 0D, 0.875D, 1D, 1D, 1D));

    public FoundryBasinBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected double topY() {
        return 1.0D;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryBasinBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FOUNDRY_BASIN).items();
    }
}
