// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.lib.Library;
import com.hbm.platform.Services;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class FluidCaps {

    public static BlockCapability<IFluidHandlerMK2, FluidFace> PROVIDER;
    public static BlockCapability<IFluidHandlerMK2, FluidFace> RECEIVER;

    private FluidCaps() {}

    public static void register() {
        PROVIDER =
                Services.CAPS.createToken(
                        Library.id("fluid_provider"), IFluidHandlerMK2.class, FluidFace.class);
        RECEIVER =
                Services.CAPS.createToken(
                        Library.id("fluid_receiver"), IFluidHandlerMK2.class, FluidFace.class);
    }
}
