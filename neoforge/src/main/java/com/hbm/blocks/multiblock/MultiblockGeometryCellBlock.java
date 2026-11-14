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

package com.hbm.blocks.multiblock;

import com.hbm.blocks.ClimbBox;
import com.hbm.world.bulk.BulkTransform;
import com.mojang.math.OctahedralGroup;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MultiblockGeometryCellBlock extends MultiblockCellBlock implements ClimbBox {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static CellBuckets.@Nullable Geometry constructing;

    private final CellBuckets.Geometry bucket;
    private final boolean analog;
    private Alphabet alphabet = Alphabet.EMPTY;
    private int[] transformedIds = new int[0];

    private MultiblockGeometryCellBlock(Properties properties) {
        super(properties);
        bucket = pending();
        analog = bucket.comparator();
        BlockState any = stateDefinition.any().setValue(bucket.shape(), 0);
        registerDefaultState(
                any.trySetValue(OPEN, Boolean.FALSE).trySetValue(COMPARATOR, Boolean.FALSE));
    }

    public static MultiblockGeometryCellBlock create(
            CellBuckets.Geometry bucket, Properties properties) {
        constructing = bucket;
        MultiblockGeometryCellBlock cell =
                bucket.part().waterlogs()
                        ? new Waterlogged(properties)
                        : new MultiblockGeometryCellBlock(properties);
        constructing = null;
        return cell;
    }

    private static CellBuckets.Geometry pending() {
        CellBuckets.Geometry bucket = constructing;
        if (bucket == null) {
            throw new IllegalStateException(
                    "a geometry cell must be built through"
                            + " BlockMultiblockGeometryCell.create(bucket, properties)");
        }
        return bucket;
    }

    public static int shapeId(BlockState state) {
        return cell(state).idOf(state);
    }

    public static BlockState withShape(BlockState state, int id) {
        return cell(state).withId(state, id);
    }

    public static boolean isOpenCell(BlockState state) {
        return cell(state).isOpen(state);
    }

    public static BlockState openCell(BlockState state, boolean open) {
        return cell(state).withOpen(state, open);
    }

    public static void setCellOpen(ServerLevel level, BlockPos cell, BlockPos core, boolean open) {
        BlockState state = level.getBlockState(cell);
        if (!(state.getBlock() instanceof MultiblockGeometryCellBlock)) return;
        if (isOpenCell(state) == open) return;
        if (MultiblockSurface.recordedCorePacked(level, cell.getX(), cell.getY(), cell.getZ())
                != core.asLong()) {
            return;
        }
        level.setBlock(cell, openCell(state, open), Block.UPDATE_ALL);
    }

    private static MultiblockGeometryCellBlock cell(BlockState state) {
        return (MultiblockGeometryCellBlock) state.getBlock();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CellBuckets.Geometry bucket = pending();
        builder.add(bucket.shape());
        if (bucket.part().opens()) builder.add(OPEN);
        if (bucket.comparator()) builder.add(COMPARATOR);
    }

    public CellBuckets.Geometry bucket() {
        return bucket;
    }

    void publish(Alphabet alphabet) {
        this.alphabet = alphabet;
        buildTransforms();
        for (BlockState state : stateDefinition.getPossibleStates()) {
            state.initCache();
            state.initCache();
        }
    }

    private void buildTransforms() {
        int count = alphabet.shapes().length;
        transformedIds = new int[count * 8];
        Arrays.fill(transformedIds, -1);
        Rotation[] rotations = Rotation.values();
        for (int mode = 0; mode < 8; mode++) {
            Mirror mirror = mode < 4 ? Mirror.NONE : Mirror.FRONT_BACK;
            OctahedralGroup rotation = rotations[mode & 3].rotation().compose(mirror.rotation());
            for (int id = 0; id < count; id++) {
                VoxelShape[] shapes = new VoxelShape[alphabet.shapes()[id].length];
                for (int variant = 0; variant < shapes.length; variant++) {
                    VoxelShape shape = alphabet.shapes()[id][variant];
                    shapes[variant] = shape == null ? null : Shapes.rotate(shape, rotation);
                }
                AABB climb = alphabet.climbs()[id];
                if (climb != null) {
                    climb = Shapes.rotate(Shapes.create(climb), rotation).bounds();
                }
                transformedIds[mode * count + id] =
                        matchingGeometry(shapes, alphabet.sounds()[id], climb);
            }
        }
    }

    private int matchingGeometry(VoxelShape[] shapes, SoundType sound, @Nullable AABB climb) {
        for (int candidate = 0; candidate < alphabet.shapes().length; candidate++) {
            if (sound != alphabet.sounds()[candidate]
                    || !Objects.equals(climb, alphabet.climbs()[candidate])) {
                continue;
            }
            boolean matches = true;
            for (int variant = 0; variant < shapes.length; variant++) {
                VoxelShape expected = shapes[variant];
                VoxelShape actual = alphabet.shapes()[candidate][variant];
                boolean sameShape;
                if (expected == null || actual == null) {
                    sameShape = expected == actual;
                } else {
                    sameShape = Shapes.equal(expected, actual);
                }
                if (!sameShape) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return candidate;
            }
        }
        return -1;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return transformed(state, rotation.ordinal());
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return transformed(
                state,
                switch (mirror) {
                    case NONE -> 0;
                    case FRONT_BACK -> 4;
                    case LEFT_RIGHT -> 6;
                });
    }

    public BlockState transform(BlockState state, BulkTransform transform) {
        BlockPos x = transform.apply(new BlockPos(1, 0, 0));
        BlockPos z = transform.apply(new BlockPos(0, 0, 1));
        if (!transform.apply(new BlockPos(0, 1, 0)).equals(new BlockPos(0, 1, 0)))
            throw new IllegalArgumentException(
                    "NTM machine cells support horizontal transformedIds only");
        if (x.getY() != 0
                || z.getY() != 0
                || Math.abs(x.getX()) + Math.abs(x.getZ()) != 1
                || Math.abs(z.getX()) + Math.abs(z.getZ()) != 1
                || x.getX() * z.getX() + x.getZ() * z.getZ() != 0)
            throw new IllegalArgumentException(
                    "NTM machine cells require a grid-aligned horizontal transform");
        int turn;
        if (x.getX() == 1) {
            turn = 0;
        } else if (x.getZ() == 1) {
            turn = 1;
        } else if (x.getX() == -1) {
            turn = 2;
        } else {
            turn = 3;
        }
        int determinant = x.getX() * z.getZ() - x.getZ() * z.getX();
        return transformed(state, determinant == 1 ? turn : 4 + ((turn + 2) & 3));
    }

    private BlockState transformed(BlockState state, int mode) {
        int id = idOf(state);
        int count = alphabet.shapes().length;
        if (id >= count)
            throw new IllegalArgumentException("Unassigned NTM geometry cell " + state);
        int result = transformedIds[mode * count + id];
        if (result < 0)
            throw new IllegalArgumentException(
                    "NTM geometry has no matching transformed state: " + state);
        return withId(state, result);
    }

    public VoxelShape shape(int id, boolean open, boolean forCollision) {
        int projection = MultiblockLayout.CellGeometry.OUTLINE_PROJECTION;
        if (forCollision) projection = MultiblockLayout.CellGeometry.COLLISION_PROJECTION;
        VoxelShape shape = projection(id, open, projection);
        return shape == null ? Shapes.block() : shape;
    }

    private @Nullable VoxelShape projection(int id, boolean open, int projection) {
        VoxelShape[][] shapes = alphabet.shapes();
        int variant = open ? MultiblockLayout.CellGeometry.PROJECTION_COUNT : 0;
        return id < shapes.length ? shapes[id][variant + projection] : null;
    }

    public GeometryCellPartition partition() {
        return bucket.part();
    }

    public int idOf(BlockState state) {
        return state.getValue(bucket.shape());
    }

    public BlockState withId(BlockState state, int id) {
        return state.setValue(bucket.shape(), id);
    }

    public boolean isOpen(BlockState state) {
        return bucket.part().opens() && state.getValue(OPEN);
    }

    public BlockState withOpen(BlockState state, boolean open) {
        return bucket.part().opens() ? state.setValue(OPEN, open) : state;
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        SoundType[] sounds = alphabet.sounds();
        int id = idOf(state);
        return id < sounds.length ? sounds[id] : SoundType.STONE;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape(idOf(state), isOpen(state), false);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape(idOf(state), isOpen(state), true);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape support =
                projection(
                        idOf(state),
                        isOpen(state),
                        MultiblockLayout.CellGeometry.SUPPORT_PROJECTION);
        return support == null ? super.getBlockSupportShape(state, level, pos) : support;
    }

    @Override
    public @Nullable AABB climbBox(BlockState state) {
        @Nullable AABB[] climbs = alphabet.climbs();
        int id = idOf(state);
        return id < climbs.length ? climbs[id] : null;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return analog && state.getValue(COMPARATOR);
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        return analogOutputAtCell(level, pos, direction);
    }

    record Alphabet(VoxelShape[][] shapes, SoundType[] sounds, @Nullable AABB[] climbs) {
        static final Alphabet EMPTY =
                new Alphabet(new VoxelShape[0][], new SoundType[0], new AABB[0]);
    }

    private static final class Waterlogged extends MultiblockGeometryCellBlock
            implements SimpleWaterloggedBlock {

        private Waterlogged(Properties properties) {
            super(properties);
            registerDefaultState(
                    defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(BlockStateProperties.WATERLOGGED);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                LevelReader level,
                ScheduledTickAccess ticks,
                BlockPos pos,
                Direction direction,
                BlockPos neighbourPos,
                BlockState neighbourState,
                RandomSource random) {
            if (state.getValue(BlockStateProperties.WATERLOGGED)) {
                ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(
                    state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return state.getValue(BlockStateProperties.WATERLOGGED)
                    ? Fluids.WATER.getSource(false)
                    : super.getFluidState(state);
        }
    }
}
