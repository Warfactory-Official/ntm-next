// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleDebris;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.level.Level;

public final class DebrisVisuals {

    private static final List<DebrisEffect> ACTIVE = new ArrayList<>();

    private DebrisVisuals() {}

    public static boolean tryInstance(Level level, ParticleDebris particle) {
        if (!VisualizationManager.supportsVisualization(level)) return false;
        DebrisEffect effect = new DebrisEffect(level, particle);
        VisualizationHelper.queueAdd(effect);
        ACTIVE.add(effect);
        return true;
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        Iterator<DebrisEffect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            DebrisEffect e = it.next();
            if (e.expired()) {
                VisualizationHelper.queueRemove(e);
                it.remove();
            }
        }
    }
}
