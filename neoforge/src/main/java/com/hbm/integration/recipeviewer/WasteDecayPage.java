/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.integration.recipeviewer;

import com.hbm.blockentity.machine.StorageDrumBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.special.WasteLongItem;
import com.hbm.items.special.WasteShortItem;
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
        for (WasteShortItem.WasteClass waste : WasteShortItem.WasteClass.VALUES) {
            rows.add(
                    row(
                            ModItems.NUCLEAR_WASTE_SHORT.stack(waste),
                            ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.stack(waste)));
            rows.add(
                    row(
                            ModItems.NUCLEAR_WASTE_SHORT_TINY.stack(waste),
                            ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.stack(waste)));
        }
        for (StorageDrumBlockEntity.DecayExtension decay : StorageDrumBlockEntity.extensions()) {
            rows.add(row(new ItemStack(decay.input().get()), decay.product().get()));
        }
        for (WasteLongItem.WasteClass waste : WasteLongItem.WasteClass.VALUES) {
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
