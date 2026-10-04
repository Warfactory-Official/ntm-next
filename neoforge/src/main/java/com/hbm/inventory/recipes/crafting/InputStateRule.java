// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.HbmMatchersIngredient;
import com.hbm.inventory.recipes.ingredient.HbmRecipeIngredient;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.Nullable;

public record InputStateRule(DataComponentMatchers matchers, Preservation preserve) {
    private static final Codec<Preservation> PRESERVATION_CODEC =
            Codec.STRING.comapFlatMap(
                    value ->
                            switch (value) {
                                case "result" -> DataResult.success(Preservation.RESULT);
                                case "remainder" -> DataResult.success(Preservation.REMAINDER);
                                default ->
                                        DataResult.error(
                                                () ->
                                                        "Unknown preserve destination '"
                                                                + value
                                                                + "'; expected result or remainder");
                            },
                    value -> value == Preservation.RESULT ? "result" : "remainder");
    public static final Codec<InputStateRule> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<InputStateRule>mapCodec(
                                    i ->
                                            i.group(
                                                            DataComponentMatchers.CODEC.forGetter(
                                                                    InputStateRule::matchers),
                                                            PRESERVATION_CODEC
                                                                    .optionalFieldOf(
                                                                            "preserve",
                                                                            Preservation.NONE)
                                                                    .forGetter(
                                                                            InputStateRule
                                                                                    ::preserve))
                                                    .apply(i, InputStateRule::new)))
                    .codec();
    public static final InputStateRule STRICT =
            new InputStateRule(DataComponentMatchers.ANY, Preservation.NONE);

    public Bound bind(Ingredient ingredient) {
        return new Bound(ingredient, matchers, preserve);
    }

    public @Nullable String resultError(ItemStackTemplate result) {
        if (result.count() != 1) return "result preservation requires count 1";
        for (var expected : matchers.exact().asPatch().entrySet()) {
            for (var override : result.components().entrySet()) {
                if (override.getKey() == expected.getKey()
                        && !override.getValue().equals(expected.getValue()))
                    return "result component override conflicts with a preserved input requirement: "
                            + expected.getKey();
            }
        }
        return null;
    }

    public enum Preservation {
        NONE,
        RESULT,
        REMAINDER
    }

    public record Bound(
            Ingredient ingredient, DataComponentMatchers matchers, Preservation preserve)
            implements StackedContents.IngredientInfo<ItemStack> {
        @Override
        public boolean acceptsItem(ItemStack stack) {
            if (!ingredient.test(stack) || !matchers.test(stack)) return false;
            return switch (preserve) {
                case RESULT -> true;
                case REMAINDER -> RecipeInputState.preservesRemainder(stack);
                case NONE -> RecipeInputState.accepts(ingredient, stack, matchers);
            };
        }

        public boolean permitsMatched(
                ItemStack stack, ItemStack output, boolean returnsRemainders) {
            return switch (preserve) {
                case RESULT ->
                        RecipeInputState.preservesComponents(stack, output)
                                && matchers.test(output);
                case REMAINDER -> returnsRemainders;
                case NONE -> true;
            };
        }

        public SlotDisplay display() {
            return matchers.isEmpty()
                    ? ingredient.display()
                    : new HbmMatchersIngredient(ingredient, matchers).display();
        }

        public Ingredient placementIngredient() {
            Ingredient constrained =
                    matchers.isEmpty()
                            ? ingredient
                            : new HbmMatchersIngredient(ingredient, matchers).toVanilla();
            return preserve == Preservation.RESULT
                    ? constrained
                    : new HbmRecipeIngredient(constrained, preserve == Preservation.REMAINDER)
                            .toVanilla();
        }
    }
}
