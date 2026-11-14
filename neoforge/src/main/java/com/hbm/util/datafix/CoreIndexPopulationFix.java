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

import com.hbm.interfaces.injected.ChunkExtension;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.LongStream;

final class CoreIndexPopulationFix extends DataFix {

    CoreIndexPopulationFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "CoreIndexPopulationFix",
                getInputSchema().getType(HbmDataFixers.CHUNK),
                chunk -> chunk.update(DSL.remainderFinder(), CoreIndexPopulationFix::fix));
    }

    private static <T> Dynamic<T> fix(Dynamic<T> tag) {
        Optional<LongStream> rows =
                tag.get(ChunkExtension.CORE_INDEX_NBT_KEY).asLongStreamOpt().result();
        if (rows.isEmpty()) return tag;
        return tag.set(
                ChunkExtension.CORE_INDEX_NBT_KEY,
                tag.createLongList(rows.get().map(row -> row | 3L << 59)));
    }
}
