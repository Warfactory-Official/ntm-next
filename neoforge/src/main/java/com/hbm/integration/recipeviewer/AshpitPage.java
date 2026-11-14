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

import com.hbm.blocks.ModBlocks;
import com.hbm.data.MachineData;
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.AshType;
import com.hbm.items.variant.CokeType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class AshpitPage extends RecipePage<AshpitPage.Recipe> {

    AshpitPage() {
        super(PageIds.page("ashpit"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<ItemStack> ovens =
                List.of(
                        new ItemStack(ModBlocks.HEATER_FIREBOX),
                        new ItemStack(ModBlocks.HEATER_OVEN));
        List<ItemStack> chimneys =
                List.of(
                        new ItemStack(ModBlocks.CHIMNEY_BRICK),
                        new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL));
        List<ItemStack> industrial = List.of(new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL));
        List<Recipe> rows = new ArrayList<>();
        rows.add(
                fuel(
                        ovens,
                        List.of(
                                new ItemStack(Items.COAL),
                                new ItemStack(ModItems.LIGNITE),
                                ModItems.COKE.stack(CokeType.COAL)),
                        AshType.COAL));
        rows.add(
                fuel(
                        ovens,
                        List.of(
                                new ItemStack(Items.OAK_LOG),
                                new ItemStack(Items.ACACIA_LOG),
                                new ItemStack(Items.OAK_PLANKS),
                                new ItemStack(Items.OAK_SAPLING)),
                        AshType.WOOD));
        rows.add(
                fuel(
                        ovens,
                        List.of(
                                new ItemStack(ModItems.SOLID_FUEL),
                                new ItemStack(ModItems.SCRAP),
                                new ItemStack(ModItems.DUST),
                                new ItemStack(ModItems.ROCKET_FUEL)),
                        AshType.MISC));
        for (Fluid smoke :
                List.of(NTMFluids.SMOKE, NTMFluids.SMOKE_LEADED, NTMFluids.SMOKE_POISON)) {
            String key = PageIds.segment(BuiltInRegistries.FLUID.getKey(smoke));
            rows.add(
                    new Recipe(
                            PageIds.derived(id(), PageIds.segment(AshType.FLY), key),
                            chimneys,
                            List.of(),
                            new FluidStackNTM(smoke, MachineData.ASHPIT_THRESHOLD_FLY.get()),
                            AshType.FLY));
            rows.add(
                    new Recipe(
                            PageIds.derived(id(), PageIds.segment(AshType.SOOT), key),
                            industrial,
                            List.of(),
                            new FluidStackNTM(smoke, MachineData.ASHPIT_THRESHOLD_SOOT.get()),
                            AshType.SOOT));
        }
        return rows;
    }

    private Recipe fuel(List<ItemStack> ovens, List<ItemStack> fuels, AshType ash) {
        return new Recipe(PageIds.derived(id(), PageIds.segment(ash)), ovens, fuels, null, ash);
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_ashpit");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ASHPIT);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ASHPIT));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        int[][] inputs = GenericMachinePage.universalInputPositions(2);
        page.input(inputs[0][0], inputs[0][1]).background().items(recipe.sources());
        PageSlot second = page.input(inputs[1][0], inputs[1][1]).background();
        if (recipe.smoke() != null) second.fluid(recipe.smoke());
        else second.items(recipe.fuels());
        page.output(102, 24).background().item(ModItems.POWDER_ASH.stack(recipe.ash()));
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ASHPIT.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id,
            List<ItemStack> sources,
            List<ItemStack> fuels,
            @Nullable FluidStackNTM smoke,
            AshType ash) {}
}
