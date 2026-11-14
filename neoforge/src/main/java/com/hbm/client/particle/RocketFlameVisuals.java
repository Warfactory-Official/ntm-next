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

package com.hbm.client.particle;

import com.hbm.particle.RocketFlameColor;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class RocketFlameVisuals {

    private static final List<RocketFlameEffect> ACTIVE = new ArrayList<>();

    private RocketFlameVisuals() {}

    public static boolean trySpawnInstanced(
            Level level,
            double x,
            double y,
            double z,
            float scale,
            double mX,
            double mY,
            double mZ,
            int maxAge,
            RocketFlameColor color) {
        if (!VisualizationManager.supportsVisualization(level)) return false;
        RocketFlameEffect effect =
                new RocketFlameEffect(level, x, y, z, scale, mX, mY, mZ, maxAge, color);
        VisualizationHelper.queueAdd(effect);
        ACTIVE.add(effect);
        return true;
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Level current = mc.level;
        boolean advance =
                current != null && !mc.isPaused() && current.tickRateManager().runsNormally();
        Iterator<RocketFlameEffect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            RocketFlameEffect e = it.next();
            if (e.level() != current) {
                it.remove();
                continue;
            }
            if (advance) e.tick();
            if (e.expired()) {
                VisualizationHelper.queueRemove(e);
                it.remove();
            }
        }
    }
}
