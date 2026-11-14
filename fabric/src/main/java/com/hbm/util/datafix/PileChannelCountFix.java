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

final class PileChannelCountFix extends DataFix {
    PileChannelCountFix(Schema schema) {
        super(schema, false);
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
                                        .map(PileChannelCountFix::pile)));
    }

    private static Dynamic<?> pile(Dynamic<?> tag) {
        if (!tag.get("id").asString("").equals("hbm:pile_core")) return tag;
        for (String key : new String[] {"fc", "vc", "cc"})
            tag = tag.update(key, count -> count.createInt(count.asInt(0) & 255));
        return tag;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "PileChannelCountFix",
                getInputSchema().getType(HbmDataFixers.CHUNK),
                chunk -> chunk.update(DSL.remainderFinder(), PileChannelCountFix::fix));
    }
}
