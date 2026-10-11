// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public final class FluidPipeGraphProvider implements IGraphProvider<PipeData> {

    public static final FluidPipeGraphProvider INSTANCE = new FluidPipeGraphProvider();

    private FluidPipeGraphProvider() {}

    @Override
    public boolean dataCompatible(PipeData a, PipeData b) {
        return a.equals(b);
    }

    @Override
    public Codec<List<PipeData>> dataTableCodec() {
        return PipeData.CODEC.listOf();
    }

    @Override
    public void tick(ServerLevel level, LevelNodeGraph<PipeData> graph) {
        FluidNetwork.tick(level, graph);
    }
}
