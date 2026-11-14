/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.generator;

import com.hbm.items.ModItems;
import com.hbm.registration.ItemFamily;
import java.util.IdentityHashMap;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class WasteFuel {

    private static IdentityHashMap<Item, WasteFuel> byItem;

    private final int rate;
    private final int duration;
    private final @Nullable Item product;
    private @Nullable DataComponentMap productDefaults;
    private @Nullable PatchedDataComponentMap productComponents;

    private WasteFuel(int rate, int duration, @Nullable Item product) {
        if (rate <= 0 || duration <= 0) throw new IllegalArgumentException();
        this.rate = rate;
        this.duration = duration;
        this.product = product;
    }

    public int rate() {
        return rate;
    }

    public int duration() {
        return duration;
    }

    public @Nullable Item product() {
        return product;
    }

    boolean fits(ItemStack slot) {
        if (product == null || slot.isEmpty()) return true;
        if (!slot.is(product) || slot.getCount() >= slot.getMaxStackSize()) return false;
        var defaults = product.components();
        if (productDefaults != defaults) {
            productComponents =
                    PatchedDataComponentMap.fromPatch(defaults, DataComponentPatch.EMPTY);
            productDefaults = defaults;
        }
        return slot.getComponents().equals(productComponents);
    }

    ItemStack finish(ItemStack slot) {
        if (product == null) return slot;
        if (slot.isEmpty()) return new ItemStack(product);
        slot.grow(1);
        return slot;
    }

    public static @Nullable WasteFuel forItem(ItemStack stack) {
        return byItem.get(stack.getItem());
    }

    public static void initialize() {
        var definitions = new IdentityHashMap<Item, WasteFuel>(28);
        family(
                definitions,
                ModItems.NUCLEAR_WASTE_SHORT,
                ModItems.NUCLEAR_WASTE_SHORT_DEPLETED,
                1500,
                36_000);
        family(
                definitions,
                ModItems.NUCLEAR_WASTE_SHORT_TINY,
                ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY,
                150,
                3600);
        family(
                definitions,
                ModItems.NUCLEAR_WASTE_LONG,
                ModItems.NUCLEAR_WASTE_LONG_DEPLETED,
                500,
                144_000);
        family(
                definitions,
                ModItems.NUCLEAR_WASTE_LONG_TINY,
                ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY,
                50,
                14_400);
        definitions.put(ModItems.SCRAP_NUCLEAR.get(), new WasteFuel(50, 6000, null));
        definitions.put(ModItems.GEM_RAD.get(), new WasteFuel(25_000, 36_000, Items.DIAMOND));
        byItem = definitions;
    }

    private static <E extends Enum<E>> void family(
            IdentityHashMap<Item, WasteFuel> definitions,
            ItemFamily<E, ?> inputs,
            ItemFamily<E, ?> products,
            int rate,
            int duration) {
        for (var type : inputs.types()) {
            definitions.put(inputs.get(type), new WasteFuel(rate, duration, products.get(type)));
        }
    }
}
