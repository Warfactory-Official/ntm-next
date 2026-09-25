// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RBMKFuelDisassemblyRecipe extends CustomRecipe {

    public static final RBMKFuelDisassemblyRecipe INSTANCE = new RBMKFuelDisassemblyRecipe();
    public static final MapCodec<RBMKFuelDisassemblyRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, RBMKFuelDisassemblyRecipe>
            STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<RBMKFuelDisassemblyRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static ItemStack getRod(CraftingInput input) {
        if (input.ingredientCount() != 1) return ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack stack = getRod(input);
        return stack.getItem() instanceof ItemRBMKRod rod
                && rod.pellet != null
                && ItemRBMKRod.getHullHeat(stack) < 50
                && ItemRBMKRod.getCoreHeat(stack) < 50;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack stack = getRod(input);

        if (stack.getItem() instanceof ItemRBMKRod rod) {

            if (rod.pellet == null) return ItemStack.EMPTY;

            if (ItemRBMKRod.getEnrichment(stack) > 0.99D) return ItemStack.EMPTY;

            if (ItemRBMKRod.getHullHeat(stack) < 50 && ItemRBMKRod.getCoreHeat(stack) < 50) {
                int enrichment =
                        4
                                - Mth.clamp(
                                        (int) Math.ceil(ItemRBMKRod.getEnrichment(stack) * 5 - 1),
                                        0,
                                        4);
                ItemStack result = new ItemStack(rod.pellet, 8);
                result.set(
                        ModDataComponents.RBMK_PELLET.get(),
                        new ItemRBMKPellet.Stage(
                                enrichment, ItemRBMKRod.getPoisonLevel(stack) >= 0.5D));
                return result;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<RBMKFuelDisassemblyRecipe> getSerializer() {
        return SERIALIZER;
    }
}
