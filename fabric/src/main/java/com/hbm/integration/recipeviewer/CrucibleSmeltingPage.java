// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public final class CrucibleSmeltingPage extends RecipePage<CrucibleSmeltingPage.Recipe> {

    CrucibleSmeltingPage() {
        super(PageIds.page("crucible_smelting"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> out = new ArrayList<>();

        for (Map.Entry<Ingredient, List<MaterialStack>> entry : Mats.smeltingCatalog().entrySet()) {
            List<ItemStack> molten = new ArrayList<>(entry.getValue().size());
            for (MaterialStack material : entry.getValue())
                molten.add(ItemScraps.create(material, true));
            if (molten.isEmpty()) continue;

            List<ItemStack> input = entry.getKey().items().map(ItemStack::new).toList();

            if (input.isEmpty()) continue;
            out.add(new Recipe(rowId(entry.getKey()), input, molten));
        }

        return out;
    }

    private Identifier rowId(Ingredient input) {
        if (input.display() instanceof SlotDisplay.TagSlotDisplay(TagKey<Item> tag)) {
            return PageIds.derived(id(), "tag", PageIds.segment(tag.location()));
        }
        String items =
                input.items()
                        .map(item -> PageIds.segment(BuiltInRegistries.ITEM.getKey(item.value())))
                        .collect(Collectors.joining("/"));
        return PageIds.derived(id(), "item", items);
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.crucible_smelting");
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
        page.input(48, 24).items(recipe.input());
        for (int i = 0; i < recipe.output().size(); i++) {
            page.output(102 + (i % 3) * 18, 6 + (i / 3) * 18).item(recipe.output().get(i));
        }
        page.catalyst(75, 42).item(ModBlocks.MACHINE_CRUCIBLE.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.CRUCIBLE_SMELTING);
    }

    public record Recipe(Identifier id, List<ItemStack> input, List<ItemStack> output) {}
}
