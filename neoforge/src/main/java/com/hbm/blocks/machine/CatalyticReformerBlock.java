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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.CatalyticReformerBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CatalyticReformerBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, PersistentItemTooltipProvider {

    public CatalyticReformerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CATALYTIC_REFORMER)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .fe()
                .faces(
                        CatalyticReformerBlockEntity.class,
                        (_, side) -> side != Direction.DOWN)
                .fluidFaces(
                        CatalyticReformerBlockEntity.class,
                        (_, face) -> face.side() != Direction.DOWN);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CatalyticReformerBlockEntity(pos, state);
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        var contents = stack.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents == null) return;

        for (int tankIndex = 0; tankIndex < contents.size(); tankIndex++) {
            int capacity = CatalyticReformerBlockEntity.OUTPUT_TANK_CAPACITY;
            if (tankIndex == 0) capacity = CatalyticReformerBlockEntity.INPUT_TANK_CAPACITY;
            adder.accept(PersistentItemTooltipProvider.tankLine(contents.get(tankIndex), capacity));
        }
    }
}
