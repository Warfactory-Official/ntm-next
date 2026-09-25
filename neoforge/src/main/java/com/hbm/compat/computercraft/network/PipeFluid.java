// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
