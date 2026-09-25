// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.SolderingRecipe;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class SolderingStationPage extends TablePage<SolderingRecipe> {

    SolderingStationPage() {
        super(PageIds.page("soldering_station"), SolderingRecipe.class);
    }

    private static int addInputs(
            PageLayout page, CountIngredient[] inputs, int[][] positions, int slot) {
        for (CountIngredient input : inputs) {
            page.input(positions[slot][0], positions[slot][1])
                    .background()
                    .items(input.displayStacks());
            slot++;
        }
        return slot;
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_soldering_station");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_SOLDERING_STATION);
    }

    @Override
    public List<SolderingRecipe> rows() {
        return SolderingRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_SOLDERING_STATION));
    }

    @Override
    public void layout(SolderingRecipe recipe, PageLayout page) {
        int count =
                recipe.toppings().length
                        + recipe.pcb().length
                        + recipe.solder().length
                        + (recipe.fluid() == null ? 0 : 1);

        int[][] positions = GenericMachinePage.universalInputPositions(count);
        int slot = addInputs(page, recipe.toppings(), positions, 0);
        slot = addInputs(page, recipe.pcb(), positions, slot);
        slot = addInputs(page, recipe.solder(), positions, slot);
        FluidStackNTM fluid = recipe.fluid();
        if (fluid != null)
            page.input(positions[slot][0], positions[slot][1]).background().fluid(fluid);
        page.output(102, 24).background().item(recipe.output());
        page.catalyst(75, 31).item(ModBlocks.MACHINE_SOLDERING_STATION.get());
    }

    @Override
    public void draw(SolderingRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
        RecipePanel.rightAligned(
                graphics,
                Component.translatable(
                        "jei.hbm.duration", String.format(Locale.US, "%,d", recipe.duration())),
                160,
                43,
                RecipePanel.TEXT);
        RecipePanel.rightAligned(
                graphics,
                Component.translatable(
                        "jei.hbm.consumption_padded",
                        String.format(Locale.US, "%,d", recipe.consumption())),
                160,
                55,
                RecipePanel.TEXT);
    }
}
