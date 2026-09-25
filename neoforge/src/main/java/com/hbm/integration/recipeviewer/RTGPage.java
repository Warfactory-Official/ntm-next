// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.machine.ItemRTGPellet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class RTGPage extends RecipePage<RTGPage.Recipe> {

    RTGPage() {
        super(PageIds.page("rtg"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> out = new ArrayList<>();
        for (ItemRTGPellet pellet : ItemRTGPellet.pelletList) {
            ItemStack decayed = pellet.getDecayItem();
            if (decayed == null) continue;
            out.add(
                    new Recipe(
                            PageIds.derived(
                                    id(), PageIds.segment(BuiltInRegistries.ITEM.getKey(pellet))),
                            new ItemStack(pellet),
                            decayed));
        }
        return out;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_rtg_grey");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_RTG);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_RTG));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().item(recipe.pellet());
        page.output(102, 24).background().item(recipe.depleted());

        page.catalyst(75, 31).item(ModBlocks.MACHINE_RTG.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack pellet, ItemStack depleted) {}
}
