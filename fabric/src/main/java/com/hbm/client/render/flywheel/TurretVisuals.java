// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.ModBlockEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

public final class TurretVisuals {
    private TurretVisuals() {}

    public static void register() {
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_CHEKHOV.get())
                .factory(TurretChekhovVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_FRIENDLY.get())
                .factory(TurretFriendlyVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_JEREMY.get())
                .factory(TurretJeremyVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_RICHARD.get())
                .factory(TurretRichardVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_HOWARD.get())
                .factory(TurretHowardVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_FRITZ.get())
                .factory(TurretFritzVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_ARTY.get())
                .factory(TurretArtyVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_HIMARS.get())
                .factory(TurretHIMARSVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_SENTRY.get())
                .factory(TurretSentryVisual::new)
                .apply();
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TURRET_SENTRY_DAMAGED.get())
                .factory(TurretSentryVisual::new)
                .apply();
    }
}
