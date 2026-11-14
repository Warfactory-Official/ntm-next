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

import com.hbm.blockentity.machine.radio.EnervationBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RadioboxBlock extends HorizontalMachineBlock implements CapabilityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape NORTH = Block.box(4, 1, 11, 12, 15, 16);
    private static final VoxelShape SOUTH = Block.box(4, 1, 0, 12, 15, 5);
    private static final VoxelShape WEST = Block.box(11, 1, 4, 16, 15, 12);
    private static final VoxelShape EAST = Block.box(0, 1, 4, 5, 15, 12);

    public RadioboxBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(ACTIVE) ? super.getTicker(level, state, type) : null;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnervationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        var radiobox = (EnervationBlockEntity) level.getBlockEntity(pos);
        if (stack.is(ModItems.BATTERY_SPARK.get()) && !radiobox.hasSpark()) {
            stack.consume(1, player);
            level.playSound(
                    null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
            radiobox.installSpark();
            return InteractionResult.SUCCESS;
        }

        toggle(state, level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()) toggle(state, level, pos);
        return InteractionResult.SUCCESS;
    }

    private static void toggle(BlockState state, Level level, BlockPos pos) {
        boolean active = !state.getValue(ACTIVE);
        level.setBlock(pos, state.setValue(ACTIVE, active), UPDATE_CLIENTS);
        level.playSound(
                null,
                pos,
                ModSounds.REACTOR_START.get(),
                SoundSource.BLOCKS,
                1.0F,
                active ? 1.0F : 0.85F);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RADIOBOX).powerIn().fe();
    }
}
