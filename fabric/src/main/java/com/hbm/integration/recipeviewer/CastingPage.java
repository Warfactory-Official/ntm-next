// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.machine.ItemScraps;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class CastingPage extends RecipePage<CastingPage.Recipe> {

    CastingPage() {
        super(PageIds.page("casting"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        return CrucibleRecipes.moldRecipes().stream()
                .map(c -> new Recipe(rowId(c), c.metal(), c.mold(), c.block(), c.result()))
                .toList();
    }

    private Identifier rowId(CrucibleRecipes.Cast cast) {
        return PageIds.derived(
                id(),
                PageIds.segment(BuiltInRegistries.ITEM.getKey(cast.mold().getItem())),
                ItemScraps.getMats(cast.metal()).material.tagPath);
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.casting");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_STRAND_CASTER);
    }

    @Override
    public int height() {
        return 60;
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModBlocks.FOUNDRY_BASIN),
                new ItemStack(ModBlocks.FOUNDRY_MOLD),
                new ItemStack(ModBlocks.MACHINE_STRAND_CASTER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).item(recipe.metal());
        page.input(75, 6).item(recipe.mold());
        page.input(75, 42).item(recipe.block());
        page.output(102, 24).item(recipe.result());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.FOUNDRY);
    }

    public record Recipe(
            Identifier id, ItemStack metal, ItemStack mold, ItemStack block, ItemStack result) {}
}
