// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public interface IGraphProvider<D> {
    boolean dataCompatible(D a, D b);

    Codec<List<D>> dataTableCodec();

    default void onNetsMerged(NodeNetwork<D> keep, NodeNetwork<D> drain) {}

    default void tick(ServerLevel level, LevelNodeGraph<D> graph) {}
}
