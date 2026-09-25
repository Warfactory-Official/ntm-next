// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.material;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.registration.IRegistrar;
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

    static Distribution live() {
        INSTANCE.ensureFilled();
        return INSTANCE.distribution;
    }

    public static int rowCount() {
        Distribution live = live();
        return live.byItem().size() + live.byOreTag().size();
    }

    @Override
    public void registerType(IRegistrar registrar) {
        this.typeId = registrar.registerRecipeType(REGISTRY_NAME, this).id();
        registrar.registerRecipeSerializer(REGISTRY_NAME, MaterialDistributionRecipe.SERIALIZER);
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

    record Distribution(
            RecipeSource.Token token,
            Map<Item, List<MaterialStack>> byItem,
            Map<TagKey<Item>, List<MaterialStack>> byOreTag) {

        static Distribution empty() {
            return new Distribution(RecipeSource.NEVER_FILLED, Map.of(), Map.of());
        }
    }
}
