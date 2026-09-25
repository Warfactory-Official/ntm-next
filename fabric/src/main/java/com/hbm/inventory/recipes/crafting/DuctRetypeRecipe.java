// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.FluidPipeBlockItem;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class DuctRetypeRecipe extends CustomRecipe {

    public static final DuctRetypeRecipe INSTANCE = new DuctRetypeRecipe();
    public static final MapCodec<DuctRetypeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, DuctRetypeRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<DuctRetypeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasId = false;
        int ducts = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof FluidIdentifierItem) {
                if (hasId) return false;
                FluidIdentifierData data =
                        stack.getOrDefault(
                                ModDataComponents.FLUID_IDENTIFIER.get(),
                                FluidIdentifierData.EMPTY);
                if (data.primary() == Fluids.EMPTY) return false;
                hasId = true;
            } else if (stack.getItem() instanceof FluidPipeBlockItem) {
                ducts++;
            } else {
                return false;
            }
        }
        return hasId && ducts > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Fluid fluid = Fluids.EMPTY;
        int ducts = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof FluidIdentifierItem) {
                fluid =
                        stack.getOrDefault(
                                        ModDataComponents.FLUID_IDENTIFIER.get(),
                                        FluidIdentifierData.EMPTY)
                                .primary();
            } else if (stack.getItem() instanceof FluidPipeBlockItem) {
                ducts++;
            }
        }
        ItemStack result = new ItemStack(ModBlocks.FLUID_PIPE.get(), ducts);
        if (fluid != Fluids.EMPTY) {
            result.set(ModDataComponents.FLUID_CONTENT.get(), new FluidStackNTM(fluid, 0, 0));
        }
        return result;
    }

    @Override
    public RecipeSerializer<DuctRetypeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
