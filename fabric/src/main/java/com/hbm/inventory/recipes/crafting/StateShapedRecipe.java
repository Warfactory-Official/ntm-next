// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class StateShapedRecipe extends ShapedRecipe implements CraftingInputState {
    private static final MapCodec<Definition> DEFINITION_CODEC =
            DataCodecs.recipe(
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(
                                                    Recipe.CommonInfo.MAP_CODEC.forGetter(
                                                            Definition::commonInfo),
                                                    CraftingRecipe.CraftingBookInfo.MAP_CODEC
                                                            .forGetter(Definition::bookInfo),
                                                    ShapedRecipePattern.Data.MAP_CODEC.forGetter(
                                                            Definition::pattern),
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
    public static final MapCodec<StateShapedRecipe> MAP_CODEC =
            DEFINITION_CODEC.flatXmap(
                    StateShapedRecipe::decode,
                    recipe ->
                            DataResult.success(
                                    new Definition(
                                            recipe.commonInfo,
                                            recipe.bookInfo,
                                            recipe.data,
                                            recipe.result,
                                            recipe.inputState)));
    public static final StreamCodec<RegistryFriendlyByteBuf, StateShapedRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());
    public static final RecipeSerializer<StateShapedRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private final ShapedRecipePattern.Data data;
    private final ItemStackTemplate result;
    private final Map<String, InputStateRule> inputState;
    private final InputStateRule.Bound[] cells;
    private final int carrier;
    private final int ingredientCount;

    private StateShapedRecipe(Definition definition) {
        ShapedRecipePattern.Data data = definition.pattern;
        ItemStackTemplate result = definition.result;
        Map<String, InputStateRule> inputState = Map.copyOf(definition.inputState);
        int left = 3;
        int top = 3;
        for (int y = 0; y < data.pattern().size(); y++) {
            String row = data.pattern().get(y);
            for (int x = 0; x < row.length(); x++)
                if (row.charAt(x) != ' ') {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                }
        }
        if (top == 3) throw new IllegalArgumentException("pattern contains no ingredients");
        ShapedRecipePattern pattern = ShapedRecipePattern.of(data.key(), data.pattern());
        for (String symbol : inputState.keySet()) {
            if (symbol.length() != 1 || !data.key().containsKey(symbol.charAt(0)))
                throw new IllegalArgumentException(
                        "input_state references undefined pattern symbol '" + symbol + "'");
        }
        InputStateRule.Bound[] cells = new InputStateRule.Bound[pattern.width() * pattern.height()];
        int resultCarrier = -1;
        for (int y = 0; y < pattern.height(); y++)
            for (int x = 0; x < pattern.width(); x++) {
                char symbol = data.pattern().get(y + top).charAt(x + left);
                if (symbol == ' ') continue;
                int index = x + y * pattern.width();
                cells[index] =
                        inputState
                                .getOrDefault(String.valueOf(symbol), InputStateRule.STRICT)
                                .bind(data.key().get(symbol));
                if (cells[index].preserve() == InputStateRule.Preservation.RESULT) {
                    if (resultCarrier != -1)
                        throw new IllegalArgumentException(
                                "input_state result preservation requires exactly one carrier occurrence");
                    resultCarrier = index;
                }
            }
        if (resultCarrier != -1) {
            InputStateRule.Bound bound = cells[resultCarrier];
            String error =
                    new InputStateRule(bound.matchers(), bound.preserve()).resultError(result);
            if (error != null) throw new IllegalArgumentException(error);
        }
        super(
                definition.commonInfo,
                definition.bookInfo,
                new ShapedRecipePattern(
                        pattern.width(),
                        pattern.height(),
                        Arrays.stream(cells)
                                .map(
                                        cell ->
                                                cell == null
                                                        ? Optional.<Ingredient>empty()
                                                        : Optional.of(cell.placementIngredient()))
                                .toList(),
                        Optional.empty()),
                result);
        this.data = data;
        this.result = result;
        this.inputState = inputState;
        this.cells = cells;
        carrier = resultCarrier;
        ingredientCount = (int) Arrays.stream(cells).filter(Objects::nonNull).count();
    }

    private static DataResult<StateShapedRecipe> decode(Definition definition) {
        try {
            return DataResult.success(new StateShapedRecipe(definition));
        } catch (IllegalArgumentException | IllegalStateException error) {
            return DataResult.error(error::getMessage);
        }
    }

    private int orientation(CraftingInput input) {
        if (input.width() != getWidth() || input.height() != getHeight()) return -1;
        for (int flip = 0; flip <= 1; flip++) {
            boolean matched = true;
            for (int slot = 0; slot < input.size(); slot++) {
                InputStateRule.Bound expected = cells[sourceCell(slot, flip)];
                ItemStack stack = input.getItem(slot);
                if (expected == null ? !stack.isEmpty() : !expected.acceptsItem(stack)) {
                    matched = false;
                    break;
                }
            }
            if (matched) return flip;
        }
        return -1;
    }

    private int sourceCell(int slot, int flip) {
        int width = getWidth();
        int x = slot % width;
        return (flip == 0 ? x : width - 1 - x) + slot / width * width;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.width() == getWidth()
                && input.height() == getHeight()
                && input.ingredientCount() == ingredientCount
                && RecipeInputState.crafting(this, input, null, true);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (carrier == -1) return result.create();
        int flip = orientation(input);
        if (flip < 0) return ItemStack.EMPTY;
        return TransmuteRecipe.createWithOriginalComponents(
                result, input.getItem(sourceCell(carrier, flip)));
    }

    @Override
    public boolean permitsInputStates(
            CraftingInput input, @Nullable ItemStack output, boolean returnsRemainders) {
        int flip = orientation(input);
        if (flip < 0) return false;
        if (carrier == -1 && returnsRemainders) return true;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || stack.getComponentsPatch().isEmpty()) continue;
            InputStateRule.Bound bound = cells[sourceCell(slot, flip)];
            if (bound.preserve() == InputStateRule.Preservation.RESULT && output == null)
                output = TransmuteRecipe.createWithOriginalComponents(result, stack);
            if (!bound.permitsMatched(
                    stack, output == null ? ItemStack.EMPTY : output, returnsRemainders))
                return false;
        }
        return true;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
                new ShapedCraftingRecipeDisplay(
                        getWidth(),
                        getHeight(),
                        Arrays.stream(cells)
                                .map(
                                        cell ->
                                                cell == null
                                                        ? SlotDisplay.Empty.INSTANCE
                                                        : cell.display())
                                .toList(),
                        new SlotDisplay.ItemStackSlotDisplay(result),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }

    private record Definition(
            Recipe.CommonInfo commonInfo,
            CraftingRecipe.CraftingBookInfo bookInfo,
            ShapedRecipePattern.Data pattern,
            ItemStackTemplate result,
            Map<String, InputStateRule> inputState) {}
}
