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
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public final class HbmDataFixers {

    public static final String VERSION_KEY = "hbm_data_version";
    public static final int CURRENT = 14;
    public static final DSL.TypeReference CHUNK = () -> "hbm:chunk";
    public static final DSL.TypeReference ENTITY_CHUNK = () -> "hbm:entity_chunk";
    public static final DSL.TypeReference PLAYER = () -> "hbm:player";
    public static final DSL.TypeReference SEALED_ITEMS = () -> "hbm:sealed_items";
    public static final DSL.TypeReference STRUCTURE = () -> "hbm:structure";
    private static final DataFixer FIXER = build();

    private HbmDataFixers() {}

    private static DataFixer build() {
        DataFixerBuilder builder = new DataFixerBuilder(CURRENT);
        builder.addSchema(0, (version, parent) -> new HbmRootSchema(version));
        Schema v1 = builder.addSchema(1, Schema::new);
        builder.addFixer(new CoreIndexWideningFix(v1));
        Schema v2 = builder.addSchema(2, Schema::new);
        builder.addFixer(new PowerLockInteractionFix(v2));
        Schema v3 = builder.addSchema(3, Schema::new);
        builder.addFixer(new PendingFallerIntroductionFix(v3));
        Schema v4 = builder.addSchema(4, Schema::new);
        builder.addFixer(new GlyphidSpawnerDelayFix(v4));
        Schema v5 = builder.addSchema(5, Schema::new);
        builder.addFixer(new GlyphidSpawnerInitialSpawnFix(v5));
        Schema v6 = builder.addSchema(6, Schema::new);
        builder.addFixer(new TrinketTypeNameFix(v6));
        builder.addFixer(new PadSizeNameFix(v6));
        Schema v7 = builder.addSchema(7, Schema::new);
        builder.addFixer(new CoreIndexPopulationFix(v7));
        Schema v8 = builder.addSchema(8, Schema::new);
        builder.addFixer(new FluidPumpBufferRemovalFix(v8));
        Schema v9 = builder.addSchema(9, Schema::new);
        builder.addFixer(new PileChannelCountFix(v9));
        Schema v10 = builder.addSchema(10, Schema::new);
        builder.addFixer(new RecipeProgressErrorIntroductionFix(v10));
        Schema v11 = builder.addSchema(11, Schema::new);
        builder.addFixer(new GunFluidKeyFix(v11));
        Schema v12 = builder.addSchema(12, Schema::new);
        builder.addFixer(new FalloutJournalIntroductionFix(v12));
        Schema v13 = builder.addSchema(13, Schema::new);
        builder.addFixer(new WeaponModKeyFix(v13));
        Schema v14 = builder.addSchema(14, Schema::new);
        builder.addFixer(new Release14Fix(v14));
        return builder.build().fixer();
    }

    public static CompoundTag upgradeChunk(CompoundTag tag) {
        return upgrade(CHUNK, tag);
    }

    public static CompoundTag upgradeEntityChunk(CompoundTag tag) {
        return upgrade(ENTITY_CHUNK, tag);
    }

    public static CompoundTag upgradePlayer(CompoundTag tag) {
        return upgrade(PLAYER, tag);
    }

    public static CompoundTag upgradeStructure(CompoundTag tag) {
        return upgrade(STRUCTURE, tag);
    }

    public static CompoundTag upgradeSealedItems(CompoundTag tag) {
        return upgrade(SEALED_ITEMS, tag);
    }

    private static CompoundTag upgrade(DSL.TypeReference type, CompoundTag tag) {
        int version = tag.getIntOr(VERSION_KEY, 0);
        if (version >= CURRENT) {
            tag.remove(DataFixClock.GAME_TIME);
            return tag;
        }
        var fixed =
                (CompoundTag)
                        FIXER.update(type, new Dynamic<>(NbtOps.INSTANCE, tag), version, CURRENT)
                                .getValue();
        fixed.remove(DataFixClock.GAME_TIME);
        fixed.putInt(VERSION_KEY, CURRENT);
        return fixed;
    }

    public static void stamp(CompoundTag chunkTag) {
        chunkTag.putInt(VERSION_KEY, CURRENT);
    }

    public static boolean chunkCurrent(CompoundTag chunkTag) {
        return chunkTag.getIntOr(VERSION_KEY, 0) >= CURRENT;
    }
}
