// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

public interface IGraphProvider<D> {

    D createData(BlockState state);

    boolean dataCompatible(D a, D b);

    Codec<D> dataCodec();

    default boolean hasTieredRates() {
        return false;
    }

    default void onNetsMerged(NodeNetwork<D> keep, NodeNetwork<D> drain) {}

    default long rateOf(D data) {
        return Long.MAX_VALUE;
    }
}
