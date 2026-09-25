// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.ShredderRecipe;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ShredderPage extends TablePage<ShredderRecipe> {

    ShredderPage() {
        super(PageIds.page("shredder"), ShredderRecipe.class);
    }

    private static List<ItemStack> claimedInputs(ShredderRecipe recipe) {
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack stack : recipe.input().displayStacks()) {
            if (ShredderRecipes.getRecipe(stack) == recipe) inputs.add(stack);
        }
        return inputs;
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_shredder");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_SHREDDER);
    }

    @Override
    public List<ShredderRecipe> rows() {
        return ShredderRecipes.INSTANCE.recipes().stream()
                .filter(r -> !claimedInputs(r).isEmpty())
                .toList();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_SHREDDER));
    }

    @Override
    public void layout(ShredderRecipe recipe, PageLayout page) {

        page.input(39, 24).items(claimedInputs(recipe));
        page.output(129, 24).item(recipe.output());

        List<ItemStack> blades =
                List.of(
                        new ItemStack(ModItems.BLADES_STEEL.get()),
                        new ItemStack(ModItems.BLADES_TITANIUM.get()),
                        new ItemStack(ModItems.BLADES_DESH.get()));
        page.display(84, 6).items(blades);
        page.display(84, 42).items(blades);
    }

    @Override
    public void draw(ShredderRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.SHREDDER);

        RecipePanel.progress(graphics, RecipePanel.SHREDDER, 3, 6, 36, 86, 16, 52, 480, 7);
        RecipePanel.progress(graphics, RecipePanel.SHREDDER, 80, 23, 100, 118, 24, 16, 48, 0);
    }
}
