// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.machine.ItemWatzPellet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class WatzFuelPage extends RecipePage<WatzFuelPage.Recipe> {

    WatzFuelPage() {
        super(PageIds.page("watz_fuel"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ItemWatzPellet pellet) || pellet.depleted) continue;
            out.add(
                    new Recipe(
                            PageIds.derived(id(), pellet.type.name().toLowerCase(Locale.ROOT)),
                            new ItemStack(pellet),
                            pellet.type.depletedStack()));
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
