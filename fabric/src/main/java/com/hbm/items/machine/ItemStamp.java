// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemStamp extends Item {

    public final StampType type;

    public ItemStamp(Properties properties, StampType type) {
        super(properties);
        this.type = type;
    }

    public static StampType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemStamp stamp ? stamp.getStampType(stack) : null;
    }

    public StampType getStampType(ItemStack stack) {
        return type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (stack.getMaxDamage() > 0 && stack.getDamageValue() == 0) {
            adder.accept(
                    Component.translatable(
                            "desc.item.stamp.durability",
                            stack.getMaxDamage() + " / " + stack.getMaxDamage()));
        }
    }

    public enum StampType {
        FLAT,
        PLATE,
        WIRE,
        CIRCUIT,
        C357,
        C44,
        C50,
        C9,
        PRINTING1,
        PRINTING2,
        PRINTING3,
        PRINTING4,
        PRINTING5,
        PRINTING6,
        PRINTING7,
        PRINTING8
    }

    private static volatile @Nullable Map<StampType, List<Item>> byType;

    public static List<Item> stampsOf(StampType type) {
        Map<StampType, List<Item>> held = byType;
        if (held == null) {
            Map<StampType, List<Item>> map = new EnumMap<>(StampType.class);
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof ItemStamp stamp)
                    map.computeIfAbsent(stamp.type, t -> new ArrayList<>()).add(item);
            }
            map.replaceAll((t, items) -> List.copyOf(items));
            byType = held = map;
        }
        return held.getOrDefault(type, List.of());
    }
}
