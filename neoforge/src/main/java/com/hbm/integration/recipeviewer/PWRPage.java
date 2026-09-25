// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class PWRPage extends RecipePage<PWRPage.Recipe> {

    PWRPage() {
        super(PageIds.page("pwr"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> out = new ArrayList<>();
        for (EnumPWRFuel fuel : EnumPWRFuel.VALUES) {
            out.add(
                    new Recipe(
                            PageIds.derived(id(), PageIds.segment(fuel)),
                            new ItemStack(ModItems.cold(fuel)),
                            ModItems.PWR_FUEL_HOT.stack(fuel)));
        }
        return out;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.pwr_controller");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.PWR_CONTROLLER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.PWR_CONTROLLER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().item(recipe.fuel());
        page.output(102, 24).background().item(recipe.hot());
        page.catalyst(75, 31).item(ModBlocks.PWR_CONTROLLER.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack fuel, ItemStack hot) {}
}
