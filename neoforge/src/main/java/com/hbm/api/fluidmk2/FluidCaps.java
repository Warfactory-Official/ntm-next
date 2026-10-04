// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.lib.Library;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class FluidCaps {

    public static BlockCapability<IFluidHandlerMK2, FluidFace> PROVIDER;
    public static BlockCapability<IFluidHandlerMK2, FluidFace> RECEIVER;

    private FluidCaps() {}

    public static void register() {
        PROVIDER =
                BlockCapability.create(
                        Library.id("fluid_provider"), IFluidHandlerMK2.class, FluidFace.class);
        RECEIVER =
                BlockCapability.create(
                        Library.id("fluid_receiver"), IFluidHandlerMK2.class, FluidFace.class);
    }
}
