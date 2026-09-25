// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.FuelPoolRecipe;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.items.machine.ItemRBMKRod;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class FuelPoolPage extends TablePage<FuelPoolRecipe> {

    FuelPoolPage() {
        super(PageIds.page("fuel_pool"), FuelPoolRecipe.class);
    }

    @Override
    public List<FuelPoolRecipe> rows() {
        return FuelPoolRecipes.INSTANCE.recipes();
    }

    @Override
    public LookupFilter lookupFilter() {
        return (looked, lookup) -> {
            if (!(looked.getItem() instanceof ItemRBMKRod)) return true;
            boolean hot =
                    ItemRBMKRod.getCoreHeat(looked) >= 50 || ItemRBMKRod.getHullHeat(looked) >= 50;
            return lookup == Lookup.USES ? hot : !hot;
        };
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.fuel_pool");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.WASTE_DRUM);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.WASTE_DRUM));
    }

    @Override
    public void layout(FuelPoolRecipe recipe, PageLayout page) {
        page.input(48, 24).background().items(recipe.input().displayStacks());
        page.output(102, 24).background().item(recipe.output());
        page.catalyst(75, 31).item(ModBlocks.WASTE_DRUM.get());
    }

    @Override
    public void draw(FuelPoolRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}
