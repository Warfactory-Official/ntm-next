// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.foundry;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class FoundryChannelGraph implements IGraphProvider<@Nullable NTMMaterial> {

    public static final FoundryChannelGraph PROVIDER = new FoundryChannelGraph();

    public static final SavedDataType<LevelNodeGraph<@Nullable NTMMaterial>> TYPE =
            LevelNodeGraph.type(PROVIDER, "foundry_channel_graph");

    public static final int OPEN_HORIZONTALS =
            (1 << Direction.NORTH.ordinal())
                    | (1 << Direction.SOUTH.ordinal())
                    | (1 << Direction.WEST.ordinal())
                    | (1 << Direction.EAST.ordinal());

    private FoundryChannelGraph() {}

    public static LevelNodeGraph<@Nullable NTMMaterial> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, TYPE);
    }

    @Override
    public @Nullable NTMMaterial createData(BlockState state) {
        return null;
    }

    @Override
    public boolean dataCompatible(@Nullable NTMMaterial a, @Nullable NTMMaterial b) {
        return true;
    }

    @Override
    public Codec<@Nullable NTMMaterial> dataCodec() {
        return Codec.INT.xmap(id -> Mats.matById.get(id), mat -> mat == null ? -1 : mat.id);
    }
}
