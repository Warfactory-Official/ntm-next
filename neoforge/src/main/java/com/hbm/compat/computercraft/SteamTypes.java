// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class SteamTypes {

    private SteamTypes() {}

    public static int toInt(@Nullable Fluid type) {
        if (type == NTMFluids.HOTSTEAM) return 1;
        if (type == NTMFluids.SUPERHOTSTEAM) return 2;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    public static Fluid fromInt(int level) {
        return switch (level) {
            case 1 -> NTMFluids.HOTSTEAM;
            case 2 -> NTMFluids.SUPERHOTSTEAM;
            case 3 -> NTMFluids.ULTRAHOTSTEAM;
            default -> NTMFluids.STEAM;
        };
    }
}
