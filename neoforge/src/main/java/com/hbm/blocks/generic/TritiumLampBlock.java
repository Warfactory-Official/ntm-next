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

package com.hbm.blocks.generic;

import com.hbm.blocks.machine.LightBeamSource;
import com.hbm.blocks.machine.SpotlightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class TritiumLampBlock extends Block implements LightBeamSource {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final int TURN_OFF_DELAY = 4;
    private static final int BEAM_LENGTH = 8;

    public TritiumLampBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public int getBeamRange() {
        return BEAM_LENGTH;
    }

    @Override
    public boolean emitsLightBeam(BlockState state, Direction dir) {
        return state.getValue(LIT);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !state.is(oldState.getBlock()))
            update(state, server, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel server) update(state, server, pos);
    }

    private void update(BlockState state, ServerLevel level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean lit = state.getValue(LIT);
        if (lit && !powered) level.scheduleTick(pos, this, TURN_OFF_DELAY);
        else if (!lit && powered) setLit(level, pos, state, true);
        if (state.getValue(LIT)) beam(level, pos, true);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && !level.hasNeighborSignal(pos)) setLit(level, pos, state, false);
    }

    private void setLit(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
        level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
        beam(level, pos, lit);
    }

    private void beam(ServerLevel level, BlockPos pos, boolean lit) {
        for (Direction dir : Direction.VALUES) {
            if (lit) SpotlightBlock.propagateBeam(level, pos, dir, BEAM_LENGTH);
            else SpotlightBlock.unpropagateBeam(level, pos, dir, BEAM_LENGTH);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        beam(level, pos, false);
    }
}
