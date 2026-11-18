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

import com.hbm.blockentity.door.DoorBaseBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.interfaces.ToolType;
import com.hbm.interfaces.ToolTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public abstract class DoorBaseBlock extends MultiblockCoreBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    volatile byte controls;

    protected DoorBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    public final boolean pulseListed() {
        return (controls & DoorControls.PULSE) != 0;
    }

    public final boolean toolableListed() {
        return (controls & DoorControls.TOOLABLE) != 0;
    }

    public final boolean redstoneOnlyListed() {
        return (controls & DoorControls.REDSTONE_ONLY) != 0;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!level.isClientSide()
                && toolableListed()
                && held.getItem() instanceof ToolTypeProvider tool
                && tool.getToolType() == ToolType.SCREWDRIVER)
            return ((DoorBaseBlockEntity) level.getBlockEntity(core)).toggleRedstoneOnly(player)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
