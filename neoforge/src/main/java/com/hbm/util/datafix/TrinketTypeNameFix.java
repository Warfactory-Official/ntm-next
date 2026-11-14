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
import java.util.Optional;

final class TrinketTypeNameFix extends DataFix {
    private static final Map<String, String[]> NAMES =
            Map.of(
                    "hbm:bobblehead",
                            new String[] {
                                "none",
                                "strength",
                                "perception",
                                "endurance",
                                "charisma",
                                "intelligence",
                                "agility",
                                "luck",
                                "bob",
                                "frizzle",
                                "pu238",
                                "vt",
                                "doc",
                                "bluehat",
                                "pheo",
                                "adam29",
                                "uffr",
                                "vaer",
                                "nos",
                                "drillgon",
                                "cirno",
                                "microwave",
                                "peep",
                                "mellow",
                                "abel"
                            },
                    "hbm:plushie", new String[] {"none", "yomi", "numbernine", "hundun", "derg"});

    TrinketTypeNameFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "TrinketTypeNameFix",
                getInputSchema().getType(HbmDataFixers.CHUNK),
                chunk -> chunk.update(DSL.remainderFinder(), TrinketTypeNameFix::fix));
    }

    private static <T> Dynamic<T> fix(Dynamic<T> chunk) {
        return chunk.update(
                "block_entities",
                entities ->
                        entities.createList(
                                entities.asStreamOpt()
                                        .result()
                                        .orElseThrow(
                                                () ->
                                                        new IllegalStateException(
                                                                "Malformed chunk block_entities"))
                                        .map(TrinketTypeNameFix::fixBlockEntity)));
    }

    private static Dynamic<?> fixBlockEntity(Dynamic<?> tag) {
        String[] names = NAMES.get(tag.get("id").asString(""));
        if (names == null) return tag;
        Optional<Number> raw = tag.get("type").asNumber().result();
        return raw.isEmpty()
                ? tag
                : tag.set(
                        "type",
                        tag.createString(names[Math.abs(raw.get().byteValue()) % names.length]));
    }
}
