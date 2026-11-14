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

package com.hbm.platform;

import com.hbm.world.BiomeTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class BiomeInjection {
    public static final List<Request> REQUESTS = new ArrayList<>();
    public static final List<SpawnRequest> SPAWNS = new ArrayList<>();

    public static List<Request> requests() {
        return REQUESTS;
    }

    public static List<SpawnRequest> spawns() {
        return SPAWNS;
    }

    @SafeVarargs
    public static void addFeaturesToBiome(
            BiomeTarget biomes,
            GenerationStep.Decoration step,
            ResourceKey<PlacedFeature>... placedFeatures) {
        REQUESTS.add(new Request(biomes, step, List.of(placedFeatures)));
    }

    public static void addSpawn(
            BiomeTarget biomes,
            TagKey<Biome> excluded,
            Supplier<? extends EntityType<?>> type,
            int weight,
            int min,
            int max) {
        SPAWNS.add(new SpawnRequest(biomes, excluded, type, weight, min, max));
    }

    public record SpawnRequest(
            BiomeTarget biomes,
            TagKey<Biome> excluded,
            Supplier<? extends EntityType<?>> type,
            int weight,
            int min,
            int max) {}

    public record Request(
            BiomeTarget biomes,
            GenerationStep.Decoration step,
            List<ResourceKey<PlacedFeature>> features) {}
}
