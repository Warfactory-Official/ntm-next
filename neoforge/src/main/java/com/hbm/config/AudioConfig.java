// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class AudioConfig {
    public static volatile boolean originalStereoExplosions = false;

    private AudioConfig() {}

    public static void loadFrom(ConfigStore c) {
        originalStereoExplosions = c.get(ConfigSchema.ORIGINAL_STEREO_EXPLOSIONS);
    }
}
