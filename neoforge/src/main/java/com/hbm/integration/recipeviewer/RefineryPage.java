// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.RefineryRecipe;
import com.hbm.inventory.recipes.RefineryRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class RefineryPage extends TablePage<RefineryRecipe> {

    private static final int[][] OUTPUTS = {{111, 6}, {129, 15}, {111, 24}, {129, 33}};
    private static final int[] SOLID = {111, 42};
    private static final int BATCH = 1000;
    private static final int FACTOR = 10;

    RefineryPage() {
        super(PageIds.page("refinery"), RefineryRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_refinery");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_REFINERY);
    }

    @Override
    public List<RefineryRecipe> rows() {
        return RefineryRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_REFINERY));
    }

    @Override
    public void layout(RefineryRecipe recipe, PageLayout page) {

        page.input(48, 24).fluid(recipe.inputFluid[0], BATCH);
        for (int i = 0; i < recipe.outputFluid.length && i < OUTPUTS.length; i++) {
            FluidStackNTM output = recipe.outputFluid[i];
            page.output(OUTPUTS[i][0], OUTPUTS[i][1]).fluid(output, output.amount() * FACTOR);
        }
        ItemStack solid = recipe.solidOutput();
        if (!solid.isEmpty()) page.output(SOLID[0], SOLID[1]).item(solid);
    }

    @Override
    public void draw(RefineryRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.REFINERY);

        RecipePanel.progress(graphics, RecipePanel.REFINERY, 3, 6, 0, 86, 16, 52, 480, 7);
        RecipePanel.progress(graphics, RecipePanel.REFINERY, 78, 24, 16, 86, 24, 17, 48, 0);
    }
}
