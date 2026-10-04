// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.client.ClientPlayerAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public final class KeyHints {
    private KeyHints() {}

    public static Component key(String mapping, String defaultKey) {
        return (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
                ? ClientPlayerAccess.keyLabel(mapping)
                : Component.translatable(defaultKey);
    }
}
