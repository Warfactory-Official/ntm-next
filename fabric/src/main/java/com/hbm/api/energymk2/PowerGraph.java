// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;

public final class PowerGraph {

    public static final LevelNodeGraph.Kind<CableData> TYPE =
            LevelNodeGraph.kind(PowerGraphProvider.INSTANCE, "power_graph");

    private PowerGraph() {}

    public static LevelNodeGraph<CableData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, TYPE);
    }
}
