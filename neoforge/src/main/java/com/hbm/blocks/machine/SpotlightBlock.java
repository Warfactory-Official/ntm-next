/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.api.block.BlockPlacements;
import com.hbm.blocks.DirectionalBlock;
import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SpotlightBlock extends DirectionalBlock
        implements LightBeamSource, SimpleWaterloggedBlock {

    private static final VoxelShape[][] SHAPES = new VoxelShape[3][6];

    static {
        float[][] halfBounds = {
            {0.25F, 0.2F, 0.15F},
            {0.5F, 0.5F, 0.1F},
            {0.35F, 0.25F, 0.2F}
        };
        for (int type = 0; type < halfBounds.length; type++) {
            float[] bounds = halfBounds[type];
            for (var direction : Direction.VALUES) {
                float halfX = bounds[0];
                float halfY = bounds[1];
                float halfZ = bounds[2];
                switch (direction.getAxis()) {
                    case X -> {
                        halfX = bounds[2];
                        halfZ = bounds[0];
                    }
                    case Y -> {
                        halfX = bounds[1];
                        halfY = bounds[2];
                        halfZ = bounds[0];
                    }
                    case Z -> {}
                }

                float centerX = 0.5F - direction.getStepX() * (0.5F - halfX);
                float centerY = 0.5F - direction.getStepY() * (0.5F - halfY);
                float centerZ = 0.5F - direction.getStepZ() * (0.5F - halfZ);
                SHAPES[type][direction.ordinal()] =
                        Shapes.box(
                                centerX - halfX,
                                centerY - halfY,
                                centerZ - halfZ,
                                centerX + halfX,
                                centerY + halfY,
                                centerZ + halfZ);
            }
        }
    }

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int TURN_OFF_DELAY = 4;
    public static final int MAX_BEAM_LENGTH = 32;

    private final int beamLength;
    private final LightType type;
    private final VoxelShape[] shapesByDirection;
    private final MapCodec<? extends net.minecraft.world.level.block.DirectionalBlock> ownCodec;

    public SpotlightBlock(int beamLength, LightType type, Properties properties) {
        super(properties);
        this.beamLength = beamLength;
        this.type = type;
        shapesByDirection = SHAPES[type.ordinal()];
        ownCodec = simpleCodec(props -> new SpotlightBlock(beamLength, type, props));
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(FACING, Direction.DOWN)
                        .setValue(BROKEN, false)
                        .setValue(LIT, true)
                        .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.DirectionalBlock> codec() {
        return ownCodec;
    }

    public LightType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BROKEN, LIT, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean waterlogged =
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var facing = state.getValue(FACING);
        var supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            if (!updatePower(state, server, pos)) {
                updateBeam(state, server, pos);
            }
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (block == Blocks.AIR
                || block == Blocks.LIGHT
                || block == ModBlocks.SPOTLIGHT_BEAM.get()) {
            return;
        }
        if (!state.canSurvive(server, pos)) {
            dropResources(state, server, pos);
            server.removeBlock(pos, false);
            return;
        }
        if (!updatePower(state, server, pos)) {
            updateBeam(state, server, pos);
        }
    }

    private boolean updatePower(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(BROKEN)) {
            return false;
        }
        boolean powered = level.hasNeighborSignal(pos);
        boolean lit = state.getValue(LIT);
        if (lit && powered) {
            level.scheduleTick(pos, this, TURN_OFF_DELAY);
            return true;
        }
        if (!lit && !powered) {
            setLit(state, level, pos, true);
            return true;
        }
        return false;
    }

    private void setLit(BlockState state, ServerLevel level, BlockPos pos, boolean lit) {
        level.setBlock(pos, state.setValue(LIT, lit), UPDATE_CLIENTS);
        var facing = state.getValue(FACING);
        if (lit) {
            propagateBeam(level, pos, facing, beamLength);
        } else {
            unpropagateBeam(level, pos, facing, beamLength);
        }
    }

    private void updateBeam(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(LIT)) {
            propagateBeam(level, pos, state.getValue(FACING), beamLength);
        } else {
            unpropagateBeam(level, pos, state.getValue(FACING), beamLength);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && level.hasNeighborSignal(pos)) {
            setLit(state, level, pos, false);
        }
    }

    static boolean isBeam(BlockState state) {
        return state.is(ModBlocks.SPOTLIGHT_BEAM.get());
    }

    public static void propagateBeam(ServerLevel level, BlockPos pos, Direction dir, int distance) {
        var cursor = pos.mutable();
        while (--distance > 0) {
            cursor.move(dir);
            var state = level.getBlockState(cursor);
            if (isBeam(state)) {
                continue;
            }
            if (!state.isAir()) {
                return;
            }
            level.setBlock(cursor, ModBlocks.SPOTLIGHT_BEAM.get().defaultBlockState(), UPDATE_ALL);
        }
    }

    private static int sourceDistance(
            ServerLevel level, BlockPos cell, Direction dir, BlockPos.MutableBlockPos cursor) {
        cursor.set(cell);
        var backward = dir.getOpposite();
        for (int step = 1; step < MAX_BEAM_LENGTH; step++) {
            cursor.move(backward);
            var state = level.getBlockState(cursor);
            if (state.getBlock() instanceof LightBeamSource fixture) {
                if (fixture.emitsLightBeam(state, dir) && step < fixture.getBeamRange()) {
                    return step;
                }
                return 0;
            }
            if (!state.isAir() && !isBeam(state)) {
                return 0;
            }
        }
        return 0;
    }

    public static List<Direction> sourceDirections(ServerLevel level, BlockPos cell) {
        var directions = new ArrayList<Direction>();
        var searchCursor = cell.mutable();
        for (var dir : Direction.VALUES) {
            if (sourceDistance(level, cell, dir, searchCursor) != 0) {
                directions.add(dir);
            }
        }
        return directions;
    }

    private static boolean hasSource(
            ServerLevel level, BlockPos cell, BlockPos.MutableBlockPos searchCursor) {
        for (var dir : Direction.VALUES) {
            if (sourceDistance(level, cell, dir, searchCursor) != 0) {
                return true;
            }
        }
        return false;
    }

    public static void unpropagateBeam(
            ServerLevel level, BlockPos pos, Direction dir, int distance) {
        var cursor = pos.mutable();
        var searchCursor = pos.mutable();
        while (--distance > 0) {
            cursor.move(dir);
            if (!isBeam(level.getBlockState(cursor))) {
                return;
            }
            if (!hasSource(level, cursor, searchCursor)) {
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
            }
        }
    }

    public static void repropagateThrough(ServerLevel level, BlockPos cell) {
        var directions = sourceDirections(level, cell);
        var searchCursor = cell.mutable();
        for (var dir : directions) {
            int distance = sourceDistance(level, cell, dir, searchCursor);
            if (distance != 0) {
                var sourcePos = cell.relative(dir.getOpposite(), distance);
                var fixture = (LightBeamSource) level.getBlockState(sourcePos).getBlock();
                propagateBeam(level, sourcePos, dir, fixture.getBeamRange());
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (state.getValue(LIT)) {
            unpropagateBeam(level, pos, state.getValue(FACING), beamLength);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(BROKEN)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            repairConnected(server, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private void repairConnected(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!state.is(this) || !state.getValue(BROKEN)) {
            return;
        }
        var repaired = state.setValue(BROKEN, false).setValue(LIT, true);
        level.setBlock(pos, repaired, UPDATE_ALL);
        BlockPlacements.repaired(level, pos);
        propagateBeam(level, pos, state.getValue(FACING), beamLength);
        for (var dir : Direction.VALUES) {
            repairConnected(level, pos.relative(dir));
        }
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapesByDirection[state.getValue(FACING).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public int getBeamRange() {
        return beamLength;
    }

    @Override
    public boolean emitsLightBeam(BlockState state, Direction dir) {
        return state.getValue(FACING) == dir && state.getValue(LIT) && !state.getValue(BROKEN);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(
                state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED)) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    public enum LightType {
        INCANDESCENT,
        FLUORESCENT,
        HALOGEN
    }
}
