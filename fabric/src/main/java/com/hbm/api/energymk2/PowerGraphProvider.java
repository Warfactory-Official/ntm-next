// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public final class PowerGraphProvider implements IGraphProvider<CableData> {

    public static final PowerGraphProvider INSTANCE = new PowerGraphProvider();

    private PowerGraphProvider() {}

    @Override
    public boolean dataCompatible(CableData a, CableData b) {
        return true;
    }

    @Override
    public Codec<List<CableData>> dataTableCodec() {
        return CableData.CODEC.listOf();
    }

    @Override
    public void onNetsMerged(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        PowerNetwork.mergeBridges(keep, drain);
    }

    @Override
    public void tick(ServerLevel level, LevelNodeGraph<CableData> graph) {
        PowerNetwork.tick(level, graph);
    }
}
