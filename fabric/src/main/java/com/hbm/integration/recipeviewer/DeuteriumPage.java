// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.machine.BlockEntityDeuteriumExtractor;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class DeuteriumPage extends RecipePage<DeuteriumPage.Recipe> {

    private static final int WATER_PER_ROW = 1_000;

    DeuteriumPage() {
        super(PageIds.page("deuterium"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        return List.of(
                new Recipe(
                        PageIds.derived(
                                id(),
                                PageIds.segment(BuiltInRegistries.FLUID.getKey(NTMFluids.WATER))),
                        NTMFluids.WATER,
                        WATER_PER_ROW,
                        NTMFluids.HEAVYWATER,
                        WATER_PER_ROW / BlockEntityDeuteriumExtractor.RATIO));
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_deuterium_extractor");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR),
                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_TOWER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().fluid(recipe.input(), recipe.inputAmount());
        page.output(102, 24).background().fluid(recipe.output(), recipe.outputAmount());
        page.catalyst(75, 31)
                .items(
                        List.of(
                                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR),
                                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_TOWER)));
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id, Fluid input, int inputAmount, Fluid output, int outputAmount) {}
}
