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

package com.hbm.particle.helper;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.EffectExplosionLargeLayout;
import com.hbm.packet.layout.EffectExplosionSmallLayout;
import com.hbm.packet.layout.EffectExplosionStandardLayout;
import com.hbm.packet.layout.RBMKMushLayout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ExplosionCreator {

    private ExplosionCreator() {}

    public static void composeEffectSmall(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel server)) return;
        PacketWire.sendToAllAround(
                EffectExplosionSmallLayout.encode(x, y, z), new TargetPoint(server, x, y, z, 300));
    }

    public static void composeEffectStandard(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel server)) return;
        PacketWire.sendToAllAround(
                EffectExplosionStandardLayout.encode(x, y, z),
                new TargetPoint(server, x, y, z, 300));
    }

    public static void composeEffectLarge(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel server)) return;
        PacketWire.sendToAllAround(
                EffectExplosionLargeLayout.encode(x, y, z), new TargetPoint(server, x, y, z, 350));
    }

    public static void composeEffectRBMKMush(
            Level level, double x, double y, double z, float scale) {
        if (!(level instanceof ServerLevel server)) return;
        PacketWire.sendToAllAround(
                RBMKMushLayout.encode(x, y, z, scale), new TargetPoint(server, x, y, z, 250));
    }
}
