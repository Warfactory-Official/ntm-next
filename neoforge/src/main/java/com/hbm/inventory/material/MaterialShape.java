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

import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public enum MaterialShape {
    ORE(0, "c:ores", null, true),
    QUANTUM(1, null, null, false),
    NUGGET(8, "c:nuggets", null, true),
    FRAGMENT(8, "hbm:bedrock_ore_fragments", "bedrock_ore_fragment", false),
    TINY_DUST(8, "c:tiny_dusts", null, true),
    FINE_WIRE(9, "c:fine_wires", "wire_fine", false),
    BOLT(9, "c:bolts", "bolt", false),
    BILLET(48, "hbm:billets", null, true),
    INGOT(72, "c:ingots", "ingot_raw", true),
    GEM(72, "c:gems", null, true),
    CRYSTAL(72, "c:crystals", null, true),
    DUST(72, "c:dusts", null, true),
    DENSE_WIRE(72, "hbm:wires_dense", "wire_dense", false),
    PLATE(72, "c:plates", null, true),
    CAST_PLATE(216, "hbm:plates_cast", "plate_cast", true),
    WELDED_PLATE(432, "hbm:plates_welded", "plate_welded", false),
    SHELL(288, "hbm:shells", "shell", false),
    PIPE(216, "hbm:pipes", "pipe", false),
    QUART(162, null, null, false),
    BLOCK(648, "c:storage_blocks", null, true),
    LIGHT_BARREL(216, "hbm:barrels_light", "part_barrel_light", false),
    HEAVY_BARREL(432, "hbm:barrels_heavy", "part_barrel_heavy", false),
    LIGHT_RECEIVER(288, "hbm:receivers_light", "part_receiver_light", false),
    HEAVY_RECEIVER(648, "hbm:receivers_heavy", "part_receiver_heavy", false),
    MECHANISM(288, "hbm:gun_mechanisms", "part_mechanism", false),
    STOCK(288, "hbm:stocks", "part_stock", false),
    GRIP(144, "hbm:grips", "part_grip", false);

    public static final List<MaterialShape> ALL = List.of(values());

    private final int quanta;
    private final @Nullable String tagNamespace;
    private final @Nullable String plural;
    private final @Nullable String itemPrefix;
    private final boolean hazardable;

    MaterialShape(
            int quanta, @Nullable String family, @Nullable String itemPrefix, boolean hazardable) {
        this.quanta = quanta;
        int colon = family == null ? -1 : family.indexOf(':');
        this.tagNamespace = colon < 0 ? null : family.substring(0, colon);
        this.plural = colon < 0 ? null : family.substring(colon + 1);
        this.itemPrefix = itemPrefix;
        this.hazardable = hazardable;
    }

    public int quanta() {
        return quanta;
    }

    public int amount(int count) {
        return quanta * count;
    }

    public int share(int units, int products) {
        if (products <= 0)
            throw new IllegalArgumentException(this + " shared over " + products + " products");
        return quanta * units / products;
    }

    public boolean hasTags() {
        return plural != null;
    }

    public String plural() {
        if (plural == null) throw new IllegalStateException("Shape has no tag path: " + this);
        return plural;
    }

    public String tagNamespace() {
        if (tagNamespace == null) throw new IllegalStateException("Shape has no tag path: " + this);
        return tagNamespace;
    }

    public TagKey<Item> tag(String segment) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(tagNamespace(), plural + "/" + segment));
    }

    public TagKey<Item> privateTag(String segment) {
        return TagKey.create(Registries.ITEM, RegistryUtil.id(plural() + "/" + segment));
    }

    public @Nullable String itemPrefix() {
        return itemPrefix;
    }

    public String autogenItemId(String segment) {
        if (itemPrefix == null)
            throw new IllegalStateException("Shape is not autogen-eligible: " + this);
        return itemPrefix + "_" + segment;
    }

    public boolean hazardable() {
        return hazardable;
    }
}
