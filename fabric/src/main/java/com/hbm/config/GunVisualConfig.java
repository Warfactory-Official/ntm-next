// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class GunVisualConfig {

    public static volatile boolean modelFov = false;
    public static volatile boolean visualRecoil = true;
    public static volatile double animationSpeed = 1.0D;

    public static volatile boolean legacyAnimations = false;

    private GunVisualConfig() {}

    public static void loadFrom(ConfigStore c) {
        modelFov = c.get(ConfigSchema.GUN_MODEL_FOV);
        visualRecoil = c.get(ConfigSchema.GUN_VISUAL_RECOIL);
        animationSpeed = c.get(ConfigSchema.GUN_ANIMATION_SPEED);
        legacyAnimations = c.get(ConfigSchema.GUN_ANIMS_LEGACY);
    }
}
