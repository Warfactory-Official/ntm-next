// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public final class RecipeIndex<K, T> {

    private static final RecipeIndex<?, ?> EMPTY = new RecipeIndex<>(Map.of());

    private final Map<K, List<T>> byKey;

    private RecipeIndex(Map<K, List<T>> byKey) {
        this.byKey = byKey;
    }

    @SuppressWarnings("unchecked")
    public static <K, T> RecipeIndex<K, T> empty() {
        return (RecipeIndex<K, T>) EMPTY;
    }

    public static <K, T> RecipeIndex<K, T> of(List<T> rows, Function<T, @Nullable K> key) {
        return of(
                rows,
                (row, file) -> {
                    K k = key.apply(row);
                    if (k != null) file.accept(k);
                });
    }

    public static <K, T> RecipeIndex<K, T> of(List<T> rows, BiConsumer<T, Consumer<K>> keys) {
        Map<K, List<T>> filing = new LinkedHashMap<>();
        for (T row : rows)
            keys.accept(row, k -> filing.computeIfAbsent(k, x -> new ArrayList<>(1)).add(row));
        Map<K, List<T>> frozen = new LinkedHashMap<>(filing.size());
        filing.forEach((k, found) -> frozen.put(k, List.copyOf(found)));
        return new RecipeIndex<>(Collections.unmodifiableMap(frozen));
    }

    public List<T> candidates(@Nullable K key) {
        if (key == null) return List.of();
        List<T> found = byKey.get(key);
        return found == null ? List.of() : found;
    }

    public @Nullable T find(@Nullable K key, Predicate<T> accepts) {
        for (T recipe : candidates(key)) {
            if (accepts.test(recipe)) return recipe;
        }
        return null;
    }

    public @Nullable T findLast(@Nullable K key) {
        List<T> found = candidates(key);
        return found.isEmpty() ? null : found.getLast();
    }

    public List<T> reachable() {
        List<T> out = new ArrayList<>(byKey.size());
        for (List<T> found : byKey.values()) out.add(found.getLast());
        return out;
    }

    public int size() {
        return byKey.size();
    }
}
