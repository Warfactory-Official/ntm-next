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

package com.hbm.inventory.machine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

public final class CustomMachineDefinitions {

    private static volatile Map<ResourceKey<CustomMachineDefinition>, CustomMachineDefinition>
            definitions = Map.of();

    private CustomMachineDefinitions() {}

    public static void applyDataPack(RegistryAccess registries) {
        Map<ResourceKey<CustomMachineDefinition>, CustomMachineDefinition> ordered =
                new LinkedHashMap<>();
        registries
                .lookup(CustomMachineDefinition.REGISTRY)
                .ifPresent(
                        registry -> {
                            List<ResourceKey<CustomMachineDefinition>> keys =
                                    new ArrayList<>(registry.registryKeySet());
                            keys.sort(Comparator.comparing(key -> key.identifier().toString()));
                            for (ResourceKey<CustomMachineDefinition> key : keys)
                                ordered.put(key, registry.getValueOrThrow(key));
                        });
        definitions = ordered;
    }

    public static @Nullable CustomMachineDefinition get(
            @Nullable ResourceKey<CustomMachineDefinition> key) {
        return key == null ? null : definitions.get(key);
    }

    public static Map<ResourceKey<CustomMachineDefinition>, CustomMachineDefinition> all() {
        return definitions;
    }
}
