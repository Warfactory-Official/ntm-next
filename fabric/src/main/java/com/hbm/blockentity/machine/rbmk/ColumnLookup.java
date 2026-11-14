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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

final class ColumnLookup {

    static final Direction[] SIDES = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private ColumnLookup() {}

    static @Nullable RBMKBaseBlockEntity column(ServerLevel level, BlockPos.MutableBlockPos cell) {
        return column(level, cell, cell);
    }

    static @Nullable RBMKBaseBlockEntity column(
            Level level, BlockPos.MutableBlockPos cell, BlockPos.MutableBlockPos probe) {
        BlockState state = ChunkUtil.blockStateIfLoaded(level, cell);
        if (state == null || !(state.getBlock() instanceof RBMKBlock block)) return null;
        if (block.isCellState(state)) {
            long core =
                    level instanceof ServerLevel server
                            ? MultiblockSurface.indexedCorePacked(
                                    server, cell.getX(), cell.getY(), cell.getZ())
                            : MultiblockSurface.clientCorePacked(level, cell, probe);
            if (!MultiblockSurface.hasCore(core)) return null;
            cell.set(core);
        }
        return ChunkUtil.blockEntityIfLoaded(level, cell) instanceof RBMKBaseBlockEntity column
                ? column
                : null;
    }
}
