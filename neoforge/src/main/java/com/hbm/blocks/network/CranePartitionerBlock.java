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

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.api.conveyor.ConveyorItem;
import com.hbm.blockentity.network.CranePartitionerBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.util.InventoryUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CranePartitionerBlock extends Block
        implements TickingEntityBlock, CapabilityBlock, ConveyorInputHandler, ConveyorBelt {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<CranePartitionerBlock> CODEC =
            simpleCodec(CranePartitionerBlock::new);

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public CranePartitionerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CRANE_PARTITIONER).items();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CranePartitionerBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TransportBlockData.PARTITIONER_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TransportBlockData.PARTITIONER_SHAPE;
    }

    public Direction getTravelDirection(Level level, BlockPos pos, @Nullable Vec3 rider) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 rider) {
        return true;
    }

    @Override
    public Vec3 getTravelPosition(Level level, BlockPos pos, Vec3 rider, double speed) {
        return BeltTravel.travel(pos, rider, getTravelDirection(level, pos, rider), speed);
    }

    @Override
    public Vec3 getSnappingPosition(Level level, BlockPos pos, Vec3 near) {
        return BeltTravel.snap(pos, near, getTravelDirection(level, pos, near));
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorItem rider) {
        return side == getTravelDirection(level, pos, null);
    }

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorItem rider) {
        var partitioner = (CranePartitionerBlockEntity) level.getBlockEntity(pos);
        var offered = rider.getItemStack().copy();
        int offeredCount = offered.getCount();
        int queueSize = CranePartitionerBlockEntity.SLOT_COUNT;
        ItemStack remainder;
        if (CrystallizerRecipes.INSTANCE.getAmount(offered) > 0) {
            remainder =
                    InventoryUtil.tryAddItemToInventory(
                            partitioner.inventory, 0, queueSize - 1, offered);
        } else {
            remainder =
                    InventoryUtil.tryAddItemToInventory(
                            partitioner.inventory, queueSize, queueSize * 2 - 1, offered);
        }
        if (remainder.getCount() != offeredCount) {
            partitioner.setChanged();
        }
        CraneBlock.spill(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder);
    }
}
