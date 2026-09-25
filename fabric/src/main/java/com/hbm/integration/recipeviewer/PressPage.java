// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.PressRecipe;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class PressPage extends TablePage<PressRecipe> {

    PressPage() {
        super(PageIds.page("press"), PressRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_press");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_PRESS);
    }

    @Override
    public List<PressRecipe> rows() {
        return PressRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModBlocks.MACHINE_PRESS),
                new ItemStack(ModBlocks.MACHINE_EPRESS),
                new ItemStack(ModBlocks.MACHINE_CONVEYOR_PRESS));
    }

    @Override
    public void layout(PressRecipe recipe, PageLayout page) {

        page.input(48, 42).items(recipe.input().displayStacks());

        List<ItemStack> stamps = new ArrayList<>();
        for (Item stamp : ItemStamp.stampsOf(recipe.type())) stamps.add(new ItemStack(stamp));
        page.input(48, 6).items(stamps);
        page.output(111, 24).item(recipe.output());
    }

    @Override
    public void draw(PressRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.PRESS);

        RecipePanel.progress(graphics, RecipePanel.PRESS, 47, 24, 0, 86, 18, 18, 20, 1);
    }
}
