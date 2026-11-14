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

package com.hbm.blocks.machine.pile;

import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class GraphiteRodBlock extends DrilledGraphiteBlock {

    public GraphiteRodBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    static void toggleRun(
            Level level, BlockPos pos, BlockState match, Direction dir, boolean withdrawn) {
        BlockPos.MutableBlockPos cursor = pos.mutable().move(dir);
        while (true) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof GraphiteRodBlock)) return;
            if (state.getValue(AXIS) != match.getValue(AXIS)) return;
            if (state.getValue(SHROUDED) != match.getValue(SHROUDED)) return;
            if (state.getValue(WITHDRAWN) != match.getValue(WITHDRAWN)) return;
            level.setBlock(cursor, state.setValue(WITHDRAWN, withdrawn), 3);
            cursor.move(dir);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WITHDRAWN);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        Direction side = hit.getDirection();
        if (!isChannelFace(state, side)) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean withdrawn = state.getValue(WITHDRAWN);
        level.setBlock(pos, state.setValue(WITHDRAWN, !withdrawn), 3);

        level.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.3F,
                withdrawn ? 0.65F : 0.75F);

        toggleRun(level, pos, state, side, !withdrawn);
        toggleRun(level, pos, state, side.getOpposite(), !withdrawn);

        return InteractionResult.SUCCESS;
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_BORON.get();
    }
}
