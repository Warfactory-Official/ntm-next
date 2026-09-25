// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.itempool;

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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public class ItemPool {

    public static final String RESOURCE = "/hbm/item_pools.json";

    public static final Map<String, ItemPool> POOLS = new HashMap<>();

    private static final List<Entry> BACKUP_POOL =
            List.of(
                    new Entry(
                            Items.BREAD.builtInRegistryHolder(),
                            DataComponentPatch.EMPTY,
                            1,
                            3,
                            10),
                    new Entry(
                            Items.STICK.builtInRegistryHolder(),
                            DataComponentPatch.EMPTY,
                            2,
                            5,
                            10));
    public final String name;
    public List<Entry> pool = List.of();

    public ItemPool(String name) {
        this.name = name;
        POOLS.put(name, this);
    }

    public static void load(HolderLookup.Provider registries) {
        POOLS.clear();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        for (Map.Entry<String, JsonElement> pool : root().entrySet()) {
            new ItemPool(pool.getKey()).pool = body(pool.getValue(), ops);
        }
    }

    public static List<Entry> read(HolderLookup.Provider registries, String name) {
        JsonElement pool = root().get(name);
        if (pool == null) throw new IllegalArgumentException(RESOURCE + " holds no pool " + name);
        return body(pool, registries.createSerializationContext(JsonOps.INSTANCE));
    }

    private static JsonObject root() {
        try (InputStream in = ItemPool.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "missing the baked pools at "
                                + RESOURCE
                                + " - run :neoforge:runData / :fabric:runDatagen; every draw would answer the backup pool");
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("reading " + RESOURCE, e);
        }
    }

    private static List<Entry> body(JsonElement pool, RegistryOps<JsonElement> ops) {
        List<Entry> body = new ArrayList<>();
        for (JsonElement element : pool.getAsJsonArray()) {
            body.add(entry(element.getAsJsonObject(), ops));
        }
        return List.copyOf(body);
    }

    private static Entry entry(JsonObject row, RegistryOps<JsonElement> ops) {
        int min = row.get("min").getAsInt();
        int max = row.get("max").getAsInt();
        int weight = row.get("weight").getAsInt();
        if (!row.has("item")) return Entry.empty(min, max, weight);
        Identifier id = Identifier.parse(row.get("item").getAsString());
        Holder<Item> item =
                BuiltInRegistries.ITEM
                        .get(id)
                        .map(h -> (Holder<Item>) h)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                RESOURCE
                                                        + " names "
                                                        + id
                                                        + ", which is not registered"));
        DataComponentPatch patch =
                row.has("components")
                        ? DataComponentPatch.CODEC
                                .parse(ops, row.get("components"))
                                .getOrThrow(
                                        err ->
                                                new IllegalStateException(
                                                        "decoding pool entry components: " + err))
                        : DataComponentPatch.EMPTY;
        return new Entry(item, patch, min, max, weight);
    }

    public static List<Entry> getPool(String name) {
        ItemPool p = POOLS.get(name);
        return p == null ? BACKUP_POOL : p.pool;
    }

    public static ItemStack getStack(String name, RandomSource rand) {
        return getStack(getPool(name), rand);
    }

    public static ItemStack getStack(List<Entry> pool, RandomSource rand) {
        Entry entry = select(pool, rand);
        return entry == null ? ItemStack.EMPTY : entry.roll(rand);
    }

    public static ItemStack getUnit(String name, RandomSource rand) {

        Entry entry = select(getPool(name), rand);
        return entry == null || entry.isEmpty()
                ? ItemStack.EMPTY
                : new ItemStack(entry.item(), 1, entry.patch());
    }

    private static @Nullable Entry select(List<Entry> pool, RandomSource rand) {
        int total = 0;
        for (Entry e : pool) total += e.weight();
        if (total <= 0) return null;

        int roll = rand.nextInt(total);
        int acc = 0;
        for (Entry e : pool) {
            acc += e.weight();
            if (roll < acc) return e;
        }
        throw new AssertionError();
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

        public ItemStack roll(RandomSource rand) {

            int count = min + rand.nextInt(max - min + 1);
            return item == null ? ItemStack.EMPTY : new ItemStack(item, count, patch);
        }
    }
}
