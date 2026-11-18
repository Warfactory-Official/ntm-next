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
import java.util.function.UnaryOperator;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;

abstract class EntityTagFix extends DataFix {

    EntityTagFix(Schema schema) {
        super(schema, false);
    }

    protected abstract Dynamic<?> fixEntity(Dynamic<?> tag);

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
                rule(HbmDataFixers.STRUCTURE, this::walk),
                rule(HbmDataFixers.SEALED_ITEMS, contents -> contents.update("items", this::walk)));
    }

    private TypeRewriteRule rule(DSL.TypeReference type, UnaryOperator<Dynamic<?>> fix) {
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
            var fixed = value;
            for (var entry : entries.get().entrySet()) {
                var child = walk(entry.getValue());
                if (child != entry.getValue())
                    fixed = fixed.set(entry.getKey().asString().getOrThrow(), child);
            }
            return fixEntity(fixed);
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

    protected static Dynamic<?> rename(Dynamic<?> tag, String oldKey, String newKey) {
        var old = tag.get(oldKey).result();
        if (old.isEmpty()) return tag;
        var fixed = tag.get(newKey).result().isPresent() ? tag : tag.set(newKey, old.get());
        return fixed.remove(oldKey);
    }
}
