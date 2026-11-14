/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.Registrar;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class SILEXRecipes extends SimpleGenericRecipes<SILEXRecipe> {

    public static final SILEXRecipes INSTANCE = new SILEXRecipes();

    private volatile @Nullable Map<Item, Item> tinyWaste;

    public record TinyWaste(Supplier<? extends Item> tiny, Supplier<? extends Item> full) {}

    private static final List<TinyWaste> ADDON_TINY_WASTE = new ArrayList<>();

    public static synchronized void addonTinyWaste(TinyWaste... pairs) {
        Registrar.assertOpen("SILEXRecipes.addonTinyWaste");
        ADDON_TINY_WASTE.addAll(List.of(pairs));
    }

    @Override
    protected String registryName() {
        return "silex";
    }

    @Override
    protected RecipeSerializer<SILEXRecipe> serializer() {
        return SILEXRecipe.SERIALIZER;
    }

    public @Nullable SILEXRecipe getOutput(ItemStack stack) {

        if (stack.isEmpty()) return null;

        SILEXRecipe direct = findByItem(stack, r -> r.inputItem[0].matchesItem(stack));
        if (direct != null) return direct;

        Item full = tinyWaste().get(stack.getItem());
        if (full != null) {
            ItemStack fullStack = new ItemStack(full, stack.getCount());

            SILEXRecipe result = getOutput(fullStack);
            if (result != null) {
                SILEXRecipe tiny = new SILEXRecipe(result.getInternalName() + ".tiny");
                tiny.setup(
                        (result.fluidProduced / 900) * 100,
                        result.fluidConsumed,
                        result.laserStrength);
                tiny.outputItems(result.outputItems());
                return tiny;
            }
        }

        return null;
    }

    public @Nullable SILEXRecipe getOutput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }

    private Map<Item, Item> tinyWaste() {
        Map<Item, Item> held = tinyWaste;
        if (held == null) {
            Map<Item, Item> map = new IdentityHashMap<>();
            pairMembers(map, ModItems.NUCLEAR_WASTE_SHORT_TINY, ModItems.NUCLEAR_WASTE_SHORT);
            pairMembers(map, ModItems.NUCLEAR_WASTE_LONG_TINY, ModItems.NUCLEAR_WASTE_LONG);
            pairMembers(
                    map,
                    ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY,
                    ModItems.NUCLEAR_WASTE_SHORT_DEPLETED);
            pairMembers(
                    map,
                    ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY,
                    ModItems.NUCLEAR_WASTE_LONG_DEPLETED);
            for (TinyWaste pair : ADDON_TINY_WASTE) map.put(pair.tiny().get(), pair.full().get());
            tinyWaste = held = Map.copyOf(map);
        }
        return held;
    }

    private static <E extends Enum<E>> void pairMembers(
            Map<Item, Item> map, ItemFamily<E, ?> tiny, ItemFamily<E, ?> full) {
        for (E type : tiny.types()) map.put(tiny.get(type), full.get(type));
    }
}
