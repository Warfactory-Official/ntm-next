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
