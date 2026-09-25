// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PowerGraph {

    public static final SavedDataType<LevelNodeGraph<CableData>> TYPE =
            LevelNodeGraph.type(PowerGraphProvider.INSTANCE, "power_graph");

    private PowerGraph() {}

    public static LevelNodeGraph<CableData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, TYPE);
    }
}
