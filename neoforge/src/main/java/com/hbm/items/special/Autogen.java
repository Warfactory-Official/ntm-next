// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.NuclearTech;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class Autogen {

    private static final List<RegistryHandle<MaterialShapeItem>> ITEMS = new ArrayList<>();
    private static final Map<MaterialShapes, Map<NTMMaterial, RegistryHandle<MaterialShapeItem>>>
            INDEX = new IdentityHashMap<>();

    private Autogen() {}

    public static void register() {
        register(NuclearTech.MOD_ID);
    }

    public static void register(String namespace) {
        Reg.Namespace reg = Reg.in(namespace);
        for (NTMMaterial mat : Mats.orderedList) {
            for (MaterialShapes shape : mat.autogen) {
                if (shape.autogenItemPrefix == null) continue;
                Map<NTMMaterial, RegistryHandle<MaterialShapeItem>> byMaterial =
                        INDEX.computeIfAbsent(shape, ignored -> new IdentityHashMap<>());
                if (byMaterial.containsKey(mat)) continue;
                String name = shape.autogenItemId(mat.tagPath).getPath();
                String layer = "item/" + AutogenIcons.sprite(shape, mat);
                RegistryHandle<MaterialShapeItem> handle =
                        reg.item(
                                name,
                                Identifier.fromNamespaceAndPath(namespace, name),
                                props -> new MaterialShapeItem(props, mat, shape),
                                Item.Properties::new);
                byMaterial.put(mat, handle);
                ITEMS.add(handle);
            }
        }
    }

    public static List<RegistryHandle<MaterialShapeItem>> items() {
        return ITEMS;
    }

    public static MaterialShapeItem require(MaterialShapes shape, NTMMaterial mat) {
        MaterialShapeItem item = find(shape, mat);
        if (item == null) {
            throw new IllegalStateException(mat.tagPath + " autogens no " + shape.name());
        }
        return item;
    }

    public static @Nullable MaterialShapeItem find(MaterialShapes shape, NTMMaterial mat) {
        RegistryHandle<MaterialShapeItem> handle = handle(shape, mat);
        return handle == null ? null : handle.get();
    }

    public static @Nullable RegistryHandle<MaterialShapeItem> handle(
            MaterialShapes shape, NTMMaterial mat) {
        Map<NTMMaterial, RegistryHandle<MaterialShapeItem>> byMaterial = INDEX.get(shape);
        return byMaterial == null ? null : byMaterial.get(mat);
    }
}
