// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.stats;

import com.hbm.registration.FabricRegistrar;
import com.hbm.registration.RegistryHandle;
import net.minecraft.resources.Identifier;

public final class ModStats {
    public static RegistryHandle<Identifier> LEGENDARY;
    public static RegistryHandle<Identifier> MINES;
    public static RegistryHandle<Identifier> BULLETS;

    private ModStats() {}

    public static void register() {
        LEGENDARY = FabricRegistrar.registerCustomStat("legendary");
        MINES = FabricRegistrar.registerCustomStat("mines");
        BULLETS = FabricRegistrar.registerCustomStat("bullets");
    }
}
