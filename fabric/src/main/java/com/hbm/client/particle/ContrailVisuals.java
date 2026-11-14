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

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class ContrailVisuals {

    private static final List<ContrailEffect> ACTIVE = new ArrayList<>();

    private ContrailVisuals() {}

    public static boolean trySpawnInstanced(
            Level level, double x, double y, double z, float r, float g, float b, float scale) {
        if (!VisualizationManager.supportsVisualization(level)) return false;
        ContrailEffect effect =
                new ContrailEffect(
                        level, x, y, z, r, g, b, scale, 100 + level.getRandom().nextInt(40));
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
        Iterator<ContrailEffect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            ContrailEffect e = it.next();
            if (e.level() != current) {
                it.remove();
                continue;
            }
            if (advance) e.age++;
            if (e.expired()) {
                VisualizationHelper.queueRemove(e);
                it.remove();
            }
        }
    }
}
