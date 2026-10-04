// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockStorage {
    private static final Set<String> registeredFamilies = new HashSet<>();
    private static ClassValue<Boolean> supported = cache();

    private BlockStorage() {}

    public static void register(Class<? extends BlockEntity> family) {
        if (!TrackedStorage.class.isAssignableFrom(family))
            throw new IllegalArgumentException("No slot adapter: " + family.getName());
        if (registeredFamilies.add(family.getName())) supported = cache();
    }

    public static boolean supported(BlockEntity owner) {
        return supported.get(owner.getClass());
    }

    private static ClassValue<Boolean> cache() {
        return new ClassValue<>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
                boolean neoForge = "NeoForge".equals("NeoForge");
                boolean admitted = false;
                for (Class<?> parent = type;
                        !admitted && parent != null;
                        parent = parent.getSuperclass()) {
                    String name = parent.getName();
                    var declaration = StorageLayouts.DECLARATIONS.get(name);
                    admitted =
                            registeredFamilies.contains(name)
                                    || declaration != null
                                            && (declaration.loader() == StorageLayouts.Loader.COMMON
                                                    || neoForge)
                                            && (declaration.admission()
                                                            == StorageLayouts.Admission.FAMILY
                                                    || parent == type
                                                            && declaration.admission()
                                                                    == StorageLayouts.Admission
                                                                            .EXACT);
                }
                if (admitted && !TrackedStorage.class.isAssignableFrom(type))
                    throw new IllegalStateException("Missing slot adapter: " + type.getName());
                return admitted;
            }
        };
    }
}
