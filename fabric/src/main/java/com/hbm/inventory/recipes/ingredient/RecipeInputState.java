// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.inventory.recipes.crafting.CraftingInputState;
import com.hbm.items.tool.ItemCraftingDegradation;
import java.util.Objects;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.ComponentsIngredient;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.AnyValue;
import net.minecraft.core.component.predicates.CustomDataPredicate;
import net.minecraft.core.component.predicates.DamagePredicate;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

public final class RecipeInputState {
    private RecipeInputState() {}

    public static boolean accepts(Ingredient ingredient, ItemStack stack) {
        return accepts(ingredient, stack, DataComponentMatchers.ANY);
    }

    public static boolean accepts(
            Ingredient ingredient, ItemStack stack, DataComponentMatchers additional) {
        if (stack.getComponentsPatch().isEmpty()) return true;
        var custom = ingredient.getCustomIngredient();
        if (custom instanceof IngredientState state) return state.acceptsState(stack, additional);
        if (custom == null) return accepts(additional, stack);
        if (custom.getClass() != ComponentsIngredient.class) return accepts(additional, stack);
        for (ItemStack expected : CountIngredient.displayStacks(ingredient, 1)) {
            if (!stack.is(expected.getItem())) continue;
            boolean matched = true;
            for (var change : stack.getComponentsPatch().entrySet()) {
                if (!claims(additional, change.getKey(), stack)
                        && !Objects.equals(
                                stack.get(change.getKey()), expected.get(change.getKey()))) {
                    matched = false;
                    break;
                }
            }
            if (matched) return true;
        }
        return false;
    }

    public static boolean accepts(DataComponentMatchers matchers, ItemStack stack) {
        return accepts(matchers, DataComponentMatchers.ANY, stack);
    }

    public static boolean accepts(
            DataComponentMatchers matchers, DataComponentMatchers additional, ItemStack stack) {
        for (var change : stack.getComponentsPatch().entrySet()) {
            if (!claims(matchers, change.getKey(), stack)
                    && !claims(additional, change.getKey(), stack)) return false;
        }
        return true;
    }

    private static boolean claims(
            DataComponentMatchers matchers, DataComponentType<?> type, ItemStack stack) {
        for (var expected : matchers.exact().expectedComponents) {
            if (expected.type() == type) return true;
        }
        for (DataComponentPredicate predicate : matchers.partial().values()) {
            boolean required = false;
            if (predicate instanceof AnyValue(DataComponentType<?> type1)) {
                required = type == type1;
            } else if (predicate instanceof ComponentStatePredicate state) {
                required = state.claims(type, stack);
            } else if (predicate instanceof DamagePredicate) {
                required = type == DataComponents.DAMAGE;
            } else if (predicate instanceof CustomDataPredicate(NbtPredicate value)
                    && type == DataComponents.CUSTOM_DATA) {
                required =
                        stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                                .tag
                                .equals(value.tag());
            }
            if (required) return true;
        }
        return false;
    }

    public static boolean only(ItemStack stack, DataComponentType<?>... required) {
        for (var change : stack.getComponentsPatch().entrySet()) {
            boolean allowed = false;
            for (DataComponentType<?> type : required) {
                if (change.getKey() == type) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return false;
        }
        return true;
    }

    public static boolean preservesRemainder(ItemStack stack) {
        ItemStackTemplate template = stack.getCraftingRemainder();
        if (template == null) return false;
        ItemStack remaining = template.create();
        for (var change : stack.getComponentsPatch().entrySet()) {
            DataComponentType<?> type = change.getKey();
            if (type == DataComponents.DAMAGE
                    && stack.getItem() instanceof ItemCraftingDegradation
                    && remaining.is(stack.getItem())
                    && remaining.getDamageValue() == stack.getDamageValue() + 1) continue;
            if (!Objects.equals(stack.get(type), remaining.get(type))) return false;
        }
        return true;
    }

    public static boolean hasChangedInputs(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && !stack.getComponentsPatch().isEmpty()) return true;
        }
        return false;
    }

    public static boolean crafting(
            CraftingRecipe recipe,
            CraftingInput input,
            @Nullable ItemStack output,
            boolean returnsRemainders) {
        if (recipe instanceof CraftingInputState state)
            return state.permitsInputStates(input, output, returnsRemainders);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || stack.getComponentsPatch().isEmpty()) continue;
            if (returnsRemainders && preservesRemainder(stack)) continue;
            boolean required = false;
            for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
                if (ingredient.test(stack) && accepts(ingredient, stack)) {
                    required = true;
                    break;
                }
            }
            if (!required) return false;
        }
        return true;
    }

    public static boolean preservesComponents(ItemStack source, ItemStack result) {
        if (result.isEmpty()) return false;
        for (var change : source.getComponentsPatch().entrySet()) {
            if (!Objects.equals(source.get(change.getKey()), result.get(change.getKey())))
                return false;
        }
        return true;
    }
}
