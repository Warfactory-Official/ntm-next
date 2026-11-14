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

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.AmmoArtyItem;
import com.hbm.platform.Platform;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class CargoShellRecipe extends HbmCustomRecipe {

    public static final CargoShellRecipe INSTANCE = new CargoShellRecipe();
    public static final MapCodec<CargoShellRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, CargoShellRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<CargoShellRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static boolean isEmptyShell(ItemStack stack) {
        return ModItems.AMMO_ARTY.is(stack, AmmoArtyItem.ArtilleryShellType.CARGO)
                && !stack.has(ModDataComponents.ARTY_CARGO.get());
    }

    @Override
    protected boolean matchesInput(CraftingInput input, Level level) {
        int itemCount = 0;
        int shellCount = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getCraftingRemainder() != null) return false;
            itemCount++;
            if (isEmptyShell(stack)) shellCount++;
            else if (!Platform.canFitInsideContainerItems(stack)
                    || stack.has(ModDataComponents.ARTY_CARGO.get())) return false;
        }
        return itemCount == 2 && shellCount == 1;
    }

    @Override
    public boolean acceptsInputState(CraftingInput input, int slot, ItemStack output) {
        if (isEmptyShell(input.getItem(slot)))
            return RecipeInputState.preservesComponents(input.getItem(slot), output);
        ItemStackTemplate cargo = output.get(ModDataComponents.ARTY_CARGO.get());
        return cargo != null
                && ItemStack.isSameItemSameComponents(input.getItem(slot), cargo.create());
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack shell = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (isEmptyShell(stack)) shell = stack;
            else cargo = stack;
        }
        if (shell.isEmpty() || cargo.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = shell.copyWithCount(1);
        result.set(
                ModDataComponents.ARTY_CARGO.get(),
                ItemStackTemplate.fromNonEmptyStack(cargo).withCount(1));
        return result;
    }

    @Override
    public RecipeSerializer<CargoShellRecipe> getSerializer() {
        return SERIALIZER;
    }
}
