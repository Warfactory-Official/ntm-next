// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.CentrifugeRecipe;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;

public final class CentrifugePage extends TablePage<CentrifugeRecipe> {

    CentrifugePage() {
        super(PageIds.page("centrifuge"), CentrifugeRecipe.class);
    }

    private static int[][] outputPositions(int count) {
        return switch (count) {
            case 1 -> new int[][] {{102, 24}};
            case 2 -> new int[][] {{102, 24}, {120, 24}};
            case 3 -> new int[][] {{102, 24}, {120, 24}, {138, 24}};
            case 4 -> new int[][] {{102, 15}, {120, 15}, {102, 33}, {120, 33}};
            default ->
                    throw new IllegalArgumentException(
                            "unsupported centrifuge output count: " + count);
        };
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_centrifuge");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_CENTRIFUGE);
    }

    @Override
    public List<CentrifugeRecipe> rows() {
        return CentrifugeRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_CENTRIFUGE));
    }

    @Override
    public void layout(CentrifugeRecipe recipe, PageLayout page) {
        page.input(48, 24).background().items(recipe.inputItem[0].displayStacks());

        WeightedList<ItemStack>[] outputs = recipe.outputItems();
        int[][] positions = outputPositions(outputs.length);
        for (int i = 0; i < outputs.length; i++) {
            List<ItemStack> stacks = new ArrayList<>();
            for (Weighted<ItemStack> entry : outputs[i].unwrap()) stacks.add(entry.value().copy());
            page.output(positions[i][0], positions[i][1]).background().items(stacks);
        }

        page.catalyst(75, 31).item(ModBlocks.MACHINE_CENTRIFUGE.get());
    }

    @Override
    public void draw(CentrifugeRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}
