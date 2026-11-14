/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.machine.pile;

import com.hbm.blockentity.machine.pile.PileFuelBlockEntity;
import com.hbm.interfaces.FanInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GraphiteFuelBlock extends DrilledGraphiteEntityBlock implements FanInteractionHandler {
    public GraphiteFuelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PU239);
    }

    @Override
    public PileFuelBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var fuel = new PileFuelBlockEntity(pos, state);
        if (state.getValue(PU239)) {
            fuel.progress = PileFuelBlockEntity.maxProgress() - 1000;
        }
        return fuel;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof PileFuelBlockEntity fuel)) {
            return 0;
        }
        int maximum = PileFuelBlockEntity.maxProgress() - 1000;
        int signal = (fuel.progress * 15) / maximum;
        return Mth.clamp(signal, 0, 15);
    }

    @Override
    protected Item getInsertedItem(BlockState state) {
        if (state.getValue(PU239)) {
            return ModItems.PILE_ROD_PU239.get();
        }
        return ModItems.PILE_ROD_URANIUM.get();
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
        if (tool == ToolType.SCREWDRIVER) {
            return super.onToolUse(level, player, pos, side, hit, tool);
        }
        if (tool == ToolType.HAND_DRILL) {
            if (player == null) {
                return false;
            }
            if (!level.isClientSide()) {
                var fuel = (PileFuelBlockEntity) level.getBlockEntity(pos);
                reportAssembly(player, pos);
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.block.graphiteFuel.heat",
                                        fuel.heat + "/" + PileFuelBlockEntity.maxHeat)
                                .withStyle(ChatFormatting.YELLOW));
                reportFuelProgress(
                        player,
                        fuel.progress,
                        PileFuelBlockEntity.maxProgress(),
                        fuel.lastNeutrons);
                if (level.getBlockState(pos).getValue(PU239)) {
                    player.sendSystemMessage(
                            Component.translatable("desc.block.graphiteFuel.pu239Rich")
                                    .withStyle(ChatFormatting.DARK_GREEN));
                }
            }
        }
        return true;
    }

    @Override
    public void applyFan(Level level, BlockPos pos, Direction stream, int distance) {
        var fuel = (PileFuelBlockEntity) level.getBlockEntity(pos);
        int oldHeat = fuel.heat;
        int newHeat = (int) (oldHeat - oldHeat * 0.025);
        if (newHeat != oldHeat) {
            fuel.heat = newHeat;
            level.blockEntityChanged(pos);
        }
    }
}
