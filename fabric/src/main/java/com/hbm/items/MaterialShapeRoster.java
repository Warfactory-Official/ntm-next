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

package com.hbm.items;

import com.hbm.NuclearTech;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.items.special.MaterialShapeItem;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class MaterialShapeRoster {

    private MaterialShapeRoster() {}

    public static void register(
            MaterialShape shape,
            String namePrefix,
            String modelPath,
            Set<Material> excluded,
            List<RegistryHandle<MaterialShapeItem>> out) {
        register(shape, namePrefix, modelPath, excluded, ignored -> new Item.Properties(), out);
    }

    public static void register(
            MaterialShape shape,
            String namePrefix,
            String modelPath,
            Set<Material> excluded,
            Function<Material, Item.Properties> properties,
            List<RegistryHandle<MaterialShapeItem>> out) {
        register(
                shape,
                namePrefix,
                excluded,
                ignored -> RegistryUtil.id(modelPath),
                properties,
                out);
    }

    public static void registerNamed(
            MaterialShape shape,
            Set<Material> excluded,
            List<RegistryHandle<MaterialShapeItem>> out) {
        registerNamed(shape, namedRoster(shape, excluded), ignored -> new Item.Properties(), out);
    }

    public static void registerNamed(
            MaterialShape shape,
            List<Material> materials,
            Function<Material, Item.Properties> properties,
            List<RegistryHandle<MaterialShapeItem>> out) {
        for (Material mat : namedMaterials(shape, materials)) {
            RegistryHandle<MaterialShapeItem> bespoke = indexed(shape, mat);
            if (bespoke != null) {
                out.add(bespoke);
                continue;
            }
            String name = mat.namedId(shape);
            RegistryHandle<MaterialShapeItem> handle =
                    Reg.item(
                            name,
                            RegistryUtil.id(name),
                            props -> new MaterialShapeItem(props, mat, shape),
                            () -> properties.apply(mat));
            index(shape, mat, handle);
            out.add(handle);
        }
    }

    public static void register(
            MaterialShape shape,
            String namePrefix,
            Set<Material> excluded,
            Function<Material, Identifier> models,
            Function<Material, Item.Properties> properties,
            List<RegistryHandle<MaterialShapeItem>> out) {
        for (Material mat : roster(shape, excluded)) {
            RegistryHandle<MaterialShapeItem> handle =
                    Reg.item(
                            namePrefix + "_" + mat.name(),
                            models.apply(mat),
                            props -> new MaterialShapeItem(props, mat, shape),
                            () -> properties.apply(mat));
            index(shape, mat, handle);
            out.add(handle);
        }
    }

    public static RegistryHandle<MaterialShapeItem> registerNamed(
            String namespace, MaterialShape shape, Material mat, Supplier<Item.Properties> props) {
        String name = mat.namedId(shape);
        List.of(mat);
        RegistryHandle<MaterialShapeItem> handle =
                Reg.in(namespace)
                        .item(
                                name,
                                Identifier.fromNamespaceAndPath(namespace, name),
                                properties -> new MaterialShapeItem(properties, mat, shape),
                                props);
        index(shape, mat, handle);
        ADDON_NAMED.add(handle);
        return handle;
    }

    private static final List<RegistryHandle<MaterialShapeItem>> ADDON_NAMED = new ArrayList<>();

    public static List<RegistryHandle<MaterialShapeItem>> addonNamed() {
        return ADDON_NAMED;
    }

    private static final Set<String> CLAIMED = new HashSet<>();

    public static void claim(Material material, MaterialShape shape) {
        CLAIMED.add(material.name() + "/" + shape.plural());
    }

    public static Reg.Handle<MaterialShapeItem> bespoke(
            MaterialShape shape, Material material, ItemFactory factory) {
        String name = material.namedId(shape);
        Reg.Handle<MaterialShapeItem> handle =
                Reg.item(
                        name,
                        RegistryUtil.id(name),
                        props -> factory.create(props, material, shape),
                        Item.Properties::new);
        index(shape, material, handle);
        return handle;
    }

    @FunctionalInterface
    public interface ItemFactory {
        MaterialShapeItem create(
                Item.Properties properties, Material material, MaterialShape shape);
    }

    public static List<Material> roster(MaterialShape shape, Set<Material> excluded) {
        List<Material> list = new ArrayList<>();
        for (Material mat : MaterialRegistry.ordered()) {
            if (excluded.contains(mat)) continue;
            if (CLAIMED.contains(mat.name() + "/" + shape.plural())) continue;
            if (mat.declares(shape)) list.add(mat);
        }
        return list;
    }

    public static List<Material> namedRoster(MaterialShape shape, Set<Material> excluded) {
        List<Material> candidates =
                roster(shape, excluded).stream()
                        .filter(mat -> mat.namespace().equals(NuclearTech.MOD_ID))
                        .toList();
        return namedMaterials(shape, candidates);
    }

    private static List<Material> namedMaterials(MaterialShape shape, List<Material> candidates) {

        return candidates.stream().filter(mat -> mat.namedId(shape) != null).toList();
    }

    private static final Map<MaterialShape, Map<Material, RegistryHandle<MaterialShapeItem>>>
            INDEX = new EnumMap<>(MaterialShape.class);

    private static void index(
            MaterialShape shape, Material material, RegistryHandle<MaterialShapeItem> handle) {
        RegistryHandle<MaterialShapeItem> clash =
                INDEX.computeIfAbsent(shape, ignored -> new IdentityHashMap<>())
                        .put(material, handle);
        if (clash != null) {
            throw new IllegalStateException(
                    material.name()
                            + " registers "
                            + shape.plural()
                            + " twice: "
                            + clash.id()
                            + " and "
                            + handle.id());
        }
    }

    private static @Nullable RegistryHandle<MaterialShapeItem> indexed(
            MaterialShape shape, Material material) {
        Map<Material, RegistryHandle<MaterialShapeItem>> byMaterial = INDEX.get(shape);
        return byMaterial == null ? null : byMaterial.get(material);
    }

    public static MaterialShapeItem require(MaterialShape shape, Material material) {
        MaterialShapeItem item = find(shape, material);
        if (item == null) {
            throw new IllegalStateException(material.name() + " declares no " + shape.plural());
        }
        return item;
    }

    public static @Nullable MaterialShapeItem find(MaterialShape shape, Material material) {
        RegistryHandle<MaterialShapeItem> handle = indexed(shape, material);
        return handle == null ? null : handle.get();
    }
}
