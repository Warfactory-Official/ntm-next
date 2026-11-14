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

package com.hbm.api.energymk2;

import com.hbm.util.ChunkUtil;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class ForeignEnergyAdapters {
    private static ForeignEnergyAdapter[] adapters = new ForeignEnergyAdapter[0];

    private ForeignEnergyAdapters() {}

    public static void register(ForeignEnergyAdapter adapter) {
        adapters = Arrays.copyOf(adapters, adapters.length + 1);
        adapters[adapters.length - 1] = adapter;
    }

    public static @Nullable ForeignEnergyNode node(
            ServerLevel level, BlockPos pos, Direction side) {
        if (adapters.length == 0) return null;
        BlockEntity be = ChunkUtil.blockEntityIfLoaded(level, pos);
        if (be == null || be.isRemoved()) return null;
        for (ForeignEnergyAdapter adapter : adapters) {
            ForeignEnergyNode node = adapter.node(level, pos, side, be);
            if (node != null) return node;
        }
        return null;
    }

    public static void committed(ServerLevel level, BlockPos pos) {
        if (adapters.length == 0) return;
        BlockEntity be = ChunkUtil.blockEntityIfLoaded(level, pos);
        if (be == null) return;
        for (ForeignEnergyAdapter adapter : adapters) adapter.committed(be);
    }
}
