// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.particle.HbmEffectNT;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WasteLeaves extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final MapCodec<WasteLeaves> CODEC = simpleCodec(WasteLeaves::new);

    private static final int SHED_CHANCE = 30;
    private static final int LEAF_CHANCE = 7;
    private static final int FALL_TIME = 2;

    public WasteLeaves(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(
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

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(SHED_CHANCE) == 0) {
            level.removeBlock(pos, false);

            if (level.getBlockState(pos.below()).isAir()) {
                FallingBlockEntity leaves =
                        new FallingBlockEntity(EntityTypes.FALLING_BLOCK, level);
                leaves.blockState = ModBlocks.LEAVES_LAYER.get().defaultBlockState();
                leaves.blocksBuilding = true;
                leaves.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                leaves.setDeltaMovement(Vec3.ZERO);
                leaves.xo = leaves.getX();
                leaves.yo = leaves.getY();
                leaves.zo = leaves.getZ();
                leaves.setStartPos(pos);
                leaves.time = FALL_TIME;
                leaves.dropItem = false;
                level.addFreshEntity(leaves);
            }
        }

        super.randomTick(state, level, pos, rand);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);

        if (rand.nextInt(LEAF_CHANCE) == 0 && level.getBlockState(pos.below()).isAir()) {
            ClientEffects.spawn(
                    HbmEffectNT.DeadLeaf,
                    level,
                    pos.getX() + rand.nextDouble(),
                    pos.getY() - 0.05D,
                    pos.getZ() + rand.nextDouble(),
                    1F);
        }
    }
}
