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

public final class RadFogVisuals {

    private static final List<RadiationFogEffect> ACTIVE = new ArrayList<>();

    private RadFogVisuals() {}

    public static boolean trySpawnInstanced(Level level, double x, double y, double z) {
        if (!VisualizationManager.supportsVisualization(level)) return false;
        RadiationFogEffect effect = new RadiationFogEffect(level, x, y, z);
        VisualizationHelper.queueAdd(effect);
        ACTIVE.add(effect);
        return true;
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        Level current = Minecraft.getInstance().level;
        Iterator<RadiationFogEffect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            RadiationFogEffect e = it.next();

            if (e.level() != current) {
                it.remove();
                continue;
            }
            e.age++;
            if (e.expired()) {
                VisualizationHelper.queueRemove(e);
                it.remove();
            }
        }
    }
}
