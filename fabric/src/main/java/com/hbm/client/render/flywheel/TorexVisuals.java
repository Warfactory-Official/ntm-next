// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.ModEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;

public final class TorexVisuals {

    private TorexVisuals() {}

    public static void register() {
        SimpleEntityVisualizer.builder(ModEntities.NUKE_TOREX.get())
                .factory(TorexVisual::new)
                .apply();
    }
}
