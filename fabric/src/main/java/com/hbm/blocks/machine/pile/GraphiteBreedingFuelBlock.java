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

import com.hbm.blockentity.machine.pile.PileBreedingFuelBlockEntity;
import com.hbm.interfaces.ToolType;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GraphiteBreedingFuelBlock extends DrilledGraphiteEntityBlock {
    public GraphiteBreedingFuelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public PileBreedingFuelBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PileBreedingFuelBlockEntity(pos, state);
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_LITHIUM.get();
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
                var fuel = (PileBreedingFuelBlockEntity) level.getBlockEntity(pos);
                reportAssembly(player, pos);
                reportFuelProgress(
                        player,
                        fuel.progress,
                        PileBreedingFuelBlockEntity.maxProgress(),
                        fuel.lastNeutrons);
            }
        }
        return true;
    }
}
