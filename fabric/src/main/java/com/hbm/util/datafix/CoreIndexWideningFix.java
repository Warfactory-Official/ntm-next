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

final class CoreIndexWideningFix extends DataFix {

    static final String LEGACY_KEY = "hbm_core_index_22";

    CoreIndexWideningFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "CoreIndexWideningFix",
                getInputSchema().getType(HbmDataFixers.CHUNK),
                chunk -> chunk.update(DSL.remainderFinder(), CoreIndexWideningFix::fix));
    }

    private static <T> Dynamic<T> fix(Dynamic<T> tag) {
        Optional<LongStream> legacy = tag.get(LEGACY_KEY).asLongStreamOpt().result();
        if (legacy.isEmpty()) return tag;
        return tag.remove(LEGACY_KEY)
                .set(
                        ChunkExtension.CORE_INDEX_NBT_KEY,
                        tag.createLongList(legacy.get().map(CoreIndexWideningFix::widen)));
    }

    static long widen(long entry) {
        long key = entry >>> 25 & 0xFFFFF;
        long dx = signExtend((int) entry & 0x3F, 6);
        long dy = signExtend((int) (entry >>> 6) & 0x1FFF, 13);
        long dz = signExtend((int) (entry >>> 19) & 0x3F, 6);
        return key << 39 | (dz & 0x1FFF) << 26 | (dy & 0x1FFF) << 13 | dx & 0x1FFF;
    }

    private static int signExtend(int value, int bits) {
        int shift = Integer.SIZE - bits;
        return (value << shift) >> shift;
    }
}
