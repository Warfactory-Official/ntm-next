// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class InteractionConfig {

    public static volatile boolean crateOpensHeld = true;
    public static volatile boolean recipeViewerHidesSecrets = true;

    private InteractionConfig() {}

    public static void loadFrom(ConfigStore c) {
        crateOpensHeld = c.get(ConfigSchema.CRATE_OPEN_HELD);
        recipeViewerHidesSecrets = c.get(ConfigSchema.JEI_HIDE_SECRETS);
    }
}
