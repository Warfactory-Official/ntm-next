// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.loot;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.CommonHooks;

public interface RandomSlotLoot {
    String FIELD = "hbm:random_slots";
    MapCodec<Boolean> FLAG_CODEC = Codec.BOOL.optionalFieldOf(FIELD, false);

    boolean hbm$randomSlots();

    void hbm$randomSlots(boolean enabled);

    static LootTable mark(LootTable table) {
        ((RandomSlotLoot) table).hbm$randomSlots(true);
        return table;
    }

    static Codec<LootTable> codec(Codec<LootTable> original) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<LootTable, T>> decode(DynamicOps<T> ops, T input) {
                return ops.getMap(input)
                        .flatMap(map -> FLAG_CODEC.decode(ops, map))
                        .flatMap(
                                flag ->
                                        original.decode(ops, input)
                                                .map(
                                                        decoded -> {
                                                            ((RandomSlotLoot) decoded.getFirst())
                                                                    .hbm$randomSlots(flag);
                                                            return decoded;
                                                        }));
            }

            @Override
            public <T> DataResult<T> encode(LootTable table, DynamicOps<T> ops, T prefix) {
                DataResult<T> encoded = original.encode(table, ops, prefix);
                return ((RandomSlotLoot) table).hbm$randomSlots()
                        ? encoded.flatMap(
                                value ->
                                        ops.mergeToMap(
                                                value,
                                                ops.createString(FIELD),
                                                ops.createBoolean(true)))
                        : encoded;
            }
        };
    }

    static void fill(LootTable table, Container container, LootContext context) {
        ObjectArrayList<ItemStack> draws = new ObjectArrayList<>();
        table.getRandomItemsRaw(
                context,
                stack -> {
                    if (stack.isItemEnabled(context.getLevel().enabledFeatures())) draws.add(stack);
                });
        ObjectArrayList<ItemStack> modified =
                CommonHooks.modifyLoot(table.getLootTableId(), draws, context);
        int size = container.getContainerSize();
        assert size > 0;
        for (int index = 0; index < modified.size(); index++) {
            ItemStack stack = modified.get(index);
            if (stack.getCount() > stack.getMaxStackSize()) {
                for (int unit = 0; unit < stack.getCount(); unit++) {
                    container.setItem(context.getRandom().nextInt(size), stack.copyWithCount(1));
                }
            } else {
                container.setItem(context.getRandom().nextInt(size), stack);
            }
        }
    }
}
