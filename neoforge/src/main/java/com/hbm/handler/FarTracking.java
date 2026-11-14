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

package com.hbm.handler;

import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class FarTracking {
    private FarTracking() {}

    public static int range(ServerPlayer player) {
        return Math.min(
                ((com.hbm.interfaces.injected.FarViewer) (player.connection)).hbm$farView(),
                ((RuntimeConfig) HbmConfig.RUNTIME).farTrackingRange());
    }

    public static boolean anyFarViewer(Entity entity) {
        for (ServerPlayer player : ((ServerLevel) entity.level()).players()) {
            double range = range(player),
                    dx = entity.getX() - player.getX(),
                    dz = entity.getZ() - player.getZ();
            if (dx * dx + dz * dz < range * range) return true;
        }
        return false;
    }
}
