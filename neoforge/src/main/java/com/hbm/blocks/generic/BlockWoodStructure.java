// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ClimbBox;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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

public class BlockWoodStructure extends Block implements ClimbBox, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final AABB CELL = new AABB(0, 0, 0, 1, 1, 1);
    private static final VoxelShape SCAFFOLD_SUPPORT =
            Shapes.or(Kind.SCAFFOLD.shape, Shapes.box(0, 0.9375, 0, 1, 1, 1));

    public final Kind kind;
    private final MapCodec<BlockWoodStructure> ownCodec;

    public BlockWoodStructure(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
        this.ownCodec = simpleCodec(p -> new BlockWoodStructure(kind, p));
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return ownCodec;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {

        return kind == Kind.SCAFFOLD ? Shapes.block() : kind.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind.shape;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return kind == Kind.SCAFFOLD ? SCAFFOLD_SUPPORT : Shapes.empty();
    }

    @Override
    public @Nullable AABB climbBox(BlockState state) {
        return kind == Kind.SCAFFOLD ? CELL : null;
    }

    public enum Kind {
        ROOF(Shapes.box(0, 0, 0, 1, 0.1875, 1)),
        SCAFFOLD(Shapes.box(0.0625, 0, 0.0625, 0.9375, 1, 0.9375)),
        CEILING(Shapes.box(0, 0.875, 0, 1, 1, 1));

        final VoxelShape shape;

        Kind(VoxelShape shape) {
            this.shape = shape;
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
