// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
