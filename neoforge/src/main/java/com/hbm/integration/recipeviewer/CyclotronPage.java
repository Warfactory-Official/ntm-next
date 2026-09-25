// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.CyclotronRecipe;
import com.hbm.inventory.recipes.CyclotronRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class CyclotronPage extends TablePage<CyclotronRecipe> {

    private static final int PARTICLE_X = 21, PARTICLE_Y = 24;
    private static final int TARGET_X = 75, TARGET_Y = 24;
    private static final int OUTPUT_X = 129, OUTPUT_Y = 24;

    CyclotronPage() {
        super(PageIds.page("cyclotron"), CyclotronRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_cyclotron");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_CYCLOTRON);
    }

    @Override
    public List<CyclotronRecipe> rows() {
        return CyclotronRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_CYCLOTRON));
    }

    @Override
    public void layout(CyclotronRecipe recipe, PageLayout page) {
        page.input(PARTICLE_X, PARTICLE_Y).items(recipe.particle().displayStacks());
        page.input(TARGET_X, TARGET_Y).items(recipe.target().displayStacks());
        page.output(OUTPUT_X, OUTPUT_Y).item(recipe.output());
    }

    @Override
    public void draw(CyclotronRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.CYCLOTRON);

        RecipePanel.progress(graphics, RecipePanel.CYCLOTRON, 44, 24, 100, 119, 24, 16, 48, 0);
    }
}
