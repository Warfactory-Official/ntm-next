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

package com.hbm.api.foundry;

import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.uninos.graph.GraphProvider;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class FoundryChannelGraph implements GraphProvider<@Nullable Material> {

    public static final FoundryChannelGraph PROVIDER = new FoundryChannelGraph();

    public static final LevelNodeGraph.Kind<@Nullable Material> TYPE =
            LevelNodeGraph.kind(PROVIDER, "foundry_channel_graph");

    public static final int OPEN_HORIZONTALS =
            (1 << Direction.NORTH.ordinal())
                    | (1 << Direction.SOUTH.ordinal())
                    | (1 << Direction.WEST.ordinal())
                    | (1 << Direction.EAST.ordinal());

    private FoundryChannelGraph() {}

    public static LevelNodeGraph<@Nullable Material> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, TYPE);
    }

    @Override
    public boolean dataCompatible(@Nullable Material a, @Nullable Material b) {
        return true;
    }

    @Override
    public Codec<List<@Nullable Material>> dataTableCodec() {
        return Codec.INT
                .listOf()
                .xmap(FoundryChannelGraph::decodeMaterials, FoundryChannelGraph::encodeMaterials);
    }

    private static List<@Nullable Material> decodeMaterials(List<Integer> ids) {
        List<@Nullable Material> materials = new ArrayList<>(ids.size());
        for (int id : ids) {
            Material material = id == -1 ? null : MaterialRegistry.byId(id);
            if (id != -1 && material == null)
                throw new IllegalArgumentException("Unknown foundry material id " + id);
            materials.add(material);
        }
        return materials;
    }

    private static List<Integer> encodeMaterials(List<@Nullable Material> materials) {
        List<Integer> ids = new ArrayList<>(materials.size());
        for (Material material : materials) ids.add(material == null ? -1 : material.id());
        return ids;
    }
}
