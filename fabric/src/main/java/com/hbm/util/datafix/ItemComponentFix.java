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

package com.hbm.util.datafix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;

abstract class ItemComponentFix extends DataFix {

    enum Traversal {
        OUTERMOST,
        NESTED_FIRST
    }

    private final Predicate<String> itemIds;
    private final String component;
    private final UnaryOperator<Dynamic<?>> fixComponent;
    private final Traversal traversal;

    ItemComponentFix(
            Schema outputSchema,
            Predicate<String> itemIds,
            String component,
            UnaryOperator<Dynamic<?>> fixComponent,
            Traversal traversal) {
        super(outputSchema, false);
        this.itemIds = itemIds;
        this.component = component;
        this.fixComponent = fixComponent;
        this.traversal = traversal;
    }

    @Override
    protected final TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(
                rule(
                        HbmDataFixers.CHUNK,
                        chunk ->
                                chunk.update("block_entities", this::walk)
                                        .update("entities", this::walk)),
                rule(HbmDataFixers.ENTITY_CHUNK, chunk -> chunk.update("Entities", this::walk)),
                rule(HbmDataFixers.PLAYER, this::walk),
                rule(HbmDataFixers.SEALED_ITEMS, contents -> contents.update("items", this::walk)));
    }

    private TypeRewriteRule rule(DSL.TypeReference type, Function<Dynamic<?>, Dynamic<?>> fix) {
        return fixTypeEverywhereTyped(
                getClass().getSimpleName() + " " + type.typeName(),
                getInputSchema().getType(type),
                typed -> typed.update(DSL.remainderFinder(), fix));
    }

    private Dynamic<?> walk(Dynamic<?> value) {
        if (value.getValue() instanceof ByteArrayTag
                || value.getValue() instanceof IntArrayTag
                || value.getValue() instanceof LongArrayTag) return value;
        var entries = value.getMapValues().result();
        if (entries.isPresent()) {
            boolean selected = value.get("id").asString().result().filter(itemIds).isPresent();
            if (selected && traversal == Traversal.OUTERMOST) return fixItem(value);
            var fixed = value;
            for (var entry : entries.get().entrySet()) {
                var child = walk(entry.getValue());
                if (child != entry.getValue())
                    fixed = fixed.set(entry.getKey().asString().getOrThrow(), child);
            }
            return selected ? fixItem(fixed) : fixed;
        }
        var elements = value.asStreamOpt().result();
        if (elements.isEmpty()) return value;
        var before = elements.get().toList();
        var after = before.stream().map(this::walk).toList();
        for (int i = 0; i < before.size(); i++) {
            if (after.get(i) != before.get(i)) return value.createList(after.stream());
        }
        return value;
    }

    private Dynamic<?> fixItem(Dynamic<?> item) {
        return item.update("components", components -> components.update(component, fixComponent));
    }
}
