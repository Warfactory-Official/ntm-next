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
import java.util.Map;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;

final class BlockStatePropertyFix extends DataFix {

    private static final String CORRODED_BARREL = "hbm:barrel_corroded";
    private static final String DIODE = "hbm:cable_diode";
    private static final Map<String, Integer> SKIN_COUNTS =
            Map.of(
                    "hbm:fire_door", 5,
                    "hbm:secure_access_door", 4,
                    "hbm:water_door", 2,
                    "hbm:qe_containment", 3,
                    "hbm:round_airlock_door", 3);

    BlockStatePropertyFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(
                rule(HbmDataFixers.CHUNK),
                rule(HbmDataFixers.ENTITY_CHUNK),
                rule(HbmDataFixers.PLAYER),
                rule(HbmDataFixers.STRUCTURE),
                rule(HbmDataFixers.SEALED_ITEMS));
    }

    private TypeRewriteRule rule(DSL.TypeReference type) {
        return fixTypeEverywhereTyped(
                "BlockStatePropertyFix " + type.typeName(),
                getInputSchema().getType(type),
                typed -> typed.update(DSL.remainderFinder(), BlockStatePropertyFix::walk));
    }

    private static Dynamic<?> walk(Dynamic<?> value) {
        if (value.getValue() instanceof ByteArrayTag
                || value.getValue() instanceof IntArrayTag
                || value.getValue() instanceof LongArrayTag) return value;
        var entries = value.getMapValues().result();
        if (entries.isPresent()) {
            if (BlockStateEncoding14.stateMap(value)) {
                String id = BlockStateEncoding14.name(value);
                return changed(id)
                        ? BlockStateEncoding14.current(
                                value, fixProperties(id, BlockStateEncoding14.properties(value)))
                        : value;
            }
            var fixed = value;
            for (var entry : entries.get().entrySet()) {
                var child = walk(entry.getValue());
                if (child != entry.getValue())
                    fixed = fixed.set(entry.getKey().asString().getOrThrow(), child);
            }
            String item = fixed.get("id").asString("");
            return changed(item)
                    ? fixed.update(
                            "components",
                            components ->
                                    components.update(
                                            "minecraft:block_state",
                                            properties -> fixProperties(item, properties)))
                    : fixed;
        }
        var elements = value.asStreamOpt().result();
        if (elements.isEmpty()) return value;
        var before = elements.get().toList();
        var after = before.stream().map(BlockStatePropertyFix::walk).toList();
        for (int i = 0; i < before.size(); i++) {
            if (after.get(i) != before.get(i)) return value.createList(after.stream());
        }
        return value;
    }

    private static boolean changed(String id) {
        return id.equals(CORRODED_BARREL) || id.equals(DIODE) || SKIN_COUNTS.containsKey(id);
    }

    private static Dynamic<?> fixProperties(String id, Dynamic<?> properties) {
        if (CORRODED_BARREL.equals(id)) {
            return properties.remove("north").remove("east").remove("south").remove("west");
        }
        if (DIODE.equals(id))
            return properties
                    .remove("north")
                    .remove("east")
                    .remove("south")
                    .remove("west")
                    .remove("up")
                    .remove("down");
        int count = SKIN_COUNTS.get(id);
        return properties.update(
                "skin",
                value -> {
                    int skin = Integer.parseInt(value.asString().getOrThrow());
                    return skin < count
                            ? value
                            : value.createString(Integer.toString(skin % count));
                });
    }
}
