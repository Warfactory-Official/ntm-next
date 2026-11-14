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

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ParticleAcceleratorRecipe extends GenericRecipe {
    private static final MapCodec<Integer> MOMENTUM_MAP_CODEC =
            Codec.INT.optionalFieldOf("momentum", 0);
    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> MOMENTUM_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.cast();
    private static final BiConsumer<ParticleAcceleratorRecipe, Integer> APPLY_MOMENTUM =
            (recipe, momentum) -> recipe.momentum = momentum;
    private static final Function<ParticleAcceleratorRecipe, Integer> EXTRACT_MOMENTUM =
            recipe -> recipe.momentum;
    public static final MapCodec<ParticleAcceleratorRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    ParticleAcceleratorRecipe::new,
                    MOMENTUM_MAP_CODEC,
                    APPLY_MOMENTUM,
                    EXTRACT_MOMENTUM);
    public static final StreamCodec<RegistryFriendlyByteBuf, ParticleAcceleratorRecipe>
            STREAM_CODEC =
                    GenericRecipe.streamCodec(
                            ParticleAcceleratorRecipe::new,
                            MOMENTUM_STREAM_CODEC,
                            APPLY_MOMENTUM,
                            EXTRACT_MOMENTUM);
    public static final RecipeSerializer<ParticleAcceleratorRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public int momentum;

    public ParticleAcceleratorRecipe(String name) {
        super(name);
    }

    public ParticleAcceleratorRecipe setMomentum(int momentum) {
        this.momentum = momentum;
        return this;
    }

    public ItemStack output1() {
        return outputItems() == null || outputItems().length == 0
                ? ItemStack.EMPTY
                : outputItems()[0].unwrap().get(0).value();
    }

    public @Nullable ItemStack output2() {
        return outputItems() == null || outputItems().length < 2
                ? null
                : outputItems()[1].unwrap().get(0).value();
    }

    public boolean matchesRecipe(ItemStack in1, ItemStack in2) {
        return this.inputItem[0].matchesItem(in1) && this.inputItem[1].matchesItem(in2)
                || this.inputItem[0].matchesItem(in2) && this.inputItem[1].matchesItem(in1);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return ParticleAcceleratorRecipes.INSTANCE;
    }
}
