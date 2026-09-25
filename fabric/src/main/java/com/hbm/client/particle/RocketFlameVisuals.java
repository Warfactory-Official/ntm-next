// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

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
            int maxAge) {
        if (!VisualizationManager.supportsVisualization(level)) return false;
        RocketFlameEffect effect = new RocketFlameEffect(level, x, y, z, scale, mX, mY, mZ, maxAge);
        VisualizationHelper.queueAdd(effect);
        ACTIVE.add(effect);
        return true;
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        Level current = Minecraft.getInstance().level;
        Iterator<RocketFlameEffect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            RocketFlameEffect e = it.next();

            if (e.level() != current) {
                it.remove();
                continue;
            }
            e.tick();
            if (e.expired()) {
                VisualizationHelper.queueRemove(e);
                it.remove();
            }
        }
    }
}
