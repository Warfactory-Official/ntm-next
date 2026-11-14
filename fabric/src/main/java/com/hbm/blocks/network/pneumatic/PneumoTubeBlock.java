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

package com.hbm.blocks.network.pneumatic;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.TransportBlockData;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.platform.Caps;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.util.ChunkUtil;
import com.hbm.util.InventoryUtil;
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
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public non-sealed class PneumoTubeBlock extends Block
        implements CapabilityDependentShape,
                TickingEntityBlock,
                ToolInteractionHandler,
                CapabilityBlock,
                SimpleWaterloggedBlock,
                PneumaticConnection {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public PneumoTubeBlock(Properties properties) {
        super(CapabilityDependentShape.deferShapeToFullChunk(properties));
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
                        .setValue(WATERLOGGED, false));
    }

    public static BooleanProperty faceProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public static int faceMask(BlockState state) {
        int mask = 0;
        for (Direction direction : Direction.VALUES) {
            if (state.getValue(faceProperty(direction))) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    public static boolean canConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockState(pos.relative(direction)).getBlock()
                instanceof PneumaticConnection;
    }

    public static boolean canConnectToAir(LevelReader level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)
                || !isAirFace(tube, direction)) return false;

        var neighbor = pos.relative(direction);
        return airContact(level, neighbor, direction.getOpposite(), level.getBlockState(neighbor));
    }

    private static boolean isAirFace(PneumaticTubeBlockEntity tube, Direction direction) {
        return tube.hasIntake() && tube.intakeFace != direction && tube.outletFace != direction;
    }

    private static boolean airContact(
            LevelReader level,
            BlockPos neighbor,
            Direction face,
            @Nullable BlockState neighborState) {
        if (neighborState != null && neighborState.getBlock() instanceof PneumoTubeBlock)
            return false;
        return PhysicalGraph.fluidContact(level, neighbor, face, NTMFluids.AIR);
    }

    private static boolean unresolvedAirPeer(
            ServerLevel level, BlockPos neighbor, @Nullable BlockState neighborState) {
        if (neighborState == null) return true;
        if (neighborState.getBlock() instanceof PneumoTubeBlock) return false;

        long owner =
                MultiblockSurface.recordedCorePacked(
                        level, neighbor.getX(), neighbor.getY(), neighbor.getZ());
        if (owner == MultiblockSurface.NO_CORE) {
            var endpoint =
                    ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                            .hbm$endpoints()
                            .declared(neighbor.asLong());
            int fluidDomains = MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT;
            if (endpoint == null || (endpoint.domains() & fluidDomains) == 0) return false;
            owner = endpoint.owner();
        }
        return MultiblockCoreBlock.readableChunk(level, BlockPos.getX(owner), BlockPos.getZ(owner))
                == null;
    }

    public static void refreshAirConnections(ServerLevel level, BlockPos pos) {
        var state = ChunkUtil.blockStateIfLoaded(level, pos);
        if (state == null || !(state.getBlock() instanceof PneumoTubeBlock)) return;
        var tube = ChunkUtil.blockEntityIfLoaded(PneumaticTubeBlockEntity.class, level, pos);
        if (tube == null) return;

        BlockState updated = state;
        int airFaces = 0;
        for (Direction direction : Direction.VALUES) {
            var neighbor = pos.relative(direction);
            var neighborState = ChunkUtil.blockStateIfLoaded(level, neighbor);
            int bit = 1 << direction.ordinal();
            boolean air = false;
            if (isAirFace(tube, direction)) {
                air = airContact(level, neighbor, direction.getOpposite(), neighborState);
                if (!air && (tube.airFaces & bit) != 0) {
                    air = unresolvedAirPeer(level, neighbor, neighborState);
                }
            }
            if (air) airFaces |= bit;

            var property = faceProperty(direction);
            boolean pneumatic;
            if (neighborState == null) {
                pneumatic = state.getValue(property);
            } else {
                pneumatic = neighborState.getBlock() instanceof PneumaticConnection;
            }
            updated = updated.setValue(property, pneumatic || air);
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        tube.setAirFaces(airFaces);
    }

    public static void refreshConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PneumoTubeBlock)) return;

        BlockState updated = state;
        for (Direction direction : Direction.VALUES) {
            boolean pneumatic = canConnectTo(level, pos, direction);
            boolean air = canConnectToAir(level, pos, direction);
            updated = updated.setValue(faceProperty(direction), pneumatic || air);
        }
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_ALL);
        refreshAir(level, pos);
    }

    private static void refreshAir(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) return;

        int airFaces = 0;
        for (Direction direction : Direction.VALUES) {
            if (canConnectToAir(level, pos, direction)) airFaces |= 1 << direction.ordinal();
        }
        tube.setAirFaces(airFaces);
    }

    public static @Nullable Direction nextDir(@Nullable Direction direction) {
        if (direction == null) return Direction.DOWN;
        int next = direction.ordinal() + 1;
        return next == Direction.VALUES.length ? null : Direction.VALUES[next];
    }

    public static boolean screwDirections(Level level, @Nullable Player player, BlockPos pos) {
        if (player != null && player.isSpectator()) return false;
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) return false;

        boolean changeOutlet = player != null && player.isShiftKeyDown();
        Direction previous;
        Direction other;
        if (changeOutlet) {
            previous = tube.outletFace;
            other = tube.intakeFace;
        } else {
            previous = tube.intakeFace;
            other = tube.outletFace;
        }

        Direction selected = previous;
        while (true) {
            selected = nextDir(selected);
            if (selected == null) break;
            if (selected == other) continue;

            var neighbor = pos.relative(selected);
            if (level.getBlockEntity(neighbor) instanceof PneumaticTubeBlockEntity) continue;
            if (InventoryUtil.inventoryAt(level, neighbor, selected.getOpposite())) break;
        }
        if (selected == previous) return true;

        if (changeOutlet) {
            tube.hbm$syncSet$PneumaticTubeBlockEntity$outletFace(selected);
        } else {
            tube.hbm$syncSet$PneumaticTubeBlockEntity$intakeFace(selected);
        }
        tube.setChanged();
        if (level instanceof ServerLevel server) Caps.invalidateCaps(server, pos);

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        refreshConnections(level, pos);

        if (changeOutlet && level instanceof ServerLevel server) {
            if (previous != null) PneumaticNetwork.setEndpoint(server, pos, tube.hasOutlet());
            LevelNodeGraph.invalidateEndpointsAt(server, pos);
        }
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;
        BlockState state = defaultBlockState().setValue(WATERLOGGED, waterlogged);
        for (Direction direction : Direction.VALUES) {
            state = state.setValue(faceProperty(direction), canConnectTo(level, pos, direction));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighbor,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        var property = faceProperty(direction);
        if (level.isClientSide()) {
            boolean pneumatic = neighborState.getBlock() instanceof PneumaticConnection;
            return PhysicalGraph.predictArm(state, property, neighborState, pneumatic);
        }

        boolean air = canConnectToAir(level, pos, direction);
        if (level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube) {
            int bit = 1 << direction.ordinal();
            int faces = tube.airFaces & ~bit;
            if (air) faces |= bit;
            tube.setAirFaces(faces);
        }
        boolean pneumatic = canConnectTo(level, pos, direction);
        return state.setValue(property, pneumatic || air);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TransportBlockData.PNEUMATIC_COLLISION[faceMask(state)];
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = faceMask(state);
        if (level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube) {
            if (tube.intakeFace != null) mask |= 1 << tube.intakeFace.ordinal();
            if (tube.outletFace != null) mask |= 1 << tube.outletFace.ordinal();
        }
        return TransportBlockData.PNEUMATIC_SELECTION[mask];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)
                || !tube.hasIntake() && !tube.hasOutlet()) return InteractionResult.PASS;

        BlockMenus.open(player, tube);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        return screwDirections(level, player, pos);
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
            PneumaticNetwork.addNode(server, pos);
        }
        if (level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube) tube.refreshPower();
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PneumaticNetwork.removeNode(level, pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumaticTubeBlockEntity(pos, state);
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
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
        if (level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube) tube.refreshPower();
        refreshAir(level, pos);
        if (level instanceof ServerLevel server) {
            PhysicalGraph.redraw(level.getBlockState(pos), server, pos);
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PNEUMATIC_TUBE)
                .fluidIn()
                .fluidFaces(
                        PneumaticTubeBlockEntity.class,
                        (tube, face) ->
                                face.fluid() == null
                                        || tube.acceptsFluid(face.fluid(), face.side()));
    }

    @java.lang.Override
    protected net.minecraft.world.level.block.state.BlockState rotate(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.Rotation rotation) {
        if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_90)
            return state.setValue(NORTH, state.getValue(WEST))
                    .setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(WEST, state.getValue(SOUTH));
        if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_180)
            return state.setValue(NORTH, state.getValue(SOUTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(WEST, state.getValue(EAST));
        if (rotation == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90)
            return state.setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
        return state;
    }

    @java.lang.Override
    protected net.minecraft.world.level.block.state.BlockState mirror(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.Mirror mirror) {
        if (mirror == net.minecraft.world.level.block.Mirror.LEFT_RIGHT)
            return state.setValue(NORTH, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(NORTH));
        if (mirror == net.minecraft.world.level.block.Mirror.FRONT_BACK)
            return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
        return state;
    }
}
