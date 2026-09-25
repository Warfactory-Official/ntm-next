// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class BoilingPage extends RecipePage<BoilingPage.Recipe> {

    BoilingPage() {
        super(PageIds.page("boiling"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (Fluid fluid : NTMFluids.displayOrder()) {
            FT_Heatable trait = NTMFluidProperties.getTrait(fluid, FT_Heatable.class);
            if (trait == null || trait.getEfficiency(HeatingType.BOILER) <= 0) continue;
            HeatingStep step = trait.getFirstStep();
            rows.add(
                    new Recipe(
                            PageIds.derived(
                                    id(), PageIds.segment(BuiltInRegistries.FLUID.getKey(fluid))),
                            fluid,
                            step.amountReq,
                            step.typeProduced(),
                            step.amountProduced));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_boiler");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_BOILER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return machines();
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().fluid(recipe.input(), recipe.inputAmount());
        page.output(102, 24).background().fluid(recipe.output(), recipe.outputAmount());
        page.catalyst(75, 31).items(machines());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    private static List<ItemStack> machines() {
        return List.of(
                new ItemStack(ModBlocks.MACHINE_BOILER),
                new ItemStack(ModBlocks.MACHINE_INDUSTRIAL_BOILER));
    }

    public record Recipe(
            Identifier id, Fluid input, int inputAmount, Fluid output, int outputAmount) {}
}
