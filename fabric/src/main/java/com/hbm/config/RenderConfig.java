// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class RenderConfig {

    public static volatile boolean skyboxes = true;
    public static volatile boolean coolingTowerParticles = true;
    public static volatile boolean cableHang = true;
    public static volatile boolean simpleRebar = false;

    private RenderConfig() {}

    public static void loadFrom(ConfigStore c) {
        skyboxes = c.get(ConfigSchema.ENABLE_SKYBOXES);
        coolingTowerParticles = c.get(ConfigSchema.COOLING_TOWER_PARTICLES);
        cableHang = c.get(ConfigSchema.RENDER_CABLE_HANG);
        simpleRebar = c.get(ConfigSchema.RENDER_REBAR_SIMPLE);
    }
}
