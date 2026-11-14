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

package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.CableConductorBlock;
import com.hbm.blocks.network.CablePaintableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CablePaintableBlockEntity extends PipePaintableBlockEntity {

    public CablePaintableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_PAINTABLE.get(), pos, state);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        CableConductorBlock.mintNode(
                server, worldPosition, getBlockState(), CableConductorBlock.OPEN_ALL);
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (!nbt.contains("paintblock")) return;
        setCamo(Block.stateById(nbt.getIntOr("paintblock", 0)));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CablePaintableBlock
                && !state.getValue(CablePaintableBlock.PAINTED)) {
            level.setBlock(
                    pos, state.setValue(CablePaintableBlock.PAINTED, true), Block.UPDATE_ALL);
        }
    }
}
