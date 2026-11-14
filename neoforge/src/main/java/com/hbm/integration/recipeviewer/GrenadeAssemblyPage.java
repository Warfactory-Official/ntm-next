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

import com.hbm.NuclearTech;
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.inventory.recipes.crafting.GrenadeCraftingRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.grenade.GrenadeExtraItem.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.GrenadeFillingDefinition;
import com.hbm.items.weapon.grenade.GrenadeFillings;
import com.hbm.items.weapon.grenade.GrenadeFuzeItem.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.GrenadeShellItem.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.GrenadeUniversalItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class GrenadeAssemblyPage extends RecipePage<GrenadeAssemblyPage.Recipe> {

    GrenadeAssemblyPage() {
        super(PageIds.page("grenade_assembly"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> recipes = new ArrayList<>();
        for (EnumGrenadeShell shell : EnumGrenadeShell.values()) {
            for (var filling : GrenadeFillings.values()) {
                if (!GrenadeCraftingRecipe.compatible(shell, filling)) continue;
                for (EnumGrenadeFuze fuze : EnumGrenadeFuze.values()) {
                    recipes.add(make(shell, filling, fuze, null));
                    for (EnumGrenadeExtra extra : EnumGrenadeExtra.values())
                        recipes.add(make(shell, filling, fuze, extra));
                }
            }
        }
        return recipes;
    }

    private Recipe make(
            EnumGrenadeShell shell,
            GrenadeFillingDefinition filling,
            EnumGrenadeFuze fuze,
            @Nullable EnumGrenadeExtra extra) {
        List<String> segments = new ArrayList<>();
        segments.add(PageIds.segment(shell));
        segments.add(
                filling.id().getNamespace().equals(NuclearTech.MOD_ID)
                        ? filling.id().getPath()
                        : PageIds.segment(filling.id()));
        segments.add(PageIds.segment(fuze));
        if (extra != null) segments.add(PageIds.segment(extra));
        return new Recipe(
                PageIds.derived(id(), segments.toArray(String[]::new)),
                ModItems.GRENADE_SHELL.stack(shell),
                new ItemStack(filling.item()),
                ModItems.GRENADE_FUZE.stack(fuze),
                extra == null ? null : ModItems.GRENADE_EXTRA.stack(extra),
                GrenadeUniversalItem.make(shell, filling, fuze, extra));
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("item.hbm.grenade_universal");
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
        int[][] positions =
                GenericMachinePage.universalInputPositions(recipe.extra() == null ? 3 : 4);
        page.input(positions[0][0], positions[0][1]).background().item(recipe.shell());
        page.input(positions[1][0], positions[1][1]).background().item(recipe.filling());
        page.input(positions[2][0], positions[2][1]).background().item(recipe.fuze());
        if (recipe.extra() != null)
            page.input(positions[3][0], positions[3][1]).background().item(recipe.extra());
        page.output(102, 24).background().item(recipe.output());
        page.catalyst(75, 31).item(Items.CRAFTING_TABLE);
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id,
            ItemStack shell,
            ItemStack filling,
            ItemStack fuze,
            @Nullable ItemStack extra,
            ItemStack output) {}
}
