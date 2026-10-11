// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
    private LootTable stackTable;
    private LootTable unitTable;

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
            loaded.stackTable =
                    LootTable.DIRECT_CODEC
                            .parse(ops, pool.getValue())
                            .getOrThrow(
                                    err ->
                                            new IllegalStateException(
                                                    "decoding " + pool.getKey() + ": " + err));
            loaded.unitTable =
                    LootTable.DIRECT_CODEC
                            .parse(ops, units.get(pool.getKey()))
                            .getOrThrow(
                                    err ->
                                            new IllegalStateException(
                                                    "decoding unit table "
                                                            + pool.getKey()
                                                            + ": "
                                                            + err));
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
        if (row.has("functions")) {
            for (JsonElement element : row.getAsJsonArray("functions")) {
                JsonObject function = element.getAsJsonObject();
                switch (function.get("function").getAsString()) {
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
        return POOLS.get(name).getStack(context);
    }

    public ItemStack getStack(LootContext context) {
        return draw(stackTable, context);
    }

    public static ItemStack getUnit(String name, LootContext context) {
        return draw(POOLS.get(name).unitTable, context);
    }

    private static ItemStack draw(LootTable table, LootContext context) {
        ItemStack[] result = new ItemStack[1];
        table.getRandomItemsRaw(
                context,
                stack -> {
                    if (result[0] != null)
                        throw new IllegalStateException(
                                "a direct item pool emitted multiple stacks");
                    result[0] = stack;
                });
        return result[0] == null ? ItemStack.EMPTY : result[0];
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
