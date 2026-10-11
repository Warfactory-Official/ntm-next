// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.blocks.network.FluidPipeBlockItem;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
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
import net.minecraft.world.level.material.Fluids;

public final class CraftingRows {

    private static final Identifier DUCT_RETYPE = Library.id("duct_retype");
    private static final Identifier DUCT_UNTYPE = Library.id("duct_untype");

    private record Variant(
            FluidPipeBlockItem duct, String segment, Ingredient plain, Ingredient typed) {}

    private List<Variant> variants;
    private List<Fluid> fluids;

    public List<RecipeHolder<CraftingRecipe>> recipesFor(ItemStack stack) {
        Variant variant = variant(stack);
        if (variant == null) return List.of();
        Fluid fluid = FluidPipeBlock.stampedFluid(stack);
        if (fluid == Fluids.EMPTY) return List.of(untype(variant, variant.typed()));
        if (!fluids().contains(fluid)) return List.of();
        List<RecipeHolder<CraftingRecipe>> out = new ArrayList<>();
        retype(out, variant, fluid, true, true);
        return out;
    }

    public List<RecipeHolder<CraftingRecipe>> usesOf(ItemStack stack) {
        List<RecipeHolder<CraftingRecipe>> out = new ArrayList<>();
        Variant variant = variant(stack);
        if (variant != null) {
            boolean typed = FluidPipeBlock.stampedFluid(stack) != Fluids.EMPTY;
            Variant looked =
                    typed
                            ? new Variant(
                                    variant.duct(),
                                    variant.segment(),
                                    variant.plain(),
                                    DefaultCustomIngredients.components(stack.copyWithCount(1)))
                            : variant;
            for (Fluid fluid : fluids()) retype(out, looked, fluid, !typed, typed);
            if (typed) out.add(untype(looked, looked.typed()));
            return out;
        }
        if (!stack.is(ModItems.FLUID_IDENTIFIER.get())) return out;
        Fluid fluid =
                stack.getOrDefault(
                                ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY)
                        .primary();
        if (!fluids().contains(fluid)) return out;
        for (Variant each : variants()) retype(out, each, fluid, true, true);
        return out;
    }

    private Variant variant(ItemStack stack) {
        for (Variant variant : variants()) {
            if (stack.is(variant.duct())) return variant;
        }
        return null;
    }

    private synchronized List<Variant> variants() {
        if (variants == null) {
            List<Variant> built = new ArrayList<>();
            for (FluidPipeBlock block :
                    List.of(
                            ModBlocks.FLUID_PIPE.get(),
                            ModBlocks.FLUID_PIPE_SILVER.get(),
                            ModBlocks.FLUID_PIPE_COLORED.get())) {
                FluidPipeBlockItem duct = (FluidPipeBlockItem) block.asItem();
                Ingredient typed =
                        DefaultCustomIngredients.any(
                                NTMFluids.displayOrder().stream()
                                        .filter(fluid -> fluid != NTMFluids.NONE)
                                        .map(
                                                fluid ->
                                                        DefaultCustomIngredients.components(
                                                                duct.of(fluid)))
                                        .toList()
                                        .toArray(Ingredient[]::new));
                built.add(
                        new Variant(
                                duct,
                                PageIds.segment(BuiltInRegistries.ITEM.getKey(duct)),
                                Ingredient.of(duct),
                                typed));
            }
            variants = List.copyOf(built);
        }
        return variants;
    }

    private synchronized List<Fluid> fluids() {
        if (fluids == null) {
            List<Fluid> built = new ArrayList<>();
            for (int id = 1; id < NTMFluids.legacyIdCount(); id++)
                built.add(NTMFluids.byLegacyId(id));
            fluids = List.copyOf(built);
        }
        return fluids;
    }

    private static void retype(
            List<RecipeHolder<CraftingRecipe>> out,
            Variant variant,
            Fluid fluid,
            boolean plain,
            boolean typed) {
        ItemStack identifier = new ItemStack(ModItems.FLUID_IDENTIFIER);
        identifier.set(
                ModDataComponents.FLUID_IDENTIFIER.get(),
                FluidIdentifierData.EMPTY.withPrimary(fluid));
        Ingredient stamp = DefaultCustomIngredients.components(identifier);
        String key = PageIds.segment(BuiltInRegistries.FLUID.getKey(fluid));
        if (plain) {
            out.add(row(variant, fluid, key, "plain", 1, variant.plain(), stamp));
            out.add(row(variant, fluid, key, "plain_bulk", 8, variant.plain(), stamp));
        }
        if (typed) {
            out.add(row(variant, fluid, key, "typed", 1, variant.typed(), stamp));
            out.add(row(variant, fluid, key, "typed_bulk", 8, variant.typed(), stamp));
        }
    }

    private static RecipeHolder<CraftingRecipe> untype(Variant variant, Ingredient typed) {
        Identifier id = PageIds.derived(DUCT_UNTYPE, variant.segment());
        return new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, id),
                shapeless(new ItemStack(variant.duct()), List.of(typed)));
    }

    private static RecipeHolder<CraftingRecipe> row(
            Variant variant,
            Fluid fluid,
            String key,
            String kind,
            int ducts,
            Ingredient input,
            Ingredient stamp) {
        List<Ingredient> ingredients = new ArrayList<>(Collections.nCopies(ducts, input));
        ingredients.add(stamp);
        return new RecipeHolder<>(
                ResourceKey.create(
                        Registries.RECIPE,
                        PageIds.derived(DUCT_RETYPE, variant.segment(), key, kind)),
                shapeless(variant.duct().of(fluid).copyWithCount(ducts), ingredients));
    }

    private static CraftingRecipe shapeless(ItemStack result, List<Ingredient> ingredients) {
        return new ShapelessRecipe(
                new Recipe.CommonInfo(false),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                ItemStackTemplate.fromNonEmptyStack(result),
                ingredients);
    }
}
