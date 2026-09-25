// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemWasteLong;
import com.hbm.items.special.ItemWasteShort;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class WasteDecayPage extends RecipePage<WasteDecayPage.Recipe> {

    WasteDecayPage() {
        super(PageIds.page("waste_decay"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (ItemWasteShort.WasteClass waste : ItemWasteShort.WasteClass.VALUES) {
            rows.add(
                    row(
                            ModItems.NUCLEAR_WASTE_SHORT.stack(waste),
                            ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.stack(waste)));
            rows.add(
                    row(
                            ModItems.NUCLEAR_WASTE_SHORT_TINY.stack(waste),
                            ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.stack(waste)));
        }
        for (ItemWasteLong.WasteClass waste : ItemWasteLong.WasteClass.VALUES) {
            rows.add(
                    row(
                            ModItems.NUCLEAR_WASTE_LONG.stack(waste),
                            ModItems.NUCLEAR_WASTE_LONG_DEPLETED.stack(waste)));
            rows.add(
                    row(
                            ModItems.NUCLEAR_WASTE_LONG_TINY.stack(waste),
                            ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.stack(waste)));
        }
        return rows;
    }

    private Recipe row(ItemStack waste, ItemStack decayed) {
        return new Recipe(
                PageIds.derived(
                        id(), PageIds.segment(BuiltInRegistries.ITEM.getKey(waste.getItem()))),
                waste,
                decayed);
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_storage_drum");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.STORAGE_DRUM);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.STORAGE_DRUM));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().item(recipe.waste());
        page.output(102, 24).background().item(recipe.decayed());
        page.catalyst(75, 31).item(ModBlocks.STORAGE_DRUM.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack waste, ItemStack decayed) {}
}
