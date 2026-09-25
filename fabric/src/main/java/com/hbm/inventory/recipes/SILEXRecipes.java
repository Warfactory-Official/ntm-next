// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.registration.ItemFamily;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class SILEXRecipes extends SimpleGenericRecipes<SILEXRecipe> {

    public static final SILEXRecipes INSTANCE = new SILEXRecipes();

    private volatile @Nullable Map<Item, Item> tinyWaste;

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
            tinyWaste = held = Map.copyOf(map);
        }
        return held;
    }

    private static <E extends Enum<E>> void pairMembers(
            Map<Item, Item> map, ItemFamily<E, ?> tiny, ItemFamily<E, ?> full) {
        for (E type : tiny.types()) map.put(tiny.get(type), full.get(type));
    }
}
