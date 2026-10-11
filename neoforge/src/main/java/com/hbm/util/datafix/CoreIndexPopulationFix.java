// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.datafix;

import com.hbm.interfaces.injected.IChunkExtension;
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
                tag.get(IChunkExtension.CORE_INDEX_NBT_KEY).asLongStreamOpt().result();
        if (rows.isEmpty()) return tag;
        return tag.set(
                IChunkExtension.CORE_INDEX_NBT_KEY,
                tag.createLongList(rows.get().map(row -> row | 3L << 59)));
    }
}
