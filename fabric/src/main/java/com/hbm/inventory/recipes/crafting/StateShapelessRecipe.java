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
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class StateShapelessRecipe extends HbmNormalCraftingRecipe {
    private static final MapCodec<Definition> DEFINITION_CODEC =
            DataCodecs.recipe(
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(
                                                    Recipe.CommonInfo.MAP_CODEC.forGetter(
                                                            Definition::commonInfo),
                                                    CraftingRecipe.CraftingBookInfo.MAP_CODEC
                                                            .forGetter(Definition::bookInfo),
                                                    Ingredient.CODEC
                                                            .listOf(1, 9)
                                                            .fieldOf("ingredients")
                                                            .forGetter(Definition::ingredients),
                                                    ItemStackTemplate.CODEC
                                                            .fieldOf("result")
                                                            .forGetter(Definition::result),
                                                    Codec.unboundedMap(
                                                                    Codec.STRING,
                                                                    InputStateRule.CODEC)
                                                            .optionalFieldOf(
                                                                    "input_state", Map.of())
                                                            .forGetter(Definition::inputState))
                                            .apply(i, Definition::new)));
    public static final MapCodec<StateShapelessRecipe> MAP_CODEC =
            DEFINITION_CODEC.flatXmap(
                    StateShapelessRecipe::decode,
                    recipe ->
                            DataResult.success(
                                    new Definition(
                                            recipe.commonInfo,
                                            recipe.bookInfo,
                                            recipe.ingredients,
                                            recipe.result,
                                            recipe.inputState)));
    public static final StreamCodec<RegistryFriendlyByteBuf, StateShapelessRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());
    public static final RecipeSerializer<StateShapelessRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private final List<Ingredient> ingredients;
    private final ItemStackTemplate result;
    private final Map<String, InputStateRule> inputState;
    private final List<InputStateRule.Bound> inputs;
    private final int carrier;
    private final boolean simple;

    private StateShapelessRecipe(Definition definition) {
        super(definition.commonInfo, definition.bookInfo);
        ingredients = List.copyOf(definition.ingredients);
        result = definition.result;
        inputState = Map.copyOf(definition.inputState);
        for (String key : inputState.keySet()) {
            if (key.length() != 1
                    || key.charAt(0) < '0'
                    || key.charAt(0) >= '0' + ingredients.size())
                throw new IllegalArgumentException(
                        "input_state references undefined ingredient index '" + key + "'");
        }
        List<InputStateRule.Bound> compiled = new ArrayList<>();
        int resultCarrier = -1;
        for (int index = 0; index < ingredients.size(); index++) {
            InputStateRule.Bound bound =
                    inputState
                            .getOrDefault(String.valueOf(index), InputStateRule.STRICT)
                            .bind(ingredients.get(index));
            compiled.add(bound);
            if (bound.preserve() == InputStateRule.Preservation.RESULT) {
                if (resultCarrier != -1)
                    throw new IllegalArgumentException(
                            "input_state result preservation requires exactly one carrier");
                resultCarrier = index;
            }
        }
        inputs = List.copyOf(compiled);
        carrier = resultCarrier;
        simple =
                inputs.stream()
                        .allMatch(
                                bound ->
                                        bound.ingredient().getCustomIngredient() == null
                                                && bound.matchers().isEmpty()
                                                && bound.preserve()
                                                        != InputStateRule.Preservation.REMAINDER);
        if (carrier != -1) {
            InputStateRule.Bound bound = inputs.get(carrier);
            String error =
                    new InputStateRule(bound.matchers(), bound.preserve()).resultError(result);
            if (error != null) throw new IllegalArgumentException(error);
        }
    }

    @Override
    public @Nullable Ingredient resultCarrier() {
        return carrier < 0 ? null : placementInfo().ingredients().get(carrier);
    }

    private static DataResult<StateShapelessRecipe> decode(Definition definition) {
        try {
            return DataResult.success(new StateShapelessRecipe(definition));
        } catch (IllegalArgumentException error) {
            return DataResult.error(error::getMessage);
        }
    }

    private boolean pick(CraftingInput input, @Nullable List<ItemStack> selected) {
        if (input.ingredientCount() != inputs.size()) return false;
        StackedContents<ItemStack> contents = new StackedContents<>();
        for (ItemStack stack : input.items()) if (!stack.isEmpty()) contents.account(stack, 1);
        return contents.tryPick(inputs, 1, selected == null ? null : selected::add);
    }

    @Override
    protected boolean matchesInput(CraftingInput input, Level level) {
        return input.ingredientCount() == inputs.size();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (carrier == -1 || !RecipeInputState.hasChangedInputs(input)) return result.create();
        List<ItemStack> selected = new ArrayList<>(inputs.size());
        if (!pick(input, selected)) return ItemStack.EMPTY;
        return TransmuteRecipe.createWithOriginalComponents(result, selected.get(carrier));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        List<ItemStack> selected = new ArrayList<>(inputs.size());
        if (!pick(input, selected))
            throw new IllegalStateException("Cannot return remainders for an unmatched grid");
        NonNullList<ItemStack> left = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            int index = selected.indexOf(stack);
            InputStateRule.Bound bound = inputs.get(index);
            left.set(
                    slot,
                    RecipeInputState.craftingRemainder(
                            bound.ingredient(), stack, bound.matchers()));
            selected.set(index, ItemStack.EMPTY);
        }
        return left;
    }

    @Override
    public boolean permitsInputStates(
            CraftingInput input, @Nullable ItemStack output, boolean returnsRemainders) {
        if (input.ingredientCount() != inputs.size()) return false;
        boolean changed = RecipeInputState.hasChangedInputs(input);
        if (inputs.size() == 1) {
            ItemStack stack = input.getItem(0);
            InputStateRule.Bound bound = inputs.getFirst();
            if (!bound.acceptsItem(stack)) return false;
            if (!changed && returnsRemainders) return true;
            if (bound.preserve() == InputStateRule.Preservation.RESULT && output == null)
                output = TransmuteRecipe.createWithOriginalComponents(result, stack);
            return bound.permitsMatched(
                    stack, output == null ? ItemStack.EMPTY : output, returnsRemainders);
        }
        if (simple && !changed) return input.stackedContents().canCraft(ingredients, null);
        if (returnsRemainders && (!changed || carrier == -1)) return pick(input, null);
        List<ItemStack> selected = new ArrayList<>(inputs.size());
        if (!pick(input, selected)) return false;
        for (int index = 0; index < inputs.size(); index++) {
            ItemStack stack = selected.get(index);
            InputStateRule.Bound bound = inputs.get(index);
            if (bound.preserve() == InputStateRule.Preservation.RESULT && output == null)
                output = TransmuteRecipe.createWithOriginalComponents(result, stack);
            if (!bound.permitsMatched(
                    stack, output == null ? ItemStack.EMPTY : output, returnsRemainders))
                return false;
        }
        return true;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(
                inputs.stream().map(InputStateRule.Bound::placementIngredient).toList());
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
                new ShapelessCraftingRecipeDisplay(
                        inputs.stream().map(InputStateRule.Bound::display).toList(),
                        new SlotDisplay.ItemStackSlotDisplay(result),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<StateShapelessRecipe> getSerializer() {
        return SERIALIZER;
    }

    private record Definition(
            Recipe.CommonInfo commonInfo,
            CraftingRecipe.CraftingBookInfo bookInfo,
            List<Ingredient> ingredients,
            ItemStackTemplate result,
            Map<String, InputStateRule> inputState) {}
}
