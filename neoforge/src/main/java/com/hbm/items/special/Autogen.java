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

package com.hbm.items.special;

import com.hbm.NuclearTech;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class Autogen {

    private static final List<RegistryHandle<MaterialShapeItem>> ITEMS = new ArrayList<>();
    private static final Map<MaterialShape, Map<Material, RegistryHandle<MaterialShapeItem>>>
            INDEX = new EnumMap<>(MaterialShape.class);

    private Autogen() {}

    public static void register() {
        register(NuclearTech.MOD_ID);
    }

    public static void register(String namespace) {
        Reg.Namespace reg = Reg.in(namespace);
        for (Material mat : MaterialRegistry.ordered()) {
            for (MaterialShape shape : mat.autoShapes()) {
                if (shape.itemPrefix() == null) continue;
                Map<Material, RegistryHandle<MaterialShapeItem>> byMaterial =
                        INDEX.computeIfAbsent(shape, ignored -> new IdentityHashMap<>());
                if (byMaterial.containsKey(mat)) continue;
                String name = shape.autogenItemId(mat.name());
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

    public static MaterialShapeItem require(MaterialShape shape, Material mat) {
        MaterialShapeItem item = find(shape, mat);
        if (item == null) {
            throw new IllegalStateException(mat.name() + " autogens no " + shape.plural());
        }
        return item;
    }

    public static @Nullable MaterialShapeItem find(MaterialShape shape, Material mat) {
        RegistryHandle<MaterialShapeItem> handle = handle(shape, mat);
        return handle == null ? null : handle.get();
    }

    public static @Nullable RegistryHandle<MaterialShapeItem> handle(
            MaterialShape shape, Material mat) {
        Map<Material, RegistryHandle<MaterialShapeItem>> byMaterial = INDEX.get(shape);
        return byMaterial == null ? null : byMaterial.get(mat);
    }
}
