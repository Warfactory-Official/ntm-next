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

package com.hbm.world.feature;

import com.hbm.util.RegistryUtil;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class JungleDungeonMarkerFeature implements Feature {
    public static final MapCodec<JungleDungeonMarkerFeature> CODEC =
            MapCodec.unit(JungleDungeonMarkerFeature::new);

    public static final Identifier TEMPLATE = RegistryUtil.id("jungle/surface_marker");
    private static final ResourceKey<Structure> DUNGEON =
            ResourceKey.create(Registries.STRUCTURE, RegistryUtil.id("jungle_dungeon"));

    @Override
    public MapCodec<JungleDungeonMarkerFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        int chunkX = featureOrigin.getX() >> 4, chunkZ = featureOrigin.getZ() >> 4;
        var structure =
                level.registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .getOrThrow(DUNGEON)
                        .value();
        var start =
                level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS, false)
                        .getStartForStructure(structure);
        if (start == null || !start.isValid()) return false;
        int x = start.getChunkPos().getMiddleBlockX(), z = start.getChunkPos().getMiddleBlockZ();
        BlockPos origin = new BlockPos(x, WorldgenHeight.lightBlocking(level, x, z), z);
        var template =
                level.getLevel()
                        .getStructureTemplateManager()
                        .get(TEMPLATE)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "missing jungle dungeon marker template "
                                                        + TEMPLATE));
        return template.placeInWorld(
                level, origin, origin, new StructurePlaceSettings(), random, 2);
    }
}
