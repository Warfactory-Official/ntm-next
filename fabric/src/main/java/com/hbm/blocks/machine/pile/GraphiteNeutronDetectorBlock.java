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

import com.hbm.blockentity.machine.pile.PileNeutronDetectorBlockEntity;
import com.hbm.interfaces.ToolType;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GraphiteNeutronDetectorBlock extends DrilledGraphiteEntityBlock {
    public GraphiteNeutronDetectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WITHDRAWN);
    }

    @Override
    public PileNeutronDetectorBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PileNeutronDetectorBlockEntity(pos, state);
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_DETECTOR.get();
    }

    public void triggerRods(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        boolean withdrawn = state.getValue(WITHDRAWN);
        boolean nextWithdrawn = !withdrawn;
        level.setBlock(pos, state.setValue(WITHDRAWN, nextWithdrawn), Block.UPDATE_ALL);
        level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                ModSounds.TECH_BLEEP.get(),
                SoundSource.BLOCKS,
                0.02F,
                1.0F);
        var direction =
                Direction.fromAxisAndDirection(
                        state.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
        GraphiteRodBlock.toggleRun(level, pos, state, direction, nextWithdrawn);
        GraphiteRodBlock.toggleRun(level, pos, state, direction.getOpposite(), nextWithdrawn);
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (player != null && player.isSpectator()) {
            return false;
        }
        if (tool != ToolType.SCREWDRIVER && tool != ToolType.DEFUSER) {
            return true;
        }
        if (player == null) {
            return false;
        }
        if (tool == ToolType.SCREWDRIVER && !player.isShiftKeyDown()) {
            return super.onToolUse(level, player, pos, side, hit, tool);
        }
        if (level.isClientSide()) {
            return true;
        }

        var detector = (PileNeutronDetectorBlockEntity) level.getBlockEntity(pos);
        if (tool == ToolType.SCREWDRIVER) {
            reportAssembly(player, pos);
            player.sendSystemMessage(
                    Component.translatable(
                                    "desc.shared.flux",
                                    detector.previousDeposit + "/" + detector.tripThreshold)
                            .withStyle(ChatFormatting.YELLOW));
        } else if (player.isShiftKeyDown()) {
            if (detector.tripThreshold > 1) {
                detector.tripThreshold--;
                level.blockEntityChanged(pos);
            }
        } else {
            detector.tripThreshold++;
            level.blockEntityChanged(pos);
        }
        return true;
    }
}
