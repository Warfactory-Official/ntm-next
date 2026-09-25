// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.datafix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public final class HbmDataFixers {

    public static final String VERSION_KEY = "hbm_data_version";

    public static final int CURRENT = 1;
    static final DSL.TypeReference CHUNK = () -> "hbm:chunk";
    private static final DataFixer FIXER = build();

    private HbmDataFixers() {}

    private static DataFixer build() {
        DataFixerBuilder builder = new DataFixerBuilder(CURRENT);
        builder.addSchema(0, (version, parent) -> new HbmRootSchema(version));
        Schema v1 = builder.addSchema(1, Schema::new);
        builder.addFixer(new CoreIndexWideningFix(v1));
        return builder.build().fixer();
    }

    public static CompoundTag upgradeChunk(CompoundTag tag) {
        int version = tag.getIntOr(VERSION_KEY, 0);
        if (version >= CURRENT) return tag;
        CompoundTag fixed =
                (CompoundTag)
                        FIXER.update(CHUNK, new Dynamic<>(NbtOps.INSTANCE, tag), version, CURRENT)
                                .getValue();
        fixed.putInt(VERSION_KEY, CURRENT);
        return fixed;
    }

    public static void stamp(CompoundTag chunkTag) {
        chunkTag.putInt(VERSION_KEY, CURRENT);
    }
}
