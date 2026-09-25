// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DigammaMatter extends Block {

    public static final MapCodec<DigammaMatter> CODEC = simpleCodec(DigammaMatter::new);

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    private static final int SHAPE_POOL = 64;
    private static final VoxelShape[] SHAPES = new VoxelShape[SHAPE_POOL];

    static {
        RandomSource rand = RandomSource.create(0L);
        for (int i = 0; i < SHAPE_POOL; i++) {
            double p = 1D / 16D;
            SHAPES[i] =
                    Shapes.box(
                            rand.nextInt(9) * p,
                            rand.nextInt(9) * p,
                            rand.nextInt(9) * p,
                            1D - rand.nextInt(9) * p,
                            1D - rand.nextInt(9) * p,
                            1D - rand.nextInt(9) * p);
        }
    }

    public DigammaMatter(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<DigammaMatter> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {

        return SHAPES[Math.floorMod(pos.hashCode(), SHAPE_POOL)];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (!level.isClientSide())
            level.scheduleTick(pos, this, 10 + level.getRandom().nextInt(40));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (age >= 7) return;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int dist = Math.abs(i) + Math.abs(j) + Math.abs(k);
                    if (dist == 0 || dist >= 3) continue;
                    BlockPos to = pos.offset(i, j, k);
                    if (level.getBlockState(to).getBlock() == this) continue;
                    level.setBlock(to, defaultBlockState().setValue(AGE, age + 1), UPDATE_ALL);
                }
            }
        }
    }
}
