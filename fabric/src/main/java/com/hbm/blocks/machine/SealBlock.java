/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import com.hbm.world.bulk.BulkAction;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class SealBlock extends Block implements TriggerHandler {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int MAX_SIZE = 6;

    public static final MapCodec<SealBlock> CODEC = simpleCodec(SealBlock::new);

    public SealBlock(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static boolean toggle(Level level, BlockPos pos, BlockState state) {
        int size = frameSize(level, pos, state);
        if (size == 0) return false;
        if (isSealClosed(level, pos, state, size)) openSeal(level, pos, state, size);
        else closeSeal(level, pos, state, size);
        return true;
    }

    private static BlockPos frameCenter(BlockPos pos, BlockState state, int size) {
        return pos.relative(state.getValue(FACING).getOpposite(), size);
    }

    public static int frameSize(Level level, BlockPos pos, BlockState state) {
        Block frame = ModBlocks.SEAL_FRAME.get();
        Block controller = ModBlocks.SEAL_CONTROLLER.get();

        for (int size = 1; size <= MAX_SIZE; size++) {
            BlockPos center = frameCenter(pos, state, size);
            boolean valid = true;

            for (int d = -size; d <= size && valid; d++) {
                for (BlockPos edge :
                        new BlockPos[] {
                            center.offset(d, 0, size),
                            center.offset(d, 0, -size),
                            center.offset(-size, 0, d),
                            center.offset(size, 0, d)
                        }) {
                    BlockState at = level.getBlockState(edge);
                    if (!at.is(frame) && !at.is(controller)) {
                        valid = false;
                        break;
                    }
                }
            }

            if (valid) return size;
        }

        return 0;
    }

    public static void closeSeal(Level level, BlockPos pos, BlockState state, int size) {
        BlockPos center = frameCenter(pos, state, size);
        Block hatch = ModBlocks.SEAL_HATCH.get();
        List<BlockPos> lids = new ArrayList<>();

        for (int dx = -size + 1; dx <= size - 1; dx++) {
            for (int dz = -size + 1; dz <= size - 1; dz++) {
                BlockPos at = center.offset(dx, 0, dz);
                if (!level.getBlockState(at).isAir()) continue;
                level.setBlockAndUpdate(at, hatch.defaultBlockState());
                lids.add(at);
            }
        }
        AssembledMembers.assemble((ServerLevel) level, pos, lids);
    }

    static boolean frames(ServerLevel level, BlockPos controller) {
        BlockState at = level.getBlockState(controller);
        return at.is(ModBlocks.SEAL_CONTROLLER.get()) && frameSize(level, controller, at) != 0;
    }

    static void revalidate(ServerLevel level, BlockPos controller) {
        if (frames(level, controller)) return;
        clearLids(level, controller);
    }

    private static void clearLids(ServerLevel level, BlockPos controller) {
        int reach = 2 * MAX_SIZE;
        BoundingBox bounds =
                new BoundingBox(
                        controller.getX() - reach,
                        controller.getY(),
                        controller.getZ() - reach,
                        controller.getX() + reach,
                        controller.getY(),
                        controller.getZ() + reach);
        for (BlockPos lid : AssembledMembers.members(level, controller, bounds)) {
            level.setBlockAndUpdate(lid, Blocks.AIR.defaultBlockState());
        }
    }

    public static void openSeal(Level level, BlockPos pos, BlockState state, int size) {
        BlockPos center = frameCenter(pos, state, size);
        Block hatch = ModBlocks.SEAL_HATCH.get();

        for (int dx = -size + 1; dx <= size - 1; dx++) {
            for (int dz = -size + 1; dz <= size - 1; dz++) {
                BlockPos at = center.offset(dx, 0, dz);
                if (level.getBlockState(at).is(hatch)) {
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    public static boolean isSealClosed(Level level, BlockPos pos, BlockState state, int size) {
        BlockPos center = frameCenter(pos, state, size);
        Block hatch = ModBlocks.SEAL_HATCH.get();

        for (int dx = -size + 1; dx <= size - 1; dx++) {
            for (int dz = -size + 1; dz <= size - 1; dz++) {
                if (level.getBlockState(center.offset(dx, 0, dz)).is(hatch)) return true;
            }
        }

        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
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
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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
        if (!level.isClientSide()) toggle(level, pos, state);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), UPDATE_CLIENTS);
        if (powered && !level.isClientSide()) toggle(level, pos, state);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return TriggerResult.NONE;
        BlockState state = level.getBlockState(pos);
        return toggle(level, pos, state) ? TriggerResult.ACTUATED : TriggerResult.INCOMPATIBLE;
    }

    public void visitBulkLids(ServerLevel level, BlockPos controller, Consumer<BlockPos> visitor) {
        int reach = 2 * MAX_SIZE;
        BoundingBox bounds =
                new BoundingBox(
                        controller.getX() - reach,
                        controller.getY(),
                        controller.getZ() - reach,
                        controller.getX() + reach,
                        controller.getY(),
                        controller.getZ() + reach);
        AssembledMembers.members(level, controller, bounds).forEach(visitor);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (BulkAction.active()) return;
        clearLids(level, pos);
    }

    public boolean bulkSealRelevant(BlockState ignoredState) {
        return true;
    }

    public boolean captureBulkLids(
            ServerLevel level,
            BlockPos pos,
            BlockState ignoredState,
            CompoundTag data,
            Consumer<BlockPos> cells) {
        visitBulkLids(level, pos, cells);
        return true;
    }
}
