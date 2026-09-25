// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class ConveyorBendableBlock extends ConveyorBlockBase
        implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final EnumProperty<ConveyorBend> BEND =
            EnumProperty.create("bend", ConveyorBend.class);

    protected ConveyorBendableBlock(Properties props) {
        super(props);
        registerDefaultState(
                defaultBlockState()
                        .setValue(BEND, ConveyorBend.STRAIGHT)
                        .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BEND, WATERLOGGED);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(BEND, state.getValue(BEND).mirrored());
    }

    @Override
    public Direction getOutputDirection(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Direction primary = state.getValue(FACING);

        return switch (state.getValue(BEND)) {
            case STRAIGHT -> primary;
            case LEFT -> primary.getCounterClockWise();
            case RIGHT -> primary.getClockWise();
        };
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        BlockState state = level.getBlockState(pos);
        Direction primary = state.getValue(FACING).getOpposite();
        ConveyorBend bend = state.getValue(BEND);

        if (bend != ConveyorBend.STRAIGHT) {
            int turn = bend.ordinal() - 1;
            Direction secondary = primary.getClockWise();

            double ix =
                    pos.getX()
                            + 0.5
                            - (-primary.getStepX() * 0.5 + secondary.getStepX() * (0.5 - turn));
            double iz =
                    pos.getZ()
                            + 0.5
                            - (-primary.getStepZ() * 0.5 + secondary.getStepZ() * (0.5 - turn));

            if (Math.abs(itemPos.x - ix) + Math.abs(itemPos.z - iz) >= 1) {
                return turn == 0 ? secondary.getOpposite() : secondary;
            }
        }

        return primary;
    }

    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BEND, state.getValue(BEND).next()), UPDATE_ALL);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        return state == null
                ? null
                : state.setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
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
