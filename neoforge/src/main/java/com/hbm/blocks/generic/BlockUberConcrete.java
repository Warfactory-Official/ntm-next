// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class BlockUberConcrete extends Block {

    public static final MapCodec<BlockUberConcrete> CODEC = simpleCodec(BlockUberConcrete::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 15);

    private static final Direction[] SPREAD_SIDES = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    public BlockUberConcrete(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<BlockUberConcrete> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (random.nextInt(age + 1) > 0) return;

        if (age < 15) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
            return;
        }

        level.removeBlock(pos, false);
        BlockState broken = ModBlocks.CONCRETE_SUPER_BROKEN.get().defaultBlockState();

        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlockAndUpdate(pos, broken);
            return;
        }

        List<Direction> sides = new ArrayList<>(List.of(SPREAD_SIDES));
        while (!sides.isEmpty()) {
            Direction dir = sides.remove(random.nextInt(sides.size()));
            BlockPos target = pos.relative(dir);
            if (level.getBlockState(target).isAir()
                    && level.getBlockState(target.below()).isAir()) {
                FallingBlockEntity debris = FallingBlockEntity.fall(level, target, broken);
                debris.time = 2;
                debris.setHurtsEntities(2.0F, 40);
                debris.disableDrop();
                return;
            }
        }

        level.setBlockAndUpdate(pos, broken);
    }
}
