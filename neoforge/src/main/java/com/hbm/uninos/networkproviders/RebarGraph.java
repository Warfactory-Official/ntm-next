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

package com.hbm.uninos.networkproviders;

import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.GraphProvider;
import com.hbm.uninos.graph.GraphSegment;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;

public final class RebarGraph implements GraphProvider<Unit> {

    public static final RebarGraph PROVIDER = new RebarGraph();

    public static final LevelNodeGraph.Kind<Unit> TYPE =
            LevelNodeGraph.kind(PROVIDER, "rebar_graph");

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
    public boolean dataCompatible(Unit a, Unit b) {
        return true;
    }

    @Override
    public Codec<List<Unit>> dataTableCodec() {
        return Unit.CODEC.listOf();
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
