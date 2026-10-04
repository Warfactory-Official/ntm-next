// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public final class NeoForgeBiomeInjection {
    public static final List<Request> REQUESTS = new ArrayList<>();
    public static final List<SpawnRequest> SPAWNS = new ArrayList<>();

    @SafeVarargs
    public static void addFeaturesToBiome(
            BiomeTarget biomes,
            GenerationStep.Decoration step,
            ResourceKey<PlacedFeature>... placedFeatures) {
        REQUESTS.add(new Request(biomes, step, List.of(placedFeatures)));
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
