// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class RadiationConfig {

    public static int hazardRate = 5;

    public static int radTickRate = 1;

    public static boolean enableDebugMode = false;

    private RadiationConfig() {}

    public static void loadFrom(ConfigStore c) {
        enableDebugMode = c.get(ConfigSchema.ENABLE_DEBUG_MODE);
        radTickRate = c.get(ConfigSchema.RAD_TICK_RATE);
        hazardRate = c.get(ConfigSchema.HAZARD_RATE);
    }
}
