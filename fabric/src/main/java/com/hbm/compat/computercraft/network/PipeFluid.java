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

package com.hbm.compat.computercraft.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

final class PipeFluid {

    private PipeFluid() {}

    static String legacyName(BlockEntity pipe) {
        long key = pipe.getBlockPos().asLong();
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt((ServerLevel) pipe.getLevel(), key);
        PipeData data = graph == null ? null : graph.dataAt(key);
        return NTMFluids.legacyName(data == null ? null : data.fluid());
    }
}
