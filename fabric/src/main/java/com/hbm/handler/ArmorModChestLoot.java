// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.items.ModItems;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public final class ArmorModChestLoot {
    private static final Map<Identifier, PoolSpec> TABLES =
            Map.of(
                    Identifier.withDefaultNamespace("chests/village/village_weaponsmith"),
                            new PoolSpec(
                                    2,
                                    20,
                                    List.of(
                                            new Entry(() -> ModItems.ARMOR_POLISH.get(), 1, 1, 3),
                                            new Entry(() -> ModItems.BATHWATER.get(), 1, 1, 1))),
                    Identifier.withDefaultNamespace("chests/abandoned_mineshaft"),
                            new PoolSpec(
                                    3,
                                    6,
                                    List.of(
                                            new Entry(() -> ModItems.BATHWATER.get(), 1, 1, 1),
                                            new Entry(() -> ModItems.SERUM.get(), 1, 1, 5),
                                            new Entry(() -> ModItems.NO9.get(), 1, 1, 5),
                                            new Entry(
                                                    () -> ModItems.KEY_RED_CRACKED.get(),
                                                    1,
                                                    1,
                                                    5))),
                    Identifier.withDefaultNamespace("chests/simple_dungeon"),
                            new PoolSpec(
                                    3,
                                    12,
                                    List.of(
                                            new Entry(() -> ModItems.HEART_PIECE.get(), 1, 1, 1),
                                            new Entry(() -> ModItems.SCRUMPY.get(), 1, 1, 1),
                                            new Entry(
                                                    () -> ModItems.KEY_RED_CRACKED.get(),
                                                    1,
                                                    1,
                                                    5))),
                    Identifier.withDefaultNamespace("chests/desert_pyramid"),
                            new PoolSpec(
                                    3,
                                    16,
                                    List.of(
                                            new Entry(() -> ModItems.HEART_PIECE.get(), 1, 1, 1),
                                            new Entry(() -> ModItems.SCRUMPY.get(), 1, 1, 1))),
                    Identifier.withDefaultNamespace("chests/jungle_temple"),
                            new PoolSpec(
                                    2,
                                    13,
                                    List.of(new Entry(() -> ModItems.HEART_PIECE.get(), 1, 1, 1))));

    public static final AddedPool BONUS_CHEST =
            new AddedPool(
                    Identifier.withDefaultNamespace("chests/spawn_bonus_chest"),
                    10,
                    64,
                    List.of(new Entry(() -> ModItems.NO9.get(), 1, 1, 7)));

    private ArmorModChestLoot() {}

    public static PoolSpec get(Identifier tableId) {
        return TABLES.get(tableId);
    }

    public record PoolSpec(
            int expectedPoolCount, int expectedPrimaryEntryCount, List<Entry> entries) {}

    public record AddedPool(Identifier table, int rolls, int emptyWeight, List<Entry> entries) {
        public LootPool.Builder pool() {
            LootPool.Builder pool =
                    LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(rolls))
                            .add(EmptyLootItem.emptyItem().setWeight(emptyWeight));
            for (Entry entry : entries)
                pool.add(LootItem.lootTableItem(entry.singleItem()).setWeight(entry.weight()));
            return pool;
        }
    }

    public record Entry(Supplier<Item> item, int minStackSize, int maxStackSize, int weight) {
        public Item singleItem() {
            if (minStackSize != 1 || maxStackSize != 1) {
                throw new IllegalStateException(
                        "Armor chest entry needs an exact stack-count function: "
                                + minStackSize
                                + "-"
                                + maxStackSize);
            }
            return item.get();
        }
    }
}
