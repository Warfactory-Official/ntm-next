// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.SolidificationRecipe;
import com.hbm.inventory.recipes.SolidificationRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class SolidificationPage extends RecipePage<SolidificationPage.Recipe> {

    SolidificationPage() {
        super(PageIds.page("solidification"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        return SolidificationRecipes.INSTANCE.reachable().stream().map(Recipe::new).toList();
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_solidifier");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_SOLIDIFIER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_SOLIDIFIER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().fluid(recipe.input(), recipe.amount());
        page.output(102, 24).background().items(recipe.outputs());
        page.catalyst(75, 31).item(ModBlocks.MACHINE_SOLIDIFIER.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, Fluid input, int amount, List<ItemStack> outputs) {
        public Recipe(SolidificationRecipe recipe) {
            this(
                    recipe.recipeId().identifier(),
                    recipe.inputFluid[0].type(),
                    recipe.fillReq(),
                    unwrap(recipe.outputItems()));
        }

        private static List<ItemStack> unwrap(WeightedList<ItemStack>[] outputs) {
            List<ItemStack> stacks = new ArrayList<>();
            if (outputs.length > 0) {
                for (Weighted<ItemStack> entry : outputs[0].unwrap())
                    stacks.add(entry.value().copy());
            }
            return stacks;
        }
    }
}
