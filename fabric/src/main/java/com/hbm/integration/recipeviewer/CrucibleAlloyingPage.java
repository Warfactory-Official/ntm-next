// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.machine.ItemScraps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class CrucibleAlloyingPage extends RecipePage<CrucibleAlloyingPage.Recipe> {

    CrucibleAlloyingPage() {
        super(PageIds.page("crucible_alloying"), Recipe.class);
    }

    private static List<ItemStack> molten(MaterialStack[] materials) {
        List<ItemStack> out = new ArrayList<>(materials.length);
        for (MaterialStack material : materials) out.add(ItemScraps.create(material, true));
        return out;
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> out = new ArrayList<>();
        for (CrucibleRecipe recipe : CrucibleRecipes.INSTANCE.recipes()) {
            out.add(
                    new Recipe(
                            recipe.recipeId().identifier(),
                            molten(recipe.input),
                            molten(recipe.output)));
        }
        return out;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.crucible_alloying");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_CRUCIBLE);
    }

    @Override
    public int height() {
        return 60;
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_CRUCIBLE));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        for (int i = 0; i < recipe.input().size(); i++) {
            page.input(12 + (i % 3) * 18, 6 + (i / 3) * 18).item(recipe.input().get(i));
        }
        for (int i = 0; i < recipe.output().size(); i++) {
            page.output(102 + (i % 3) * 18, 6 + (i / 3) * 18).item(recipe.output().get(i));
        }
        page.catalyst(75, 42).item(ModBlocks.MACHINE_CRUCIBLE.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.CRUCIBLE);
    }

    public record Recipe(Identifier id, List<ItemStack> input, List<ItemStack> output) {}
}
