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

import com.hbm.blockentity.network.CableDiodeBlockEntity;
import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.BlockMenus;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.util.NumberFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CableDiodeBlock extends Block
        implements CapabilityDependentShape.Endpoint,
                TickingEntityBlock,
                ToolInteractionHandler,
                LookOverlayProvider,
                CapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public CableDiodeBlock(BlockBehaviour.Properties props) {
        super(CapabilityDependentShape.deferShapeToFullChunk(props));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction dir,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (level instanceof Level world
                && world.getBlockEntity(pos) instanceof CableDiodeBlockEntity diode) {
            boolean joins = PhysicalGraph.powerContact(world, neighbourPos, dir.getOpposite());
            diode.updateContact(dir, joins, neighbourState.isAir());
        }
        return state;
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
        if (level instanceof ServerLevel sl
                && level.getBlockEntity(pos) instanceof CableDiodeBlockEntity diode) {
            diode.refreshContacts(sl);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableDiodeBlockEntity(pos, state);
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
        ToolType tool = ToolType.getType(stack);
        if (tool == ToolType.SCREWDRIVER
                || tool == ToolType.HAND_DRILL
                || tool == ToolType.DEFUSER) {
            return InteractionResult.PASS;
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof CableDiodeBlockEntity diode) {
            BlockMenus.open(player, diode);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (!(level.getBlockEntity(pos) instanceof CableDiodeBlockEntity diode)) return false;
        if (level.isClientSide())
            return tool == ToolType.SCREWDRIVER
                    || tool == ToolType.HAND_DRILL
                    || tool == ToolType.DEFUSER;

        switch (tool) {
            case SCREWDRIVER -> {
                diode.increaseLimit();
            }
            case HAND_DRILL -> {
                diode.decreaseLimit();
            }
            case DEFUSER -> {
                diode.cyclePriority();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof CableDiodeBlockEntity diode)) return;
        info.title(getName())
                .row(
                        Component.translatable(
                                "overlay.hbm.max_rate",
                                NumberFormatter.formatCompact(diode.getMaxPower())))
                .row(
                        Component.translatable(
                                "overlay.hbm.priority", diode.receiverPriority().name()));
    }

    @Override
    public boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return true;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CABLE_DIODE)
                .powerIn()
                .powerOut()
                .roleFaces(CableDiodeBlockEntity.class, CableDiodeBlockEntity::acceptsFace);
    }
}
