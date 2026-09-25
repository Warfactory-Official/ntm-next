// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface GraphResident {

    static void onLoad(ServerLevel server, BlockEntity be) {
        LevelNodeGraph.invalidateEndpointsAround(server, be.getBlockPos());

        if (!(be.getBlockState().getBlock() instanceof BlockMultiblockCore)) {
            EndpointRegistry.declareSelfIfEndpoint(server, be);
        }
        ((GraphResident) be).onGraphLoad(server);
    }

    default void onGraphLoad(ServerLevel server) {}
}
