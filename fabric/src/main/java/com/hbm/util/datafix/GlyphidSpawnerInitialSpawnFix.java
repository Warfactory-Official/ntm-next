// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.datafix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

final class GlyphidSpawnerInitialSpawnFix extends DataFix {
    GlyphidSpawnerInitialSpawnFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "GlyphidSpawnerInitialSpawnFix",
                getInputSchema().getType(HbmDataFixers.CHUNK),
                chunk -> chunk.update(DSL.remainderFinder(), GlyphidSpawnerInitialSpawnFix::fix));
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
                                        .map(GlyphidSpawnerInitialSpawnFix::fixBlockEntity)));
    }

    private static Dynamic<?> fixBlockEntity(Dynamic<?> tag) {
        if (!tag.get("id").asString("").equals("hbm:glyphid_spawner")) return tag;
        return tag.remove("initialSpawn")
                .set("initial_spawn", tag.createBoolean(true))
                .set("spawn_delay", tag.createInt(0));
    }
}
