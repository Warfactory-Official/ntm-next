// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.fabricmc.loader.api.FabricLoader;

final class FabricTrEnergy {

    static final boolean AVAILABLE = FabricLoader.getInstance().isModLoaded("team_reborn_energy");

    private FabricTrEnergy() {}
}
