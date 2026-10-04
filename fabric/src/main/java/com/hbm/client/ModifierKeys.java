// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class ModifierKeys {
    private ModifierKeys() {}

    public static boolean detailsHeld() {
        return (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
                && Keybinds.DETAILS.isDown();
    }
}
