// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.lib.Library;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;

public final class EnergyCaps {

    public static BlockApiLookup<IEnergyHandlerMK2, Direction> PROVIDER;
    public static BlockApiLookup<IEnergyHandlerMK2, Direction> RECEIVER;

    private EnergyCaps() {}

    public static void register() {
        PROVIDER =
                BlockApiLookup.get(
                        Library.id("energy_provider"), IEnergyHandlerMK2.class, Direction.class);
        RECEIVER =
                BlockApiLookup.get(
                        Library.id("energy_receiver"), IEnergyHandlerMK2.class, Direction.class);
    }
}
