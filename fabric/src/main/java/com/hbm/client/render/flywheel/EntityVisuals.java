// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

public final class EntityVisuals {

    private EntityVisuals() {}

    public static void register() {
        MissileVisuals.register();
        OrdnanceVisuals.register();
        MobVisuals.register();
        EffectVisuals.register();
        VehicleVisuals.register();
    }
}
