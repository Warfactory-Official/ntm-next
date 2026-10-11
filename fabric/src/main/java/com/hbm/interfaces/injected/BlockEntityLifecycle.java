// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.uninos.graph.EndpointRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BlockEntityLifecycle {
    default void hbm$declareEndpoint(ServerLevel level, BlockEntity be) {
        EndpointRegistry.declareSelfIfEndpoint(level, be);
    }

    default void hbm$blockEntityLoaded(ServerLevel level, BlockEntity be) {}

    default void hbm$blockEntityRemoved(ServerLevel level, BlockEntity be) {}
}
