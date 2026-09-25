// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class FluidPipeGraph {

    public static final SavedDataType<LevelNodeGraph<PipeData>> TYPE =
            LevelNodeGraph.type(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph");

    public static final SavedDataType<LevelNodeGraph<PipeData>> TYPE_SMOKE =
            LevelNodeGraph.type(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph_smoke");
    public static final SavedDataType<LevelNodeGraph<PipeData>> TYPE_SMOKE_LEADED =
            LevelNodeGraph.type(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph_smoke_leaded");
    public static final SavedDataType<LevelNodeGraph<PipeData>> TYPE_SMOKE_POISON =
            LevelNodeGraph.type(FluidPipeGraphProvider.INSTANCE, "fluid_pipe_graph_smoke_poison");

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

    public static SavedDataType<LevelNodeGraph<PipeData>> typeFor(Fluid fluid) {
        if (fluid == NTMFluids.SMOKE) return TYPE_SMOKE;
        if (fluid == NTMFluids.SMOKE_LEADED) return TYPE_SMOKE_LEADED;
        if (fluid == NTMFluids.SMOKE_POISON) return TYPE_SMOKE_POISON;
        return TYPE;
    }

    public static LevelNodeGraph<PipeData>[] all(ServerLevel level) {
        @SuppressWarnings("unchecked")
        LevelNodeGraph<PipeData>[] graphs =
                new LevelNodeGraph[] {
                    LevelNodeGraph.getOrCreate(level, TYPE),
                    LevelNodeGraph.getOrCreate(level, TYPE_SMOKE),
                    LevelNodeGraph.getOrCreate(level, TYPE_SMOKE_LEADED),
                    LevelNodeGraph.getOrCreate(level, TYPE_SMOKE_POISON)
                };
        return graphs;
    }

    public static @Nullable LevelNodeGraph<PipeData> graphAt(ServerLevel level, long posKey) {
        LevelNodeGraph<PipeData> graph = LevelNodeGraph.getOrCreate(level, TYPE);
        if (graph.containsCell(posKey)) return graph;
        graph = LevelNodeGraph.getOrCreate(level, TYPE_SMOKE);
        if (graph.containsCell(posKey)) return graph;
        graph = LevelNodeGraph.getOrCreate(level, TYPE_SMOKE_LEADED);
        if (graph.containsCell(posKey)) return graph;
        graph = LevelNodeGraph.getOrCreate(level, TYPE_SMOKE_POISON);
        return graph.containsCell(posKey) ? graph : null;
    }

    public static @Nullable PipeData dataAt(ServerLevel level, long posKey) {
        PipeData data = LevelNodeGraph.getOrCreate(level, TYPE).dataAt(posKey);
        if (data != null) return data;
        data = LevelNodeGraph.getOrCreate(level, TYPE_SMOKE).dataAt(posKey);
        if (data != null) return data;
        data = LevelNodeGraph.getOrCreate(level, TYPE_SMOKE_LEADED).dataAt(posKey);
        if (data != null) return data;
        return LevelNodeGraph.getOrCreate(level, TYPE_SMOKE_POISON).dataAt(posKey);
    }
}
