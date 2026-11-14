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

package com.hbm.inventory.recipes.ingredient;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record HbmMatchersIngredient(Ingredient base, DataComponentMatchers matchers)
        implements IngredientState, CustomIngredient {

    public static final Identifier ID = RegistryUtil.id("matchers");

    private static final MapCodec<HbmMatchersIngredient> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Ingredient.CODEC
                                                    .fieldOf("base")
                                                    .forGetter(HbmMatchersIngredient::base),
                                            DataComponentMatchers.CODEC.forGetter(
                                                    HbmMatchersIngredient::matchers))
                                    .apply(i, HbmMatchersIngredient::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, HbmMatchersIngredient> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    HbmMatchersIngredient::base,
                    ByteBufCodecs.fromCodecWithRegistries(DataComponentMatchers.CODEC.codec()),
                    HbmMatchersIngredient::matchers,
                    HbmMatchersIngredient::new);

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack) && matchers.test(stack);
    }

    @Override
    public boolean acceptsState(ItemStack stack) {
        return RecipeInputState.accepts(matchers, stack);
    }

    @Override
    public boolean acceptsState(ItemStack stack, DataComponentMatchers additional) {
        return RecipeInputState.accepts(matchers, additional, stack);
    }

    @Override
    public SlotDisplay display() {
        List<SlotDisplay> shown = new ArrayList<>();
        for (Holder<Item> item : items().toList()) {
            ItemStack stack = new ItemStack(item);
            stack.applyComponents(matchers.exact().asPatch());
            if (matchers.partial().get(FluidContentPredicate.TYPE.get())
                            instanceof FluidContentPredicate(Fluid fluid)
                    && fluid != Fluids.EMPTY
                    && stack.getItem() instanceof FluidContainerItem container
                    && container.canStore(fluid)) {
                stack =
                        container.filledContainer(
                                stack, new FluidStackNTM(fluid, container.capacity(stack)));
            }
            shown.add(
                    new SlotDisplay.ItemStackSlotDisplay(
                            ItemStackTemplate.fromNonEmptyStack(stack)));
        }
        return new SlotDisplay.Composite(shown);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return base.items();
    }

    public static final CustomIngredientSerializer<HbmMatchersIngredient> SERIALIZER =
            new Serializer();

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private static final class Serializer
            implements CustomIngredientSerializer<HbmMatchersIngredient> {

        @Override
        public Identifier getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<HbmMatchersIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HbmMatchersIngredient> getStreamCodec() {
            return STREAM_CODEC;
        }
    }
}
