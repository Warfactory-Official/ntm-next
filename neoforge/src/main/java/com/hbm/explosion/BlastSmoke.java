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

package com.hbm.explosion;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.SmokeBurstLayout;
import com.hbm.packet.layout.SmokeCloudLayout;
import com.hbm.packet.layout.SmokeFoamLayout;
import com.hbm.packet.layout.SmokeRadialLayout;
import com.hbm.packet.layout.SmokeShockLayout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class BlastSmoke {

    private static final double AUDIENCE = 250D;
    private static final double FLAME_AUDIENCE = 150D;

    private BlastSmoke() {}

    private static TargetPoint around(Level level, double x, double y, double z, double radius) {
        return new TargetPoint((ServerLevel) level, x, y, z, radius);
    }

    public static void shock(
            Level level, double x, double y, double z, int count, double strength) {
        var audience = around(level, x, y, z, AUDIENCE);
        PacketWire.sendToAllAround(
                SmokeShockLayout.encode(x, y + 0.5D, z, count, strength), audience);
    }

    public static void flameRing(
            Level level, double x, double y, double z, int count, double strength) {
        var audience = around(level, x, y, z, FLAME_AUDIENCE);
        PacketWire.sendToAllAround(SmokeBurstLayout.encode(x, y, z, count, strength), audience);
    }

    public static void radial(Level level, double x, double y, double z, int count) {
        var audience = around(level, x, y, z, AUDIENCE);
        PacketWire.sendToAllAround(SmokeRadialLayout.encode(x, y, z, count), audience);
    }

    public static void foam(Level level, double x, double y, double z, int count) {
        var audience = around(level, x, y, z, AUDIENCE);
        PacketWire.sendToAllAround(SmokeFoamLayout.encode(x, y, z, count), audience);
    }

    public static void cloud(Level level, double x, double y, double z, int count) {
        var audience = around(level, x, y, z, AUDIENCE);
        PacketWire.sendToAllAround(SmokeCloudLayout.encode(x, y, z, count), audience);
    }
}
