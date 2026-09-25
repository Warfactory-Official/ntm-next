// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.inventory.recipes.MagicRecipe;
import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class MagicPage extends TablePage<MagicRecipe> {

    private static final Identifier SHEET = Library.id("textures/gui/processing/gui_book.png");
    private static final int MAX_INPUTS = 4;

    MagicPage() {
        super(PageIds.page("magic"), MagicRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("item.hbm.book_of_");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModItems.BOOK_OF);
    }

    @Override
    public List<MagicRecipe> rows() {
        return MagicRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModItems.BOOK_OF));
    }

    @Override
    public void layout(MagicRecipe recipe, PageLayout page) {
        CountIngredient[] inputs = recipe.inputItem;
        for (int i = 0; i < Math.min(inputs.length, MAX_INPUTS); i++) {
            page.input(25 + (i % 2) * 36, 6 + (i / 2) * 36).items(inputs[i].displayStacks());
        }
        page.output(119, 24).item(recipe.result());
    }

    @Override
    public void draw(MagicRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, SHEET);
    }
}
