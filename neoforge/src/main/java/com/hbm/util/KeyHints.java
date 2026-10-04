// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.client.ClientPlayerAccess;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

public final class KeyHints {
    private KeyHints() {}

    public static Component key(String mapping, String defaultKey) {
        return (FMLLoader.getCurrent().getDist() == Dist.CLIENT)
                ? ClientPlayerAccess.keyLabel(mapping)
                : Component.translatable(defaultKey);
    }
}
