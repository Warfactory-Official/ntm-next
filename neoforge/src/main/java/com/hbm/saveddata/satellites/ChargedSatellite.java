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

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public abstract class ChargedSatellite extends Satellite {

    private final int charge;
    public long firedAt;

    protected ChargedSatellite(int charge, long firedAt) {
        this.charge = charge;
        this.firedAt = firedAt;
    }

    protected final boolean charged(ServerLevel level) {
        return firedAt + charge < level.getGameTime();
    }

    protected final boolean discharge(ServerLevel level) {
        if (!charged(level)) return false;
        firedAt = level.getGameTime();
        changed();
        return true;
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        if (tokens[0].equals("canfire")) reply(charged(level) ? "TRUE" : "FALSE");
    }

    @Override
    public List<Component> info(ServerLevel level) {
        long left = firedAt + charge - level.getGameTime();
        if (left < 0) return List.of(kind().stationName(), READY);
        return List.of(
                kind().stationName(),
                Component.translatable(
                        "satellite.cooldown", Component.translatable("unit.seconds", left / 20)));
    }
}
