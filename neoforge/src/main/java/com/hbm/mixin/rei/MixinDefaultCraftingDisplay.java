// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
