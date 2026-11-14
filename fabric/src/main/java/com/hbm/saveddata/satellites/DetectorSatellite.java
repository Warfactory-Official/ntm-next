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

public final class DetectorSatellite extends Satellite {

    public static final MapCodec<DetectorSatellite> CODEC = MapCodec.unit(DetectorSatellite::new);

    private final List<RadiationBursts.Burst> surveyed = new ArrayList<>();

    @Override
    public SatelliteType kind() {
        return SatelliteType.DETECTOR;
    }

    public List<RadiationBursts.Burst> surveyed() {
        return Collections.unmodifiableList(surveyed);
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        switch (tokens[0]) {
            case "survey" -> {
                surveyed.clear();
                RadiationBursts.collect(level, surveyed);
            }
            case "count" -> reply(Integer.toString(surveyed.size()));
            case "gettype" -> {
                if (tokens.length == 2) {
                    int index = index(tokens[1], surveyed.size());
                    reply(index < 0 ? "" : surveyed.get(index).intensity().name());
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
}
