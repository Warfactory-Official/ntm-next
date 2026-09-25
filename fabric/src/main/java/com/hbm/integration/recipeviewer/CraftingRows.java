// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.FluidPipeBlockItem;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.material.Fluid;

public final class CraftingRows {

    private static final Identifier DUCT_RETYPE = Library.id("duct_retype");

    private CraftingRows() {}

    public static List<RecipeHolder<CraftingRecipe>> rows() {
        List<RecipeHolder<CraftingRecipe>> rows = new ArrayList<>();
        FluidPipeBlockItem duct = (FluidPipeBlockItem) ModBlocks.FLUID_PIPE.get().asItem();
        Ingredient plain = Ingredient.of(duct);

        Ingredient typed =
                Services.PLATFORM.anyIngredient(
                        NTMFluids.displayOrder().stream()
                                .filter(fluid -> fluid != NTMFluids.NONE)
                                .map(fluid -> Services.PLATFORM.exactIngredient(duct.of(fluid)))
                                .toList());

        for (int id = 1; id < NTMFluids.legacyIdCount(); id++) {
            Fluid fluid = NTMFluids.byLegacyId(id);
            ItemStack identifier = new ItemStack(ModItems.FLUID_IDENTIFIER);
            identifier.set(
                    ModDataComponents.FLUID_IDENTIFIER.get(),
                    FluidIdentifierData.EMPTY.withPrimary(fluid));
            Ingredient stamp = Services.PLATFORM.exactIngredient(identifier);
            String key = PageIds.segment(BuiltInRegistries.FLUID.getKey(fluid));
            rows.add(
                    row(
                            PageIds.derived(DUCT_RETYPE, key, "plain"),
                            duct.of(fluid),
                            1,
                            plain,
                            stamp));
            rows.add(
                    row(
                            PageIds.derived(DUCT_RETYPE, key, "plain_bulk"),
                            duct.of(fluid),
                            8,
                            plain,
                            stamp));
            rows.add(
                    row(
                            PageIds.derived(DUCT_RETYPE, key, "typed"),
                            duct.of(fluid),
                            1,
                            typed,
                            stamp));
            rows.add(
                    row(
                            PageIds.derived(DUCT_RETYPE, key, "typed_bulk"),
                            duct.of(fluid),
                            8,
                            typed,
                            stamp));
        }

        rows.add(
                new RecipeHolder<>(
                        ResourceKey.create(
                                Registries.RECIPE,
                                PageIds.derived(Library.id("duct_untype"), "typed")),
                        shapeless(new ItemStack(duct), List.of(typed))));
        return rows;
    }

    private static RecipeHolder<CraftingRecipe> row(
            Identifier id, ItemStack result, int ducts, Ingredient duct, Ingredient stamp) {
        List<Ingredient> ingredients = new ArrayList<>(Collections.nCopies(ducts, duct));
        ingredients.add(stamp);
        return new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, id),
                shapeless(result.copyWithCount(ducts), ingredients));
    }

    private static CraftingRecipe shapeless(ItemStack result, List<Ingredient> ingredients) {
        return new ShapelessRecipe(
                new Recipe.CommonInfo(false),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                ItemStackTemplate.fromNonEmptyStack(result),
                ingredients);
    }
}
