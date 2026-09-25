// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockSteelScaffold extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final MapCodec<BlockSteelScaffold> CODEC = simpleCodec(BlockSteelScaffold::new);
    public static final EnumProperty<Orient> ORIENT = EnumProperty.create("orient", Orient.class);

    private static final VoxelShape Z_THIN = Shapes.box(0D, 0D, 2 / 16D, 1D, 1D, 14 / 16D);
    private static final VoxelShape Y_THIN = Shapes.box(0D, 2 / 16D, 0D, 1D, 14 / 16D, 1D);
    private static final VoxelShape X_THIN = Shapes.box(2 / 16D, 0D, 0D, 14 / 16D, 1D, 1D);

    public BlockSteelScaffold(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(ORIENT, Orient.NS_UPRIGHT)
                        .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90)
            return state;
        return state.setValue(
                ORIENT,
                switch (state.getValue(ORIENT)) {
                    case NS_UPRIGHT -> Orient.EW_UPRIGHT;
                    case EW_UPRIGHT -> Orient.NS_UPRIGHT;
                    case EW_FLAT -> Orient.NS_FLAT;
                    case NS_FLAT -> Orient.EW_FLAT;
                });
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENT, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction placed = ctx.getClickedFace();
        Orient orient;
        if (placed.getAxis() == Direction.Axis.Y) {
            orient =
                    ctx.getHorizontalDirection().getAxis() == Direction.Axis.Z
                            ? Orient.NS_UPRIGHT
                            : Orient.EW_UPRIGHT;
        } else if (placed.getAxis() == Direction.Axis.Z) {
            orient = Orient.EW_FLAT;
        } else {
            orient = Orient.NS_FLAT;
        }
        return defaultBlockState()
                .setValue(ORIENT, orient)
                .setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(ORIENT)) {
            case NS_UPRIGHT -> Z_THIN;
            case EW_UPRIGHT -> X_THIN;
            case EW_FLAT, NS_FLAT -> Y_THIN;
        };
    }

    public enum Orient implements StringRepresentable {
        NS_UPRIGHT("ns_upright"),
        EW_FLAT("ew_flat"),
        EW_UPRIGHT("ew_upright"),
        NS_FLAT("ns_flat");

        private final String name;

        Orient(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
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
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(
                state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
