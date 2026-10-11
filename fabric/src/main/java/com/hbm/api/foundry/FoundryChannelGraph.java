// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.foundry;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class FoundryChannelGraph implements IGraphProvider<@Nullable NTMMaterial> {

    public static final FoundryChannelGraph PROVIDER = new FoundryChannelGraph();

    public static final LevelNodeGraph.Kind<@Nullable NTMMaterial> TYPE =
            LevelNodeGraph.kind(PROVIDER, "foundry_channel_graph");

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
    public boolean dataCompatible(@Nullable NTMMaterial a, @Nullable NTMMaterial b) {
        return true;
    }

    @Override
    public Codec<List<@Nullable NTMMaterial>> dataTableCodec() {
        return Codec.INT
                .listOf()
                .xmap(FoundryChannelGraph::decodeMaterials, FoundryChannelGraph::encodeMaterials);
    }

    private static List<@Nullable NTMMaterial> decodeMaterials(List<Integer> ids) {
        List<@Nullable NTMMaterial> materials = new ArrayList<>(ids.size());
        for (int id : ids) {
            NTMMaterial material = id == -1 ? null : Mats.matById.get(id);
            if (id != -1 && material == null)
                throw new IllegalArgumentException("Unknown foundry material id " + id);
            materials.add(material);
        }
        return materials;
    }

    private static List<Integer> encodeMaterials(List<@Nullable NTMMaterial> materials) {
        List<Integer> ids = new ArrayList<>(materials.size());
        for (NTMMaterial material : materials) ids.add(material == null ? -1 : material.id);
        return ids;
    }
}
