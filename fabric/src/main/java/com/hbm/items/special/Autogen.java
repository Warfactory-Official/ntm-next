// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.Library;
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
        for (NTMMaterial mat : Mats.orderedList) {
            for (MaterialShapes shape : mat.autogen) {
                if (shape.autogenItemPrefix == null) continue;
                String name = shape.autogenItemId(mat.tagPath).getPath();

                boolean owns = AutogenIcons.ownsSprite(shape, mat);

                String layer = "item/" + AutogenIcons.sprite(shape, mat);
                Identifier model = owns ? Library.id(name) : Library.id(shape.autogenItemPrefix);
                RegistryHandle<MaterialShapeItem> handle =
                        Reg.item(
                                name,
                                model,
                                props -> new MaterialShapeItem(props, mat, shape),
                                Item.Properties::new);
                RegistryHandle<MaterialShapeItem> clash =
                        INDEX.computeIfAbsent(shape, ignored -> new IdentityHashMap<>())
                                .put(mat, handle);
                if (clash != null) {
                    throw new IllegalStateException(
                            mat.tagPath
                                    + " autogens "
                                    + shape.name()
                                    + " twice: "
                                    + clash.id()
                                    + " and "
                                    + handle.id());
                }
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
        Map<NTMMaterial, RegistryHandle<MaterialShapeItem>> byMaterial = INDEX.get(shape);
        if (byMaterial == null) return null;
        RegistryHandle<MaterialShapeItem> handle = byMaterial.get(mat);
        return handle == null ? null : handle.get();
    }
}
