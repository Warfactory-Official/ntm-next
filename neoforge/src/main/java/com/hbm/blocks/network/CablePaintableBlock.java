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

import com.hbm.blockentity.network.CablePaintableBlockEntity;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CablePaintableBlock extends CableConductorBlock
        implements EntityBlock, ToolInteractionHandler, CamouflageStateProvider {

    public static final BooleanProperty PAINTED = BooleanProperty.create("painted");
    public static final BooleanProperty OVERLAY = BooleanProperty.create("overlay");

    public CablePaintableBlock(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(PAINTED, false).setValue(OVERLAY, true));
    }

    @Override
    public BooleanProperty paintedProperty() {
        return PAINTED;
    }

    @Override
    public BooleanProperty overlayProperty() {
        return OVERLAY;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PAINTED, OVERLAY);
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
        if (stack.getItem() instanceof BlockItem blockItem
                && !state.getValue(PAINTED)
                && FluidDuctPaintableBlock.allowedPaint(blockItem.getBlock(), this)) {
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof CablePaintableBlockEntity cable) {
                cable.setCamo(blockItem.getBlock().defaultBlockState());
                level.setBlock(pos, state.setValue(PAINTED, true), Block.UPDATE_ALL);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        BlockState state = level.getBlockState(pos);
        if (tool == ToolType.SCREWDRIVER) {
            if (state.getValue(PAINTED)
                    && level.getBlockEntity(pos) instanceof CablePaintableBlockEntity cable
                    && cable.getCamo() != null) {
                if (!level.isClientSide()) {
                    cable.setCamo(null);
                    level.setBlock(pos, state.setValue(PAINTED, false), Block.UPDATE_ALL);
                }
                return true;
            }
            return false;
        }
        if (tool == ToolType.DEFUSER) {
            if (!level.isClientSide()) {
                level.setBlock(
                        pos, state.setValue(OVERLAY, !state.getValue(OVERLAY)), Block.UPDATE_ALL);
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CablePaintableBlockEntity(pos, state);
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }
}
