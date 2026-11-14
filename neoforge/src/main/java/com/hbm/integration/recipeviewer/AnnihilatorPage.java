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

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.AnnihilatorRecipe.Key;
import com.hbm.inventory.recipes.AnnihilatorRecipe.Milestone;
import com.hbm.inventory.recipes.AnnihilatorRecipe;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.items.ModItems;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class AnnihilatorPage extends RecipePage<AnnihilatorPage.Recipe> {

    AnnihilatorPage() {
        super(PageIds.page("annihilator"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        Set<Item> secret = new HashSet<>();
        for (var member : ModItems.ITEM_SECRET) secret.add(member.get());
        List<Recipe> rows = new ArrayList<>();
        for (RecipeHolder<AnnihilatorRecipe> holder : AnnihilatorRecipes.rows()) {
            Key key = holder.value().key();
            List<ItemStack> input = key.fluid() != null ? List.of() : inputStacks(key);
            if (input.stream().anyMatch(stack -> secret.contains(stack.getItem()))) continue;
            for (Milestone milestone : holder.value().milestones()) {
                ItemStack payout = milestone.payout().create();
                if (secret.contains(payout.getItem())) continue;
                rows.add(
                        new Recipe(
                                PageIds.derived(
                                        holder.id().identifier(), milestone.amount().toString()),
                                key,
                                input,
                                milestone.amount(),
                                payout));
            }
        }
        return rows;
    }

    private static List<ItemStack> inputStacks(Key key) {
        if (key.item() != null) return List.of(new ItemStack(key.item()));
        if (key.stack() != null) return List.of(key.stack().create());
        List<ItemStack> stacks = new ArrayList<>();
        BuiltInRegistries.ITEM
                .getTagOrEmpty(key.tag())
                .forEach(item -> stacks.add(new ItemStack(item)));
        return stacks;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.annihilator");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ANNIHILATOR);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ANNIHILATOR));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        PageSlot input = page.input(48, 24).background();
        if (recipe.key().fluid() != null) {
            input.fluid(recipe.key().fluid());
        } else {
            input.items(recipe.input());
        }
        input.tooltip(
                (shown, lines) ->
                        lines.accept(
                                Component.literal(
                                                String.format(Locale.ROOT, "%,d", recipe.amount()))
                                        .withStyle(ChatFormatting.RED)));
        page.output(102, 24).background().item(recipe.payout());
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ANNIHILATOR.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id, Key key, List<ItemStack> input, BigInteger amount, ItemStack payout) {}
}
