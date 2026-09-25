// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RBMKDisassemblyPage extends RecipePage<RBMKDisassemblyPage.Recipe> {

    RBMKDisassemblyPage() {
        super(PageIds.page("rbmk_rod_disassembly"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (RegistryHandle<ItemRBMKRod> handle : ModItems.CRAFTABLE_RODS) {
            ItemRBMKRod rod = handle.get();
            ItemRBMKPellet pellet = rod.pellet;
            for (int enrichment = 0; enrichment <= 4; enrichment++) {
                rows.add(row(rod, pellet, enrichment, false));
                if (pellet.isXenonEnabled()) rows.add(row(rod, pellet, enrichment, true));
            }
        }
        return rows;
    }

    private Recipe row(ItemRBMKRod rod, ItemRBMKPellet pellet, int enrichment, boolean poison) {
        String key = PageIds.segment(BuiltInRegistries.ITEM.getKey(rod));
        Identifier id =
                poison
                        ? PageIds.derived(id(), key, String.valueOf(enrichment), "xenon")
                        : PageIds.derived(id(), key, String.valueOf(enrichment));
        ItemStack pellets = pellet.of(new ItemRBMKPellet.Stage(enrichment, poison));
        pellets.setCount(8);
        ItemStack stack = new ItemStack(rod);
        ItemRBMKRod.setYield(stack, Math.min(1 - enrichment / 5D, 0.99) * rod.yield);
        if (poison) ItemRBMKRod.setPoison(stack, 50);
        return new Recipe(id, stack, pellets);
    }

    @Override
    public LookupFilter lookupFilter() {
        return (looked, lookup) ->
                lookup != Lookup.USES
                        || !(looked.getItem() instanceof ItemRBMKRod)
                        || ItemRBMKRod.getCoreHeat(looked) <= 50
                                && ItemRBMKRod.getHullHeat(looked) <= 50;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.rbmk_rod_disassembly");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.CRAFTING_TABLE);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(Items.CRAFTING_TABLE));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().item(recipe.rod());
        page.output(102, 24).background().item(recipe.pellets());
        page.catalyst(75, 31).item(Items.CRAFTING_TABLE);
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack rod, ItemStack pellets) {}
}
