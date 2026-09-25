// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemWatzPellet.EnumWatzType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class WatzFuelPage extends RecipePage<WatzFuelPage.Recipe> {

    WatzFuelPage() {
        super(PageIds.page("watz_fuel"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> out = new ArrayList<>();
        for (EnumWatzType type : EnumWatzType.values()) {
            out.add(
                    new Recipe(
                            PageIds.derived(id(), PageIds.segment(type)),
                            ModItems.WATZ_PELLET.stack(type),
                            ModItems.WATZ_PELLET_DEPLETED.stack(type)));
        }
        return out;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.watz");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.WATZ);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.WATZ));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {

        page.input(48, 24).background().items(List.of(recipe.input()));
        page.output(102, 24).background().items(List.of(recipe.output()));
        page.catalyst(75, 31).item(ModBlocks.WATZ.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack input, ItemStack output) {}
}
