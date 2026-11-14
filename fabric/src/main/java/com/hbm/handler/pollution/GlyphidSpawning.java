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

package com.hbm.handler.pollution;

import com.hbm.data.MobData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.GlyphidDiggerEntity;
import com.hbm.entity.mob.glyphid.GlyphidScoutEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class GlyphidSpawning {

    private static @Nullable Vec3 guidance;

    private GlyphidSpawning() {}

    public static @Nullable Vec3 guidance() {
        return guidance;
    }

    public static void onSleepAttempt(BlockPos bed) {
        if (MobData.rampantGlyphidGuidance()) guidance = Vec3.atLowerCornerOf(bed);
    }

    public static void populate(ServerLevel level, BlockPos pos) {
        if (!MobData.rampantNaturalScoutSpawn()
                || level.dimension() != Level.OVERWORLD
                || !level.canSeeSky(pos)) return;
        var random = level.getRandom();
        if (random.nextInt(MobData.RAMPANT_SCOUT_SPAWN_CHANCE.get()) != 0) return;
        if (PollutionHandler.read(level, pos, PollutionType.SOOT)
                < MobData.RAMPANT_SCOUT_SPAWN_THRESH.get()) return;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        var scout = new GlyphidScoutEntity(ModEntities.GLYPHID_SCOUT.get(), level);
        scout.snapTo(x, y, z, random.nextFloat() * 360F, 0F);
        if (!scout.isValidLightLevel()) return;
        var digger = new GlyphidDiggerEntity(ModEntities.GLYPHID_DIGGER.get(), level);
        scout.snapTo(x, y, z, random.nextFloat() * 360F, 0F);
        digger.snapTo(x, y, z, random.nextFloat() * 360F, 0F);
        if (fits(level, scout)) level.addFreshEntity(scout);
        if (fits(level, digger)) level.addFreshEntity(digger);
    }

    private static boolean fits(ServerLevel level, Mob mob) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.isUnobstructed(mob)
                && level.noCollision(mob)
                && !level.containsAnyLiquid(mob.getBoundingBox());
    }
}
