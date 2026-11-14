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

import com.hbm.handler.ConstructionCatalog;
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ConstructionPage extends RecipePage<ConstructionCatalog.Entry> {
    private static final int LABEL_ABOVE = 64;

    ConstructionPage() {
        super(PageIds.page("construction"), ConstructionCatalog.Entry.class);
    }

    private static void stackSizeLabel(@Nullable ItemStack shown, Consumer<Component> lines) {
        if (shown == null || shown.getCount() <= LABEL_ABOVE) return;
        int stacks = shown.getCount() / LABEL_ABOVE;
        int items = shown.getCount() % LABEL_ABOVE;
        Component label =
                items > 0
                        ? Component.translatable("jei.hbm.stacks_remainder", stacks, items)
                        : Component.translatable("jei.hbm.stacks", stacks);
        lines.accept(label.copy().withStyle(ChatFormatting.RED));
    }

    @Override
    public List<ConstructionCatalog.Entry> rows() {
        return ConstructionCatalog.entries();
    }

    @Override
    public Identifier rowId(ConstructionCatalog.Entry entry) {
        return PageIds.derived(
                id(), PageIds.segment(BuiltInRegistries.ITEM.getKey(entry.assembled().getItem())));
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.construction");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModItems.ACETYLENE_TORCH);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModItems.ACETYLENE_TORCH),
                new ItemStack(ModItems.BLOWTORCH),
                new ItemStack(ModItems.BOLTGUN));
    }

    @Override
    public void layout(ConstructionCatalog.Entry entry, PageLayout page) {
        List<List<ItemStack>> pile = entry.pile();
        int[][] inputs = GenericMachinePage.universalInputPositions(pile.size());
        for (int i = 0; i < pile.size(); i++) {
            page.input(inputs[i][0], inputs[i][1])
                    .background()
                    .items(pile.get(i))
                    .tooltip(ConstructionPage::stackSizeLabel);
        }

        int[][] outputs = GenericMachinePage.outputPositions(1);
        page.output(outputs[0][0], outputs[0][1]).background().item(entry.assembled());
        page.catalyst(75, 31).item(entry.seed());
    }

    @Override
    public void draw(ConstructionCatalog.Entry entry, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}
