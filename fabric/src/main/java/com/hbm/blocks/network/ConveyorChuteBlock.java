// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConveyorChuteBlock extends ConveyorBlockBase {

    public static final MapCodec<ConveyorChuteBlock> CODEC = simpleCodec(ConveyorChuteBlock::new);
    public static final BooleanProperty FEEDS_DOWN = BooleanProperty.create("feeds_down");

    public ConveyorChuteBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(FEEDS_DOWN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public static boolean feedsOn(BlockGetter level, BlockPos pos) {
        return ConveyorNeighbors.receiverAt(level, pos.below());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FEEDS_DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(FEEDS_DOWN, feedsOn(context.getLevel(), context.getClickedPos()));
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
        return direction == Direction.DOWN
                ? state.setValue(FEEDS_DOWN, feedsOn(level, pos))
                : state;
    }

    private static boolean falling(BlockGetter level, BlockPos pos, Vec3 itemPos) {
        return level.getBlockState(pos).getValue(FEEDS_DOWN) || itemPos.y > pos.getY() + 0.25;
    }

    @Override
    public Direction getInputDirection(Level level, BlockPos pos) {
        return Direction.UP;
    }

    @Override
    public Direction getOutputDirection(Level level, BlockPos pos) {
        return Direction.DOWN;
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        if (falling(level, pos, itemPos)) return Direction.UP;
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        if (level.getBlockState(pos).getValue(FEEDS_DOWN)) speed *= 5;
        else if (itemPos.y > pos.getY() + 0.25) speed *= 3;

        return super.getTravelLocation(level, pos, itemPos, speed);
    }

    @Override
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        if (falling(level, pos, itemPos)) {
            return new Snap(new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5), itemPos);
        }
        return super.snap(level, pos, itemPos);
    }

    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        level.setBlock(
                pos,
                ModBlocks.CONVEYOR
                        .get()
                        .defaultBlockState()
                        .setValue(FACING, state.getValue(FACING)),
                UPDATE_ALL);
    }
}
