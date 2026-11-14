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

package com.hbm.world.placement;

import com.hbm.data.DataGroups;
import com.hbm.data.WorldData;
import com.hbm.registration.Registrar;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

public final class WorldConfigPlacement implements PlacementFilter {
    public static final MapCodec<WorldConfigPlacement> CODEC =
            WorldData.WORLDGEN_SWITCH
                    .listOf()
                    .fieldOf("switches")
                    .xmap(WorldConfigPlacement::new, placement -> placement.switches);

    private final List<DataGroups.Param<Boolean>> switches;

    public WorldConfigPlacement(List<DataGroups.Param<Boolean>> switches) {
        this.switches = List.copyOf(switches);
    }

    public static void register() {
        Registrar.registerPlacementModifierType("world_config", CODEC);
    }

    public boolean open() {
        for (DataGroups.Param<Boolean> gate : switches) {
            if (!gate.get()) return false;
        }
        return true;
    }

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return open();
    }

    @Override
    public MapCodec<WorldConfigPlacement> codec() {
        return CODEC;
    }
}
