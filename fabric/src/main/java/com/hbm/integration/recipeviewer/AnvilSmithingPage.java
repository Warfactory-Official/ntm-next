// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AnvilSmithingPage extends TablePage<AnvilSmithingRecipe> {

    AnvilSmithingPage() {
        super(PageIds.page("anvil_smithing"), AnvilSmithingRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("desc.misc.anvilConstructionCategory.anvil");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.ANVIL_IRON);
    }

    @Override
    public List<AnvilSmithingRecipe> rows() {
        return AnvilSmithingRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return AnvilConstructionPage.anvils();
    }

    @Override
    public void layout(AnvilSmithingRecipe recipe, PageLayout page) {
        page.input(39, 24).items(recipe.left().displayStacks());
        page.input(75, 24).items(recipe.right().displayStacks());
        page.output(111, 24).item(recipe.getSimpleOutput());
    }

    @Override
    public void draw(AnvilSmithingRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.SMITHING);
        RecipePanel.leftAligned(
                graphics,
                Component.translatable("jei.hbm.anvil_tier", recipe.tier),
                52,
                43,
                RecipePanel.TEXT);
    }
}
