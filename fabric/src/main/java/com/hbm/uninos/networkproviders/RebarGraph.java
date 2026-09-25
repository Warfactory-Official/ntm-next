// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.networkproviders;

import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.GraphSegment;
import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class RebarGraph implements IGraphProvider<Unit> {

    public static final RebarGraph PROVIDER = new RebarGraph();

    public static final SavedDataType<LevelNodeGraph<Unit>> TYPE =
            LevelNodeGraph.type(PROVIDER, "rebar_graph");

    public static final int OPEN_ALL = 0b111111;

    private RebarGraph() {}

    public static LevelNodeGraph<Unit> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, TYPE);
    }

    public static long[] lowestLayer(NodeNetwork<Unit> net) {
        if (net.attachment instanceof long[] cached) return cached;
        Layer layer = new Layer();
        for (GraphNode<Unit> node : net.nodes) layer.admit(node.posKey);
        for (GraphSegment<Unit> segment : net.segments) {
            for (int i = 0; i < segment.cellCount(); i++) layer.admit(segment.cellAt(i));
        }
        long[] cells = layer.cells.toLongArray();
        net.attachment = cells;
        return cells;
    }

    @Override
    public Unit createData(BlockState state) {
        return Unit.INSTANCE;
    }

    @Override
    public boolean dataCompatible(Unit a, Unit b) {
        return true;
    }

    @Override
    public Codec<Unit> dataCodec() {
        return Unit.CODEC;
    }

    private static final class Layer {
        final LongArrayList cells = new LongArrayList();
        int y = Integer.MAX_VALUE;

        void admit(long cell) {
            int cellY = BlockPos.getY(cell);
            if (cellY > y) return;
            if (cellY < y) {
                y = cellY;
                cells.clear();
            }
            cells.add(cell);
        }
    }
}
