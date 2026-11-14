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

package com.hbm.inventory.material;

import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.registration.Registrar;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

public class MatDistribution extends SerializableRecipe
        implements RecipeType<MaterialDistributionRecipe> {

    public static final MatDistribution INSTANCE = new MatDistribution();
    private static final String REGISTRY_NAME = "material_distribution";
    private Identifier typeId;
    private Distribution distribution = Distribution.empty();

    public static Distribution live() {
        INSTANCE.ensureFilled();
        return INSTANCE.distribution;
    }

    public static int rowCount() {
        Distribution live = live();
        return live.byItem().size() + live.byOreTag().size();
    }

    @Override
    public void registerType() {
        this.typeId = Registrar.registerRecipeType(REGISTRY_NAME, this).id();
        Registrar.registerRecipeSerializer(REGISTRY_NAME, MaterialDistributionRecipe.SERIALIZER);
    }

    public RecipeType<MaterialDistributionRecipe> type() {
        return this;
    }

    @Override
    public Identifier datapackTypeId() {
        return typeId;
    }

    @Override
    public RecipeType<?> datapackType() {
        return this;
    }

    @Override
    public String toString() {
        return typeId == null ? super.toString() : typeId.toString();
    }

    @Override
    public void ensureFilled() {
        RecipeSource.Token token = RecipeSource.token();
        if (token == null) return;
        if (distribution.token().equals(token)) return;
        synchronized (this) {
            if (distribution.token().equals(token)) return;
            fill(RecipeSource.recipes(), token);
        }
    }

    private void fill(Collection<RecipeHolder<?>> loaded, RecipeSource.Token token) {
        Map<Item, List<MaterialStack>> byItem = new HashMap<>();
        Map<TagKey<Item>, List<MaterialStack>> byOreTag = new HashMap<>();

        for (RecipeHolder<?> holder : loaded) {
            if (holder.value().getType() != this) continue;
            MaterialDistributionRecipe row = (MaterialDistributionRecipe) holder.value();
            row.targets()
                    .unwrap()
                    .ifLeft(tag -> byOreTag.put(tag, row.materials()))
                    .ifRight(
                            items ->
                                    items.forEach(
                                            item -> byItem.put(item.value(), row.materials())));
        }

        this.distribution = new Distribution(token, Map.copyOf(byItem), Map.copyOf(byOreTag));
    }

    public record Distribution(
            RecipeSource.Token token,
            Map<Item, List<MaterialStack>> byItem,
            Map<TagKey<Item>, List<MaterialStack>> byOreTag) {
        static Distribution empty() {
            return new Distribution(RecipeSource.NEVER_FILLED, Map.of(), Map.of());
        }
    }
}
