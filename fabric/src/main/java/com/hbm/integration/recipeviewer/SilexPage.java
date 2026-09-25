// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.SILEXRecipe;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.machine.EnumWavelengths;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.item.ItemStack;

public final class SilexPage extends TablePage<SILEXRecipe> {

    SilexPage() {
        super(PageIds.page("silex"), SILEXRecipe.class);
    }

    private static int[] outputPosition(int index, int count) {
        int separator = count > 4 ? 3 : 2;
        if (index < separator) {
            return new int[] {68, 24 + index * 18 - 9 * ((Math.min(count, separator) + 1) / 2)};
        }
        return new int[] {
            116,
            24 + (index - separator) * 18 - 9 * ((Math.min(count - separator, separator) + 1) / 2)
        };
    }

    private static List<Weighted<ItemStack>> entries(SILEXRecipe recipe) {
        return new ArrayList<>(recipe.outputs().unwrap());
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_silex");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_SILEX);
    }

    @Override
    public List<SILEXRecipe> rows() {
        return SILEXRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_SILEX));
    }

    @Override
    public void layout(SILEXRecipe recipe, PageLayout page) {
        if (recipe.inputFluid.length > 0) page.input(12, 24).fluid(recipe.inputFluid[0].type());
        else page.input(12, 24).items(recipe.inputItem[0].displayStacks());
        List<Weighted<ItemStack>> outputs = entries(recipe);
        for (int i = 0; i < outputs.size(); i++) {
            int[] at = outputPosition(i, outputs.size());
            page.output(at[0], at[1]).item(outputs.get(i).value());
        }
    }

    @Override
    public void draw(SILEXRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.SILEX);
        List<Weighted<ItemStack>> outputs = entries(recipe);
        int weight = 0;
        for (Weighted<ItemStack> entry : outputs) weight += entry.weight();
        for (int i = 0; i < outputs.size(); i++) {
            int[] at = outputPosition(i, outputs.size());
            double chance = weight == 0 ? 0D : 100D * outputs.get(i).weight() / weight;
            RecipePanel.leftAligned(
                    graphics,
                    Component.translatable("jei.hbm.silex_chance", (int) (chance * 10D) / 10D),
                    at[0] + 18,
                    at[1] + 4,
                    RecipePanel.TEXT);
        }

        int produced = recipe.fluidConsumed == 0 ? 0 : recipe.fluidProduced / recipe.fluidConsumed;
        RecipePanel.centred(
                graphics,
                Component.translatable("jei.hbm.silex_yield", (int) (produced * 10D) / 10D),
                52,
                43,
                RecipePanel.TEXT);
        EnumWavelengths strength = recipe.laserStrength;
        Component wavelength =
                strength == EnumWavelengths.NULL
                        ? Component.translatable("jei.hbm.silex_no_laser")
                                .withStyle(ChatFormatting.WHITE)
                        : Component.translatable(strength.name).withStyle(strength.textColor);
        RecipePanel.centred(graphics, wavelength, 33, 8, RecipePanel.TEXT);
    }
}
