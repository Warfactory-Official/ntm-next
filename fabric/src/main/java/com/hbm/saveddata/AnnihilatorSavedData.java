// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata;

import com.hbm.inventory.recipes.AnnihilatorRecipes.StackKey;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class AnnihilatorSavedData extends SavedData {

    private static final Codec<KeyEntry> KEY_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("keyType")
                                                    .forGetter(KeyEntry::keyType),
                                            Codec.STRING
                                                    .optionalFieldOf("id", "")
                                                    .forGetter(KeyEntry::id),
                                            ItemStackTemplate.CODEC
                                                    .optionalFieldOf("stack")
                                                    .forGetter(KeyEntry::stack),
                                            Codec.STRING
                                                    .fieldOf("amount")
                                                    .forGetter(KeyEntry::amount))
                                    .apply(i, KeyEntry::new));
    private static final Codec<Entry> ENTRY_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.STRING.fieldOf("pool").forGetter(Entry::poolName),
                                            KEY_CODEC
                                                    .listOf()
                                                    .fieldOf("items")
                                                    .forGetter(Entry::items))
                                    .apply(i, Entry::new));
    public static final SavedDataType<AnnihilatorSavedData> TYPE =
            new SavedDataType<>(
                    Library.id("annihilator"), AnnihilatorSavedData::new, codec(), null);
    public final HashMap<String, AnnihilatorPool> pools = new HashMap<>();

    public static AnnihilatorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static AnnihilatorSavedData fromEntries(List<Entry> entries) {
        AnnihilatorSavedData data = new AnnihilatorSavedData();
        for (Entry e : entries) {
            AnnihilatorPool pool = new AnnihilatorPool();
            for (KeyEntry k : e.items()) {
                Object key = decodeKey(k);

                if (key != null) pool.items.put(key, new BigInteger(k.amount(), 10));
            }
            data.pools.put(e.poolName(), pool);
        }
        return data;
    }

    private static @Nullable KeyEntry encodeKey(Object key, BigInteger amount) {
        return switch (key) {
            case Item item ->
                    new KeyEntry(
                            0,
                            BuiltInRegistries.ITEM.getKey(item).toString(),
                            Optional.empty(),
                            amount.toString());
            case Fluid fluid ->
                    new KeyEntry(
                            1,
                            BuiltInRegistries.FLUID.getKey(fluid).toString(),
                            Optional.empty(),
                            amount.toString());
            case StackKey stack ->
                    new KeyEntry(2, "", Optional.of(stack.prototype()), amount.toString());
            case TagKey<?> tag ->
                    new KeyEntry(3, tag.location().toString(), Optional.empty(), amount.toString());
            default -> null;
        };
    }

    private static @Nullable Object decodeKey(KeyEntry k) {
        try {
            return switch (k.keyType()) {
                case 0 -> {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(k.id()));
                    yield item == Items.AIR ? null : item;
                }
                case 1 -> {
                    Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(k.id()));
                    yield fluid == Fluids.EMPTY ? null : fluid;
                }
                case 2 -> {
                    ItemStack stack =
                            k.stack().map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
                    yield stack.isEmpty() ? null : StackKey.of(stack);
                }
                case 3 -> TagKey.create(Registries.ITEM, Identifier.parse(k.id()));
                default -> null;
            };
        } catch (Exception ex) {
            return null;
        }
    }

    private static Codec<AnnihilatorSavedData> codec() {
        return RecordCodecBuilder.create(
                i ->
                        i.group(
                                        ENTRY_CODEC
                                                .listOf()
                                                .optionalFieldOf("pools", List.of())
                                                .forGetter(AnnihilatorSavedData::toEntries))
                                .apply(i, AnnihilatorSavedData::fromEntries));
    }

    public AnnihilatorPool grabPool(String pool) {
        return pools.computeIfAbsent(pool, k -> new AnnihilatorPool());
    }

    public @Nullable ItemStack pushToPool(
            String pool, Fluid type, long amount, boolean alwaysPayOut) {
        ItemStack payout = grabPool(pool).increment(type, amount, alwaysPayOut);
        setDirty();
        return payout;
    }

    public @Nullable ItemStack pushToPool(String pool, ItemStack stack, boolean alwaysPayOut) {
        AnnihilatorPool poolInstance = grabPool(pool);

        ItemStack itemPayout =
                poolInstance.increment(stack.getItem(), stack.getCount(), alwaysPayOut);
        ItemStack stackPayout =
                poolInstance.increment(StackKey.of(stack), stack.getCount(), alwaysPayOut);
        ItemStack tagPayout = null;
        for (Iterator<TagKey<Item>> tags = stack.typeHolder().tags().iterator(); tags.hasNext(); ) {
            ItemStack payout = poolInstance.increment(tags.next(), stack.getCount(), alwaysPayOut);
            if (payout != null) tagPayout = payout;
        }

        setDirty();

        return tagPayout != null ? tagPayout : stackPayout != null ? stackPayout : itemPayout;
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(pools.size());
        for (Map.Entry<String, AnnihilatorPool> pool : pools.entrySet()) {
            List<KeyEntry> items = new ArrayList<>(pool.getValue().items.size());
            for (Map.Entry<Object, BigInteger> item : pool.getValue().items.entrySet()) {
                KeyEntry encoded = encodeKey(item.getKey(), item.getValue());
                if (encoded != null) items.add(encoded);
            }
            entries.add(new Entry(pool.getKey(), items));
        }
        return entries;
    }

    public static class AnnihilatorPool {

        public final HashMap<Object, BigInteger> items = new HashMap<>();

        public @Nullable ItemStack increment(Object type, long amount, boolean alwaysPayOut) {
            ItemStack payout;
            BigInteger counter = items.get(type);
            if (counter == null) {
                counter = BigInteger.valueOf(amount);
                payout = AnnihilatorRecipes.getHighestPayoutFromKey(type, BigInteger.ZERO, counter);
            } else {
                BigInteger prev = counter;
                counter = counter.add(BigInteger.valueOf(amount));
                payout =
                        AnnihilatorRecipes.getHighestPayoutFromKey(
                                type, alwaysPayOut ? null : prev, counter);
            }
            items.put(type, counter);
            return payout;
        }
    }

    private record KeyEntry(
            int keyType, String id, Optional<ItemStackTemplate> stack, String amount) {}

    private record Entry(String poolName, List<KeyEntry> items) {}
}
