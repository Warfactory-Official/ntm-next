// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.stats;

import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import net.minecraft.resources.Identifier;

public final class ModStats {

    public static RegistryHandle<Identifier> LEGENDARY;

    public static RegistryHandle<Identifier> MINES;

    public static RegistryHandle<Identifier> BULLETS;

    private ModStats() {}

    public static void register(IRegistrar registrar) {

        LEGENDARY = registrar.registerCustomStat("legendary");
        MINES = registrar.registerCustomStat("mines");
        BULLETS = registrar.registerCustomStat("bullets");
    }
}
