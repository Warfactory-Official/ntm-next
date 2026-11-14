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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class MaterialTags {

    private final String segment;
    private final @Nullable String alias;
    private final List<MaterialTags> members;
    private final TagKey<Item>[] tags;
    private final TagKey<Item> any;

    @SuppressWarnings("unchecked")
    private MaterialTags(
            String segment, @Nullable String alias, boolean internal, List<MaterialTags> members) {
        this.segment = segment;
        this.alias = alias;
        this.members = members;
        this.tags = new TagKey[MaterialShape.ALL.size()];
        for (var shape : MaterialShape.ALL) {
            if (shape.hasTags())
                tags[shape.ordinal()] = internal ? shape.privateTag(segment) : shape.tag(segment);
        }
        this.any = TagKey.create(Registries.ITEM, RegistryUtil.id("any_" + segment));
    }

    public static MaterialTags of(String segment) {
        return new MaterialTags(segment, null, false, List.of());
    }

    static MaterialTags aliased(String segment, String alias) {
        return new MaterialTags(segment, alias, false, List.of());
    }

    static MaterialTags internal(String segment) {
        return new MaterialTags(segment, null, true, List.of());
    }

    public static MaterialTags anyOf(String segment, MaterialTags... members) {
        return new MaterialTags(segment, null, true, List.of(members));
    }

    public String segment() {
        return segment;
    }

    public @Nullable String alias() {
        return alias;
    }

    public List<MaterialTags> members() {
        return members;
    }

    public TagKey<Item> tag(MaterialShape shape) {
        return tags[shape.ordinal()];
    }

    public TagKey<Item> any() {
        return any;
    }

    public TagKey<Item> ore() {
        return tags[MaterialShape.ORE.ordinal()];
    }

    public TagKey<Item> nugget() {
        return tags[MaterialShape.NUGGET.ordinal()];
    }

    public TagKey<Item> fragment() {
        return tags[MaterialShape.FRAGMENT.ordinal()];
    }

    public TagKey<Item> dustTiny() {
        return tags[MaterialShape.TINY_DUST.ordinal()];
    }

    public TagKey<Item> wireFine() {
        return tags[MaterialShape.FINE_WIRE.ordinal()];
    }

    public TagKey<Item> bolt() {
        return tags[MaterialShape.BOLT.ordinal()];
    }

    public TagKey<Item> billet() {
        return tags[MaterialShape.BILLET.ordinal()];
    }

    public TagKey<Item> ingot() {
        return tags[MaterialShape.INGOT.ordinal()];
    }

    public TagKey<Item> gem() {
        return tags[MaterialShape.GEM.ordinal()];
    }

    public TagKey<Item> crystal() {
        return tags[MaterialShape.CRYSTAL.ordinal()];
    }

    public TagKey<Item> dust() {
        return tags[MaterialShape.DUST.ordinal()];
    }

    public TagKey<Item> wireDense() {
        return tags[MaterialShape.DENSE_WIRE.ordinal()];
    }

    public TagKey<Item> plate() {
        return tags[MaterialShape.PLATE.ordinal()];
    }

    public TagKey<Item> plateCast() {
        return tags[MaterialShape.CAST_PLATE.ordinal()];
    }

    public TagKey<Item> plateWelded() {
        return tags[MaterialShape.WELDED_PLATE.ordinal()];
    }

    public TagKey<Item> shell() {
        return tags[MaterialShape.SHELL.ordinal()];
    }

    public TagKey<Item> pipe() {
        return tags[MaterialShape.PIPE.ordinal()];
    }

    public TagKey<Item> block() {
        return tags[MaterialShape.BLOCK.ordinal()];
    }

    public TagKey<Item> barrelLight() {
        return tags[MaterialShape.LIGHT_BARREL.ordinal()];
    }

    public TagKey<Item> barrelHeavy() {
        return tags[MaterialShape.HEAVY_BARREL.ordinal()];
    }

    public TagKey<Item> receiverLight() {
        return tags[MaterialShape.LIGHT_RECEIVER.ordinal()];
    }

    public TagKey<Item> receiverHeavy() {
        return tags[MaterialShape.HEAVY_RECEIVER.ordinal()];
    }

    public TagKey<Item> mechanism() {
        return tags[MaterialShape.MECHANISM.ordinal()];
    }

    public TagKey<Item> stock() {
        return tags[MaterialShape.STOCK.ordinal()];
    }

    public TagKey<Item> grip() {
        return tags[MaterialShape.GRIP.ordinal()];
    }

    @Override
    public String toString() {
        return segment;
    }
}
