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

package com.hbm.blocks.network;

import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.api.conveyor.ConveyorItem;
import com.hbm.api.conveyor.ConveyorPackage;
import com.hbm.blockentity.network.CraneInserterBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.util.InventoryUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CraneInserterBlock extends CraneBlock
        implements CapabilityBlock, ConveyorInputHandler {
    public static final MapCodec<CraneInserterBlock> CODEC = simpleCodec(CraneInserterBlock::new);

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public CraneInserterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CRANE_INSERTER).items();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraneInserterBlockEntity(pos, state);
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorItem rider) {
        return side == getInputSide(level, pos);
    }

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorItem rider) {
        var output = getOutputSide(level, pos);
        var accessSide = output.getOpposite();
        var remainder = rider.getItemStack().copy();
        if (!level.hasNeighborSignal(pos)) {
            InventoryUtil.insertAt(level, pos.relative(output), accessSide, remainder);
        }
        InventoryUtil.insertAt(level, pos, accessSide, remainder);
        var inserter = (CraneInserterBlockEntity) level.getBlockEntity(pos);
        if (!inserter.destroyer) {
            CraneBlock.spill(
                    level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder);
        }
    }

    @Override
    public boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorPackage rider) {
        return true;
    }

    @Override
    public void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorPackage rider) {
        var output = getOutputSide(level, pos);
        var accessSide = output.getOpposite();
        var stacks = rider.getItemStacks();
        if (!level.hasNeighborSignal(pos)) {
            var destination = pos.relative(output);
            for (var stack : stacks) {
                InventoryUtil.insertAt(level, destination, accessSide, stack);
            }
        }

        var inserter = (CraneInserterBlockEntity) level.getBlockEntity(pos);
        for (var stack : stacks) {
            InventoryUtil.insertAt(level, pos, accessSide, stack);
            if (!inserter.destroyer) {
                CraneBlock.spill(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
            }
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
