// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipe;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ParticleAcceleratorPage extends TablePage<ParticleAcceleratorRecipe> {

    ParticleAcceleratorPage() {
        super(PageIds.page("particle_accelerator"), ParticleAcceleratorRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.particle_accelerator");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.PA_DETECTOR);
    }

    @Override
    public List<ParticleAcceleratorRecipe> rows() {
        return ParticleAcceleratorRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.PA_DETECTOR));
    }

    @Override
    public void layout(ParticleAcceleratorRecipe recipe, PageLayout page) {

        int[][] positions = GenericMachinePage.universalInputPositions(2);
        page.input(positions[0][0], positions[0][1])
                .background()
                .items(recipe.inputItem[0].displayStacks());
        page.input(positions[1][0], positions[1][1])
                .background()
                .items(recipe.inputItem[1].displayStacks());

        ItemStack output2 = recipe.output2();
        int[][] outputs = GenericMachinePage.outputPositions(output2 == null ? 1 : 2);
        page.output(outputs[0][0], outputs[0][1]).background().item(recipe.output1());
        if (output2 != null) page.output(outputs[1][0], outputs[1][1]).background().item(output2);
        page.catalyst(75, 31).item(ModBlocks.PA_DETECTOR.get());
    }

    @Override
    public void draw(ParticleAcceleratorRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);

        RecipePanel.leftAligned(
                graphics,
                Component.translatable(
                        "jei.hbm.particle_accelerator.momentum",
                        String.format(Locale.US, "%,d", recipe.momentum)),
                8,
                52,
                RecipePanel.TEXT);
    }
}
