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

package com.hbm.blocks.gas;

import com.hbm.particle.AshRevealParticleOptions;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GasBlock extends Block {

    public static BooleanSupplier REVEALED_TO_VIEWER = () -> false;

    protected GasBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .replaceable()
                .noCollision()
                .noOcclusion()
                .instabreak()
                .noLootTable()
                .pushReaction(((PushReaction) (PushReaction.POPPED)));
    }

    protected abstract AshRevealParticleOptions revealTint();

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (REVEALED_TO_VIEWER.getAsBoolean()) {
            level.addParticle(
                    revealTint(),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var firstDirection = getFirstDirection(level, pos, random);
        if (tryMove(level, pos, firstDirection)) {
            return;
        }

        var secondDirection = getSecondDirection(level, pos, random);
        if (tryMove(level, pos, secondDirection)) {
            return;
        }

        level.scheduleTick(pos, this, getDelay(level));
    }

    protected boolean tryMove(ServerLevel level, BlockPos pos, Direction direction) {
        var destination = pos.relative(direction);
        if (!level.getBlockState(destination).isAir()) {
            return false;
        }

        level.removeBlock(pos, false);
        level.setBlockAndUpdate(destination, defaultBlockState());
        return true;
    }

    public abstract Direction getFirstDirection(Level level, BlockPos pos, RandomSource random);

    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return getFirstDirection(level, pos, random);
    }

    protected Direction randomHorizontal(RandomSource random) {
        return Direction.from2DDataValue(random.nextInt(4));
    }

    public int getDelay(Level level) {
        return 2;
    }
}
