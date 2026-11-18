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

import com.hbm.config.BombConfig;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;

final class ExplosionClockFix extends DataFix {
    ExplosionClockFix(Schema schema) {
        super(schema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "ExplosionClockFix",
                getInputSchema().getType(HbmDataFixers.ENTITY_CHUNK),
                typed -> typed.update(DSL.remainderFinder(), ExplosionClockFix::fix));
    }

    private static <T> Dynamic<T> fix(Dynamic<T> input) {
        var chunk = ((CompoundTag) input.convert(NbtOps.INSTANCE).getValue()).copy();
        long now = System.currentTimeMillis();
        long lifespan = BombConfig.limitExplosionLifespan;
        chunk.put("Entities", entities(chunk.getListOrEmpty("Entities"), chunk, now, lifespan));
        return new Dynamic<>(NbtOps.INSTANCE, chunk).convert(input.getOps());
    }

    private static ListTag entities(ListTag source, CompoundTag context, long now, long lifespan) {
        var result = new ListTag();
        for (var entity : source.compoundStream().toList()) {
            if (entity.getStringOr("id", "").equals("hbm:nuke_mk3")
                    && !entity.contains("savedGameTick")) {
                if (lifespan > 0 && now - entity.getLongOr("milliTime", 0L) > lifespan * 1000L)
                    continue;
                entity.putLong(
                        "savedGameTick", context.getLong(DataFixClock.GAME_TIME).orElseThrow());
                entity.remove("milliTime");
            }
            if (entity.contains("Passengers"))
                entity.put(
                        "Passengers",
                        entities(entity.getListOrEmpty("Passengers"), context, now, lifespan));
            result.add(entity);
        }
        return result;
    }
}
