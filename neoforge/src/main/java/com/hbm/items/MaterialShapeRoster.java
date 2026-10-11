// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.NuclearTech;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.special.MaterialShapeItem;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
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
            MaterialShapes shape,
            String namePrefix,
            String modelPath,
            Set<NTMMaterial> excluded,
            List<RegistryHandle<MaterialShapeItem>> out) {
        register(shape, namePrefix, modelPath, excluded, ignored -> new Item.Properties(), out);
    }

    public static void register(
            MaterialShapes shape,
            String namePrefix,
            String modelPath,
            Set<NTMMaterial> excluded,
            Function<NTMMaterial, Item.Properties> properties,
            List<RegistryHandle<MaterialShapeItem>> out) {
        register(shape, namePrefix, excluded, ignored -> Library.id(modelPath), properties, out);
    }

    public static void registerNamed(
            MaterialShapes shape,
            Set<NTMMaterial> excluded,
            List<RegistryHandle<MaterialShapeItem>> out) {
        registerNamed(shape, namedRoster(shape, excluded), ignored -> new Item.Properties(), out);
    }

    public static void registerNamed(
            MaterialShapes shape,
            List<NTMMaterial> materials,
            Function<NTMMaterial, Item.Properties> properties,
            List<RegistryHandle<MaterialShapeItem>> out) {
        for (NTMMaterial mat : namedMaterials(shape, materials)) {
            RegistryHandle<MaterialShapeItem> bespoke = indexed(shape, mat);
            if (bespoke != null) {
                out.add(bespoke);
                continue;
            }
            String name = mat.legacyId(shape);
            RegistryHandle<MaterialShapeItem> handle =
                    Reg.item(
                            name,
                            Library.id(name),
                            props -> new MaterialShapeItem(props, mat, shape),
                            () -> properties.apply(mat));
            index(shape, mat, handle);
            out.add(handle);
        }
    }

    public static void register(
            MaterialShapes shape,
            String namePrefix,
            Set<NTMMaterial> excluded,
            Function<NTMMaterial, Identifier> models,
            Function<NTMMaterial, Item.Properties> properties,
            List<RegistryHandle<MaterialShapeItem>> out) {
        for (NTMMaterial mat : roster(shape, excluded)) {
            RegistryHandle<MaterialShapeItem> handle =
                    Reg.item(
                            namePrefix + "_" + mat.tagPath,
                            models.apply(mat),
                            props -> new MaterialShapeItem(props, mat, shape),
                            () -> properties.apply(mat));
            index(shape, mat, handle);
            out.add(handle);
        }
    }

    public static RegistryHandle<MaterialShapeItem> registerNamed(
            String namespace,
            MaterialShapes shape,
            NTMMaterial mat,
            Supplier<Item.Properties> props) {
        String name = mat.legacyId(shape);
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

    public static void claim(NTMMaterial material, MaterialShapes shape) {
        CLAIMED.add(material.tagPath + "/" + shape.name());
    }

    public static Reg.Handle<MaterialShapeItem> bespoke(
            MaterialShapes shape, NTMMaterial material, ItemFactory factory) {
        String name = material.legacyId(shape);
        Reg.Handle<MaterialShapeItem> handle =
                Reg.item(
                        name,
                        Library.id(name),
                        props -> factory.create(props, material, shape),
                        Item.Properties::new);
        index(shape, material, handle);
        return handle;
    }

    @FunctionalInterface
    public interface ItemFactory {
        MaterialShapeItem create(
                Item.Properties properties, NTMMaterial material, MaterialShapes shape);
    }

    public static List<NTMMaterial> roster(MaterialShapes shape, Set<NTMMaterial> excluded) {
        List<NTMMaterial> list = new ArrayList<>();
        for (NTMMaterial mat : Mats.orderedList) {
            if (excluded.contains(mat)) continue;
            if (CLAIMED.contains(mat.tagPath + "/" + shape.name())) continue;
            if (mat.autogen.contains(shape) || mat.tagOnly.contains(shape)) list.add(mat);
        }
        return list;
    }

    public static List<NTMMaterial> namedRoster(MaterialShapes shape, Set<NTMMaterial> excluded) {
        List<NTMMaterial> candidates =
                roster(shape, excluded).stream()
                        .filter(mat -> mat.namespace.equals(NuclearTech.MOD_ID))
                        .toList();
        return namedMaterials(shape, candidates);
    }

    private static List<NTMMaterial> namedMaterials(
            MaterialShapes shape, List<NTMMaterial> candidates) {

        return candidates.stream().filter(mat -> mat.legacyId(shape) != null).toList();
    }

    private static final Map<MaterialShapes, Map<NTMMaterial, RegistryHandle<MaterialShapeItem>>>
            INDEX = new IdentityHashMap<>();

    private static void index(
            MaterialShapes shape, NTMMaterial material, RegistryHandle<MaterialShapeItem> handle) {
        RegistryHandle<MaterialShapeItem> clash =
                INDEX.computeIfAbsent(shape, ignored -> new IdentityHashMap<>())
                        .put(material, handle);
        if (clash != null) {
            throw new IllegalStateException(
                    material.tagPath
                            + " registers "
                            + shape.name()
                            + " twice: "
                            + clash.id()
                            + " and "
                            + handle.id());
        }
    }

    private static @Nullable RegistryHandle<MaterialShapeItem> indexed(
            MaterialShapes shape, NTMMaterial material) {
        Map<NTMMaterial, RegistryHandle<MaterialShapeItem>> byMaterial = INDEX.get(shape);
        return byMaterial == null ? null : byMaterial.get(material);
    }

    public static MaterialShapeItem require(MaterialShapes shape, NTMMaterial material) {
        MaterialShapeItem item = find(shape, material);
        if (item == null) {
            throw new IllegalStateException(material.tagPath + " declares no " + shape.name());
        }
        return item;
    }

    public static @Nullable MaterialShapeItem find(MaterialShapes shape, NTMMaterial material) {
        RegistryHandle<MaterialShapeItem> handle = indexed(shape, material);
        return handle == null ? null : handle.get();
    }
}
