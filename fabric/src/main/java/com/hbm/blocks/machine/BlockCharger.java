// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityCharger;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockCharger extends HorizontalDirectionalBlock
        implements ITickingBlock, ICapabilityBlock {

    public static final MapCodec<BlockCharger> CODEC = simpleCodec(BlockCharger::new);

    private final VoxelShape[] shapesByFacing = new VoxelShape[4];

    public BlockCharger(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            shapesByFacing[dir.get2DDataValue()] = buildShape(dir);
        }
    }

    private static VoxelShape buildShape(Direction facing) {
        double lo = 5.0 / 16, hi = 11.0 / 16, yLo = 0.25, yHi = 0.75, thin = 4.0 / 16;
        return switch (facing.getOpposite()) {
            case NORTH -> Shapes.box(lo, yLo, 0, hi, yHi, thin);
            case SOUTH -> Shapes.box(lo, yLo, 1 - thin, hi, yHi, 1);
            case WEST -> Shapes.box(0, yLo, lo, thin, yHi, hi);
            case EAST -> Shapes.box(1 - thin, yLo, lo, 1, yHi, hi);
            default -> Shapes.box(lo, yLo, lo, hi, yHi, hi);
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapesByFacing[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCharger(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.NTM_CHARGER)
                .powerIn()
                .fe()
                .faces(BlockEntityCharger.class, BlockEntityCharger::acceptsFace);
    }
}
