// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ItemStates {

    private static final Map<Reg.Handle<?>, List<Supplier<? extends DataComponentType<?>>>>
            DECLARED = new IdentityHashMap<>();
    private static volatile @Nullable Resolved resolved;

    private record Resolved(
            Map<Item, List<DataComponentType<?>>> byItem, Set<DataComponentType<?>> types) {}

    private ItemStates() {}

    static void declare(
            Reg.Handle<?> handle, List<Supplier<? extends DataComponentType<?>>> types) {
        if (DECLARED.putIfAbsent(handle, List.copyOf(types)) != null) {
            throw new IllegalStateException(handle.id() + " states its item state twice");
        }
    }

    private static Resolved resolved() {
        Resolved table = resolved;
        if (table != null) return table;
        Map<Item, List<DataComponentType<?>>> byItem = new IdentityHashMap<>();
        Set<DataComponentType<?>> types = Collections.newSetFromMap(new IdentityHashMap<>());
        DECLARED.forEach(
                (handle, suppliers) -> {
                    List<DataComponentType<?>> list = new ArrayList<>(suppliers.size());
                    for (Supplier<? extends DataComponentType<?>> supplier : suppliers)
                        list.add(supplier.get());
                    byItem.put(handle.asItem(), List.copyOf(list));
                    types.addAll(list);
                });
        return resolved = new Resolved(byItem, types);
    }

    public static List<DataComponentType<?>> of(Item item) {
        return resolved().byItem().getOrDefault(item, List.of());
    }

    public static boolean isState(DataComponentType<?> type) {
        return resolved().types().contains(type);
    }

    public static List<@Nullable Object> key(ItemStack stack) {
        List<DataComponentType<?>> types = of(stack.getItem());
        if (types.isEmpty()) return List.of();
        List<@Nullable Object> values = new ArrayList<>(types.size());
        for (DataComponentType<?> type : types) values.add(stack.get(type));
        return values;
    }

    public static boolean isPrototype(ItemStack stack) {
        for (DataComponentType<?> type : of(stack.getItem())) {
            if (!Objects.equals(stack.get(type), stack.getItem().components().get(type)))
                return false;
        }
        return true;
    }

    public static boolean sameState(ItemStack a, ItemStack b) {
        return Objects.equals(key(a), key(b));
    }
}
