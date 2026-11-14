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

package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidCounterValveBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.interfaces.LookOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FluidCounterValveBlock extends FluidValveBlock implements TickingEntityBlock {

    public FluidCounterValveBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidCounterValveBlockEntity(pos, state);
    }

    public void restoreNode(ServerLevel level, BlockPos pos, BlockState state) {
        createNodes(level, pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        super.buildLookOverlay(level, pos, info);
        if (level.getBlockEntity(pos) instanceof FluidCounterValveBlockEntity valve) {
            info.row(Component.translatable("overlay.hbm.counter", valve.getTotal()));
        }
    }
}
