// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.blocks.network.FluidPipeBlockItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public class DuctUntypeRecipe extends CustomRecipe {

    public static final DuctUntypeRecipe INSTANCE = new DuctUntypeRecipe();
    public static final MapCodec<DuctUntypeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, DuctUntypeRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<DuctUntypeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean typedDuct = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (typedDuct
                    || !(stack.getItem() instanceof FluidPipeBlockItem)
                    || FluidPipeBlock.stampedFluid(stack) == Fluids.EMPTY) return false;
            typedDuct = true;
        }
        return typedDuct;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(ModBlocks.FLUID_PIPE);
    }

    @Override
    public RecipeSerializer<DuctUntypeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
