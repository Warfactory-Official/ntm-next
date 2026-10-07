// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.datafix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

final class PadSizeNameFix extends DataFix {
    private static final String[] NAMES = {"ANY", "NONE", "SIZE_10", "SIZE_15", "SIZE_20"};

    PadSizeNameFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "PadSizeNameFix",
                getInputSchema().getType(HbmDataFixers.CHUNK),
                chunk -> chunk.update(DSL.remainderFinder(), PadSizeNameFix::fix));
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
                                        .map(PadSizeNameFix::fixBlockEntity)));
    }

    private static Dynamic<?> fixBlockEntity(Dynamic<?> tag) {
        if (!tag.get("id").asString("").equals("hbm:large_launch_table")) return tag;
        Optional<Number> raw = tag.get("padSize").asNumber().result();
        return raw.isEmpty()
                ? tag
                : tag.set(
                        "padSize",
                        tag.createString(
                                NAMES[Math.clamp(raw.get().intValue(), 0, NAMES.length - 1)]));
    }
}
