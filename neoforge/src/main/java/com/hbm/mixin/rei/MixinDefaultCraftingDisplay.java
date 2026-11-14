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

package com.hbm.mixin.rei;

import com.hbm.inventory.recipes.crafting.StateShapedRecipe;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.CraftingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapedDisplay;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DefaultCraftingDisplay.class, remap = false)
public abstract class MixinDefaultCraftingDisplay {
    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void hbm$stateShaped(
            RecipeHolder<? extends Recipe<?>> holder, CallbackInfoReturnable<CraftingDisplay> cir) {
        if (!(holder.value() instanceof StateShapedRecipe recipe)) return;
        ShapedCraftingRecipeDisplay display =
                (ShapedCraftingRecipeDisplay) recipe.display().getFirst();
        cir.setReturnValue(
                new DefaultCustomShapedDisplay(
                        display.ingredients().stream()
                                .map(EntryIngredients::ofSlotDisplay)
                                .toList(),
                        List.of(EntryIngredients.ofSlotDisplay(display.result())),
                        Optional.of(holder.id().identifier()),
                        display.width(),
                        display.height()));
    }
}
