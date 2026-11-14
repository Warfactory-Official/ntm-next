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

package com.hbm.inventory.recipes.loader;

import com.hbm.packet.SyncWire;
import com.hbm.registration.Registrar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public abstract class GenericRecipes<T extends GenericRecipe, I> extends SerializableRecipe
        implements RecipeType<T> {
    public static final RandomSource RNG = RandomSource.create();
    public static final String POOL_PREFIX_ALT = "alt.";
    public static final String POOL_PREFIX_DISCOVER = "discover.";
    public static final String POOL_PREFIX_SECRET = "secret.";
    public static final String POOL_PREFIX_528 = "528.";

    private static volatile long tagEpoch;
    private static volatile PoolState poolState =
            new PoolState(RecipeSource.NEVER_FILLED, Map.of(), Map.of());
    private @Nullable Identifier registeredId;
    private volatile @Nullable Generation<T, I> generation;

    private record Generation<T, I>(
            RecipeSource.Token source,
            long epoch,
            List<T> rows,
            Map<String, T> names,
            Map<String, List<GenericRecipe>> groups,
            RecipeIndex<Item, T> items,
            RecipeIndex<Fluid, T> fluids,
            @Nullable I custom) {}

    private record PoolState(
            RecipeSource.Token source,
            Map<String, List<String>> members,
            Map<String, GenericRecipe> recipes) {}

    protected abstract String registryName();

    protected abstract RecipeSerializer<? extends T> serializer();

    protected abstract @Nullable I indexRows(List<T> rows);

    public static synchronized void invalidateIndexes() {
        SyncWire.invalidateRecipeInputs();
        tagEpoch++;
    }

    public static Map<String, List<String>> pools() {
        return currentPools().members();
    }

    public static @Nullable GenericRecipe pooled(String name) {
        return currentPools().recipes().get(name);
    }

    public static Collection<String> poolNames() {
        var members = currentPools().members();
        return members.isEmpty() ? RecipeSource.clientPoolNames() : members.keySet();
    }

    private static PoolState currentPools() {
        var source = RecipeSource.token();
        var held = poolState;
        if (source == null || source.equals(held.source())) {
            return held;
        }
        synchronized (GenericRecipes.class) {
            source = RecipeSource.token();
            held = poolState;
            if (source == null || source.equals(held.source())) {
                return held;
            }
            for (var handler : recipeHandlers) {
                handler.ensureFilled();
            }
            var members = new HashMap<String, List<String>>();
            var recipes = new HashMap<String, GenericRecipe>();
            for (var handler : recipeHandlers) {
                if (handler instanceof GenericRecipes<?, ?> table) {
                    for (var row : table.recipes()) {
                        if (!row.isPooled()) {
                            continue;
                        }
                        var name = row.getInternalName();
                        recipes.put(name, row);
                        for (var pool : row.getPools()) {
                            members.computeIfAbsent(
                                            pool, _ -> new ArrayList<>())
                                    .add(name);
                        }
                    }
                }
            }
            members.replaceAll(
                    (_, names) -> Collections.unmodifiableList(names));
            var prepared =
                    new PoolState(
                            source,
                            Collections.unmodifiableMap(members),
                            Collections.unmodifiableMap(recipes));
            poolState = prepared;
            return prepared;
        }
    }

    @Override
    public final void registerType() {
        var name = registryName();
        registeredId = Registrar.registerRecipeType(name, this).id();
        Registrar.registerRecipeSerializer(name, serializer());
    }

    public final RecipeType<T> type() {
        return this;
    }

    @Override
    public final Identifier datapackTypeId() {
        return registeredId;
    }

    @Override
    public final RecipeType<?> datapackType() {
        return this;
    }

    @Override
    public String toString() {
        return registeredId == null ? super.toString() : registeredId.toString();
    }

    @Override
    public void ensureFilled() {
        var source = RecipeSource.token();
        var held = generation;
        long epoch = tagEpoch;
        if (held != null
                && (source == null || source.equals(held.source()) && epoch == held.epoch())) {
            return;
        }
        synchronized (this) {
            source = RecipeSource.token();
            held = generation;
            epoch = tagEpoch;
            if (source == null) {
                if (held == null) {
                    generation = emptyGeneration(epoch);
                }
                return;
            }
            if (held != null && source.equals(held.source()) && epoch == held.epoch()) {
                return;
            }
            generation =
                    loadGeneration(source, epoch, held != null && source.equals(held.source()));
        }
    }

    private Generation<T, I> emptyGeneration(long epoch) {
        List<T> rows = List.of();
        return new Generation<>(
                RecipeSource.NEVER_FILLED,
                epoch,
                rows,
                Map.of(),
                Map.of(),
                RecipeIndex.empty(),
                RecipeIndex.empty(),
                indexRows(rows));
    }

    @SuppressWarnings("unchecked")
    private Generation<T, I> loadGeneration(
            RecipeSource.Token source, long epoch, boolean tagsOnly) {
        var loaded = new ArrayList<T>();
        for (var holder : RecipeSource.recipes()) {
            if (holder.value() instanceof GenericRecipe row && row.getType() == this) {
                row.bind(holder.id());
                if (tagsOnly) row.refreshInputDisplay();
                else row.materialize();
                loaded.add((T) row);
            }
        }
        loaded.sort(
                Comparator.comparingInt((T row) -> row.order)
                        .thenComparing(GenericRecipe::getInternalName));
        List<T> rows = loaded.isEmpty() ? List.of() : Collections.unmodifiableList(loaded);
        var names = new HashMap<String, T>();
        var groups = new HashMap<String, List<GenericRecipe>>();
        for (var row : rows) {
            if (names.putIfAbsent(row.getInternalName(), row) == null
                    && row.autoSwitchGroup != null) {
                groups.computeIfAbsent(
                                row.autoSwitchGroup, _ -> new ArrayList<>())
                        .add(row);
            }
        }
        groups.replaceAll(
                (_, members) -> Collections.unmodifiableList(members));
        var items =
                RecipeIndex.<Item, T>of(
                        rows,
                        (row, add) -> {
                            if (row.inputItem != null && row.inputItem.length > 0) {
                                row.inputItem[0]
                                        .ingredient()
                                        .items()
                                        .forEach(holder -> add.accept(holder.value()));
                            }
                        });
        var fluids =
                RecipeIndex.<Fluid, T>of(
                        rows, row -> row.inputFluid.length == 0 ? null : row.inputFluid[0].type());
        return new Generation<>(
                source,
                epoch,
                rows,
                Collections.unmodifiableMap(names),
                Collections.unmodifiableMap(groups),
                items,
                fluids,
                indexRows(rows));
    }

    public List<T> recipes() {
        ensureFilled();
        return generation.rows();
    }

    public @Nullable T getRecipe(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        ensureFilled();
        return generation.names().get(name);
    }

    public List<GenericRecipe> autoSwitchGroup(@Nullable String group) {
        if (group == null) {
            return List.of();
        }
        ensureFilled();
        return generation.groups().getOrDefault(group, List.of());
    }

    public RecipeIndex<Item, T> byInputItem() {
        ensureFilled();
        return generation.items();
    }

    public RecipeIndex<Fluid, T> byInputFluid() {
        ensureFilled();
        return generation.fluids();
    }

    public @Nullable T findByItem(ItemStack stack, Predicate<T> accepts) {
        if (stack.isEmpty()) {
            return null;
        }
        return byInputItem().find(stack.getItem(), accepts);
    }

    protected final I index() {
        ensureFilled();
        return generation.custom();
    }
}
