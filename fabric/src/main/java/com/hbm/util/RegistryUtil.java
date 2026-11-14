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

package com.hbm.util;

import com.hbm.NuclearTech;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class RegistryUtil {

    private RegistryUtil() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NuclearTech.MOD_ID, path);
    }

    public static Identifier resolve(String reference) {
        return reference.indexOf(':') < 0 ? id(reference) : Identifier.parse(reference);
    }

    public static <T> T valueOrThrow(Registry<T> registry, Identifier id) {
        return registry.getValueOrThrow(ResourceKey.create(registry.key(), id));
    }

    public static <T> T valueOrThrow(Registry<T> registry, String reference) {
        return valueOrThrow(registry, resolve(reference));
    }

    public static <T> Identifier keyOrThrow(Registry<T> registry, T value) {
        return registry.getResourceKey(value).orElseThrow().identifier();
    }

    public static Identifier keyOf(Block block) {
        return keyOrThrow(BuiltInRegistries.BLOCK, block);
    }

    public static Identifier keyOf(Fluid fluid) {
        return keyOrThrow(BuiltInRegistries.FLUID, fluid);
    }

    public static Block block(String reference) {
        return valueOrThrow(BuiltInRegistries.BLOCK, reference);
    }

    public static Item item(String reference) {
        return valueOrThrow(BuiltInRegistries.ITEM, reference);
    }

    public static Fluid fluid(Identifier id) {
        return valueOrThrow(BuiltInRegistries.FLUID, id);
    }
}
