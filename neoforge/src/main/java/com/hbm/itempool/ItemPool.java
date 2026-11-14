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

package com.hbm.itempool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jspecify.annotations.Nullable;

public class ItemPool {
    public static final String RESOURCE = "/hbm/item_pools.json";
    public static final String UNIT_RESOURCE = "/hbm/item_pools_unit.json";

    public static final Map<String, ItemPool> POOLS = new HashMap<>();

    public final String name;
    public List<Entry> pool = List.of();

    public ItemPool(String name) {
        this.name = name;
        POOLS.put(name, this);
    }

    public static void load(HolderLookup.Provider registries) {
        POOLS.clear();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        JsonObject units = root(UNIT_RESOURCE);
        for (Map.Entry<String, JsonElement> pool : root(RESOURCE).entrySet()) {
            ItemPool loaded = new ItemPool(pool.getKey());
            loaded.pool = body(pool.getValue(), ops);
            LootTable.DIRECT_CODEC
                    .parse(ops, pool.getValue())
                    .getOrThrow(
                            err ->
                                    new IllegalStateException(
                                            "decoding " + pool.getKey() + ": " + err));
            LootTable.DIRECT_CODEC
                    .parse(ops, units.get(pool.getKey()))
                    .getOrThrow(
                            err ->
                                    new IllegalStateException(
                                            "decoding unit table " + pool.getKey() + ": " + err));
        }
    }

    public static List<Entry> read(HolderLookup.Provider registries, String name) {
        JsonElement pool = root(RESOURCE).get(name);
        if (pool == null) throw new IllegalArgumentException(RESOURCE + " holds no pool " + name);
        return body(pool, registries.createSerializationContext(JsonOps.INSTANCE));
    }

    private static JsonObject root(String resource) {
        try (InputStream in = ItemPool.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(
                        "missing the baked pools at "
                                + resource
                                + " - run :neoforge:runData / :fabric:runDatagen");
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("reading " + resource, e);
        }
    }

    private static List<Entry> body(JsonElement pool, RegistryOps<JsonElement> ops) {
        var pools = pool.getAsJsonObject().getAsJsonArray("pools");
        if (pools.size() != 1 || pools.get(0).getAsJsonObject().get("rolls").getAsInt() != 1) {
            throw new IllegalStateException(
                    RESOURCE + " requires one single-roll pool per direct draw");
        }
        List<Entry> body = new ArrayList<>();
        entries(pools.get(0).getAsJsonObject().getAsJsonArray("entries"), body, ops);
        return List.copyOf(body);
    }

    private static void entries(JsonArray entries, List<Entry> body, RegistryOps<JsonElement> ops) {
        for (JsonElement element : entries) {
            JsonObject row = element.getAsJsonObject();
            if (row.get("type").getAsString().equals("minecraft:alternatives")) {
                entries(row.getAsJsonArray("children"), body, ops);
            } else {
                body.add(entry(row, ops));
            }
        }
    }

    private static Entry entry(JsonObject row, RegistryOps<JsonElement> ops) {
        int min = 1;
        int max = 1;
        int weight = row.get("weight").getAsInt();
        Holder<Item> item =
                switch (row.get("type").getAsString()) {
                    case "minecraft:empty" -> null;
                    case "minecraft:item" -> {
                        Identifier id = Identifier.parse(row.get("name").getAsString());
                        yield BuiltInRegistries.ITEM
                                .get(id)
                                .map(h -> (Holder<Item>) h)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        RESOURCE
                                                                + " names unregistered item "
                                                                + id));
                    }
                    default ->
                            throw new IllegalStateException("unsupported direct-pool entry " + row);
                };
        DataComponentPatch patch = DataComponentPatch.EMPTY;
        if (row.has("modifier")) {
            JsonElement modifier = row.get("modifier");
            Iterable<JsonElement> functions =
                    modifier.isJsonArray() ? modifier.getAsJsonArray() : List.of(modifier);
            for (JsonElement element : functions) {
                JsonObject function = element.getAsJsonObject();
                switch (function.get("type").getAsString()) {
                    case "minecraft:set_components" ->
                            patch =
                                    DataComponentPatch.CODEC
                                            .parse(ops, function.get("components"))
                                            .getOrThrow(
                                                    err ->
                                                            new IllegalStateException(
                                                                    "decoding pool entry components: "
                                                                            + err));
                    case "minecraft:set_count" -> {
                        JsonElement count = function.get("count");
                        if (count.isJsonPrimitive()) {
                            min = max = count.getAsInt();
                        } else {
                            JsonObject provider = count.getAsJsonObject();
                            switch (provider.get("type").getAsString()) {
                                case "minecraft:constant" ->
                                        min = max = provider.get("value").getAsInt();
                                case "minecraft:uniform" -> {
                                    min = provider.get("min").getAsInt();
                                    max = provider.get("max").getAsInt();
                                }
                                default ->
                                        throw new IllegalStateException(
                                                "unsupported direct-pool count " + provider);
                            }
                        }
                    }
                    default ->
                            throw new IllegalStateException(
                                    "unsupported direct-pool function " + function);
                }
            }
        }
        return new Entry(item, patch, min, max, weight);
    }

    public static List<Entry> getPool(String name) {
        return POOLS.get(name).pool;
    }

    public static LootContext context(ServerLevel level, RandomSource random) {
        return new LootContext.Builder(
                        new LootParams.Builder(level).create(LootContextParamSets.EMPTY))
                .withOptionalRandomSource(random)
                .create(Optional.empty());
    }

    public static ItemStack getStack(String name, LootContext context) {
        return POOLS.get(name).getStack(context.getRandom());
    }

    public static ItemStack getStack(String name, RandomSource random) {
        return POOLS.get(name).getStack(random);
    }

    public ItemStack getStack(LootContext context) {
        return getStack(context.getRandom());
    }

    public ItemStack getStack(RandomSource random) {
        Entry entry = select(random);
        int count = entry.min() + random.nextInt(entry.max() - entry.min() + 1);
        return entry.isEmpty()
                ? ItemStack.EMPTY
                : new ItemStack(entry.item(), count, entry.patch());
    }

    public static ItemStack getUnit(String name, LootContext context) {
        Entry entry = POOLS.get(name).select(context.getRandom());
        return entry.isEmpty() ? ItemStack.EMPTY : new ItemStack(entry.item(), 1, entry.patch());
    }

    private Entry select(RandomSource random) {
        int total = 0;
        for (Entry entry : pool) total = Math.addExact(total, entry.weight());
        int choice = random.nextInt(total);
        for (Entry entry : pool) {
            choice -= entry.weight();
            if (choice < 0) return entry;
        }
        throw new IllegalStateException("item pool lost its selection weight: " + name);
    }

    public record Entry(
            @Nullable Holder<Item> item, DataComponentPatch patch, int min, int max, int weight) {

        public static Entry of(Item item, int min, int max, int weight) {
            return new Entry(
                    item.builtInRegistryHolder(), DataComponentPatch.EMPTY, min, max, weight);
        }

        public static Entry empty(int min, int max, int weight) {
            return new Entry(null, DataComponentPatch.EMPTY, min, max, weight);
        }

        public boolean isEmpty() {
            return item == null;
        }
    }
}
