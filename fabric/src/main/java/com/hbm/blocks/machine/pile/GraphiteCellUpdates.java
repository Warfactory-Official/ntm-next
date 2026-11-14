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

import com.hbm.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GraphiteCellUpdates {
    public static void publish(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        var status = level.getChunkAt(pos).getFullStatus();
        if (status != null && status.isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
            level.sendBlockUpdated(pos, oldState, newState, Block.UPDATE_ALL);
        }

        level.updateNeighborsAt(pos, oldState.getBlock());
        if (newState.hasAnalogOutputSignal()) {
            Platform.updateNeighbourForOutputSignal(level, pos, newState.getBlock());
        }

        int updateLimit = Block.UPDATE_LIMIT - 1;
        oldState.updateIndirectNeighbourShapes(level, pos, Block.UPDATE_CLIENTS, updateLimit);
        newState.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS, updateLimit);
        newState.updateIndirectNeighbourShapes(level, pos, Block.UPDATE_CLIENTS, updateLimit);
    }
}
