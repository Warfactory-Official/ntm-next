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

package com.hbm.handler;

import com.hbm.hazard.Exposure;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;

public final class EquipmentProtections {
    private static final Exposure[] NO_HAZARDS = new Exposure[0];
    private static Reference2IntOpenHashMap<Item> hazards = new Reference2IntOpenHashMap<>(0);
    private static Reference2IntOpenHashMap<Item> masks = emptyMasks();
    private static Reference2IntOpenHashMap<Item> claddingSlots = new Reference2IntOpenHashMap<>(0);
    private static Reference2DoubleOpenHashMap<Item> claddingResistance =
            new Reference2DoubleOpenHashMap<>(0);
    private static ReferenceOpenHashSet<Item> filters = new ReferenceOpenHashSet<>(0);
    private static ReferenceOpenHashSet<Item> lateFilterTooltips = new ReferenceOpenHashSet<>(0);
    private static ReferenceOpenHashSet<Item> claddingTooltips = new ReferenceOpenHashSet<>(0);
    private static Reference2ObjectOpenHashMap<Item, Exposure[]> orderedHazards =
            new Reference2ObjectOpenHashMap<>(0);
    private static Reference2ObjectOpenHashMap<Item, Exposure[]> orderedExclusions =
            new Reference2ObjectOpenHashMap<>(0);

    private EquipmentProtections() {}

    private static Reference2IntOpenHashMap<Item> emptyMasks() {
        var map = new Reference2IntOpenHashMap<Item>(0);
        map.defaultReturnValue(-1);
        return map;
    }

    public static void applyDataPack(RegistryAccess registries) {
        var nextHazards = new Reference2IntOpenHashMap<Item>();
        var nextMasks = emptyMasks();
        var nextSlots = new Reference2IntOpenHashMap<Item>();
        var nextResistance = new Reference2DoubleOpenHashMap<Item>();
        var nextFilters = new ReferenceOpenHashSet<Item>();
        var nextLate = new ReferenceOpenHashSet<Item>();
        var nextCladdingTooltips = new ReferenceOpenHashSet<Item>();
        var nextOrdered = new Reference2ObjectOpenHashMap<Item, Exposure[]>();
        var nextExcludedOrder = new Reference2ObjectOpenHashMap<Item, Exposure[]>();
        Map<Item, Identifier> claims = new HashMap<>();
        for (var holder :
                registries.lookupOrThrow(EquipmentProtection.REGISTRY).listElements().toList()) {
            var rule = holder.value();
            int hazardBits = 0;
            for (Exposure hazard : rule.hazards()) hazardBits |= hazard.bit;
            int excluded = 0;
            if (rule.filterExclusions().isPresent()) {
                for (Exposure hazard : rule.filterExclusions().get()) excluded |= hazard.bit;
            }
            for (var target : rule.targets()) {
                Item item = target.value();
                Identifier prior = claims.putIfAbsent(item, holder.key().identifier());
                if (prior != null)
                    throw new IllegalStateException(
                            "Duplicate equipment protection target "
                                    + target.unwrapKey().orElseThrow().identifier()
                                    + ": "
                                    + prior
                                    + " and "
                                    + holder.key().identifier());
                nextHazards.put(item, hazardBits);
                nextOrdered.put(item, rule.hazards().toArray(Exposure[]::new));
                if (rule.filter()) nextFilters.add(item);
                if (rule.filterTooltipAfterItem()) nextLate.add(item);
                if (rule.filterExclusions().isPresent()) {
                    nextMasks.put(item, excluded);
                    nextExcludedOrder.put(
                            item, rule.filterExclusions().get().toArray(Exposure[]::new));
                }
                if (rule.cladding().isPresent()) {
                    var cladding = rule.cladding().get();
                    int slots = 0;
                    for (var slot : cladding.slots()) slots |= slot.bit();
                    nextSlots.put(item, slots);
                    nextResistance.put(item, cladding.resistance());
                    if (cladding.showTooltip()) nextCladdingTooltips.add(item);
                }
            }
        }
        hazards = nextHazards;
        masks = nextMasks;
        claddingSlots = nextSlots;
        claddingResistance = nextResistance;
        filters = nextFilters;
        lateFilterTooltips = nextLate;
        claddingTooltips = nextCladdingTooltips;
        orderedHazards = nextOrdered;
        orderedExclusions = nextExcludedOrder;
    }

    public static int hazards(ItemInstance stack) {
        return stack.count() == 0 ? 0 : hazards.getInt(stack.typeHolder().value());
    }

    public static boolean isMask(ItemInstance stack) {
        return stack.count() != 0 && masks.getInt(stack.typeHolder().value()) >= 0;
    }

    public static int filterExclusions(ItemInstance stack) {
        return masks.getInt(stack.typeHolder().value());
    }

    public static boolean isFilter(ItemInstance stack) {
        return stack.count() != 0 && filters.contains(stack.typeHolder().value());
    }

    public static boolean lateFilterTooltip(ItemInstance stack) {
        return lateFilterTooltips.contains(stack.typeHolder().value());
    }

    public static boolean claddingTooltip(ItemInstance stack) {
        return claddingTooltips.contains(stack.typeHolder().value());
    }

    public static Exposure[] hazardOrder(ItemInstance stack) {
        return orderedHazards.getOrDefault(stack.typeHolder().value(), NO_HAZARDS);
    }

    public static Exposure[] exclusionOrder(ItemInstance stack) {
        return orderedExclusions.getOrDefault(stack.typeHolder().value(), NO_HAZARDS);
    }

    public static boolean isCladding(ItemInstance stack) {
        return stack.count() != 0 && claddingSlots.getInt(stack.typeHolder().value()) != 0;
    }

    public static double claddingResistance(ItemInstance stack) {
        return claddingResistance.getDouble(stack.typeHolder().value());
    }

    public static boolean claddingAllows(ItemInstance stack, EquipmentSlot slot) {
        int bit =
                switch (slot) {
                    case HEAD -> EquipmentProtection.ArmorSlot.HEAD.bit();
                    case CHEST -> EquipmentProtection.ArmorSlot.CHEST.bit();
                    case LEGS -> EquipmentProtection.ArmorSlot.LEGS.bit();
                    case FEET -> EquipmentProtection.ArmorSlot.FEET.bit();
                    default -> 0;
                };
        return (claddingSlots.getInt(stack.typeHolder().value()) & bit) != 0;
    }

    public static void appendCladdingTooltip(ItemStack stack, Consumer<Component> adder) {
        if (!claddingTooltip(stack)) return;
        adder.accept(
                Component.translatable("desc.item.armorMod.cladding", claddingResistance(stack))
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("armorMod.applicableTo")
                        .withStyle(ChatFormatting.DARK_PURPLE));
        int slots = claddingSlots.getInt(stack.getItem());
        if (slots == 15)
            adder.accept(Component.literal("  ").append(Component.translatable("armorMod.all")));
        else {
            String[] keys = {
                "armorMod.helmets", "armorMod.chestplates", "armorMod.leggings", "armorMod.boots"
            };
            for (int i = 0; i < keys.length; i++) {
                if ((slots & (1 << i)) != 0)
                    adder.accept(Component.literal("  ").append(Component.translatable(keys[i])));
            }
        }
        adder.accept(
                Component.translatable("desc.item.armorMod.slot")
                        .withStyle(ChatFormatting.DARK_PURPLE));
        adder.accept(
                Component.literal("  ").append(Component.translatable("armorMod.type.cladding")));
    }

    public static void clear() {
        hazards = new Reference2IntOpenHashMap<>(0);
        masks = emptyMasks();
        claddingSlots = new Reference2IntOpenHashMap<>(0);
        claddingResistance = new Reference2DoubleOpenHashMap<>(0);
        filters = new ReferenceOpenHashSet<>(0);
        lateFilterTooltips = new ReferenceOpenHashSet<>(0);
        claddingTooltips = new ReferenceOpenHashSet<>(0);
        orderedHazards = new Reference2ObjectOpenHashMap<>(0);
        orderedExclusions = new Reference2ObjectOpenHashMap<>(0);
    }
}
