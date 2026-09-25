// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.ModBlockEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

public final class DoorVisuals {
    private DoorVisuals() {}

    public static void register() {
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.NTM_DOOR.get())
                .factory(DoorVisual::new)
                .apply();
    }
}
