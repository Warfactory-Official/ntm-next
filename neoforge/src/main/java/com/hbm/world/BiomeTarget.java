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
