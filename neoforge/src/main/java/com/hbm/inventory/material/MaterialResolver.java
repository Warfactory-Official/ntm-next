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

import com.hbm.inventory.material.MatDistribution.Distribution;
import com.hbm.inventory.material.MaterialRegistry.ShapeBinding;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ScrapsItem.ScrapData;
import com.hbm.items.machine.ScrapsItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

public final class MaterialResolver {
    private static final Map<Item, Resolution> MEMO = new ConcurrentHashMap<>();

    private MaterialResolver() {}

    public static final class CachedContents {
        private ItemStack previous = ItemStack.EMPTY;
        private Set<TagKey<Item>> tags;
        private Distribution distribution;
        private List<MaterialStack> answer = List.of();

        public List<MaterialStack> get(ItemStack stack) {
            if (stack.isEmpty()) return List.of();
            var currentTags = stack.getItem().builtInRegistryHolder().tags;
            var currentDistribution = MatDistribution.live();
            if (tags != currentTags
                    || distribution != currentDistribution
                    || !ItemStack.isSameItemSameComponents(previous, stack)) {
                previous = stack.copyWithCount(1);
                tags = currentTags;
                distribution = currentDistribution;
                answer = contents(stack);
            }
            return answer;
        }
    }

    public static List<MaterialStack> contents(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        Resolution resolution = resolve(stack.getItem());
        ScrapData scrap = scrap(stack);
        Material scrapMaterial = scrap == null ? null : MaterialRegistry.byId(scrap.material());
        return answer(
                resolution.materials,
                resolution.amounts,
                scrapMaterial,
                scrapMaterial == null ? 0 : scrap.amount());
    }

    public static List<MaterialStack> smelting(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        ScrapData scrap = scrap(stack);
        DataComponentPatch patch = stack.getComponentsPatch();
        if (!patch.isEmpty() && (scrap == null || patch.size() != 1)) return List.of();
        Resolution resolution = resolve(stack.getItem());
        Material scrapMaterial = scrap == null ? null : MaterialRegistry.byId(scrap.material());
        return answer(
                resolution.smelted,
                resolution.smeltedAmounts,
                scrapMaterial == null ? null : scrapMaterial.smeltsInto(),
                scrapMaterial == null ? 0 : scrapMaterial.convert(scrap.amount()));
    }

    public static Map<Ingredient, List<MaterialStack>> smeltingCatalog() {
        Map<Ingredient, List<MaterialStack>> catalog = new LinkedHashMap<>();
        for (ShapeBinding binding : MaterialRegistry.bindings()) {
            Material material = binding.material();
            if (!material.smeltsInto().behaviour().melts()) continue;
            Optional<HolderSet.Named<Item>> members =
                    BuiltInRegistries.ITEM.get(material.tag(binding.shape()));
            if (members.isEmpty()) continue;
            catalog.put(
                    Ingredient.of(members.get()),
                    List.of(
                            new MaterialStack(
                                    material.smeltsInto(), material.convert(binding.quanta()))));
        }
        Distribution distribution = MatDistribution.live();
        distribution
                .byOreTag()
                .forEach(
                        (tag, materials) -> {
                            Optional<HolderSet.Named<Item>> members =
                                    BuiltInRegistries.ITEM.get(tag);
                            if (members.isPresent() && !materials.isEmpty()) {
                                catalog.put(Ingredient.of(members.get()), copies(materials));
                            }
                        });
        distribution
                .byItem()
                .forEach(
                        (item, materials) -> {
                            if (!materials.isEmpty())
                                catalog.put(Ingredient.of(item), copies(materials));
                        });
        return catalog;
    }

    private static @Nullable ScrapData scrap(ItemStack stack) {
        return stack.getItem() instanceof ScrapsItem
                ? stack.get(ModDataComponents.SCRAP.get())
                : null;
    }

    private static List<MaterialStack> copies(List<MaterialStack> materials) {
        List<MaterialStack> copies = new ArrayList<>(materials.size());
        for (MaterialStack material : materials) copies.add(material.copy());
        return copies;
    }

    private static List<MaterialStack> answer(
            Material[] materials, int[] amounts, @Nullable Material extra, int extraAmount) {
        int count = materials.length + (extra == null ? 0 : 1);
        if (count == 0) return List.of();
        MaterialStack[] answer = new MaterialStack[count];
        for (int i = 0; i < materials.length; i++)
            answer[i] = new MaterialStack(materials[i], amounts[i]);
        if (extra != null) answer[materials.length] = new MaterialStack(extra, extraAmount);
        return Arrays.asList(answer);
    }

    private static Resolution resolve(Item item) {
        Set<TagKey<Item>> tags = item.builtInRegistryHolder().tags;
        Distribution distribution = MatDistribution.live();
        Resolution held = MEMO.get(item);
        if (held != null && held.tags == tags && held.distribution == distribution) return held;
        Resolution resolution = Resolution.of(item, tags, distribution);
        MEMO.put(item, resolution);
        return resolution;
    }

    private static final class Resolution {
        private static final Material[] NONE = {};
        private static final int[] ZERO = {};

        final Set<TagKey<Item>> tags;
        final Distribution distribution;
        final Material[] materials;
        final int[] amounts;
        final Material[] smelted;
        final int[] smeltedAmounts;

        private Resolution(
                Set<TagKey<Item>> tags, Distribution distribution, List<MaterialStack> found) {
            this.tags = tags;
            this.distribution = distribution;
            int count = found.size();
            materials = count == 0 ? NONE : new Material[count];
            amounts = count == 0 ? ZERO : new int[count];
            smelted = count == 0 ? NONE : new Material[count];
            smeltedAmounts = count == 0 ? ZERO : new int[count];
            for (int i = 0; i < count; i++) {
                Material material = found.get(i).material;
                int amount = found.get(i).amount;
                materials[i] = material;
                amounts[i] = amount;
                smelted[i] = material.smeltsInto();
                smeltedAmounts[i] = material.convert(amount);
            }
        }

        static Resolution of(Item item, Set<TagKey<Item>> tags, Distribution distribution) {
            if (tags == null) throw new IllegalStateException("Tags not bound: " + item);
            List<MaterialStack> found = new ArrayList<>(2);
            TagKey<Item> rowTag = null;
            for (TagKey<Item> tag : tags) {
                List<MaterialStack> row = distribution.byOreTag().get(tag);
                if (row != null && !row.isEmpty() && precedes(tag, rowTag)) rowTag = tag;
            }
            if (rowTag != null) {
                found.addAll(distribution.byOreTag().get(rowTag));
            } else {
                TagKey<Item> shapeTag = null;
                for (TagKey<Item> tag : tags) {
                    ShapeBinding binding = MaterialRegistry.binding(tag);
                    if (binding != null
                            && binding.material().smeltsInto().behaviour().melts()
                            && precedes(tag, shapeTag)) {
                        shapeTag = tag;
                    }
                }
                if (shapeTag != null) {
                    ShapeBinding binding = MaterialRegistry.binding(shapeTag);
                    found.add(new MaterialStack(binding.material(), binding.quanta()));
                }
            }
            List<MaterialStack> itemRow = distribution.byItem().get(item);
            if (itemRow != null) found.addAll(itemRow);
            return new Resolution(tags, distribution, found);
        }

        private static boolean precedes(TagKey<Item> tag, @Nullable TagKey<Item> best) {
            return best == null || tag.location().compareTo(best.location()) < 0;
        }
    }
}
