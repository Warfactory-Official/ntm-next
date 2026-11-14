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

import com.hbm.potion.HbmPotion;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class HazmatRegistry {

    private static volatile Reference2DoubleOpenHashMap<Item> resistance =
            new Reference2DoubleOpenHashMap<>(0);

    private HazmatRegistry() {}

    public static void applyDataPack(RegistryAccess registries) {
        Reference2DoubleOpenHashMap<Item> next = new Reference2DoubleOpenHashMap<>();
        Map<Item, Identifier> claims = new HashMap<>();
        for (var holder :
                registries.lookupOrThrow(HazmatProtection.REGISTRY).listElements().toList()) {
            HazmatProtection rule = holder.value();
            Identifier id = holder.key().identifier();
            for (Holder<Item> target : rule.targets()) {
                Item item = target.value();
                Identifier prior = claims.putIfAbsent(item, id);
                if (prior != null) {
                    throw new IllegalStateException(
                            "Duplicate hazmat protection target "
                                    + target.unwrapKey().orElseThrow().identifier()
                                    + ": "
                                    + prior
                                    + " and "
                                    + id);
                }
                next.put(item, rule.resistance());
            }
        }
        resistance = next;
    }

    public static void clear() {
        resistance = new Reference2DoubleOpenHashMap<>(0);
    }

    public static double getResistance(ItemStack stack) {
        return resistance.getDouble(stack.getItem()) + getCladding(stack);
    }

    public static double getCladding(ItemStack stack) {
        var cladding = ArmorModHandler.mods(stack).template(ArmorModHandler.CLADDING);
        return cladding == null ? 0D : EquipmentProtections.claddingResistance(cladding);
    }

    public static float getResistance(LivingEntity entity) {
        float sum = 0F;
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) sum += getResistance(stack);
        }
        if (entity.hasEffect(HbmPotion.radx())) sum += 0.2F;
        return sum;
    }
}
