// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidPipeGraph {

    public static final LevelNodeGraph.Kind<PipeData> TYPE =
            LevelNodeGraph.kind(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph");

    public static final LevelNodeGraph.Kind<PipeData> TYPE_SMOKE =
            LevelNodeGraph.kind(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph_smoke");
    public static final LevelNodeGraph.Kind<PipeData> TYPE_SMOKE_LEADED =
            LevelNodeGraph.kind(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph_smoke_leaded");
    public static final LevelNodeGraph.Kind<PipeData> TYPE_SMOKE_POISON =
            LevelNodeGraph.kind(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph_smoke_poison");

    @SuppressWarnings("unchecked")
    public static final LevelNodeGraph.Kind<PipeData>[] KINDS =
            new LevelNodeGraph.Kind[] {TYPE, TYPE_SMOKE, TYPE_SMOKE_LEADED, TYPE_SMOKE_POISON};

    private FluidPipeGraph() {}

    public static Fluid[] smokes() {
        return new Fluid[] {NTMFluids.SMOKE, NTMFluids.SMOKE_LEADED, NTMFluids.SMOKE_POISON};
    }

    public static boolean isSmoke(Fluid fluid) {
        return fluid == NTMFluids.SMOKE
                || fluid == NTMFluids.SMOKE_LEADED
                || fluid == NTMFluids.SMOKE_POISON;
    }

    public static LevelNodeGraph<PipeData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, TYPE);
    }

    public static LevelNodeGraph<PipeData> get(ServerLevel level, Fluid fluid) {
        return LevelNodeGraph.getOrCreate(level, typeFor(fluid));
    }

    public static LevelNodeGraph.Kind<PipeData> typeFor(Fluid fluid) {
        if (fluid == NTMFluids.SMOKE) return TYPE_SMOKE;
        if (fluid == NTMFluids.SMOKE_LEADED) return TYPE_SMOKE_LEADED;
        if (fluid == NTMFluids.SMOKE_POISON) return TYPE_SMOKE_POISON;
        return TYPE;
    }

    @SuppressWarnings("unchecked")
    public static LevelNodeGraph<PipeData>[] all(ServerLevel level) {
        LevelNodeGraph<PipeData>[] graphs = new LevelNodeGraph[KINDS.length];
        for (int i = 0; i < KINDS.length; i++)
            graphs[i] = LevelNodeGraph.getOrCreate(level, KINDS[i]);
        return graphs;
    }

    public static @Nullable LevelNodeGraph<PipeData> graphAt(ServerLevel level, long posKey) {
        for (LevelNodeGraph.Kind<PipeData> kind : KINDS) {
            LevelNodeGraph<PipeData> graph = LevelNodeGraph.getOrCreate(level, kind);
            if (graph.containsCell(posKey)) return graph;
        }
        return null;
    }

    public static @Nullable PipeData dataAt(ServerLevel level, long posKey) {
        for (LevelNodeGraph.Kind<PipeData> kind : KINDS) {
            PipeData data = LevelNodeGraph.getOrCreate(level, kind).dataAt(posKey);
            if (data != null) return data;
        }
        return null;
    }
}
