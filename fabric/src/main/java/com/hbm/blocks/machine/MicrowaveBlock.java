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

import com.hbm.blockentity.machine.MicrowaveBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.BlockMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MicrowaveBlock extends HorizontalMachineBlock implements CapabilityBlock {
    private static final VoxelShape NORTH_SHAPE =
            Shapes.or(
                    Shapes.box(0.112109, 0.002154, 0.414363, 0.862732, 0.398358, 1.0),
                    Shapes.box(0.246749, 0.067258, 0.387786, 0.282202, 0.333254, 0.422432));
    private static final VoxelShape EAST_SHAPE =
            Shapes.or(
                    Shapes.box(0.0, 0.002154, 0.112109, 0.585637, 0.398358, 0.862732),
                    Shapes.box(0.577568, 0.067258, 0.246749, 0.612214, 0.333254, 0.282202));
    private static final VoxelShape SOUTH_SHAPE =
            Shapes.or(
                    Shapes.box(0.137268, 0.002154, 0.0, 0.887891, 0.398358, 0.585637),
                    Shapes.box(0.717798, 0.067258, 0.577568, 0.753251, 0.333254, 0.612214));
    private static final VoxelShape WEST_SHAPE =
            Shapes.or(
                    Shapes.box(0.414363, 0.002154, 0.137268, 1.0, 0.398358, 0.887891),
                    Shapes.box(0.387786, 0.067258, 0.717798, 0.422432, 0.333254, 0.753251));

    public MicrowaveBlock(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicrowaveBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            BlockMenus.open(player, provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MICROWAVE).powerIn().items().fe();
    }
}
