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

import com.hbm.blockentity.prop.BehemothLanternBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCellBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.RepairLookOverlay;
import com.hbm.extprop.PlayerState;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.RepairHandler;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class LanternBehemothBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, ToolInteractionHandler, LookOverlayProvider {

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    public LanternBehemothBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BROKEN);
    }

    @Override
    public int cellSound() {
        return MultiblockCellBlock.SOUND_METAL;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos core,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.TORCH || !RepairHandler.tryRepair(level, core, player)) return false;
        if (!level.isClientSide()) {
            PlayerState props = PlayerState.getOrCreate(player);
            if (props.reputation < 25) props.reputation++;
        }
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        RepairLookOverlay.build(level, pos, info);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BehemothLanternBlockEntity(pos, state);
    }
}
