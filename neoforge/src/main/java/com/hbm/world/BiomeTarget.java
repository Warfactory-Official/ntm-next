// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public record BiomeTarget(List<TagKey<Biome>> tags, List<ResourceKey<Biome>> biomes) {

    @SafeVarargs
    public static BiomeTarget tags(TagKey<Biome>... tags) {
        return new BiomeTarget(List.of(tags), List.of());
    }

    public static BiomeTarget vanilla(String... paths) {
        return new BiomeTarget(
                List.of(),
                Arrays.stream(paths)
                        .map(
                                path ->
                                        ResourceKey.create(
                                                Registries.BIOME,
                                                Identifier.withDefaultNamespace(path)))
                        .toList());
    }
}
