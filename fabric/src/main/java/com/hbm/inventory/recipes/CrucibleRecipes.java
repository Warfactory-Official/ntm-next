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

package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.SmeltingBehavior;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.MoldItem;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CrucibleRecipes extends SimpleGenericRecipes<CrucibleRecipe> {

    public static final CrucibleRecipes INSTANCE = new CrucibleRecipes();

    @Override
    protected String registryName() {
        return "crucible";
    }

    @Override
    protected RecipeSerializer<CrucibleRecipe> serializer() {
        return CrucibleRecipe.SERIALIZER;
    }

    public static List<Cast> moldRecipes() {
        List<Cast> out = new ArrayList<>();

        for (Material material : MaterialRegistry.ordered()) {
            if (material.behaviour() != SmeltingBehavior.SMELTABLE) continue;

            for (RegistryHandle<MoldItem> handle : ModItems.MOLDS) {
                MoldItem item = handle.get();
                ItemStack result = item.mold.getOutput(material);
                if (result == null) continue;

                out.add(
                        new Cast(
                                ScrapsItem.create(
                                        new MaterialStack(material, item.mold.getCost()), true),
                                new ItemStack(item),
                                new ItemStack(
                                        item.mold.size == MoldItem.SIZE_SMALL
                                                ? ModBlocks.FOUNDRY_MOLD.get()
                                                : ModBlocks.FOUNDRY_BASIN.get()),
                                result));
            }
        }

        return out;
    }

    public record Cast(ItemStack metal, ItemStack mold, ItemStack block, ItemStack result) {}
}
