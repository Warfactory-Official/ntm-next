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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConveyorLiftBlock extends ConveyorBlockBase {

    public static final MapCodec<ConveyorLiftBlock> CODEC = simpleCodec(ConveyorLiftBlock::new);
    public static final EnumProperty<ConveyorLiftPart> PART =
            EnumProperty.create("part", ConveyorLiftPart.class);

    private static final VoxelShape HEAD = Shapes.box(0D, 0D, 0D, 1D, 0.5D, 1D);

    public ConveyorLiftBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(PART, ConveyorLiftPart.BOTTOM));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    public static ConveyorLiftPart partAt(BlockGetter level, BlockPos pos) {
        if (!ConveyorNeighbors.beltAt(level, pos.below())) return ConveyorLiftPart.BOTTOM;
        return ConveyorNeighbors.receiverAt(level, pos.above())
                ? ConveyorLiftPart.MIDDLE
                : ConveyorLiftPart.TOP;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(PART, partAt(context.getLevel(), context.getClickedPos()));
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
        if (direction != Direction.UP && direction != Direction.DOWN) return state;
        return state.setValue(PART, partAt(level, pos));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == ConveyorLiftPart.TOP ? HEAD : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public Direction getInputDirection(Level level, BlockPos pos) {
        return Direction.DOWN;
    }

    @Override
    public Direction getOutputDirection(Level level, BlockPos pos) {
        return Direction.UP;
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        if (level.getBlockState(pos).getValue(PART) != ConveyorLiftPart.TOP) return Direction.DOWN;
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        if (level.getBlockState(pos).getValue(PART) != ConveyorLiftPart.TOP) {
            return new Snap(new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5), itemPos);
        }
        return super.snap(level, pos, itemPos);
    }

    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        level.setBlock(
                pos,
                ModBlocks.CONVEYOR_CHUTE
                        .get()
                        .defaultBlockState()
                        .setValue(FACING, state.getValue(FACING))
                        .setValue(
                                ConveyorChuteBlock.FEEDS_DOWN,
                                ConveyorChuteBlock.feedsOn(level, pos)),
                UPDATE_ALL);
    }
}
