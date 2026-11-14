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

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public final class RayScanSatellite extends Satellite {

    public static final MapCodec<RayScanSatellite> CODEC = MapCodec.unit(RayScanSatellite::new);
    private static final long SCAN_RANGE_SQ = 250L * 250L;

    private final List<SatelliteRayEvents.RayEvent> surveyed = new ArrayList<>();

    @Override
    public SatelliteType kind() {
        return SatelliteType.RAY_SCAN;
    }

    public List<SatelliteRayEvents.RayEvent> surveyed() {
        return Collections.unmodifiableList(surveyed);
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        switch (tokens[0]) {
            case "survey" -> survey(level);
            case "count" -> reply(Integer.toString(surveyed.size()));
            case "getinfo" -> {
                if (tokens.length == 2) {
                    int index = index(tokens[1], surveyed.size());
                    reply(index < 0 ? "" : surveyed.get(index).info());
                }
            }
            case "getposition" -> {
                if (tokens.length == 2) {
                    int index = index(tokens[1], surveyed.size());
                    reply(index < 0 ? "" : surveyed.get(index).x() + ";" + surveyed.get(index).z());
                }
            }
            default -> {}
        }
    }

    private void survey(ServerLevel level) {
        surveyed.clear();
        for (var event : SatelliteRayEvents.events(level).values()) {
            long dx = (long) event.x() - targetX();
            long dz = (long) event.z() - targetZ();
            if (dx * dx + dz * dz <= SCAN_RANGE_SQ) surveyed.add(event);
        }
    }
}
