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

package com.hbm.saveddata.satellites;

import com.hbm.entity.missile.Missile;
import com.hbm.entity.missile.MissileAntiBallisticEntity;
import com.hbm.entity.projectile.ArtilleryRocketEntity;
import com.hbm.entity.projectile.ArtilleryShellEntity;
import com.hbm.entity.projectile.MeteorEntity;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class RadarSatellite extends Satellite {

    public static final MapCodec<RadarSatellite> CODEC = MapCodec.unit(RadarSatellite::new);
    private static final long SCAN_RANGE_SQ = 1000L * 1000L;
    private static final ClassValue<String> CLASS_NAMES =
            new ClassValue<>() {
                @Override
                protected String computeValue(Class<?> type) {
                    if (type == MeteorEntity.class) return "entitymeteor";
                    if (type == MissileAntiBallisticEntity.class)
                        return "entitymissileantiballistic";
                    if (type == ArtilleryShellEntity.class) return "entityartilleryshell";
                    if (type == ArtilleryRocketEntity.class) return "entityartilleryrocket";
                    if (type == ServerPlayer.class) return "entityplayermp";
                    return type.getSimpleName().toLowerCase(Locale.ROOT);
                }
            };

    private static String sourceName(Entity entity) {
        return entity instanceof Missile missile
                ? missile.sourceName().toLowerCase(Locale.ROOT)
                : CLASS_NAMES.get(entity.getClass());
    }

    private final List<Entity> surveyed = new ArrayList<>();
    private final List<Entity> listed = new ArrayList<>();

    @Override
    public SatelliteType kind() {
        return SatelliteType.RADAR;
    }

    public List<Entity> surveyed() {
        return Collections.unmodifiableList(surveyed);
    }

    public List<Entity> listed() {
        return Collections.unmodifiableList(listed);
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        switch (tokens[0]) {
            case "survey" -> survey(level);
            case "filter" -> {
                if (tokens.length == 2) filter(tokens[1].toLowerCase(Locale.ROOT));
            }
            case "count" -> reply(Integer.toString(listed.size()));
            case "gettargetid" -> {
                if (tokens.length == 2) {
                    var entity = pick(tokens[1]);
                    reply(entity == null ? "" : Integer.toString(entity.getId()));
                }
            }
            case "getposition" -> {
                if (tokens.length == 2) {
                    var entity = pick(tokens[1]);
                    reply(
                            entity == null
                                    ? ""
                                    : Mth.floor(entity.getX())
                                            + ";"
                                            + Mth.floor(entity.getY())
                                            + ";"
                                            + Mth.floor(entity.getZ()));
                }
            }
            case "getname" -> {
                if (tokens.length == 2) {
                    var entity = pick(tokens[1]);
                    reply(entity == null ? "" : sourceName(entity));
                }
            }
            default -> {}
        }
    }

    private void survey(ServerLevel level) {
        surveyed.clear();
        for (var entity :
                ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$radarTargets()) {
            if (entity.isSpectator()) continue;
            long dx = (long) Mth.floor(entity.getX()) - targetX();
            long dz = (long) Mth.floor(entity.getZ()) - targetZ();
            if (dx * dx + dz * dz <= SCAN_RANGE_SQ) surveyed.add(entity);
        }
        listed.clear();
        listed.addAll(surveyed);
    }

    private void filter(String name) {
        listed.clear();
        for (var entity : surveyed) {
            if (!entity.isRemoved() && sourceName(entity).contains(name)) listed.add(entity);
        }
    }

    private @Nullable Entity pick(String token) {
        int index = index(token, listed.size());
        if (index < 0) return null;
        var entity = listed.get(index);
        return entity.isRemoved() ? null : entity;
    }
}
