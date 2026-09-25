// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.lib.Library;
import com.hbm.platform.Services;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class EnergyCaps {

    public static BlockCapability<IEnergyHandlerMK2, Direction> PROVIDER;
    public static BlockCapability<IEnergyHandlerMK2, Direction> RECEIVER;

    private EnergyCaps() {}

    public static void register() {
        PROVIDER =
                Services.CAPS.createToken(
                        Library.id("energy_provider"), IEnergyHandlerMK2.class, Direction.class);
        RECEIVER =
                Services.CAPS.createToken(
                        Library.id("energy_receiver"), IEnergyHandlerMK2.class, Direction.class);
    }
}
