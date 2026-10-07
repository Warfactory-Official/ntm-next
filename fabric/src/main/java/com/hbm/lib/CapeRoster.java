// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

import com.hbm.registration.FabricRegistrar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class CapeRoster {

    private static final Map<UUID, Identifier> ADDON = new HashMap<>();

    private CapeRoster() {}

    public static synchronized void addon(UUID player, Identifier texture) {
        FabricRegistrar.assertOpen("CapeRoster.addon");
        if (ADDON.putIfAbsent(player, texture) != null)
            throw new IllegalStateException(player + " already has a cape");
    }

    public static @Nullable Identifier addon(UUID player) {
        return ADDON.get(player);
    }
}
