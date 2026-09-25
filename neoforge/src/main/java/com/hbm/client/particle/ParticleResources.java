// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

public final class ParticleResources {
    private ParticleResources() {}

    public static void initialize() {
        ParticleModels.initModels();
        ContrailVisual.initModels();
        RadiationFogVisual.initModels();
        RocketFlameVisual.initModels();
    }
}
